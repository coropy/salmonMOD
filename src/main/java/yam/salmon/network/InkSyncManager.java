package yam.salmon.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.ink.InkFaceData;
import yam.salmon.ink.InkStorage;
import yam.salmon.ink.InkSurfaceKey;
import yam.salmon.ink.InkSurfacePatchId;
import yam.salmon.ink.MultiSurfacePaintResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * インクデータのサーバー→クライアント同期を管理する。
 *
 * <p>同期方式: 同ディメンション全プレイヤーへブロードキャスト。
 * Phase 3初期版では正しさを優先し、距離フィルタリングは将来対応。</p>
 *
 * <p>リビジョン: アリーナ単位の単調増加 long 値。
 * 塗装・クリア・削除のたびにインクリメントされ、
 * 古いPayloadの上書きを防止する。</p>
 */
public class InkSyncManager {

    private static final InkSyncManager INSTANCE = new InkSyncManager();

    /** アリーナUUID → リビジョン */
    private final Map<UUID, AtomicLong> arenaRevisions = new ConcurrentHashMap<>();

    /** 同期セッションIDのカウンター */
    private final AtomicLong sessionIdCounter = new AtomicLong(0);

    private InkSyncManager() {}

    public static InkSyncManager getInstance() {
        return INSTANCE;
    }

    // ===================================================================
    // リビジョン管理
    // ===================================================================

    /**
     * アリーナの現在リビジョンを取得する。
     */
    public long getRevision(UUID arenaUuid) {
        AtomicLong rev = arenaRevisions.get(arenaUuid);
        return rev != null ? rev.get() : 0;
    }

    /**
     * アリーナのリビジョンをインクリメントし、新しい値を返す。
     */
    public long incrementRevision(UUID arenaUuid) {
        AtomicLong rev = arenaRevisions.computeIfAbsent(arenaUuid, k -> new AtomicLong(0));
        long newRev = rev.incrementAndGet();
        Salmon.LOGGER.info("Ink arena revision incremented: arenaUuid={} revision={}",
                arenaUuid, newRev);
        return newRev;
    }

    /**
     * アリーナ削除時にリビジョンを削除する。
     */
    public void removeRevision(UUID arenaUuid) {
        arenaRevisions.remove(arenaUuid);
    }

    // ===================================================================
    // 差分同期: 塗装時
    // ===================================================================

    /**
     * 1面のインク更新を同ディメンション全プレイヤーにブロードキャストする。
     *
     * @param level      サーバーレベル
     * @param arena      対象アリーナ
     * @param blockPos   ブロック座標
     * @param face       面
     * @param faceData   変更後の面データ
     * @param changedCells 変更されたセル数（>0 であること）
     * @param patchId    Surface Patch ID
     */
    public void broadcastFaceUpdate(ServerLevel level, InkArena arena,
                                     BlockPos blockPos, Direction face,
                                     InkFaceData faceData, int changedCells,
                                     InkSurfacePatchId patchId) {
        if (changedCells <= 0) {
            return;
        }

        long revision = incrementRevision(arena.getArenaId());
        Identifier dimensionId = level.dimension().identifier();

        InkFaceUpdatePayload payload = new InkFaceUpdatePayload(
                arena.getArenaId(),
                arena.getArenaNumber(),
                dimensionId,
                blockPos,
                face.getSerializedName(),
                patchId,
                faceData.copyCells(),
                revision
        );

        int count = 0;
        for (ServerPlayer p : level.players()) {
            ServerPlayNetworking.send(p, payload);
            count++;
        }

        Salmon.LOGGER.info("Ink face update sent: arena #{} block={} face={} changedCells={} revision={} recipients={}",
                arena.getArenaNumber(), blockPos, face, changedCells, revision, count);
    }

    /**
     * 複数面のインク更新を同ディメンション全プレイヤーにブロードキャストする。
     * 1回の塗装操作で複数面が更新された場合、すべて同じリビジョンを使用する。
     *
     * @param level           サーバーレベル
     * @param arena           対象アリーナ
     * @param updatedSurfaces 更新された面のリスト
     */
    public void broadcastMultiFaceUpdate(ServerLevel level, InkArena arena,
                                          List<MultiSurfacePaintResult.UpdatedInkSurface> updatedSurfaces) {
        if (updatedSurfaces.isEmpty()) {
            return;
        }

        // 1つの塗装操作で1回だけリビジョンを増やす
        long revision = incrementRevision(arena.getArenaId());
        Identifier dimensionId = level.dimension().identifier();

        int payloadCount = 0;
        for (var surface : updatedSurfaces) {
            if (surface.changedCells() <= 0) continue;

            InkSurfacePatchId patchId = surface.surfaceKey().patchId();

            InkFaceUpdatePayload payload = new InkFaceUpdatePayload(
                    arena.getArenaId(),
                    arena.getArenaNumber(),
                    dimensionId,
                    surface.blockPos(),
                    surface.face().getSerializedName(),
                    patchId,
                    surface.cells(),
                    revision
            );

            int count = 0;
            for (ServerPlayer p : level.players()) {
                ServerPlayNetworking.send(p, payload);
                count++;
            }
            payloadCount++;
        }

        Salmon.LOGGER.info("Multi-surface ink update sent: arena #{} surfaces={} revision={}",
                arena.getArenaNumber(), payloadCount, revision);
    }

