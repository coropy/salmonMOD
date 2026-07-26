package yam.salmon.arena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import yam.salmon.Salmon;
import yam.salmon.ink.InkStorage;
import yam.salmon.ink.InkFaceData;
import yam.salmon.ink.InkSurfaceKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * インクアリーナのディメンションごとの管理、作成、削除、永続化を担当する。
 */
public class InkArenaManager {

    private static final InkArenaManager INSTANCE = new InkArenaManager();

    /** アリーナサイズ制限 */
    public static final int MIN_EDGE_SIZE = 2;
    public static final int MAX_SIZE_X = 256;
    public static final int MAX_SIZE_Y = 128;
    public static final int MAX_SIZE_Z = 256;
    public static final long MAX_VOLUME = 4_194_304L;

    /** SavedData のバージョン。dataVersion=2 で arenaNumber 対応。dataVersion=3 でインクデータ保存。 */
    public static final int SAVED_DATA_VERSION = 3;

    /** インクデータストレージ */
    private final InkStorage inkStorage = new InkStorage();

    /** デバッグ表示が有効なプレイヤーUUIDのセット（サーバー側で管理） */
    private final Set<UUID> debugEnabledPlayers = ConcurrentHashMap.newKeySet();

    /** ディメンションごとのアリーナマップ */
    private final Map<ResourceKey<Level>, Map<UUID, InkArena>> arenasByDimension = new ConcurrentHashMap<>();

    /** マーカーUUID → 所属アリーナ の逆引き索引 */
    private final Map<UUID, UUID> markerToArena = new ConcurrentHashMap<>();

    /** 現在のサーバー参照（保存操作用） */
    private MinecraftServer currentServer;

    private InkArenaManager() {}

    public InkStorage getInkStorage() {
        return inkStorage;
    }

    /**
     * 現在のサーバーワールドのインクデータを強制的に保存する。
     */
    public void saveInkDataNow(ServerLevel level) {
        saveAllForDimension(level.dimension());
    }

    public static InkArenaManager getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------
    // ライフサイクル
    // -----------------------------------------------------------------------

    public void onServerStarted(MinecraftServer server) {
        this.currentServer = server;
        for (ServerLevel world : server.getAllLevels()) {
            loadDimensionData(world);
        }
        Salmon.LOGGER.info("InkArenaManager loaded arenas from all dimensions");
    }

    public void onServerStopping() {
        if (currentServer != null) {
            for (ServerLevel world : currentServer.getAllLevels()) {
                saveDimensionData(world);
            }
        }
        arenasByDimension.clear();
        markerToArena.clear();
        debugEnabledPlayers.clear();
        currentServer = null;
        Salmon.LOGGER.info("InkArenaManager saved all arena data");
    }

    // -----------------------------------------------------------------------
    // 永続化
    // -----------------------------------------------------------------------

    private void loadDimensionData(ServerLevel world) {
        ResourceKey<Level> dimension = world.dimension();
        SavedDataStorage storage = world.getDataStorage();
        SavedDataType<ArenaSavedData> type = ArenaSavedData.getType();
        ArenaSavedData data = storage.computeIfAbsent(type);

        // 旧データ移行: dataVersion < 2 の場合は arenaNumber を割り当てる
        if (data.dataVersion < 2) {
            data.migrateFromV1();
        }

        // インクデータのロード（dataVersion >= 3）
        if (data.dataVersion >= 3 && data.inkData != null) {
            inkStorage.importData(data.inkData);
            Salmon.LOGGER.info("Loaded ink data for dimension {}", dimension.identifier());
        }

        Map<UUID, InkArena> dimensionMap = new ConcurrentHashMap<>(data.getArenas());
        arenasByDimension.put(dimension, dimensionMap);

        // 逆引き索引を構築
        for (InkArena arena : dimensionMap.values()) {
            markerToArena.put(arena.getMarkerAId(), arena.getArenaId());
            markerToArena.put(arena.getMarkerBId(), arena.getArenaId());
        }

        // 番号重複チェック
        Map<Integer, UUID> numberCheck = new HashMap<>();
        for (InkArena arena : dimensionMap.values()) {
            UUID existing = numberCheck.put(arena.getArenaNumber(), arena.getArenaId());
            if (existing != null) {
                Salmon.LOGGER.warn("Duplicate arenaNumber {} detected: arenaIds={} and {}",
                        arena.getArenaNumber(), existing, arena.getArenaId());
            }
        }

        Salmon.LOGGER.info("Loaded {} arenas for dimension {} (nextArenaNumber={})",
                dimensionMap.size(), dimension.identifier(), data.nextArenaNumber);
    }

