package yam.salmon.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.ink.InkPaintAccumulator;
import yam.salmon.ink.InkPaintability;
import yam.salmon.ink.InkPaintingService;
import yam.salmon.ink.InkStorage;
import yam.salmon.ink.InkTeam;
import yam.salmon.ink.MultiSurfacePaintResult;
import yam.salmon.ink.PlayerInkState;
import yam.salmon.network.InkSyncManager;
import yam.salmon.team.TeamManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * イカが自チームインクから出たときに、実際の床インクを移動方向へ短く延長するサービス。
 *
 * <p>{@link PlayerInkState#CROUCHING_ON_OWN_INK} のプレイヤーが自チームインクの
 * 境界を越えた（前tickは自インク上・現在tickはインク外）ことをサーバー側で検出し、
 * 「最後に自チームインク上だった位置」から「イカの移動方向」へ短い距離だけ
 * 既存の床インク塗装パイプライン（{@link InkPaintingService#paintInto}）で
 * 追加塗装する。</p>
 *
 * <p>クライアント側エフェクトは一切使わない。更新はサーバー権威の
 * {@code InkStorage} / {@code InkFaceData} への通常の書き込みであり、
 * 既存の {@link InkSyncManager#commitAccumulator} 経由で
 * {@code InkFaceUpdatePayload} により全クライアントへ同期されるため、
 * 通常の弾塗装と同じデータ構造・同じRendererで表示される。</p>
 *
 * <p>塗装は境界越えの瞬間に1回だけ行い、クールダウンと延長長の上限により
 * 移動し続けてもインクが無限に伸び続けることはない。</p>
 */
public final class SquidInkStretchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".state");

    private static final SquidInkStretchService INSTANCE = new SquidInkStretchService();

    /** 1回の境界越えで延長する最大長さ（ブロック）。 */
    public static final double MAX_STRETCH_LENGTH = 1.0;

    /** 境界越え検出のクールダウン（tick）。境界付近での状態フリッカーによる連続塗装を防ぐ。 */
    public static final int STRETCH_COOLDOWN_TICKS = 20;

    /** 延長塗装のブラシ半径（ブロック）。足元塗装よりやや小さめ。 */
    public static final double STRETCH_PAINT_RADIUS = 0.30;

    /** 延長方向へのサンプル点間隔（ブロック）。 */
    private static final double SAMPLE_SPACING = 0.30;

    /** 延長を実行する最小水平移動距離（ブロック）。これ未満は伸ばさない。 */
    private static final double MIN_MOVE_DISTANCE = 0.05;

    /** プレイヤーUUID → 最後に自チームインク上だった足元位置。 */
    private final Map<UUID, Vec3> lastOnInkFootPos = new ConcurrentHashMap<>();

    /** プレイヤーUUID → 最後に延長塗装したtick（レベルゲームタイム）。 */
    private final Map<UUID, Long> lastStretchTick = new ConcurrentHashMap<>();

    private SquidInkStretchService() {}

    public static SquidInkStretchService getInstance() {
        return INSTANCE;
    }

    /**
     * 毎サーバーtickで {@link PlayerInkStateManager#tickPlayer} から呼ぶ。
     *
     * @param player   対象プレイヤー
     * @param previous 前tickのインク状態
     * @param current  現在tickのインク状態
     */
    public void tick(ServerPlayer player, PlayerInkState previous, PlayerInkState current) {
        UUID playerId = player.getUUID();

        // イカ状態の間は「最後に自チームインク上だった位置」を更新し続ける
        if (current == PlayerInkState.CROUCHING_ON_OWN_INK) {
            if (player.onGround()) {
                lastOnInkFootPos.put(playerId, player.position());
            }
            return;
        }

        // 境界越え以外（NORMAL継続など）は何もしない
        if (previous != PlayerInkState.CROUCHING_ON_OWN_INK) {
            return;
        }

        // --- 境界越え検出: 自インク上イカ → インク外 ---

        Vec3 from = lastOnInkFootPos.remove(playerId);
        if (from == null) {
            return;
        }

        // 立ち上がり・空中への遷移では延長しない（イカがインク外へ歩き出た瞬間のみ）
        if (!player.isCrouching() || !player.onGround()) {
            return;
        }

        ServerLevel level = player.level();
        long now = level.getGameTime();
        Long lastStretch = lastStretchTick.get(playerId);
        if (lastStretch != null && now - lastStretch < STRETCH_COOLDOWN_TICKS) {
            return;
        }

        Vec3 exitPos = player.position();
        double dx = exitPos.x - from.x;
        double dz = exitPos.z - from.z;
        double distSq = dx * dx + dz * dz;
        if (distSq < MIN_MOVE_DISTANCE * MIN_MOVE_DISTANCE) {
            return;
        }

        double dist = Math.sqrt(distSq);
        double length = Math.min(MAX_STRETCH_LENGTH, dist);
        Vec3 direction = new Vec3(dx / dist, 0.0, dz / dist);

        lastStretchTick.put(playerId, now);

        paintStretch(player, from, direction, length);
    }

    /**
     * 最後にインク上だった位置から移動方向へ、サンプル点を順に追加塗装する。
     *
     * <p>UP面のみ・塗装可能ブロックのみに塗るため、壁・天井・空中には
     * 塗装されない。複数アリーナにまたがる場合もアリーナごとに
     * アキュムレータへ集約し、1回ずつコミットする。</p>
     */
    private void paintStretch(ServerPlayer player, Vec3 from, Vec3 direction, double length) {
        byte team = TeamManager.getInstance().getTeam(player);
        if (!InkTeam.isValidTeam(team)) {
            return;
        }

        ServerLevel level = player.level();

        Map<UUID, InkArena> arenas = new LinkedHashMap<>();
        Map<UUID, InkPaintAccumulator> accumulators = new LinkedHashMap<>();

        int steps = (int) Math.ceil(length / SAMPLE_SPACING);
        int paintedSamples = 0;
        for (int i = 1; i <= steps; i++) {
            double t = Math.min(SAMPLE_SPACING * i, length);
            Vec3 sample = from.add(direction.scale(t));
            if (paintSample(level, team, sample, arenas, accumulators)) {
                paintedSamples++;
            }
        }

        int totalCells = 0;
        for (Map.Entry<UUID, InkPaintAccumulator> entry : accumulators.entrySet()) {
            InkArena arena = arenas.get(entry.getKey());
            InkPaintAccumulator accumulator = entry.getValue();
            if (arena == null || accumulator.isEmpty()) {
                continue;
            }
            totalCells += accumulator.changedCellCount();
            // 保存 + revision増加 + InkFaceUpdatePayloadブロードキャスト（通常塗装と同一経路）
            InkSyncManager.getInstance().commitAccumulator(level, arena, accumulator);
        }

        LOGGER.info("Squid ink stretch: player={} from=({}, {}, {}) dir=({}, {}) "
                        + "length={:.2f} samples={} cells={}",
                player.getUUID(), from.x, from.y, from.z,
                direction.x, direction.z,
                length, paintedSamples, totalCells);
    }

    /**
     * 1サンプル点の足元UP面を塗装する（paintFootStep と同じ判定・座標系）。
     *
     * @return このサンプルで塗装可能面に接触して塗れたか（noChange含む成功）
     */
    private boolean paintSample(ServerLevel level, byte team, Vec3 sample,
                                Map<UUID, InkArena> arenas,
                                Map<UUID, InkPaintAccumulator> accumulators) {
        // 足元ブロック（InkShooterItem.paintFootStep / InkStandingChecker と同一方式）
        BlockPos footBlock = BlockPos.containing(sample.x, sample.y - 0.1, sample.z);

        Optional<InkArena> arenaOpt =
                InkArenaManager.getInstance().findArenaContaining(level, footBlock);
        if (arenaOpt.isEmpty()) {
            return false;
        }
        InkArena arena = arenaOpt.get();

        BlockState targetState = level.getBlockState(footBlock);
        Direction hitFace = Direction.UP;

        // 塗装不可ブロック・非露出面（壁・天井・水中など）には塗らない
        if (!InkPaintability.isPaintableBlock(level, footBlock, targetState)) {
            return false;
        }
        if (!InkPaintability.isSurfaceExposed(level, footBlock, hitFace)) {
            return false;
        }

        // ブロック上面のワールド位置
        Vec3 hitLoc = new Vec3(sample.x, footBlock.getY() + 1.0, sample.z);

        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
        InkPaintAccumulator accumulator = accumulators.computeIfAbsent(
                arena.getArenaId(), k -> new InkPaintAccumulator());
        arenas.putIfAbsent(arena.getArenaId(), arena);

        // 境界付近の自然な延長のため歪みなし円（足元塗装形状）を使用
        MultiSurfacePaintResult result = InkPaintingService.paintInto(
                level, arena, inkStorage,
                footBlock, hitFace, hitLoc,
                STRETCH_PAINT_RADIUS, team,
                null,
                accumulator);

        return result.success();
    }

    /**
     * プレイヤーの追跡状態を削除する（切断・リスポーン時）。
     */
    public void removePlayer(UUID playerId) {
        lastOnInkFootPos.remove(playerId);
        lastStretchTick.remove(playerId);
    }

    /**
     * 全追跡状態を消去する（サーバー停止時）。
     */
    public void clearAll() {
        lastOnInkFootPos.clear();
        lastStretchTick.clear();
    }
}