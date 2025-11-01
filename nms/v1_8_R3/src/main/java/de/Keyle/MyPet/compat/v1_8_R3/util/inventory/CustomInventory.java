/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License,
 * as published by the Free Software Foundation, either version 3 of the License, or
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

package de.Keyle.MyPet.compat.v1_8_R3.util.inventory;

import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagString;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 1.8.8-compatible override. Replaces Material#isAir usage with legacy checks.
 */
@Compat("v1_8_R3")
public class CustomInventory extends de.Keyle.MyPet.api.util.inventory.CustomInventory {

    public CustomInventory() {
        super();
    }

    public CustomInventory(int size, String name) {
        super(size, name);
    }

    private static boolean isAir(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR;
    }

    @Override
    public int addItem(ItemStack itemAdd) {
        if (isAir(itemAdd)) {
            return 0;
        }
        Inventory inv = getBukkitInventory();
        ItemStack toAdd = itemAdd.clone();
        Map<Integer, ItemStack> leftover = inv.addItem(toAdd);
        int remaining = 0;
        for (ItemStack rem : leftover.values()) {
            if (rem != null) remaining += rem.getAmount();
        }
        return remaining;
    }

    @Override
    public void dropContentAt(Location loc) {
        Inventory inv = getBukkitInventory();
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack is = removeItemNoUpdate(i);
            if (!isAir(is)) {
                world.dropItem(loc, is);
            }
        }
    }

    @Override
    public ItemStack removeItem(int slot, int subtract) {
        Inventory inv = getBukkitInventory();
        if (slot < 0 || slot >= inv.getSize()) return null;
        ItemStack current = inv.getItem(slot);
        if (isAir(current)) return null;
        int take = Math.min(subtract, current.getAmount());
        ItemStack result = current.clone();
        result.setAmount(take);
        int remaining = current.getAmount() - take;
        if (remaining <= 0) {
            inv.clear(slot);
        } else {
            current.setAmount(remaining);
            inv.setItem(slot, current);
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        Inventory inv = getBukkitInventory();
        for (ItemStack is : inv.getContents()) {
            if (!isAir(is)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TagCompound save(TagCompound compound) {
        Inventory inv = getBukkitInventory();
        List<TagCompound> itemList = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack itemStack = inv.getItem(i);
            if (!isAir(itemStack)) {
                TagCompound item = new TagCompound();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
                    oos.writeObject(itemStack);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String b64 = Base64.getEncoder().encodeToString(bos.toByteArray());
                item.getCompoundData().put("Slot", new TagByte((byte) i));
                item.getCompoundData().put("BukkitItem", new TagString(b64));
                itemList.add(item);
            }
        }
        compound.getCompoundData().put("Items", new TagList(itemList));
        return compound;
    }

    @Override
    public void load(TagCompound nbtTagCompound) {
        Inventory inv = getBukkitInventory();
        TagList items = nbtTagCompound.getAs("Items", TagList.class);
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            TagCompound itemCompound = items.getTagAs(i, TagCompound.class);
            int slot = itemCompound.getAs("Slot", TagByte.class).getByteData();
            String b64 = itemCompound.getAs("BukkitItem", TagString.class).getStringData();
            byte[] bytes = Base64.getDecoder().decode(b64);
            ItemStack itemStack = null;
            try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                Object obj = ois.readObject();
                if (obj instanceof ItemStack) {
                    itemStack = (ItemStack) obj;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, itemStack);
            }
        }
    }
}