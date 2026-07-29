package yam.salmon.weapon;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 武器設定のレジストリ。
 *
 * <p>武器ID（{@link Identifier}）から {@link InkWeaponConfig} を解決する。
 * 現在はプログラム登録だが、将来はJSON/Data Packローダーに差し替え可能な構造。</p>
 */
public final class InkWeaponRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    private static final Map<Identifier, InkWeaponConfig> CONFIGS = new HashMap<>();

    private InkWeaponRegistry() {}

    /**
     * 武器設定を登録する。
     * 同じIDがすでに登録されている場合は上書きされ、警告ログを出力する。
     */
    public static void register(InkWeaponConfig config) {
        Identifier id = config.weaponId();
        if (CONFIGS.containsKey(id)) {
            LOGGER.warn("Weapon config already registered, overwriting: {}", id);
        }
        CONFIGS.put(id, config);
        LOGGER.debug("Registered weapon config: {}", id);
    }

    /**
     * 武器IDから設定を取得する。
     * 存在しない場合は empty を返す。
     */
    public static Optional<InkWeaponConfig> get(Identifier weaponId) {
        return Optional.ofNullable(CONFIGS.get(weaponId));
    }

    /**
     * 武器IDから設定を取得する。
     * 存在しない場合は IllegalArgumentException を送出する。
     */
    public static InkWeaponConfig getOrThrow(Identifier weaponId) {
        InkWeaponConfig config = CONFIGS.get(weaponId);
        if (config == null) {
            throw new IllegalArgumentException("No weapon config registered for: " + weaponId);
        }
        return config;
    }

    /**
     * 全登録済み武器設定のマップを返す（読み取り専用）。
     */
    public static Map<Identifier, InkWeaponConfig> getAll() {
        return Map.copyOf(CONFIGS);
    }

    /**
     * デフォルト武器設定を登録する。
     * Mod初期化時に呼ばれる。
     */
    public static void registerDefaults() {
        register(InkWeaponConfig.INK_SHOOTER);
        LOGGER.info("Registered {} weapon config(s)", CONFIGS.size());
    }

    /**
     * 全設定をクリアする（主にテスト用）。
     */
    public static void clear() {
        CONFIGS.clear();
    }

    /**
     * 登録済み武器数を返す。
     */
    public static int count() {
        return CONFIGS.size();
    }
}