    private void saveDimensionData(ServerLevel world) {
        ResourceKey<Level> dimension = world.dimension();
        SavedDataStorage storage = world.getDataStorage();
        SavedDataType<ArenaSavedData> type = ArenaSavedData.getType();
        ArenaSavedData data = storage.computeIfAbsent(type);

        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(dimension);
        if (dimensionMap != null) {
            data.arenas.clear();
            data.arenas.putAll(dimensionMap);
        }

        // インクデータの保存
        if (inkStorage.hasData()) {
            data.inkData = inkStorage.exportData();
        } else {
            data.inkData = null;
        }

        data.dataVersion = SAVED_DATA_VERSION;
        data.setDirty();
    }

    private ArenaSavedData getOrCreateSavedData(ResourceKey<Level> dimension) {
        if (currentServer == null) return null;
        ServerLevel world = currentServer.getLevel(dimension);
        if (world == null) return null;
        SavedDataStorage storage = world.getDataStorage();
        SavedDataType<ArenaSavedData> type = ArenaSavedData.getType();
        return storage.computeIfAbsent(type);
    }

    // -----------------------------------------------------------------------
    // アリーナ番号 管理
    // -----------------------------------------------------------------------

    private int allocateArenaNumber(ResourceKey<Level> dimension) {
        Map<UUID, InkArena> dimMap = arenasByDimension.get(dimension);

        // 既存番号を収集し、欠番があれば最小のものを再利用する
        Set<Integer> usedNumbers = new HashSet<>();
        if (dimMap != null) {
            for (InkArena a : dimMap.values()) {
                usedNumbers.add(a.getArenaNumber());
            }
        }

        // 最小の欠番を探す（1から順に）
        int number = 1;
        while (usedNumbers.contains(number)) {
            number++;
            if (number <= 0) break; // オーバーフロー対策
        }

        // SavedData の nextArenaNumber を更新（欠番再利用時は変更不要だが、
        // 新規最大値の場合は次に備えて更新する）
        ArenaSavedData data = getOrCreateSavedData(dimension);
        if (data != null) {
            if (number >= data.nextArenaNumber) {
                data.nextArenaNumber = number + 1;
                if (data.nextArenaNumber <= 0) {
                    data.nextArenaNumber = 1;
                }
                data.setDirty();
                saveDimensionDataForDimension(dimension);
            }
        }

        Salmon.LOGGER.info("Allocated arenaNumber={} for dimension {} (used={}, nextArenaNumber={})",
                number, dimension.identifier(), usedNumbers,
                data != null ? data.nextArenaNumber : "N/A");
        return number;
    }

    // -----------------------------------------------------------------------
    // アリーナ操作
    // -----------------------------------------------------------------------

