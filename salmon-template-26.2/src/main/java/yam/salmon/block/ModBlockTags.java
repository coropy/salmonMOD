package yam.salmon.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import yam.salmon.Salmon;

/**
 * このMODで使用するブロックタグの定義。
 * Java コード内では {@code state.is(ModBlockTags.INK_PAINTABLE)} のように使用する。
 */
public final class ModBlockTags {

    /** 塗装可能なブロックを表すタグ (salmon:ink_paintable) */
    public static final TagKey<Block> INK_PAINTABLE = TagKey.create(
            Registries.BLOCK, Salmon.id("ink_paintable"));

    /** ツルハシで採掘可能なタグ (minecraft:mineable/pickaxe) */
    public static final TagKey<Block> MINEABLE_PICKAXE = TagKey.create(
            Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "mineable/pickaxe"));

    private ModBlockTags() {}
}