    // ===================================================================
    // 差分同期: クリア時
    // ===================================================================

    /**
     * アリーナのインククリアを同ディメンション全プレイヤーにブロードキャストする。
     */
    public void broadcastArenaClear(ServerLevel level, InkArena arena) {
        long revision = incrementRevision(arena.getArenaId());
        Identifier dimensionId = level.dimension().identifier();

        InkArenaClearPayload payload = new InkArenaClearPayload(
                arena.getArenaId(),
                arena.getArenaNumber(),
                dimensionId,
                revision
        );

        int count = 0;
        for (ServerPlayer p : level.players()) {
            ServerPlayNetworking.send(p, payload);
            count++;
        }

        Salmon.LOGGER.info("Arena ink clear sent: arena #{} revision={} recipients={}",
                arena.getArenaNumber(), revision, count);
    }

    // ===================================================================
    // 差分同期: アリーナ削除時
    // ===================================================================

    /**
     * アリーナ削除に伴うインクデータクリアをブロードキャストする。
     */
    public void broadcastArenaRemoved(ServerLevel level, InkArena arena) {
        long revision = incrementRevision(arena.getArenaId());
        Identifier dimensionId = level.dimension().identifier();

        InkArenaClearPayload payload = new InkArenaClearPayload(
                arena.getArenaId(),
                arena.getArenaNumber(),
                dimensionId,
                revision
        );

        int count = 0;
        for (ServerPlayer p : level.players()) {
            ServerPlayNetworking.send(p, payload);
            count++;
        }

        Salmon.LOGGER.info("Arena deletion ink cleanup sent: arena #{} revision={} recipients={}",
                arena.getArenaNumber(), revision, count);

        // リビジョン管理から削除
        removeRevision(arena.getArenaId());
    }

    // ===================================================================
    // 完全同期
    // ===================================================================

    /**
     * 指定プレイヤーに現在ディメンションの全インクデータを完全同期する。
     * リビジョンは永続化されたアリーナ単位の値を送信する。
     */
    public void sendFullSync(ServerPlayer player) {
        ServerLevel level = player.level();
        Identifier dimensionId = level.dimension().identifier();
        long sessionId = sessionIdCounter.incrementAndGet();

        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
        Map<UUID, Map<InkSurfaceKey, InkFaceData>> allData = inkStorage.exportData();

        // 表面数カウント（事前計算）
        int faceCount = 0;
        List<UUID> dimensionArenaIds = new ArrayList<>();
        for (var arenaEntry : allData.entrySet()) {
            UUID arenaUuid = arenaEntry.getKey();
            Map<InkSurfaceKey, InkFaceData> surfaces = arenaEntry.getValue();
            if (surfaces.isEmpty()) continue;

            // アリーナのディメンションを確認
            boolean inDimension = false;
            for (InkArena arena : InkArenaManager.getInstance().getArenasInDimension(level.dimension())) {
                if (arena.getArenaId().equals(arenaUuid)) {
                    inDimension = true;
                    break;
                }
            }
            if (!inDimension) continue;

            faceCount += surfaces.size();
            dimensionArenaIds.add(arenaUuid);
        }

        // 全インクデータから最大リビジョンを計算
        long maxRevision = 0;
        for (UUID arenaUuid : dimensionArenaIds) {
            long rev = getRevision(arenaUuid);
            if (rev > maxRevision) maxRevision = rev;
        }

        // BEGIN送信（リビジョンに基づきクライアント側で古いデータを無視しないようにする）
        ServerPlayNetworking.send(player, new InkSyncBeginPayload(
                dimensionId, sessionId, faceCount, maxRevision));

        Salmon.LOGGER.info("Full ink sync begin: player={} dim={} faces={} sessionId={} baseRevision={}",
                player.getUUID(), dimensionId, faceCount, sessionId, maxRevision);

        int sent = 0;
        for (UUID arenaUuid : dimensionArenaIds) {
            Map<InkSurfaceKey, InkFaceData> surfaces = allData.get(arenaUuid);
            if (surfaces == null) continue;

            // アリーナ検索
            InkArena foundArena = null;
            for (InkArena arena : InkArenaManager.getInstance().getArenasInDimension(level.dimension())) {
                if (arena.getArenaId().equals(arenaUuid)) {
                    foundArena = arena;
                    break;
                }
            }
            if (foundArena == null) continue;

            long revision = getRevision(arenaUuid);
            int arenaNumber = foundArena.getArenaNumber();

            for (var se : surfaces.entrySet()) {
                InkSurfaceKey key = se.getKey();
                InkFaceData faceData = se.getValue();
                if (faceData.isEmpty()) continue;

                InkFaceUpdatePayload payload = new InkFaceUpdatePayload(
                        arenaUuid,
                        arenaNumber,
                        dimensionId,
                        key.blockPos(),
                        key.patchId().normal().getSerializedName(),
                        key.patchId(),
                        faceData.copyCells(),
                        revision
                );
                ServerPlayNetworking.send(player, payload);
                sent++;
            }
        }

        // END送信
        ServerPlayNetworking.send(player, new InkSyncEndPayload(sessionId, sent));

        Salmon.LOGGER.info("Full ink sync end: player={} dim={} sessionId={} sent={}",
                player.getUUID(), dimensionId, sessionId, sent);
    }
}