package yam.salmon.client.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import yam.salmon.Salmon;
import yam.salmon.ink.InkFaceData;
import yam.salmon.ink.InkTeam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側のインクデータキャッシュ。
 *
 * <p>サーバーから受信したインク面データを保持し、
 * 描画用の読み取り専用ビューを提供する。</p>
 *
 * <p>スレッドセーフ: ConcurrentHashMap を使用し、
 * ネットワーク受信スレッドとレンダースレッド間の安全を確保する。</p>
 *
 * <p>キー: arenaId + blockPos + face → 面データ</p>
 */
public class ClientInkCache {

    private static final ClientInkCache INSTANCE = new ClientInkCache();

    /** ディメンション → アリーナ → 面キー → 面データ */
    private final Map<Identifier, Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>>> cache = new ConcurrentHashMap<>();

    /** 完全同期のバッファ */
    private final Map<Identifier, List<ClientInkSurface>> syncBuffer = new ConcurrentHashMap<>();
    private boolean syncInProgress = false;
    private long currentSessionId = -1;
    private Identifier syncDimension = null;

    /** クライアント側のインクデバッグ表示有効フラグ */
    private boolean debugEnabled = false;

    /** 診断用カウント */
    private long lastDiagnosticLogTime = 0;

    private ClientInkCache() {}

    public static ClientInkCache getInstance() {
        return INSTANCE;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    // ===================================================================
    // 完全同期
    // ===================================================================

    /**
     * 完全同期開始: 指定ディメンションのキャッシュをクリアし、バッファリング開始。
     */
    public void beginFullSync(Identifier dimensionId, long sessionId, int faceCount) {
        syncInProgress = true;
        currentSessionId = sessionId;
        syncDimension = dimensionId;

        // 当該ディメンションの古いキャッシュをクリア
        clearDimension(dimensionId);

        // バッファを準備
        syncBuffer.put(dimensionId, Collections.synchronizedList(new ArrayList<>(faceCount)));

        Salmon.LOGGER.info("[Client] Ink full sync begin: dim={} sessionId={} expectedFaces={}",
                dimensionId, sessionId, faceCount);
    }

    /**
     * 完全同期中の面データをバッファに追加。
     */
    public void addSyncFace(
            UUID arenaUuid, int arenaNumber, Identifier dimensionId,
            BlockPos blockPos, Direction face, byte[] cells, long revision) {
        if (!syncInProgress) {
            // 完全同期中でなければ単独で適用
            applyFaceUpdate(arenaUuid, arenaNumber, dimensionId, blockPos, face, cells, revision);
            return;
        }

        if (!dimensionId.equals(syncDimension)) {
            Salmon.LOGGER.warn("[Client] Sync face for wrong dimension: {} vs {}", dimensionId, syncDimension);
            return;
        }

        ClientInkSurface surface = createSurface(arenaUuid, arenaNumber, dimensionId, blockPos, face, cells, revision);
        if (surface != null) {
            List<ClientInkSurface> buffer = syncBuffer.get(dimensionId);
            if (buffer != null) {
                buffer.add(surface);
            }
        }
    }

    /**
     * 完全同期終了: バッファをコミット。
     */
    public void endFullSync(long sessionId) {
        if (!syncInProgress || sessionId != currentSessionId) {
            Salmon.LOGGER.warn("[Client] Sync end with mismatched sessionId: expected={} got={}",
                    currentSessionId, sessionId);
            return;
        }

        syncInProgress = false;
        currentSessionId = -1;

        List<ClientInkSurface> buffer = syncBuffer.remove(syncDimension);
        if (buffer == null) {
            Salmon.LOGGER.info("[Client] Ink full sync end: 0 faces");
            syncDimension = null;
            return;
        }

        Identifier dimId = syncDimension;
        syncDimension = null;

        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache =
                cache.computeIfAbsent(dimId, k -> new ConcurrentHashMap<>());

        for (ClientInkSurface surface : buffer) {
            Map<ClientInkSurfaceKey, ClientInkSurface> arenaMap =
                    dimCache.computeIfAbsent(surface.arenaUuid(), k -> new ConcurrentHashMap<>());
            arenaMap.put(surface.key(), surface);
        }

        Salmon.LOGGER.info("[Client] Ink full sync end: dim={} sessionId={} faces={}",
                dimId, sessionId, buffer.size());
    }

    // ===================================================================
    // 差分更新
    // ===================================================================

    /**
     * 1面の差分更新を適用する。
     */
    public void applyFaceUpdate(
            UUID arenaUuid, int arenaNumber, Identifier dimensionId,
            BlockPos blockPos, Direction face, byte[] cells, long revision) {

        // 検証
        if (cells == null || cells.length != InkFaceData.CELL_COUNT) {
            Salmon.LOGGER.warn("[Client] Invalid cell count in face update: {}",
                    cells != null ? cells.length : 0);
            return;
        }

        if (face == null) {
            Salmon.LOGGER.warn("[Client] Invalid face direction in update");
            return;
        }

        // 完全同期中ならバッファへ
        if (syncInProgress) {
            addSyncFace(arenaUuid, arenaNumber, dimensionId, blockPos, face, cells, revision);
            return;
        }

        // 既存データのリビジョンをチェック
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache = cache.get(dimensionId);
        if (dimCache != null) {
            Map<ClientInkSurfaceKey, ClientInkSurface> arenaMap = dimCache.get(arenaUuid);
            if (arenaMap != null) {
                ClientInkSurfaceKey key = new ClientInkSurfaceKey(arenaUuid, blockPos, face);
                ClientInkSurface existing = arenaMap.get(key);
                if (existing != null && existing.revision() > revision) {
                    // 既存のほうが新しい → この更新を無視
                    Salmon.LOGGER.debug("[Client] Old revision ignored: arena={} pos={} face={} existingRev={} newRev={}",
                            arenaUuid, blockPos, face, existing.revision(), revision);
                    return;
                }
            }
        }

        // 面データを作成して保存
        ClientInkSurface surface = createSurface(arenaUuid, arenaNumber, dimensionId, blockPos, face, cells, revision);
        if (surface == null) return;

        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dim =
                cache.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());
        Map<ClientInkSurfaceKey, ClientInkSurface> arena =
                dim.computeIfAbsent(arenaUuid, k -> new ConcurrentHashMap<>());

        boolean existed = arena.containsKey(surface.key());
        arena.put(surface.key(), surface);

        if (!existed) {
            Salmon.LOGGER.debug("[Client] Cache surface created: arena #{} pos={} face={}",
                    arenaNumber, blockPos, face);
        } else {
            Salmon.LOGGER.debug("[Client] Cache surface updated: arena #{} pos={} face={}",
                    arenaNumber, blockPos, face);
        }
    }

