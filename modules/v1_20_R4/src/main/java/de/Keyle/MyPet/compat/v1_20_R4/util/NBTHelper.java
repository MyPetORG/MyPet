package de.Keyle.MyPet.compat.v1_20_R4.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.craftbukkit.v1_20_R4.CraftRegistry;

public class NBTHelper {
    public static RegistryAccess registryAccess = CraftRegistry.getMinecraftRegistry();
    public static DataComponentType<CustomData> customData = DataComponents.CUSTOM_DATA;

    public static void setTag(ItemStack itemStack, CompoundTag tag) {
        if (tag == null) {
            itemStack.set(customData, null);
            return;
        }

        itemStack.set(customData, CustomData.of(tag));
    }

    public static CompoundTag getTag(ItemStack itemStack) {
        CustomData customData1 = itemStack.get(customData);
        if (customData1 == null) {
            return null;
        }
        return customData1.copyTag();
    }

    public static Tag save(ItemStack itemStack) {
        return itemStack.save(registryAccess);
    }

    public static Tag save(ItemStack itemStack, CompoundTag tag) {
        return itemStack.save(registryAccess, tag);
    }

    public static ItemStack parseItemStack(Tag tag) {
        return ItemStack.parse(registryAccess, tag).orElseThrow();
    }

    public static String serializeComponent(Component cm) {
        return Component.Serializer.toJson(cm, registryAccess);
    }
}