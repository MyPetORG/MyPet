/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_21_R1.util.iconmenu;

import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_21_R1.util.NBTHelper;
import de.Keyle.MyPet.compat.v1_21_R1.util.inventory.CustomInventory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R1.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Compat("v1_21_R1")
public class IconMenuInventory implements de.Keyle.MyPet.api.gui.IconMenuInventory {

    private static Method applyToItemMethod = null;

    static {
        try {
            Class<?> craftMetaItemClass = Class.forName("org.bukkit.craftbukkit.v1_21_R1.inventory.CraftMetaItem");
            applyToItemMethod = ReflectionUtil.getMethod(craftMetaItemClass, "applyToItem", CompoundTag.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    CustomInventory minecraftInventory;
    int size = 0;

    public CustomInventory getMinecraftInventory() {
        return minecraftInventory;
    }

    @Override
    public void open(IconMenu menu, HumanEntity player) {
        size = menu.getSize();
        minecraftInventory = new CustomInventory(size, menu.getTitle());

        for (int slot = 0; slot < size; slot++) {
            IconMenuItem menuItem = menu.getOption(slot);
            if (menuItem != null) {
                ItemStack item = createItemStack(menuItem);
                minecraftInventory.setItem(slot, item);
            }
        }
        minecraftInventory.open((Player) player);
    }

    @Override
    public void update(IconMenu menu) {
        if (minecraftInventory != null) {
            for (int slot = 0; slot < size; slot++) {
                IconMenuItem menuItem = menu.getOption(slot);
                if (menuItem != null) {
                    ItemStack item = createItemStack(menuItem);
                    minecraftInventory.setItem(slot, item);
                } else {
                    minecraftInventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void close() {
        List<HumanEntity> viewers = new ArrayList<>(getViewers());
        for (HumanEntity viewer : viewers) {
            viewer.closeInventory();
        }
        minecraftInventory = null;
    }

    @Override
    public boolean isMenuInventory(Inventory inv) {
        return minecraftInventory != null && minecraftInventory.getBukkitInventory().equals(inv);
    }

    @Override
    public List<HumanEntity> getViewers() {
        if (minecraftInventory == null) {
            return Collections.emptyList();
        }
        return minecraftInventory.getBukkitInventory().getViewers();
    }

    @Override
    public int getSize() {
        return size;
    }

    protected ItemStack createItemStack(IconMenuItem icon) {
        ItemStack is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(icon.getMaterial(), icon.getAmount()));
        if (is == null) {
            is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.STONE));
        }

        if (NBTHelper.getTag(is) == null) {
            NBTHelper.setTag(is, new CompoundTag());
        }

        /*if (icon.getBukkitMeta() != null) {
            try {
                applyToItemMethod.invoke(icon.getBukkitMeta(), NBTHelper.getTag(is));
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }*/

        //add enchantment glowing
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        if (icon.isGlowing()) {
            Registry<Enchantment> registry = CraftRegistry.getMinecraftRegistry(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> optional = registry.getHolder(Enchantments.FEATHER_FALLING);
            enchantments.set((Holder) optional.get(), 1);
        }
        is.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());

        // hide item attributes like attack damage
        is.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);


        // set Title
        if (!icon.getTitle().equals("")) {
            is.set(DataComponents.CUSTOM_NAME, Component.literal(icon.getTitle()));
        }

        if (!icon.getLore().isEmpty()) {
            // set Lore
            List<Component> loreTag = new ArrayList<>();
            for (String loreLine : icon.getLore()) {
                if(loreLine.isEmpty())
                    loreLine = " ";
                Component cm = CraftChatMessage.fromStringOrNull(loreLine);
                loreTag.add(cm);
            }
            is.set(DataComponents.LORE, new ItemLore(loreTag));
        }

        /*
        if (icon.hasMeta()) {
            TagCompound tag = new TagCompound();
            icon.getMeta().applyTo(tag);
            CompoundTag vanillaTag = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(tag);
            for (String key : vanillaTag.getAllKeys()) {
                NBTHelper.getTag(is).put(key, vanillaTag.get(key));
            }
        }
        if (icon.getTags() != null) {
        	CompoundTag vanillaTag = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(icon.getTags());
            for (String key : vanillaTag.getAllKeys()) {
                NBTHelper.getTag(is).put(key, vanillaTag.get(key));
            }
        }
         */
        return is;
    }
}
