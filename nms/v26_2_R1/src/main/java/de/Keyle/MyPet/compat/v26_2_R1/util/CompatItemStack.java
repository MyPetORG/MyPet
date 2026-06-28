package de.Keyle.MyPet.compat.v26_2_R1.util;

import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

/**
 * Helper for NMS {@link ItemStack} → Bukkit conversions.
 * <p>
 * Paper 26.2 made {@code CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack)}
 * private (the public API now takes {@code ItemInstance}). The Spigot compile artifact still
 * exposes the old public overload, so calls compile fine but throw {@link IllegalAccessError}
 * at runtime. {@code asCraftMirror(ItemStack)} stays public, so mirroring a fresh copy
 * reproduces the old copy semantics without touching the private method.
 */
public final class CompatItemStack {
    private CompatItemStack() {}

    public static org.bukkit.inventory.ItemStack asBukkitCopy(ItemStack stack) {
        return CraftItemStack.asCraftMirror(stack.copy());
    }
}
