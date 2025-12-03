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
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * API-only inventory implementation
 * <p>
 * This class provides methods for sizing, naming, persisting (to K-NBT),
 * and opening/closing the GUI for players. It avoids direct CraftBukkit/NMS
 * usage and should work across modern Bukkit/Paper versions.
 * </p>
 */
public class CustomInventory implements InventoryHolder {
    /**
     * Bukkit inventory that stores the actual contents.
     */
    private Inventory bukkitInventory;

    /**
     * Current display name (title) of the inventory GUI.
     * Defaults to "Inventory".
     */
    @Getter
    private String name = "Inventory";
    /**
     * Logical size of the inventory in slots. Always a multiple of 9 in the
     * inclusive range [9, 54].
     * Defaults to null.
     */
    @Getter
    private int size;
    /**
     * Max stack size applied to the backing Bukkit inventory.
     * Defaults to 64.
     */
    private int stackSize = 64;
    /**
     * Auxiliary viewer list used to track open/close when the backing inventory
     * may not yet be initialized.
     */
    private final List<HumanEntity> transaction = new ArrayList<>();


    /**
     * Track instances and register a single shared shutdown listener (no per-instance listeners)
     */
    private static final Set<CustomInventory> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean LISTENER_REGISTERED = false;
    private static final Listener SHUTDOWN_LISTENER = new Listener() {
        @EventHandler
        public void onPluginDisable(PluginDisableEvent e) {
            if (!e.getPlugin().equals(MyPetApi.getPlugin())) return;
            closeAllOpen();
            INSTANCES.clear();
            LISTENER_REGISTERED = false;
        }
    };

    /**
     * Ensures the shared shutdown listener is registered exactly once for the plugin.
     * Thread-safe to guard against concurrent initialization.
     */
    private static synchronized void ensureListener() {
        if (!LISTENER_REGISTERED) {
            Bukkit.getPluginManager().registerEvents(SHUTDOWN_LISTENER, MyPetApi.getPlugin());
            LISTENER_REGISTERED = true;
        }
    }

    /**
     * Closes all open inventories for all currently tracked CustomInventory instances.
     * Invoked on plugin disable by the shared shutdown listener.
     */
    static void closeAllOpen() {
        for (CustomInventory ci : new ArrayList<>(INSTANCES)) {
            if (ci != null) ci.close();
        }
    }

    /**
     * Creates a new CustomInventory instance and ensures the shared shutdown listener
     * is registered. The instance is also tracked for lifecycle management so that
     * all open inventories can be closed on plugin disable.
     */
    public CustomInventory() {
        ensureListener();
        INSTANCES.add(this);
    }

    /**
     * Convenience constructor to initialize size and name.
     *
     * @param size initial size in slots (will be normalized to a multiple of 9 and clamped [9,54])
     * @param name initial GUI title (will be truncated to 64 chars if longer)
     */
    public CustomInventory(int size, String name) {
        this();
        setSize(size);
        setName(name);
    }

    /**
     * Sets the inventory size in slots.
     * <p>
     * The value is clamped to a multiple of 9 between [9, 54]
     *
     * @param newSize desired slot count
     */
    public void setSize(int newSize) {
        newSize = (int) (newSize / 9.);
        newSize *= 9;
        this.size = Util.clamp(newSize, 9, 54);
        ItemStack[] old = bukkitInventory != null ? bukkitInventory.getContents() : null;

        this.bukkitInventory = Bukkit.createInventory(this, this.size, this.name);
        this.bukkitInventory.setMaxStackSize(this.stackSize);
        if (old != null) {
            for (int i = 0; i < Math.min(old.length, this.size); i++) {
                this.bukkitInventory.setItem(i, old[i]);
            }
        }
    }

    /**
     * Sets the inventory GUI title.
     * <p>
     * Renaming reconstructs the underlying Bukkit inventory to apply the new
     * title while preserving contents and max stack size.
     *
     * @param name new title (null is ignored)
     */
    public void setName(String name) {
        if (name == null) return;

        // Enforce reasonable title bounds and remove control chars
        String cleaned = name.replaceAll("\\p{Cntrl}", "");
        String newName = cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;

        if (Objects.equals(this.name, newName)) return;

        this.name = newName;

        // If no backing inventory yet, we only cache the title
        if (bukkitInventory == null) {
            return;
        }

        // Preserve contents and viewers, then rebuild inventory with the new title.
        ItemStack[] contents = bukkitInventory.getContents();
        List<HumanEntity> viewers = new ArrayList<>(bukkitInventory.getViewers());

        this.bukkitInventory = Bukkit.createInventory(this, this.size, this.name);
        this.bukkitInventory.setMaxStackSize(this.stackSize);
        this.bukkitInventory.setContents(contents);

        // Reopen for all viewers so the new title applies immediately.
        for (HumanEntity viewer : viewers) {
            if (viewer instanceof Player) {
                viewer.openInventory(this.bukkitInventory);
            }
        }
    }

