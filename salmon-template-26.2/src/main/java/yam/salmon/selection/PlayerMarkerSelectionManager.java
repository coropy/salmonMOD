package yam.salmon.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import yam.salmon.Salmon;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーごとの第1地点マーカー選択を管理する。
 * 選択状態はサーバー側に一時保存し、永続化しない。
 */
public class PlayerMarkerSelectionManager {

    private static final PlayerMarkerSelectionManager INSTANCE = new PlayerMarkerSelectionManager();

    /**
     * プレイヤーの選択情報。
     */
    public record MarkerSelection(UUID markerId, BlockPos pos, ResourceKey<Level> dimension) {}

    private final Map<UUID, MarkerSelection> selections = new ConcurrentHashMap<>();

    private PlayerMarkerSelectionManager() {}

    public static PlayerMarkerSelectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * プレイヤーの第1地点選択を設定する。
     */
    public void setSelection(UUID playerId, UUID markerId, BlockPos pos, ResourceKey<Level> dimension) {
        selections.put(playerId, new MarkerSelection(markerId, pos, dimension));
        Salmon.LOGGER.info("Player {} selected first marker: markerId={}, pos={}, dim={}",
                playerId, markerId, pos, dimension);
    }

    /**
     * プレイヤーの第1地点選択を取得する。
     */
    public MarkerSelection getSelection(UUID playerId) {
        return selections.get(playerId);
    }

    /**
     * プレイヤーの第1地点選択を解除する。
     */
    public void clearSelection(UUID playerId) {
        MarkerSelection removed = selections.remove(playerId);
        if (removed != null) {
            Salmon.LOGGER.info("Cleared marker selection for player {}: markerId={}",
                    playerId, removed.markerId());
        }
    }

    /**
     * 全プレイヤーの選択を解除する（サーバー停止時など）。
     */
    public void clearAll() {
        int count = selections.size();
        selections.clear();
        Salmon.LOGGER.info("Cleared all marker selections ({} player(s))", count);
    }

    /**
     * プレイヤーが現在選択を持っているか。
     */
    public boolean hasSelection(UUID playerId) {
        return selections.containsKey(playerId);
    }
}