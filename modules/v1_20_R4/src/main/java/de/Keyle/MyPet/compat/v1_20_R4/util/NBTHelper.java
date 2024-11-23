package de.Keyle.MyPet.compat.v1_20_R4.util;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class NBTHelper {
    public static DataComponentType<CustomData> customData = DataComponents.CUSTOM_DATA;

    // TODO: Change.
    public static void setTag(ItemStack itemStack, CompoundTag tag) {
        if (tag == null) {
            itemStack.set(customData, null);
            return;
        }

        itemStack.set(customData, CustomData.of(tag));
    }
}