    /**
     * Creates a Bukkit inventory for this CustomInventory instance if one has not been created already.
     */
    private void createInventoryIfNeeded() {
        if (this.size <= 0) {
            return;
        }
        if (this.bukkitInventory == null || this.bukkitInventory.getSize() != this.size || this.bukkitInventory.getHolder() != this) {
            ItemStack[] contents = this.bukkitInventory != null ? this.bukkitInventory.getContents() : null;
            this.bukkitInventory = Bukkit.createInventory(this, this.size, this.name);
            this.bukkitInventory.setMaxStackSize(this.stackSize);
            if (contents != null) {
                this.bukkitInventory.setContents(contents);
            }
        }
    }

    /**
     * Gets the item currently stored at the given slot index.
     *
     * @param i zero-based slot index
     * @return the item at the slot or null if out of bounds, uninitialized, or empty
     */
    public ItemStack getItem(int i) {
        if (this.bukkitInventory == null) return null;
        if (i < 0 || i >= this.size) return null;
        return this.bukkitInventory.getItem(i);
    }

    /**
     * Sets the item for the specified slot.
     *
     * @param i          zero-based slot index
     * @param itemStack  the item to set (may be null to clear)
     */
    public void setItem(int i, ItemStack itemStack) {
        createInventoryIfNeeded();
        if (this.bukkitInventory == null) return;
        if (i < 0 || i >= this.size) return;
        this.bukkitInventory.setItem(i, itemStack);
    }

    /**
     * Attempts to add the provided item stack to the inventory.
     * <p>
     * Delegates to Bukkit's Inventory#addItem and returns the number of items
     * that could not be inserted due to lack of space.
     *
     * @param itemAdd the stack to add (null or air results in 0 leftover)
     * @return the amount that could not be added (0 if fully inserted)
     */
    public int addItem(ItemStack itemAdd) {
        if (itemAdd == null || itemAdd.getType() == Material.AIR) {
            return 0;
        }
        createInventoryIfNeeded();
        if (this.bukkitInventory == null) {
            return itemAdd.getAmount();
        }
        ItemStack toAdd = itemAdd.clone();
        Map<Integer, ItemStack> leftover = this.bukkitInventory.addItem(toAdd);
        int remaining = 0;
        for (ItemStack rem : leftover.values()) {
            if (rem != null) remaining += rem.getAmount();
        }
        return remaining;
    }

    /**
     * Returns the underlying Bukkit Inventory instance for integration points
     * that require direct access.
     *
     * @return the backing Bukkit inventory, creating it if needed
     */
    public Inventory getBukkitInventory() {
        return getInventory();
    }

    /**
     * InventoryHolder contract. Returns the held Bukkit Inventory, creating it
     * lazily if the size has been set. The holder is always this instance.
     *
     * @return the held Bukkit inventory or null if size has not been set yet
     */
    @Override
    public Inventory getInventory() {
        return bukkitInventory;
    }

    /**
     * Drops all non-air contents of this inventory into the world at the given
     * location and clears the slots.
     *
     * @param loc the world location where items should be spawned (ignored if null or world missing)
     */
    public void dropContentAt(Location loc) {
        createInventoryIfNeeded();
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;
        for (int i = 0; i < this.getSize(); i++) {
            ItemStack is = this.removeItemNoUpdate(i);
            if (is != null && is.getType() != Material.AIR) {
                world.dropItem(loc, is);
            }
        }
    }

