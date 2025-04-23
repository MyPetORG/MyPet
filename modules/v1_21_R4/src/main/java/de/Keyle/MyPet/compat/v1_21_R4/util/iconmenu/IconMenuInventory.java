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

package de.Keyle.MyPet.compat.v1_21_R4.util.iconmenu;

import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_21_R4.util.inventory.CustomInventory;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R4.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Compat("v1_21_R4")
public class IconMenuInventory implements de.Keyle.MyPet.api.gui.IconMenuInventory {

    private static Method applyToItemMethod = null;

    static {
        try {
            Class<?> craftMetaItemClass = Class.forName("org.bukkit.craftbukkit.v1_21_R4.inventory.CraftMetaItem");
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

        if (is.get(DataComponents.CUSTOM_DATA) == null) {
            is.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        }

        /*if (icon.getBukkitMeta() != null) {
            try {
                applyToItemMethod.invoke(icon.getBukkitMeta(), NBTHelper.getTag(is));
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }*/

        //add enchantment glowing
        if (icon.isGlowing()) {
            is.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        is.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, ReferenceLinkedOpenHashSet.of(
          DataComponents.ATTRIBUTE_MODIFIERS,
          DataComponents.DAMAGE,
          DataComponents.INSTRUMENT,
          DataComponents.MAP_ID,
          DataComponents.BLOCK_STATE,
          DataComponents.FIREWORKS,
          DataComponents.POTION_CONTENTS,
          DataComponents.TROPICAL_FISH_PATTERN,
          DataComponents.WRITTEN_BOOK_CONTENT
        )));

        ItemAttributeModifiers itemattributemodifiers = (ItemAttributeModifiers) is.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> newEntries = new ArrayList<>();
        for (ItemAttributeModifiers.Entry modifierEntry : itemattributemodifiers.modifiers()) {
            AttributeModifier modifier = modifierEntry.modifier();
            if (!modifier.is(Item.BASE_ATTACK_DAMAGE_ID) && !modifier.is(Item.BASE_ATTACK_SPEED_ID)) {
                newEntries.add(modifierEntry);
            }
        }
        is.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(newEntries));

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
            for (String key : vanillaTag.keySet()) {
                NBTHelper.getTag(is).put(key, vanillaTag.get(key));
            }
        }
        if (icon.getTags() != null) {
        	CompoundTag vanillaTag = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(icon.getTags());
            for (String key : vanillaTag.keySet()) {
                NBTHelper.getTag(is).put(key, vanillaTag.get(key));
            }
        }
         */
        return is;
    }
}
