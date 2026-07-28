package yam.salmon.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import yam.salmon.Salmon;

import java.util.Set;

/**
 * MODで使用するブロック、ブロックアイテム、BlockEntityTypeの登録。
 * BlockEntityType はブロック登録後に構築する必要がある（Minecraft 26.2 では
 * BlockEntityType コンストラクタ内でブロックのレジストリIDを参照するため）。
 */
public class ModBlocks {

    public static final InkAreaMarkerBlock INK_AREA_MARKER_BLOCK = new InkAreaMarkerBlock();
    public static final InkableBlock INKABLE_BLOCK = new InkableBlock();

    /** register() 内でブロック登録後に初期化される（Block id not set 回避） */
    public static BlockEntityType<InkAreaMarkerBlockEntity> INK_AREA_MARKER_BLOCK_ENTITY;

    public static void register() {
        // ブロック用 ResourceKey
        ResourceKey<Block> blockKey =
                ResourceKey.create(Registries.BLOCK, Salmon.id("ink_area_marker"));

        // ブロック登録（BlockEntityType 構築より先に行う）
        Registry.register(BuiltInRegistries.BLOCK, blockKey, INK_AREA_MARKER_BLOCK);

        // ブロックアイテム生成と登録（Item.Properties に ResourceKey<Item> を設定）
        ResourceKey<Item> itemKey =
                ResourceKey.create(Registries.ITEM, Salmon.id("ink_area_marker"));

        Item.Properties itemProperties = new Item.Properties()
                .setId(itemKey)
                .useBlockDescriptionPrefix();

        BlockItem blockItem = new BlockItem(INK_AREA_MARKER_BLOCK, itemProperties);
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        // BlockEntityType 構築と登録（ブロック登録後に実行）
        INK_AREA_MARKER_BLOCK_ENTITY = new BlockEntityType<>(InkAreaMarkerBlockEntity::new, Set.of(INK_AREA_MARKER_BLOCK));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Salmon.id("ink_area_marker"),
                INK_AREA_MARKER_BLOCK_ENTITY);

        registerInkableBlock();

        Salmon.LOGGER.info("Registered InkAreaMarker block, item, and block entity");
    }

    private static void registerInkableBlock() {
        // ブロック用 ResourceKey
        ResourceKey<Block> blockKey =
                ResourceKey.create(Registries.BLOCK, Salmon.id("inkable_block"));

        // ブロック登録
        Registry.register(BuiltInRegistries.BLOCK, blockKey, INKABLE_BLOCK);

        // ブロックアイテム生成と登録
        ResourceKey<Item> itemKey =
                ResourceKey.create(Registries.ITEM, Salmon.id("inkable_block"));

        Item.Properties itemProperties = new Item.Properties()
                .setId(itemKey)
                .useBlockDescriptionPrefix();

        BlockItem blockItem = new BlockItem(INKABLE_BLOCK, itemProperties);
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        Salmon.LOGGER.info("Registered InkableBlock block and item (salmon:inkable_block)");
    }
}