    // ===================================================================
    // アリーナクリア
    // ===================================================================

    /**
     * 指定アリーナの全インクデータをキャッシュから削除する。
     */
    public void clearArena(UUID arenaUuid, Identifier dimensionId, long revision) {
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache = cache.get(dimensionId);
        if (dimCache == null) return;

        Map<ClientInkSurfaceKey, ClientInkSurface> arenaMap = dimCache.get(arenaUuid);
        if (arenaMap != null) {
            int count = arenaMap.size();
            arenaMap.clear();
            dimCache.remove(arenaUuid);
            Salmon.LOGGER.info("[Client] Arena cache cleared: arenaUuid={} dim={} faces={}",
                    arenaUuid, dimensionId, count);
        }
    }

    // ===================================================================
    // ディメンションクリア
    // ===================================================================

    /**
     * 指定ディメンションの全キャッシュを削除する。
     */
    public void clearDimension(Identifier dimensionId) {
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache = cache.remove(dimensionId);
        if (dimCache != null) {
            int totalFaces = 0;
            for (var arena : dimCache.values()) {
                totalFaces += arena.size();
                arena.clear();
            }
            dimCache.clear();
            Salmon.LOGGER.info("[Client] Dimension cache cleared: dim={} arenas={} faces={}",
                    dimensionId, dimCache.size(), totalFaces);
        }
    }

    /**
     * 全キャッシュを削除する。
     */
    public void clearAll() {
        for (var dimEntry : cache.entrySet()) {
            for (var arenaEntry : dimEntry.getValue().values()) {
                arenaEntry.clear();
            }
            dimEntry.getValue().clear();
        }
        cache.clear();
        syncBuffer.clear();
        syncInProgress = false;
        currentSessionId = -1;
        syncDimension = null;
        Salmon.LOGGER.info("[Client] All ink cache cleared");
    }

    // ===================================================================
    // データ取得
    // ===================================================================

    /**
     * 指定ディメンションの全サーフェスデータを返す。
     */
    public Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> getSurfacesForDimension(Identifier dimensionId) {
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache = cache.get(dimensionId);
        if (dimCache == null) return Collections.emptyMap();
        // 読み取り専用の防御コピーを返す（スレッドセーフのため）
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> copy = new HashMap<>();
        for (var entry : dimCache.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * 指定ディメンションの面数を返す。
     */
    public int getSurfaceCount(Identifier dimensionId) {
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache = cache.get(dimensionId);
        if (dimCache == null) return 0;
        int total = 0;
        for (var arena : dimCache.values()) {
            total += arena.size();
        }
        return total;
    }

    // ===================================================================
    // 内部ヘルパー
    // ===================================================================

    private ClientInkSurface createSurface(
            UUID arenaUuid, int arenaNumber, Identifier dimensionId,
            BlockPos blockPos, Direction face, byte[] cells, long revision) {

        // セル配列をコピーして正規化
        byte[] normalizedCells = new byte[InkFaceData.CELL_COUNT];
        int teamACount = 0;
        int teamBCount = 0;
        for (int i = 0; i < InkFaceData.CELL_COUNT && i < cells.length; i++) {
            byte normalized = InkTeam.normalize(cells[i]);
            normalizedCells[i] = normalized;
            if (normalized == InkTeam.TEAM_A) teamACount++;
            else if (normalized == InkTeam.TEAM_B) teamBCount++;
        }

        ClientInkSurfaceKey key = new ClientInkSurfaceKey(arenaUuid, blockPos, face);

        return new ClientInkSurface(
                arenaUuid, arenaNumber, dimensionId, blockPos, face, key,
                normalizedCells, revision, teamACount, teamBCount);
    }

    // ===================================================================
    // 診断
    // ===================================================================

    /**
     * デバッグ表示ONのとき、定期的に統計情報をログ出力する。
     */
    public void logDiagnostics(Identifier currentDim) {
        if (!debugEnabled) return;

        long now = System.currentTimeMillis();
        if (now - lastDiagnosticLogTime < 1000) return;
        lastDiagnosticLogTime = now;

        int cachedSurfaces = getSurfaceCount(currentDim);
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> dimCache = cache.get(currentDim);
        int teamACells = 0;
        int teamBCells = 0;

        if (dimCache != null) {
            for (var arenaMap : dimCache.values()) {
                for (ClientInkSurface surface : arenaMap.values()) {
                    teamACells += surface.teamACells();
                    teamBCells += surface.teamBCells();
                }
            }
        }

        Salmon.LOGGER.info("[Client] Ink stats: dim={} cachedSurfaces={} teamACells={} teamBCells={} syncInProgress={}",
                currentDim, cachedSurfaces, teamACells, teamBCells, syncInProgress);
    }
}