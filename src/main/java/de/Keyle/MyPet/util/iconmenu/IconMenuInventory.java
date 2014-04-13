/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util.iconmenu;

import net.minecraft.server.v1_7_R3.EntityHuman;
import net.minecraft.server.v1_7_R3.IInventory;
import net.minecraft.server.v1_7_R3.ItemStack;
import org.apache.commons.lang.Validate;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class IconMenuInventory {
    MinecraftInventory mi;
    CraftBukkitInventory cbi;

    public IconMenuInventory(int size, String title) {
        mi = new MinecraftInventory(size, title);
        cbi = new CraftBukkitInventory();
    }

    public MinecraftInventory getMinecraftInventory() {
        return mi;
    }

    public CraftBukkitInventory getCraftBukkitInventory() {
        return cbi;
    }

    class CraftBukkitInventory extends CraftInventory {
        public CraftBukkitInventory() {
            super(mi);
        }
    }

    class MinecraftInventory implements IInventory {
        protected final IconMenuItem[] items;
        private int maxStack = 64;
        private final List<HumanEntity> viewers;
        private final String title;

        public MinecraftInventory(int size, String title) {
            Validate.notNull(title, "Title cannot be null");
            Validate.isTrue(title.length() <= 32, "Title cannot be longer than 32 characters");
            this.items = new IconMenuItem[size];
            this.title = title;
            this.viewers = new ArrayList<HumanEntity>();
        }

        public int getSize() {
            return this.items.length;
        }

        public IconMenuItem getIcon(int position) {
            return this.items[position];
        }

        public ItemStack getItem(int position) {
            if (this.items[position] != null) {
                return this.items[position].createNmsItemStack();
            }
            return null;
        }

        public ItemStack splitStack(int position, int j) {
            update();
            return getItem(position);
        }

        public ItemStack splitWithoutUpdate(int position) {
            return getItem(position);
        }

        public void setItem(int i, ItemStack itemstack) {
        }

        public String getInventoryName() {
            return this.title;
        }

        public int getMaxStackSize() {
            return this.maxStack;
        }

        public void setMaxStackSize(int size) {
            this.maxStack = size;
        }

        public void update() {
        }

        public boolean a(EntityHuman entityhuman) {
            return true;
        }

        public ItemStack[] getContents() {
            ItemStack[] itemStacks = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) {
                itemStacks[i] = getItem(i);
            }
            return itemStacks;
        }

        public void onOpen(CraftHumanEntity who) {
            this.viewers.add(who);
        }

        public void onClose(CraftHumanEntity who) {
            this.viewers.remove(who);
        }

        public List<HumanEntity> getViewers() {
            return this.viewers;
        }

        public void l_() {
        }

        public InventoryHolder getOwner() {
            return null;
        }

        public void startOpen() {
        }

        public boolean k_() {
            return false;
        }

        public boolean b(int i, ItemStack itemstack) {
            return true;
        }
    }
}