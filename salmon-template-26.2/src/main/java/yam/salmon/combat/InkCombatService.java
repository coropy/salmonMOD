package yam.salmon.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;

/**
 * インク武器による戦闘処理サービス。
 *
 * <p>Entityへのダメージ適用を一元管理する。
 * 将来のPhaseでチーム判定、インク蓄積ダメージ、アシスト等を追加する際は
 * このクラスを拡張する。</p>
 */
public final class InkCombatService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".combat");

    private InkCombatService() {}

    /**
     * シューターによるEntity命中時のダメージを適用する。
     *
     * <p>現段階では通常の汎用ダメージを適用する。
     * 将来は専用DamageTypeやチーム判定をここに追加する。</p>
     *
     * @param attacker 攻撃者
     * @param target   被弾Entity
     * @param damage   ダメージ量
     * @return ダメージが実際に適用された場合 true（無敵時間経過後は実際には適用されない場合もあるが、
     *         最低限の事前チェックを通過したら成功とみなす）
     */
    public static boolean applyShooterHit(ServerPlayer attacker, Entity target, float damage) {
        // 自分自身にはダメージを与えない
        if (target == attacker) {
            return false;
        }

        // 無敵状態チェック（Minecraft標準の無敵時間）
        if (target.invulnerableTime > 0) {
            return false;
        }

        // 生存可能なEntityか簡易チェック
        if (!target.isAlive()) {
            return false;
        }

        // スペクテイターは除外
        if (target.isSpectator()) {
            return false;
        }

        // 攻撃不可能なEntityを除外（アイテム、経験値オーブ、投射物など）
        if (!target.isAttackable()) {
            return false;
        }

        // ダメージソース生成（プレイヤーによる攻撃）
        DamageSource damageSource = attacker.damageSources().playerAttack(attacker);

        // MC 26.2: hurtServer は void を返す
        target.hurtServer(attacker.level(), damageSource, damage);

        LOGGER.info("Shooter hit: attacker={} target={} damage={}",
                attacker.getUUID(), target.getId(), damage);

        return true;
    }
}