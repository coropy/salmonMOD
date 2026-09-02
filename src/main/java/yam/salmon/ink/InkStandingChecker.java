package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.team.TeamManager;

import java.util.List;
import java.util.Optional;

/**
 * サーバー側でプレイヤーが「自分のチームのインク上に立っているか」を判定するユーティリティ。
 *
 * <p>既存の {@link InkTeam} チーム管理、{@link InkPaintability} の塗装可否判定、
 * {@link InkStorage} の面データ、{@link InkSurfacePatchExtractor} のパッチ抽出を
 * すべて再利用する。独自の「空気・草・液体かどうか」という除外判定は持たない。</p>
 *
 * <p>足元ブロックの決定は {@code yam.salmon.item.InkShooterItem#paintFootStep} と
 * 同じ {@code BlockPos.containing(x, y - 0.1, z)} 方式を使い、塗装位置と判定位置が
 * ずれないようにする。</p>
 *
 * <p>16×16全セル走査は行わず、プレイヤーの足元位置が該当するパッチとその1セルだけを判定する。</p>
 */
public final class InkStandingChecker {

    /** 法線方向の平面一致判定で許容する誤差（ブロック単位）。 */
    private static final double PLANE_EPSILON = 0.1;

    private InkStandingChecker() {}

    /**
     * プレイヤーの現在のインク上状態を判定する。
     *
     * <p>ここでは「自チームのインク上に立っているか」までを返す。
     * しゃがみ（イカ状態）への変換は {@code PlayerInkStateManager} で行う。</p>
     *
     * @param player 判定対象のプレイヤー
     * @return {@link PlayerInkState#ON_OWN_INK} または {@link PlayerInkState#NORMAL}
     */
    public static PlayerInkState determineState(ServerPlayer player) {
        // 接地していない場合は即 NORMAL
        if (!player.onGround()) {
            return PlayerInkState.NORMAL;
        }

        // チーム未割り当て（NONE）は判定不要
        byte myTeam = TeamManager.getInstance().getTeam(player);
        if (!InkTeam.isValidTeam(myTeam)) {
            return PlayerInkState.NORMAL;
        }

        ServerLevel level = player.level();

        // 足元ブロック（InkShooterItem.paintFootStep と同一ロジック）
        BlockPos footBlock = BlockPos.containing(
                player.getX(), player.getY() - 0.1, player.getZ());

        // アリーナ外は NORMAL
        Optional<InkArena> arenaOpt = InkArenaManager.getInstance()
                .findArenaContaining(level, footBlock);
        if (arenaOpt.isEmpty()) {
            return PlayerInkState.NORMAL;
        }

        InkArena arena = arenaOpt.get();
        BlockState state = level.getBlockState(footBlock);

        // 塗装不可（空気・草・液体などを含む既存ルール）は NORMAL
        if (!InkPaintability.isPaintableBlock(level, footBlock, state)) {
            return PlayerInkState.NORMAL;
        }

        // プレイヤーの足元ワールド位置（Entity の Y は足元 = AABB 下端）
        Vec3 feetPos = new Vec3(player.getX(), player.getY(), player.getZ());

        // 足元を含む UP 面パッチを特定し、その1セルだけを判定する
        List<InkSurfacePatch> patches = InkSurfacePatchExtractor.extract(state, level, footBlock);
        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();

        for (InkSurfacePatch patch : patches) {
            if (patch.id().normal() != Direction.UP) {
                continue;
            }
            // UV矩形と法線方向平面の両方が足元位置に一致するパッチだけを対象にする
            if (!patch.containsWorldPoint(feetPos, PLANE_EPSILON)) {
                continue;
            }

            FaceBasis.LocalUV localUV = patch.projectOntoPatch(feetPos);
            if (!localUV.isInBounds()) {
                continue;
            }

            int cellU = clampCell((int) Math.floor(localUV.u() * InkFaceData.GRID_SIZE));
            int cellV = clampCell((int) Math.floor(localUV.v() * InkFaceData.GRID_SIZE));

            Optional<InkFaceData> faceOpt = inkStorage.getFace(arena, footBlock, patch.id());
            if (faceOpt.isEmpty()) {
                continue;
            }

            InkFaceData faceData = faceOpt.get();
            if (faceData.isEmpty()) {
                continue;
            }

            if (faceData.getCell(cellU, cellV) == myTeam) {
                return PlayerInkState.ON_OWN_INK;
            }
        }

        return PlayerInkState.NORMAL;
    }

    private static int clampCell(int cell) {
        return Math.min(InkFaceData.GRID_SIZE - 1, Math.max(0, cell));
    }
}