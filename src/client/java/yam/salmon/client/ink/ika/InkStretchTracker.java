package yam.salmon.client.ink.ika;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import yam.salmon.client.ink.ClientInkCache;
import yam.salmon.client.ink.ClientInkSurface;
import yam.salmon.client.state.ClientPlayerInkState;
import yam.salmon.ink.InkTeam;
import yam.salmon.ink.PlayerInkState;
import yam.salmon.weapon.InkVisualColorResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * イカ状態の「インク伸び」演出を管理するクライアント側トラッカー。
 *
 * <p>{@link PlayerInkState#CROUCHING_ON_OWN_INK} の間、プレイヤーの足元位置と
 * その地点の自チームインク色を追跡し、イカ状態が解除された瞬間に
 * 「最後にいた自チームインクの位置 → 解除時の位置」へ伸びる短いインクの
 * 視覚エフェクトを生成する。</p>
 *
 * <p>純粋なクライアント側視覚エフェクトであり、
 * {@code InkStorage} / {@code InkFaceData} などの地面インクデータには一切書き込まない。
 * インク色は {@link ClientInkCache} に同期済みの面データから解決する。</p>
 *
 * <p>すべてのメソッドはクライアントスレッド（レンダースレッド）からのみ
 * 呼ばれるため、同期化は行っていない。</p>
 */
public final class InkStretchTracker {

    /** ストレッチエフェクトの寿命（tick）。 */
    public static final int EFFECT_DURATION_TICKS = 12;

    /** ストレッチの最大長さ（ブロック）。 */
    public static final double MAX_STRETCH_LENGTH = 2.5;

    /**
     * 生成済みのストレッチエフェクト。
     *
     * @param start         伸びの起点（最後にいた自チームインク上の位置）
     * @param end           伸びの終点（イカ状態解除時の位置）
     * @param colorArgb     インク色（ARGB）
     * @param startGameTime 開始tick（レベルゲームタイム）
     */
    public record StretchEffect(Vec3 start, Vec3 end, int colorArgb, long startGameTime) {}

    /** プレイヤーごとの「最後にいた自チームインク上の足元位置」とその色。 */
    private record InkFootprint(Vec3 pos, int colorArgb) {}

    private static final InkStretchTracker INSTANCE = new InkStretchTracker();

    /** プレイヤーUUID → 最後にいた自チームインク上の足元位置。 */
    private final Map<UUID, InkFootprint> footprints = new HashMap<>();

    /** 有効なストレッチエフェクト（寿命順に自然淘汰）。 */
    private final List<StretchEffect> effects = new ArrayList<>();

    private InkStretchTracker() {}

    public static InkStretchTracker getInstance() {
        return INSTANCE;
    }

    /** 現在有効なストレッチエフェクト（描画用・読み取り専用ではないので変更しないこと）。 */
    public List<StretchEffect> getActiveEffects() {
        return effects;
    }

    /**
     * 毎クライアントtickで呼ぶ（{@code ClientTickEvents.END_CLIENT_TICK}）。
     */
    public void tick(Minecraft client) {
        if (client.level == null) {
            footprints.clear();
            effects.clear();
            return;
        }

        long now = client.level.getGameTime();
        effects.removeIf(effect -> now - effect.startGameTime() >= EFFECT_DURATION_TICKS);

        Set<UUID> present = new HashSet<>();

        for (AbstractClientPlayer player : client.level.players()) {
            UUID playerId = player.getUUID();
            present.add(playerId);

            PlayerInkState state = ClientPlayerInkState.getInstance().get(playerId);
            if (state == PlayerInkState.CROUCHING_ON_OWN_INK) {
                // 接地している間だけ足元位置を「最後にいたインク上の位置」として更新する
                if (player.onGround()) {
                    footprints.put(playerId,
                            new InkFootprint(player.position(), resolveInkColor(client, player)));
                }
            } else {
                // イカ状態解除の瞬間: 最後のインク位置から現在位置へ伸びるエフェクトを生成
                InkFootprint footprint = footprints.remove(playerId);
                if (footprint != null) {
                    spawnStretch(footprint, player.position(), now);
                }
            }
        }

        // 消えたプレイヤーの追跡状態を掃除
        footprints.keySet().removeIf(playerId -> !present.contains(playerId));
    }

    private void spawnStretch(InkFootprint footprint, Vec3 exitPos, long now) {
        Vec3 delta = exitPos.subtract(footprint.pos());
        double horizontalSq = delta.x * delta.x + delta.z * delta.z;
        if (horizontalSq < 0.05) {
            // ほぼその場で解除された場合は演出しない
            return;
        }
        double horizontal = Math.sqrt(horizontalSq);
        if (horizontal > MAX_STRETCH_LENGTH) {
            double scale = MAX_STRETCH_LENGTH / horizontal;
            exitPos = new Vec3(
                    footprint.pos().x + delta.x * scale,
                    footprint.pos().y + delta.y * scale,
                    footprint.pos().z + delta.z * scale);
        }
        effects.add(new StretchEffect(footprint.pos(), exitPos, footprint.colorArgb(), now));
    }

    /**
     * プレイヤー足元ブロックのUP面インクの多数派チームから色を解決する。
     *
     * <p>読み取りのみで {@link ClientInkCache} は汚さない。</p>
     */
    private static int resolveInkColor(Minecraft client, AbstractClientPlayer player) {
        Identifier dimension = client.level.dimension().identifier();
        BlockPos footBlock = BlockPos.containing(
                player.getX(), player.getY() - 0.1, player.getZ());

        for (var arenaSurfaces
                : ClientInkCache.getInstance().getSurfacesForDimension(dimension).values()) {
            for (ClientInkSurface surface : arenaSurfaces.values()) {
                Direction normal = surface.patchId() != null
                        ? surface.patchId().normal()
                        : surface.face();
                if (normal != Direction.UP) {
                    continue;
                }
                if (!surface.blockPos().equals(footBlock)) {
                    continue;
                }
                if (surface.isEmpty()) {
                    continue;
                }

                byte team = surface.teamACells() >= surface.teamBCells()
                        ? InkTeam.TEAM_A
                        : InkTeam.TEAM_B;
                return team == InkTeam.TEAM_B
                        ? InkVisualColorResolver.COLOR_TEAM_B
                        : InkVisualColorResolver.COLOR_TEAM_A;
            }
        }

        return InkVisualColorResolver.COLOR_DEFAULT;
    }
}
