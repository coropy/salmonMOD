package yam.salmon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import yam.salmon.Salmon;
import yam.salmon.arena.ArenaPermission;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.ink.InkFaceData;
import yam.salmon.ink.InkStorage;
import yam.salmon.ink.InkTeam;
import yam.salmon.network.ArenaDebugSync;
import yam.salmon.network.InkSyncManager;
import yam.salmon.team.TeamManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * /salmon arena コマンドを登録するクラス。
 */
public class SalmonCommands {

    private static final int PAGE_SIZE = 10;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext buildContext,
                                 Commands.CommandSelection selection) {

        dispatcher.register(
                Commands.literal("salmon")
                        .then(Commands.literal("arena")
                                .requires(source -> {
                                    if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                                        return ArenaPermission.canConfigure(player);
                                    }
                                    return source.permissions().hasPermission(
                                            new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
                                })
                                .then(Commands.literal("list")
                                        .executes(ctx -> listArenas(ctx, 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listArenas(ctx, IntegerArgumentType.getInteger(ctx, "page")))
                                        )
                                )
                                .then(Commands.literal("info")
                                        .then(Commands.argument("arenaNumber", IntegerArgumentType.integer(1))
                                                .executes(SalmonCommands::infoArena)
                                        )
                                )
                                .then(Commands.literal("here")
                                        .executes(SalmonCommands::hereArena)
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("arenaNumber", IntegerArgumentType.integer(1))
                                                .executes(SalmonCommands::removeArena)
                                        )
                                )
                                .then(Commands.literal("debug")
                                        .executes(SalmonCommands::toggleDebug)
                                        .then(Commands.literal("on")
                                                .executes(SalmonCommands::debugOn)
                                        )
                                        .then(Commands.literal("off")
                                                .executes(SalmonCommands::debugOff)
                                        )
                                        .then(Commands.literal("status")
                                                .executes(SalmonCommands::debugStatus)
                                        )
                                )
                        )
                        .then(Commands.literal("ink")
                                .requires(source -> {
                                    if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                                        return ArenaPermission.canConfigure(player);
                                    }
                                    return source.permissions().hasPermission(
                                            new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
                                })
                                .then(Commands.literal("inspect")
                                        .executes(SalmonCommands::inkInspect)
                                )
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("arenaNumber", IntegerArgumentType.integer(1))
                                                .executes(SalmonCommands::inkClear)
                                        )
                                )
                                .then(Commands.literal("debug")
                                        .executes(SalmonCommands::inkDebugToggle)
                                        .then(Commands.literal("on")
                                                .executes(SalmonCommands::inkDebugOn)
                                        )
                                        .then(Commands.literal("off")
                                                .executes(SalmonCommands::inkDebugOff)
                                        )
                                        .then(Commands.literal("status")
                                                .executes(SalmonCommands::inkDebugStatus)
                                        )
                                )
                        )
                        .then(Commands.literal("team")
                                .requires(source -> source.permissions().hasPermission(
                                        new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                .then(Commands.literal("assign")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.literal("A")
                                                        .executes(ctx -> assignTeam(ctx, InkTeam.TEAM_A))
                                                )
                                                .then(Commands.literal("B")
                                                        .executes(ctx -> assignTeam(ctx, InkTeam.TEAM_B))
                                                )
                                        )
                                )
                        )
        );
    }

    private static int assignTeam(CommandContext<CommandSourceStack> ctx, byte team)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        TeamManager.getInstance().assignTeam(target, team);

        source.sendSuccess(() -> Component.literal(
                "プレイヤー " + target.getName().getString()
                        + " を " + InkTeam.toName(team) + " に割り当てました"), true);
        Salmon.LOGGER.info("Command team assign: player={} -> {}",
                target.getUUID(), InkTeam.toName(team));
        return 1;
    }

    // -----------------------------------------------------------------------
    // list
    // -----------------------------------------------------------------------

    private static int listArenas(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        Collection<InkArena> arenas = InkArenaManager.getInstance().getArenas(level);

        if (arenas.isEmpty()) {
            source.sendSuccess(() -> Component.literal("登録済みインクアリーナ: 0件"), false);
            Salmon.LOGGER.info("Command list: 0 arenas in {}", level.dimension().identifier());
            return 0;
        }

        ArrayList<InkArena> sorted = new ArrayList<>(arenas);
        sorted.sort(Comparator.comparingInt(InkArena::getArenaNumber));

        int totalPages = (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page > totalPages) {
            page = totalPages;
        }

        final int p = page;
        int start = (p - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());

        source.sendSuccess(() -> Component.literal("登録済みインクアリーナ: " + sorted.size() + "件 (ページ " + p + "/" + totalPages + ")"), false);

        for (int i = start; i < end; i++) {
            InkArena arena = sorted.get(i);
            source.sendSuccess(() -> Component.literal(
                    "#" + arena.getArenaNumber()
                            + "  X=" + arena.getMin().getX() + ".." + arena.getMax().getX()
                            + ", Y=" + arena.getMin().getY() + ".." + arena.getMax().getY()
                            + ", Z=" + arena.getMin().getZ() + ".." + arena.getMax().getZ()
                            + "  [" + shortenUuid(arena.getArenaId()) + "]"
            ), false);
        }

        Salmon.LOGGER.info("Command list: page={}, {} arenas total in {}", p, sorted.size(), level.dimension().identifier());
        return sorted.size();
    }

    // -----------------------------------------------------------------------
    // info
    // -----------------------------------------------------------------------

    private static int infoArena(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        int arenaNumber = IntegerArgumentType.getInteger(ctx, "arenaNumber");

        Optional<InkArena> optArena = InkArenaManager.getInstance().getArenaByNumber(level, arenaNumber);

        if (optArena.isEmpty()) {
            source.sendFailure(Component.literal("アリーナ #" + arenaNumber + " は存在しません"));
            Salmon.LOGGER.warn("Command info: arena #{} not found in {}", arenaNumber, level.dimension().identifier());
            return 0;
        }

        InkArena arena = optArena.get();

        String markerAStatus = getMarkerStatus(level, arena.getMarkerAId(), arena.getCornerA());
        String markerBStatus = getMarkerStatus(level, arena.getMarkerBId(), arena.getCornerB());

        source.sendSuccess(() -> Component.literal("===== Arena #" + arena.getArenaNumber() + " ====="), false);
        source.sendSuccess(() -> Component.literal("UUID: " + arena.getArenaId()), false);
        source.sendSuccess(() -> Component.literal("Dimension: " + arena.getDimension().identifier()), false);
        source.sendSuccess(() -> Component.literal("Corner A: X=" + arena.getCornerA().getX()
                + ", Y=" + arena.getCornerA().getY()
                + ", Z=" + arena.getCornerA().getZ()), false);
        source.sendSuccess(() -> Component.literal("Corner B: X=" + arena.getCornerB().getX()
                + ", Y=" + arena.getCornerB().getY()
                + ", Z=" + arena.getCornerB().getZ()), false);
        source.sendSuccess(() -> Component.literal("Bounds: X=" + arena.getMin().getX()
                + ".." + arena.getMax().getX()
                + ", Y=" + arena.getMin().getY()
                + ".." + arena.getMax().getY()
                + ", Z=" + arena.getMin().getZ()
                + ".." + arena.getMax().getZ()), false);
        source.sendSuccess(() -> Component.literal("Size: " + arena.getSizeX()
                + " x " + arena.getSizeY()
                + " x " + arena.getSizeZ()), false);
        source.sendSuccess(() -> Component.literal("Volume: " + arena.getVolume()), false);
        source.sendSuccess(() -> Component.literal("Marker A: " + markerAStatus), false);
        source.sendSuccess(() -> Component.literal("Marker B: " + markerBStatus), false);

        Salmon.LOGGER.info("Command info: arena #{} in {}", arenaNumber, level.dimension().identifier());
        return 1;
    }

    // -----------------------------------------------------------------------
    // here
    // -----------------------------------------------------------------------

    private static int hereArena(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        Optional<InkArena> optArena = InkArenaManager.getInstance().findArenaContaining(level, pos);

        if (optArena.isPresent()) {
            InkArena arena = optArena.get();
            source.sendSuccess(() -> Component.literal("現在地はArena #" + arena.getArenaNumber() + "の内部です"), false);
            Salmon.LOGGER.info("Command here: player at {} is inside arena #{}", pos, arena.getArenaNumber());
        } else {
            source.sendSuccess(() -> Component.literal("現在地はどのインクアリーナにも含まれていません"), false);
            Salmon.LOGGER.info("Command here: player at {} is not inside any arena", pos);
        }
        return 0;
    }

    // -----------------------------------------------------------------------
    // remove
    // -----------------------------------------------------------------------

    private static int removeArena(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        int arenaNumber = IntegerArgumentType.getInteger(ctx, "arenaNumber");

        Optional<InkArena> optArena = InkArenaManager.getInstance().getArenaByNumber(level, arenaNumber);

        if (optArena.isEmpty()) {
            source.sendFailure(Component.literal("アリーナ #" + arenaNumber + " は存在しません"));
            Salmon.LOGGER.warn("Command remove: arena #{} not found in {}", arenaNumber, level.dimension().identifier());
            return 0;
        }

        InkArena arena = optArena.get();

        // ペアマーカーの所属情報を解除
        clearOtherMarker(level, arena.getMarkerAId(), arena.getCornerA());
        clearOtherMarker(level, arena.getMarkerBId(), arena.getCornerB());
        clearSelfMarker(level, arena.getMarkerAId(), arena.getCornerA());
        clearSelfMarker(level, arena.getMarkerBId(), arena.getCornerB());

        // 削除前にデバッグ通知用にコピーを保持
        InkArena oldArena = arena;
        boolean removed = InkArenaManager.getInstance().removeArenaByNumber(level, arenaNumber);

        if (removed) {
            source.sendSuccess(() -> Component.literal("Arena #" + arenaNumber + " を削除しました"), true);

            // 同次元の全デバッグ有効プレイヤーにブロードキャスト
            ArenaDebugSync.broadcastArenaRemoved(level, oldArena);

            Salmon.LOGGER.info("Command remove: arena #{} deleted in {}", arenaNumber, level.dimension().identifier());
        } else {
            source.sendFailure(Component.literal("アリーナ #" + arenaNumber + " の削除に失敗しました"));
        }
        return removed ? 1 : 0;
    }

    // -----------------------------------------------------------------------
    // debug
    // -----------------------------------------------------------------------

    private static int toggleDebug(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("プレイヤーのみ実行可能です"));
            return 0;
        }

        InkArenaManager manager = InkArenaManager.getInstance();
        if (manager.isDebugEnabled(player)) {
            manager.disableDebug(player);
            ArenaDebugSync.sendClear(player);
            source.sendSuccess(() -> Component.literal("デバッグ表示をOFFにしました"), false);
            Salmon.LOGGER.info("Command debug: OFF for player {}", player.getUUID());
        } else {
            manager.enableDebug(player);
            ArenaDebugSync.sendFullSync(player);
            source.sendSuccess(() -> Component.literal("デバッグ表示をONにしました"), false);
            Salmon.LOGGER.info("Command debug: ON for player {}", player.getUUID());
        }
        return 1;
    }

    private static int debugOn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("プレイヤーのみ実行可能です"));
            return 0;
        }

        InkArenaManager manager = InkArenaManager.getInstance();
        manager.enableDebug(player);
        ArenaDebugSync.sendFullSync(player);
        source.sendSuccess(() -> Component.literal("デバッグ表示をONにしました"), false);
        Salmon.LOGGER.info("Command debug on: player {}", player.getUUID());
        return 1;
    }

    private static int debugOff(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("プレイヤーのみ実行可能です"));
            return 0;
        }

        InkArenaManager manager = InkArenaManager.getInstance();
        manager.disableDebug(player);
        ArenaDebugSync.sendClear(player);
        source.sendSuccess(() -> Component.literal("デバッグ表示をOFFにしました"), false);
        Salmon.LOGGER.info("Command debug off: player {}", player.getUUID());
        return 1;
    }

    private static int debugStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("プレイヤーのみ実行可能です"));
            return 0;
        }

        boolean enabled = InkArenaManager.getInstance().isDebugEnabled(player);
        source.sendSuccess(() -> Component.literal("デバッグ表示: " + (enabled ? "ON" : "OFF")), false);
        return 1;
    }

    // -----------------------------------------------------------------------
    // ink inspect
    // -----------------------------------------------------------------------

    private static int inkInspect(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("プレイヤーのみ実行可能です"));
            return 0;
        }

        ServerLevel level = source.getLevel();

        // レイキャストで見ているブロックを取得
        var eyePos = player.getEyePosition();
        var lookVec = player.getLookAngle();
        var reachVec = eyePos.add(lookVec.x * 5.0, lookVec.y * 5.0, lookVec.z * 5.0);

        BlockHitResult hitResult = level.clip(new ClipContext(
                eyePos,
                reachVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("ブロックを見てください"));
            return 0;
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();

        // アリーナを検索
        Optional<InkArena> optArena = InkArenaManager.getInstance().findArenaContaining(level, pos);
        if (optArena.isEmpty()) {
            source.sendFailure(Component.literal("このブロックはインクアリーナ内にありません"));
            return 0;
        }

        InkArena arena = optArena.get();
        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
        InkFaceData faceData = inkStorage.getFaceOrEmpty(arena, pos, face);

        // ヘッダー表示
        source.sendSuccess(() -> Component.literal(
                "Arena #" + arena.getArenaNumber()
                        + " / Block=(" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                        + ") / Face=" + face
        ), false);

        // 8×8 グリッド表示
        StringBuilder sb = new StringBuilder();
        for (int v = 0; v < InkFaceData.GRID_SIZE; v++) {
            for (int u = 0; u < InkFaceData.GRID_SIZE; u++) {
                sb.append(yam.salmon.ink.InkTeam.toChar(faceData.getCell(u, v)));
            }
            if (v < InkFaceData.GRID_SIZE - 1) {
                source.sendSuccess(() -> Component.literal(sb.toString()), false);
                sb.setLength(0);
            }
        }
        // 最終行
        source.sendSuccess(() -> Component.literal(sb.toString()), false);

        Salmon.LOGGER.info("Command ink inspect: arena #{} block={} face={} player={}",
                arena.getArenaNumber(), pos, face, player.getUUID());
        return 1;
    }

    // -----------------------------------------------------------------------
    // ink clear
    // -----------------------------------------------------------------------

    private static int inkClear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        int arenaNumber = IntegerArgumentType.getInteger(ctx, "arenaNumber");

        Optional<InkArena> optArena = InkArenaManager.getInstance().getArenaByNumber(level, arenaNumber);
        if (optArena.isEmpty()) {
            source.sendFailure(Component.literal("アリーナ #" + arenaNumber + " は存在しません"));
            return 0;
        }

        InkArena arena = optArena.get();
        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
        int removed = inkStorage.clearArena(arena);

        // 保存
        InkArenaManager.getInstance().saveInkDataNow(level);

        // クライアント同期
        InkSyncManager.getInstance().broadcastArenaClear(level, arena);

        source.sendSuccess(() -> Component.literal(
                "Arena #" + arenaNumber + " のインクを消去しました: " + removed + "面"), true);

        Salmon.LOGGER.info("Command ink clear: arena #{} / {} surfaces removed by {}",
                arenaNumber, removed, source.getTextName());
        return 1;
    }

    // -----------------------------------------------------------------------
    // ink debug
    // -----------------------------------------------------------------------

    private static int inkDebugToggle(CommandContext<CommandSourceStack> ctx) {
        // クライアント側機能のため、サーバー側ではトグル制御用のフラグを
        // InkArenaManager経由で管理していない。代替としてコマンド成功通知のみ。
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(
                "/salmon ink debug on/off を使用してください（クライアント側診断表示）"), false);
        return 1;
    }

    private static int inkDebugOn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        // クライアント側の診断表示ONはPacketベースの専用機構がないため、
        // サーバー側からはログ出力で代替
        source.sendSuccess(() -> Component.literal(
                "インクデバッグ表示をONに設定しました（クライアント側のログ出力が有効になります）"), false);
        Salmon.LOGGER.info("Ink debug ON requested by {}", source.getTextName());
        return 1;
    }

    private static int inkDebugOff(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(
                "インクデバッグ表示をOFFに設定しました"), false);
        Salmon.LOGGER.info("Ink debug OFF requested by {}", source.getTextName());
        return 1;
    }

    private static int inkDebugStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(
                "インクデバッグ状態: クライアント側の /salmon ink debug on/off で制御"), false);
        return 1;
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private static String shortenUuid(UUID uuid) {
        String s = uuid.toString();
        return s.substring(0, 8) + "...";
    }

    private static String getMarkerStatus(ServerLevel level, UUID markerId, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return "未ロード";
        }
        if (level.getBlockEntity(pos) instanceof yam.salmon.block.InkAreaMarkerBlockEntity be) {
            if (be.getMarkerId().equals(markerId)) {
                return "存在";
            }
            return "不明 (UUID不一致)";
        }
        return "不明";
    }

    private static void clearOtherMarker(ServerLevel level, UUID markerId, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            Salmon.LOGGER.info("Other marker chunk not loaded for clear: pos={}, markerId={}", pos, markerId);
            return;
        }
        if (level.getBlockEntity(pos) instanceof yam.salmon.block.InkAreaMarkerBlockEntity be) {
            be.clearPairing();
        }
    }

    private static void clearSelfMarker(ServerLevel level, UUID markerId, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof yam.salmon.block.InkAreaMarkerBlockEntity be) {
            be.clearPairing();
        }
    }
}