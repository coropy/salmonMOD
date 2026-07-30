package yam.salmon.weapon;

import net.minecraft.resources.Identifier;
import yam.salmon.Salmon;

/**
 * 武器ごとの射撃特性を表現する設定レコード。
 *
 * <p>射程、弾速、重力、拡散、連射速度、塗装半径、ダメージなど
 * 武器の全パラメータを一箇所に集約する。
 * 将来のJSON/Data Packロードは {@link InkWeaponRegistry} 経由で差し替え可能。</p>
 */
public record InkWeaponConfig(
        /** 武器ID（例: salmon:ink_shooter） */
        Identifier weaponId,

        /** 発射間隔（tick単位）。1以上 */
        int fireIntervalTicks,

        /** 初速（blocks/tick）。0より大きいこと */
        double initialSpeed,

        /** 1tickあたりの重力加速度（下向き） */
        double gravityPerTick,

        /** 弾の最大飛翔tick数。1以上 */
        int maxFlightTicks,

        /** 最大射程（blocks）。0より大きいこと */
        double maxRange,

        /** 水平方向の拡散角度（度数法）。0以上 */
        double horizontalSpreadDegrees,

        /** 垂直方向の拡散角度（度数法）。0以上 */
        double verticalSpreadDegrees,

        /** ダメージ量（ハート半分単位。2.0 = ハート1つ） */
        float damage,

        /** 塗装ブラシ半径（ブロック単位）。0より大きいこと */
        double paintRadius,

        /** 1tickあたりのsubstep数（衝突判定の分割数）。1以上 */
        int trajectorySubstepsPerTick,

        /** 衝突判定用の弾半径（ブロック単位）。Entity AABB拡張に使用。0以上 */
        double collisionRadius,

        /** クライアント描画用の弾サイズ（ブロック単位） */
        float visualProjectileSize,

        /** ブロック命中時に塗装を行うか */
        boolean paintOnBlockHit,

        /** Entity命中時にダメージを与えるか */
        boolean damageEntities,

        /** トレイル塗装設定（弾道下へのインク滴） */
        InkTrailPaintConfig trailPaintConfig
) {
    /** デフォルトのインクシューター設定（短射程・高重力・低頻度大粒トレイル） */
    public static final InkWeaponConfig INK_SHOOTER = new InkWeaponConfig(
            Salmon.id("ink_shooter"),
            3,      // fireIntervalTicks
            1.85,   // initialSpeed (blocks/tick)
            0.11,   // gravityPerTick
            9,      // maxFlightTicks
            13.0,   // maxRange (blocks)
            1.5,    // horizontalSpreadDegrees
            1.0,    // verticalSpreadDegrees
            2.0f,   // damage
            0.25,   // paintRadius (main impact)
            6,      // trajectorySubstepsPerTick
            0.07,   // collisionRadius
            0.10f,  // visualProjectileSize
            true,   // paintOnBlockHit
            true,   // damageEntities
            InkTrailPaintConfig.STANDARD
    );

    /** 短射程・高拡散シューター（プリセット例） */
    public static final InkWeaponConfig SHORT_RANGE = new InkWeaponConfig(
            Salmon.id("ink_shooter_short"),
            2,      // fireIntervalTicks
            2.5,    // initialSpeed
            0.06,   // gravityPerTick
            10,     // maxFlightTicks
            16.0,   // maxRange
            4.0,    // horizontalSpreadDegrees
            3.0,    // verticalSpreadDegrees
            2.5f,   // damage
            0.35,   // paintRadius
            4,      // trajectorySubstepsPerTick
            0.1,    // collisionRadius
            0.16f,  // visualProjectileSize
            true,   // paintOnBlockHit
            true,   // damageEntities
            InkTrailPaintConfig.SHORT_RANGE
    );

    /** 長射程・低拡散シューター（プリセット例） */
    public static final InkWeaponConfig LONG_RANGE = new InkWeaponConfig(
            Salmon.id("ink_shooter_long"),
            5,      // fireIntervalTicks
            4.5,    // initialSpeed
            0.02,   // gravityPerTick
            20,     // maxFlightTicks
            36.0,   // maxRange
            0.8,    // horizontalSpreadDegrees
            0.5,    // verticalSpreadDegrees
            3.0f,   // damage
            0.18,   // paintRadius
            8,      // trajectorySubstepsPerTick
            0.05,   // collisionRadius
            0.12f,  // visualProjectileSize
            true,   // paintOnBlockHit
            true,   // damageEntities
            InkTrailPaintConfig.LONG_RANGE
    );

    /**
     * 設定値の妥当性を検証する。
     * 不正値がある場合は {@link IllegalArgumentException} を送出する。
     */
    public InkWeaponConfig {
        if (weaponId == null) {
            throw new IllegalArgumentException("weaponId must not be null");
        }
        if (fireIntervalTicks < 1) {
            throw new IllegalArgumentException("fireIntervalTicks must be >= 1, got " + fireIntervalTicks);
        }
        if (initialSpeed <= 0) {
            throw new IllegalArgumentException("initialSpeed must be > 0, got " + initialSpeed);
        }
        if (gravityPerTick < 0) {
            throw new IllegalArgumentException("gravityPerTick must be >= 0, got " + gravityPerTick);
        }
        if (maxFlightTicks < 1) {
            throw new IllegalArgumentException("maxFlightTicks must be >= 1, got " + maxFlightTicks);
        }
        if (maxRange <= 0) {
            throw new IllegalArgumentException("maxRange must be > 0, got " + maxRange);
        }
        if (horizontalSpreadDegrees < 0) {
            throw new IllegalArgumentException("horizontalSpreadDegrees must be >= 0, got " + horizontalSpreadDegrees);
        }
        if (verticalSpreadDegrees < 0) {
            throw new IllegalArgumentException("verticalSpreadDegrees must be >= 0, got " + verticalSpreadDegrees);
        }
        if (paintRadius <= 0) {
            throw new IllegalArgumentException("paintRadius must be > 0, got " + paintRadius);
        }
        if (trajectorySubstepsPerTick < 1) {
            throw new IllegalArgumentException("trajectorySubstepsPerTick must be >= 1, got " + trajectorySubstepsPerTick);
        }
        if (collisionRadius < 0) {
            throw new IllegalArgumentException("collisionRadius must be >= 0, got " + collisionRadius);
        }

        // パフォーマンス上限: 最大判定セグメント数 <= 256
        int maxSegments = maxFlightTicks * trajectorySubstepsPerTick;
        if (maxSegments > 256) {
            throw new IllegalArgumentException(
                    "maxFlightTicks * trajectorySubstepsPerTick must be <= 256, got " + maxSegments);
        }
    }

    /** 旧設定からの移行用ファクトリ */
    public static InkWeaponConfig fromLegacy(InkShooterConfig legacy) {
        return new InkWeaponConfig(
                Salmon.id("ink_shooter"),
                legacy.fireIntervalTicks(),
                legacy.range() / legacy.fireIntervalTicks() * 0.375, // 概算初速
                0.0,    // gravityPerTick (旧設定は重力なし)
                8,      // maxFlightTicks
                legacy.range(),
                legacy.spreadDegrees(),
                legacy.spreadDegrees(),
                legacy.damage(),
                legacy.brushRadius(),
                1,      // trajectorySubstepsPerTick (旧は単一ClipContext)
                0.0,    // collisionRadius
                0.14f,  // visualProjectileSize
                true,   // paintOnBlockHit
                true,   // damageEntities
                InkTrailPaintConfig.DISABLED
        );
    }
}