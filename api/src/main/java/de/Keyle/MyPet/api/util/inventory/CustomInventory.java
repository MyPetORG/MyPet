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

package de.Keyle.MyPet.api.util.inventory;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.util.io.BukkitObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.*;

/**
 * API-level CustomInventory implementation backed by Spigot's Inventory API only.
 * No NMS or CraftBukkit classes are referenced.
 */
public class CustomInventory implements Listener {

    private String inventoryName = "";
    private int size = 0;
    private int stackSize = 64;
    private final List<HumanEntity> transaction = new ArrayList<>();
    private Inventory bukkitInventory = null;

    public CustomInventory() {
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
    }

    public CustomInventory(int size, String name) {
        this();
        setSize(size);
        setName(name);
    }

    /** API preserved: previously declared on interface **/
    public void setSize(int size) {
        int normalized = (size / 9) * 9;
        this.size = Util.clamp(Math.max(normalized, 9), 9, 54);
        if (bukkitInventory != null) {
            List<ItemStack> old = Arrays.asList(bukkitInventory.getContents());
            createInventoryIfNeeded();
            for (int i = 0; i < Math.min(old.size(), this.size); i++) {
                bukkitInventory.setItem(i, old.get(i));
            }
        } else {
            createInventoryIfNeeded();
        }
    }

    /** API preserved: previously declared on interface **/
    public String getName() {
        return inventoryName;
    }

    /** API preserved: previously declared on interface **/
    public void setName(String name) {
        if (name != null) {
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            this.inventoryName = name;
            if (bukkitInventory != null) {
                ItemStack[] contents = bukkitInventory.getContents();
                this.bukkitInventory = Bukkit.createInventory(null, this.size, this.inventoryName);
                this.bukkitInventory.setMaxStackSize(this.stackSize);
                this.bukkitInventory.setContents(contents);
            }
        }
    }

    private void createInventoryIfNeeded() {
        if (this.bukkitInventory == null || this.bukkitInventory.getSize() != this.size) {
            ItemStack[] contents = this.bukkitInventory != null ? this.bukkitInventory.getContents() : null;
            this.bukkitInventory = Bukkit.createInventory(null, this.size, this.inventoryName);
            this.bukkitInventory.setMaxStackSize(this.stackSize);
            if (contents != null) {
                this.bukkitInventory.setContents(contents);
            }
        }
    }

    /** Convenience accessors **/
    public int getContainerSize() {
        return this.size;
    }

    public ItemStack getItem(int i) {
        createInventoryIfNeeded();
        if (i < 0 || i >= this.size) return null;
        return this.bukkitInventory.getItem(i);
    }

    public void setItem(int i, ItemStack itemStack) {
        createInventoryIfNeeded();
        if (i < 0 || i >= this.size) return;
        this.bukkitInventory.setItem(i, itemStack);
        setChanged();
    }

    /** API preserved: previously declared on interface **/
    public int addItem(ItemStack itemAdd) {
        if (itemAdd == null || itemAdd.getType().isAir()) {
            return 0;
        }
        createInventoryIfNeeded();
        ItemStack toAdd = itemAdd.clone();
        Map<Integer, ItemStack> leftover = this.bukkitInventory.addItem(toAdd);
        int remaining = 0;
        for (ItemStack rem : leftover.values()) {
            if (rem != null) remaining += rem.getAmount();
        }
        return remaining;
    }

    /** API preserved: previously declared on interface **/
    public Inventory getBukkitInventory() {
        createInventoryIfNeeded();
        return bukkitInventory;
    }

    /** API preserved: previously declared on interface **/
    public void dropContentAt(Location loc) {
        createInventoryIfNeeded();
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack is = this.removeItemNoUpdate(i);
            if (is != null && !is.getType().isAir()) {
                world.dropItem(loc, is);
            }
        }
    }

    public ItemStack removeItem(int slot, int subtract) {
        createInventoryIfNeeded();
        if (slot < 0 || slot >= this.size) return null;
        ItemStack current = this.bukkitInventory.getItem(slot);
        if (current == null || current.getType().isAir()) return null;
        int take = Math.min(subtract, current.getAmount());
        ItemStack result = current.clone();
        result.setAmount(take);
        int remaining = current.getAmount() - take;
        if (remaining <= 0) {
            this.bukkitInventory.clear(slot);
        } else {
            current.setAmount(remaining);
            this.bukkitInventory.setItem(slot, current);
        }
        return result;
    }

    public List<ItemStack> getContents() {
        createInventoryIfNeeded();
        return new ArrayList<>(Arrays.asList(this.bukkitInventory.getContents()));
    }

    /** API preserved: previously declared on interface **/
    public TagCompound save(TagCompound compound) {
        createInventoryIfNeeded();
        List<TagCompound> itemList = new ArrayList<>();
        for (int i = 0; i < this.bukkitInventory.getSize(); i++) {
            ItemStack itemStack = this.bukkitInventory.getItem(i);
            if (itemStack != null && !itemStack.getType().isAir()) {
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

    /** API preserved: previously declared on interface **/
    public void load(TagCompound nbtTagCompound) {
        createInventoryIfNeeded();
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
            if (slot >= 0 && slot < this.size) {
                this.bukkitInventory.setItem(slot, itemStack);
            }
        }
    }

    /** Optional hooks retained without CraftBukkit types **/
    public void onOpen(HumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(HumanEntity who) {
        this.transaction.remove(who);
    }

    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(MyPetApi.getPlugin())) {
            close();
        }
    }

    /** API preserved: previously declared on interface **/
    public void close() {
        createInventoryIfNeeded();
        for (HumanEntity viewer : new ArrayList<>(getViewers())) {
            viewer.closeInventory();
        }
    }

    /** API preserved: previously declared on interface **/
    public void open(Player player) {
        createInventoryIfNeeded();
        player.openInventory(getBukkitInventory());
    }

    public List<HumanEntity> getViewers() {
        createInventoryIfNeeded();
        List<HumanEntity> v = this.bukkitInventory.getViewers();
        if (!transaction.isEmpty()) {
            Set<HumanEntity> merged = new LinkedHashSet<>(v);
            merged.addAll(transaction);
            return new ArrayList<>(merged);
        }
        return v;
    }

    public InventoryHolder getOwner() {
        return null;
    }

    public int getMaxStackSize() {
        createInventoryIfNeeded();
        return this.bukkitInventory.getMaxStackSize();
    }

    public void setMaxStackSize(int i) {
        this.stackSize = i;
        createInventoryIfNeeded();
        this.bukkitInventory.setMaxStackSize(i);
    }

    public Location getLocation() {
        return null;
    }

    public ItemStack removeItemNoUpdate(int i) {
        createInventoryIfNeeded();
        ItemStack current = this.bukkitInventory.getItem(i);
        if (current == null) return null;
        this.bukkitInventory.clear(i);
        return current;
    }

    public void setChanged() {
        // No-op for Bukkit inventory
    }

    public void clearContent() {
        createInventoryIfNeeded();
        this.bukkitInventory.clear();
    }

    public boolean isNotEmpty() {
        return !this.isEmpty();
    }

    public boolean isEmpty() {
        createInventoryIfNeeded();
        for (ItemStack is : this.bukkitInventory.getContents()) {
            if (is != null && !is.getType().isAir()) {
                return false;
            }
        }
        return true;
    }
}