    /**
     * Removes up to the requested amount from the specified slot and returns
     * the removed items as a separate stack.
     *
     * @param slot     zero-based slot index
     * @param subtract the maximum number of items to take from the slot
     * @return a stack representing the removed items, or null if slot was empty/out of bounds
     */
    public ItemStack removeItem(int slot, int subtract) {
        createInventoryIfNeeded();
        if (this.bukkitInventory == null) return null;
        if (slot < 0 || slot >= this.size) return null;
        ItemStack current = this.bukkitInventory.getItem(slot);
        if (current == null || current.getType() == Material.AIR) return null;
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

    /**
     * Returns a copy of the current contents of the inventory.
     *
     * @return a mutable list containing the current slot contents (may include nulls)
     */
    public List<ItemStack> getContents() {
        if (this.bukkitInventory == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(this.bukkitInventory.getContents()));
    }

    /**
     * Serializes the inventory contents into the provided K-NBT compound.
     * <p>
     * Each non-empty slot is stored as a Base64-encoded Bukkit ItemStack under
     * the key "BukkitItem" along with its slot index.
     *
     * @param compound destination compound to write into (must not be null)
     * @return the same compound instance for chaining
     */
    public TagCompound save(TagCompound compound) {
        createInventoryIfNeeded();
        List<TagCompound> itemList = new ArrayList<>();
        if (this.bukkitInventory == null) {
            compound.getCompoundData().put("Items", new TagList(itemList));
            return compound;
        }
        for (int i = 0; i < this.bukkitInventory.getSize(); i++) {
            ItemStack itemStack = this.bukkitInventory.getItem(i);
            if (itemStack != null && itemStack.getType() != Material.AIR) {
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

    /**
     * Restores inventory contents from the provided K-NBT compound created by {@link #save(TagCompound)}.
     *
     * @param nbtTagCompound source compound containing an "Items" list
     */
    public void load(TagCompound nbtTagCompound) {
        createInventoryIfNeeded();
        TagList items = nbtTagCompound.getAs("Items", TagList.class);
        if (items == null) return;
        if (this.bukkitInventory == null) return;
        for (int i = 0; i < items.size(); i++) {
            TagCompound itemCompound = items.getTagAs(i, TagCompound.class);
            TagByte slotTag = itemCompound.getAs("Slot", TagByte.class);
            TagString b64Tag = itemCompound.getAs("BukkitItem", TagString.class);
            if (slotTag == null || b64Tag == null) {
                continue; // Skip items without required tags
            }
            int slot = slotTag.getByteData();
            String b64 = b64Tag.getStringData();
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

    /**
     * Should be called when a player opens this inventory to keep the viewer
     * list in sync while the underlying inventory may be lazily created.
     *
     * @param who the human entity opening the inventory
     */
    public void onOpen(HumanEntity who) {
        this.transaction.add(who);
    }

    /**
     * Should be called when a player closes this inventory to remove them from
     * the tracked viewer list.
     *
     * @param who the human entity closing the inventory
     */
    public void onClose(HumanEntity who) {
        this.transaction.remove(who);
    }


    /**
     * Closes this inventory for all current viewers, if any.
     */
    public void close() {
        for (HumanEntity viewer : new ArrayList<>(getViewers())) {
            viewer.closeInventory();
        }
    }

    /**
     * Opens this inventory GUI for the specified player.
     *
     * @param player the player who should see the inventory
     */
    public void open(Player player) {
        createInventoryIfNeeded();
        Inventory inv = getBukkitInventory();
        if (inv != null) {
            player.openInventory(inv);
        }
    }

    /**
     * Returns the current viewers of this inventory. If the backing inventory
     * has not been created yet, falls back to the internally tracked openers.
     *
     * @return a list of human entities currently viewing this inventory
     */

    public List<HumanEntity> getViewers() {
        if (bukkitInventory != null) return bukkitInventory.getViewers();
        return new ArrayList<>(transaction); // soft fallback, no allocation
    }

    /**
     * Returns the InventoryHolder for the backing inventory. Always {@code this}.
     *
     * @return this instance
     */
    public InventoryHolder getOwner() {
        return this;
    }

    /**
     * Gets the maximum stack size used by the backing Bukkit inventory.
     *
     * @return the max stack size (defaults to 64 before initialization)
     */
    public int getMaxStackSize() {
        return this.bukkitInventory != null ? this.bukkitInventory.getMaxStackSize() : this.stackSize;
    }

    /**
     * Sets the maximum stack size applied to the backing Bukkit inventory and
     * updates the cached value used before initialization.
     *
     * @param i the new max stack size
     */
    public void setMaxStackSize(int i) {
        this.stackSize = i;
        createInventoryIfNeeded();
        if (this.bukkitInventory != null) {
            this.bukkitInventory.setMaxStackSize(i);
        }
    }

    /**
     * Removes and returns the item currently in the specified slot without
     * triggering any additional logic.
     *
     * @param i zero-based slot index
     * @return the previous item in the slot or null if empty/out of bounds
     */
    public ItemStack removeItemNoUpdate(int i) {
        createInventoryIfNeeded();
        if (this.bukkitInventory == null) return null;
        ItemStack current = this.bukkitInventory.getItem(i);
        if (current == null) return null;
        this.bukkitInventory.clear(i);
        return current;
    }

    /**
     * Checks whether the inventory contains no items (all slots null or air).
     *
     * @return true if inventory has no meaningful items; false otherwise
     */
    public boolean isEmpty() {
        if (this.bukkitInventory == null) {
            return true;
        }
        for (ItemStack is : this.bukkitInventory.getContents()) {
            if (is != null && is.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }
}