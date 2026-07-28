package yam.salmon.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import yam.salmon.Salmon;

/**
 * MODで使用するアイテムの登録。
 */
public class ModItems {

    public static final InkShooterItem INK_SHOOTER = new InkShooterItem();

    public static void register() {
        ResourceKey<Item> itemKey =
                ResourceKey.create(Registries.ITEM, Salmon.id("ink_shooter"));

        Registry.register(BuiltInRegistries.ITEM, itemKey, INK_SHOOTER);

        Salmon.LOGGER.info("Registered InkShooter item (salmon:ink_shooter)");
    }
}