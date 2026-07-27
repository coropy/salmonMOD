package yam.salmon.weapon;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * インクシューターの射撃エフェクト。
 *
 * <p>発射音、着弾音、着弾パーティクル、飛翔エフェクトを
 * サーバー側から周囲プレイヤーへ送信する。</p>
 */
public final class InkShotEffects {

    /** パーティクル数（多すぎないように） */
    private static final int HIT_PARTICLE_COUNT = 12;

    /** パーティクル拡散半径 */
    private static final double PARTICLE_SPREAD = 0.3;

    private InkShotEffects() {}

    /**
     * 発射音を再生する。
     *
     * @param level  サーバーレベル
     * @param pos    発射位置
     * @param player 発射者
     */
    public static void spawnFireEffect(ServerLevel level, Vec3 pos, ServerPlayer player) {
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS,
                0.3f, 1.5f + level.getRandom().nextFloat() * 0.5f);
    }

    /**
     * ブロック命中エフェクトを表示する。
     *
     * @param level    サーバーレベル
     * @param position 着弾位置
     * @param face     着弾面
     */
    public static void spawnBlockHitEffect(ServerLevel level, Vec3 position, Direction face) {
        var random = level.getRandom();

        // 着弾音
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.SLIME_HURT, SoundSource.PLAYERS,
                0.6f, 0.8f + random.nextFloat() * 0.4f);

        // 着弾パーティクル（色付きスプラッシュ）
        for (int i = 0; i < HIT_PARTICLE_COUNT; i++) {
            double dx = (random.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double dy = (random.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double dz = (random.nextDouble() - 0.5) * PARTICLE_SPREAD;

            // 面方向にバイアスをかける
            double spreadX = dx + face.getStepX() * 0.15;
            double spreadY = dy + face.getStepY() * 0.15;
            double spreadZ = dz + face.getStepZ() * 0.15;

            level.sendParticles(ParticleTypes.SPLASH,
                    position.x, position.y, position.z,
                    1,
                    spreadX, spreadY, spreadZ,
                    0.05);
        }
    }

    /**
     * Entity命中エフェクトを表示する。
     *
     * @param level    サーバーレベル
     * @param position 着弾位置
     * @param player   発射者
     */
    public static void spawnEntityHitEffect(ServerLevel level, Vec3 position, ServerPlayer player) {
        var random = level.getRandom();

        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.SLIME_HURT, SoundSource.PLAYERS,
                0.4f, 1.0f + random.nextFloat() * 0.5f);

        // 小規模なスプラッシュ
        for (int i = 0; i < 6; i++) {
            double dx = (random.nextDouble() - 0.5) * 0.2;
            double dy = (random.nextDouble() - 0.5) * 0.2;
            double dz = (random.nextDouble() - 0.5) * 0.2;

            level.sendParticles(ParticleTypes.SPLASH,
                    position.x, position.y, position.z,
                    1,
                    dx, dy, dz,
                    0.03);
        }
    }

    /**
     * 何にも当たらなかった場合の射程終端エフェクト。
     *
     * @param level    サーバーレベル
     * @param position 終端位置
     * @param player   発射者
     */
    public static void spawnMissEffect(ServerLevel level, Vec3 position, ServerPlayer player) {
        // 終端で小さなエフェクトのみ
        level.sendParticles(ParticleTypes.SPLASH,
                position.x, position.y, position.z,
                2,
                0.1, 0.1, 0.1,
                0.02);
    }
}