    public void addArena(ResourceKey<Level> dimension, InkArena arena) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
        dimensionMap.put(arena.getArenaId(), arena);
        markerToArena.put(arena.getMarkerAId(), arena.getArenaId());
        markerToArena.put(arena.getMarkerBId(), arena.getArenaId());
        saveAllForDimension(dimension);
        Salmon.LOGGER.info("Arena added: arenaId={}, arenaNumber={}, dim={}",
                arena.getArenaId(), arena.getArenaNumber(), dimension.identifier());
    }

    public InkArena findArenaByMarker(UUID markerId) {
        UUID arenaId = markerToArena.get(markerId);
        if (arenaId == null) return null;
        for (Map<UUID, InkArena> dimensionMap : arenasByDimension.values()) {
            InkArena arena = dimensionMap.get(arenaId);
            if (arena != null) return arena;
        }
        return null;
    }

    public Collection<InkArena> getArenasInDimension(ResourceKey<Level> dimension) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(dimension);
        if (dimensionMap == null) return Collections.emptyList();
        return Collections.unmodifiableCollection(dimensionMap.values());
    }

    public Collection<InkArena> getArenas(ServerLevel level) {
        return getArenasInDimension(level.dimension());
    }

    public Optional<InkArena> getArenaByNumber(ServerLevel level, int arenaNumber) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(level.dimension());
        if (dimensionMap == null) return Optional.empty();
        for (InkArena arena : dimensionMap.values()) {
            if (arena.getArenaNumber() == arenaNumber) {
                return Optional.of(arena);
            }
        }
        return Optional.empty();
    }

    public Optional<InkArena> findArenaContaining(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(findArenaContaining(level.dimension(), pos));
    }

    public InkArena findArenaContaining(ResourceKey<Level> dimension, BlockPos pos) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(dimension);
        if (dimensionMap == null) return null;
        for (InkArena arena : dimensionMap.values()) {
            if (arena.contains(pos)) return arena;
        }
        return null;
    }

    public InkArena findArenaContainingGlobal(BlockPos pos) {
        for (Map.Entry<ResourceKey<Level>, Map<UUID, InkArena>> entry : arenasByDimension.entrySet()) {
            for (InkArena arena : entry.getValue().values()) {
                if (arena.contains(pos)) return arena;
            }
        }
        return null;
    }

    public boolean removeArena(ResourceKey<Level> dimension, UUID arenaId) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(dimension);
        if (dimensionMap == null) return false;
        InkArena arena = dimensionMap.remove(arenaId);
        if (arena != null) {
            markerToArena.remove(arena.getMarkerAId());
            markerToArena.remove(arena.getMarkerBId());

            // インクデータも削除
            int removedSurfaces = inkStorage.removeArena(arena);
            Salmon.LOGGER.info("Arena removed: arenaId={}, arenaNumber={}, dim={}, inkSurfacesRemoved={}",
                    arena.getArenaId(), arena.getArenaNumber(), dimension.identifier(), removedSurfaces);

            saveAllForDimension(dimension);
            return true;
        }
        return false;
    }

    public boolean removeArenaByNumber(ServerLevel level, int arenaNumber) {
        Optional<InkArena> optArena = getArenaByNumber(level, arenaNumber);
        if (optArena.isPresent()) {
            InkArena arena = optArena.get();
            return removeArena(arena.getDimension(), arena.getArenaId());
        }
        return false;
    }

    public int getArenaCount(ResourceKey<Level> dimension) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(dimension);
        return dimensionMap == null ? 0 : dimensionMap.size();
    }

    public boolean hasIntersection(ResourceKey<Level> dimension, InkArena newArena) {
        Map<UUID, InkArena> dimensionMap = arenasByDimension.get(dimension);
        if (dimensionMap == null) return false;
        for (InkArena existing : dimensionMap.values()) {
            if (existing.intersects(newArena)) return true;
        }
        return false;
    }

    public boolean isMarkerUsed(UUID markerId) {
        return markerToArena.containsKey(markerId);
    }

    public boolean arenaExists(UUID arenaId) {
        for (Map<UUID, InkArena> dimensionMap : arenasByDimension.values()) {
            if (dimensionMap.containsKey(arenaId)) return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // デバッグ表示 管理
    // -----------------------------------------------------------------------

    public boolean enableDebug(ServerPlayer player) {
        boolean added = debugEnabledPlayers.add(player.getUUID());
        if (added) {
            Salmon.LOGGER.info("Debug display enabled for player: {} ({})",
                    player.getUUID(), player.getName().getString());
        }
        return added;
    }

    public boolean disableDebug(ServerPlayer player) {
        boolean removed = debugEnabledPlayers.remove(player.getUUID());
        if (removed) {
            Salmon.LOGGER.info("Debug display disabled for player: {} ({})",
                    player.getUUID(), player.getName().getString());
        }
        return removed;
    }

    public boolean isDebugEnabled(ServerPlayer player) {
        return debugEnabledPlayers.contains(player.getUUID());
    }

    public boolean isDebugEnabled(UUID playerId) {
        return debugEnabledPlayers.contains(playerId);
    }

    public void onPlayerDisconnect(UUID playerId) {
        if (debugEnabledPlayers.remove(playerId)) {
            Salmon.LOGGER.info("Debug display auto-disabled for disconnected player: {}", playerId);
        }
    }

    // -----------------------------------------------------------------------
    // 内部保存ヘルパー
    // -----------------------------------------------------------------------

    private void saveAllForDimension(ResourceKey<Level> dimension) {
        if (currentServer == null) return;
        ServerLevel world = currentServer.getLevel(dimension);
        if (world != null) {
            saveDimensionData(world);
        }
    }

    private void saveDimensionDataForDimension(ResourceKey<Level> dimension) {
        saveAllForDimension(dimension);
    }

    // -----------------------------------------------------------------------
    // 外部向け便利メソッド
    // -----------------------------------------------------------------------

    public Optional<InkArena> getArenaByMarker(UUID markerId) {
        return Optional.ofNullable(findArenaByMarker(markerId));
    }

    public boolean removeArenaByMarker(UUID markerId) {
        InkArena arena = findArenaByMarker(markerId);
        if (arena != null) {
            return removeArena(arena.getDimension(), arena.getArenaId());
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // アリーナ作成
    // -----------------------------------------------------------------------

    public record ArenaCreateResult(boolean success, @org.jetbrains.annotations.Nullable InkArena arena, String message) {}

    public ArenaCreateResult createArena(ResourceKey<Level> dimension,
                                         BlockPos posA, BlockPos posB,
                                         UUID markerAId, UUID markerBId) {
        int sizeX = Math.abs(posA.getX() - posB.getX()) + 1;
        int sizeY = Math.abs(posA.getY() - posB.getY()) + 1;
        int sizeZ = Math.abs(posA.getZ() - posB.getZ()) + 1;

        if (sizeX < MIN_EDGE_SIZE || sizeY < MIN_EDGE_SIZE || sizeZ < MIN_EDGE_SIZE) {
            return new ArenaCreateResult(false, null,
                    "アリーナの各辺は最小" + MIN_EDGE_SIZE + "ブロック以上必要です");
        }
        if (sizeX > MAX_SIZE_X || sizeY > MAX_SIZE_Y || sizeZ > MAX_SIZE_Z) {
            return new ArenaCreateResult(false, null,
                    "アリーナのサイズが上限を超えています (最大 X=" + MAX_SIZE_X + ", Y=" + MAX_SIZE_Y + ", Z=" + MAX_SIZE_Z + ")");
        }
        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > MAX_VOLUME) {
            return new ArenaCreateResult(false, null,
                    "アリーナの体積が上限(" + MAX_VOLUME + ")を超えています");
        }

        UUID arenaId = UUID.randomUUID();
        InkArena tempArena = new InkArena(arenaId, 0, dimension, posA, posB, markerAId, markerBId);

        if (hasIntersection(dimension, tempArena)) {
            return new ArenaCreateResult(false, null, "既存のアリーナと重複しています");
        }

        int arenaNumber = allocateArenaNumber(dimension);
        InkArena arena = new InkArena(arenaId, arenaNumber, dimension, posA, posB, markerAId, markerBId);

        addArena(dimension, arena);
        return new ArenaCreateResult(true, arena, "アリーナを作成しました");
    }

    // -----------------------------------------------------------------------
    // ArenaSavedData
    // -----------------------------------------------------------------------

    public static class ArenaSavedData extends SavedData {

        public static final Codec<ArenaSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("dataVersion", 1).forGetter(d -> d.dataVersion),
                Codec.INT.optionalFieldOf("nextArenaNumber", 1).forGetter(d -> d.nextArenaNumber),
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, InkArena.CODEC)
                        .fieldOf("arenas")
                        .forGetter(d -> d.arenas),
                InkStorage.STORAGE_CODEC.optionalFieldOf("inkData", null)
                        .forGetter(d -> d.inkData)
        ).apply(instance, ArenaSavedData::new));

        private static final SavedDataType<ArenaSavedData> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(Salmon.MOD_ID, "ink_arenas"),
                ArenaSavedData::new,
                ArenaSavedData.CODEC,
                DataFixTypes.SAVED_DATA_RAIDS
        );

        private int dataVersion;
        private int nextArenaNumber;
        private final Map<UUID, InkArena> arenas;
        private Map<UUID, Map<InkSurfaceKey, InkFaceData>> inkData;

        public ArenaSavedData() {
            this(1, 1, new HashMap<>(), null);
        }

        private ArenaSavedData(int dataVersion, int nextArenaNumber,
                                Map<UUID, InkArena> arenas,
                                Map<UUID, Map<InkSurfaceKey, InkFaceData>> inkData) {
            this.dataVersion = dataVersion;
            this.nextArenaNumber = nextArenaNumber;
            this.arenas = new HashMap<>(arenas);
            this.inkData = inkData;
        }

        public Map<UUID, InkArena> getArenas() {
            return Collections.unmodifiableMap(arenas);
        }

        public static SavedDataType<ArenaSavedData> getType() {
            return TYPE;
        }

        public void migrateFromV1() {
            if (dataVersion >= 2) {
                return;
            }

            Salmon.LOGGER.info("Migrating ArenaSavedData from v1 to v2 ({} arenas)", arenas.size());

            List<Map.Entry<UUID, InkArena>> sorted = new ArrayList<>(arenas.entrySet());
            sorted.sort(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)));

            Map<UUID, InkArena> migrated = new LinkedHashMap<>();
            int number = 1;
            for (Map.Entry<UUID, InkArena> entry : sorted) {
                InkArena oldArena = entry.getValue();
                InkArena newArena = new InkArena(
                        oldArena.getArenaId(),
                        number,
                        oldArena.getDimension(),
                        oldArena.getCornerA(),
                        oldArena.getCornerB(),
                        oldArena.getMarkerAId(),
                        oldArena.getMarkerBId()
                );
                migrated.put(entry.getKey(), newArena);
                number++;
            }

            arenas.clear();
            arenas.putAll(migrated);
            nextArenaNumber = number;
            dataVersion = SAVED_DATA_VERSION;
            setDirty();

            Salmon.LOGGER.info("Migration complete: {} arenas assigned numbers 1-{}, nextArenaNumber={}",
                    arenas.size(), number - 1, nextArenaNumber);
        }
    }
}