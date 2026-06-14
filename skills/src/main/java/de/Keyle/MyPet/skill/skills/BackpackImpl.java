/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.event.PetInventoryActionEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Locale;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Backpack Pet Skill
 * <p>
 * This skill provides a per-Pet expandable inventory that can be opened by the
 * owner. The size of the inventory is driven by the number of configured rows
 * (each row represents 9 slots). Contents are stored as a raw {@code ItemStack[]}
 * and exposed to the GUI layer via {@link #readContents(int)} and
 * {@link #writeContents(ItemStack[])}. A transient {@link CustomInventory} view
 * is available for drop-on-death and self-feeding integration points that have
 * not yet been migrated to the new menu system.
 */
public class BackpackImpl implements Backpack {

    /**
     * Number of inventory rows available to the Pet. Each row equals 9 slots.
     * This is controlled by the skill system and can change over time.
     */
    @Getter
    protected UpgradeComputer<Number> rows = new UpgradeComputer<>(0);

    /**
     * Determines whether the backpack contents should drop when the Pet dies.
     * The semantics are applied by listeners outside this class.
     */
    @Getter
    protected UpgradeComputer<Boolean> dropOnDeath = new UpgradeComputer<>(false);

    /**
     * Canonical persistent store for the backpack's items.
     * The GUI layer reads and writes via {@link #readContents(int)} /
     * {@link #writeContents(ItemStack[])}.
     */
    protected ItemStack[] contents = new ItemStack[0];

    /**
     * The owning Pet.
     */
    @Getter
    protected Pet pet;

    /**
     * Creates a new Backpack skill instance for the given Pet.
     *
     * @param pet owning Pet
     */
    public BackpackImpl(Pet pet) {
        this.pet = pet;
        // Shrink the persisted array whenever the granted row count decreases.
        // Items in newly-unreachable slots are packed into empty surviving slots;
        // anything that can't fit is dropped at the pet's location (or the owner's).
        //
        // Skips when newValue == 0: PetImpl.setSkilltree() calls Skill::reset on
        // every skill before re-applying the new tree, which transiently zeroes
        // the row count. Acting on that would destroy the entire backpack on
        // every skilltree switch. If rows actually settle at zero, items stay
        // in the array (inaccessible via menu, restored if rows upgrade later).
        rows.addCallback((newValue, reason) -> {
            int rowCount = newValue.intValue();
            if (rowCount <= 0) return;
            int target = rowCount * 9;
            if (contents.length <= target) return;
            shrinkTo(rowCount, resolveDropLocation());
        });
    }

    private Location resolveDropLocation() {
        Location petLoc = pet.getLocation().orElse(null);
        if (petLoc != null) return petLoc;
        if (pet.getOwner() != null && pet.getOwner().getPlayer() != null) {
            return pet.getOwner().getPlayer().getLocation();
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Menu-handler API
    // -----------------------------------------------------------------------

    /** Return up to {@code capacity} stacks for opening into a Backpack menu. */
    public ItemStack[] readContents(int capacity) {
        ItemStack[] out = new ItemStack[capacity];
        for (int i = 0; i < capacity && i < contents.length; i++) out[i] = contents[i];
        return out;
    }

    /** Persist the contents the player edited in the menu. */
    public void writeContents(ItemStack[] newContents) {
        this.contents = newContents.clone();
    }

    public int currentCapacity() { return contents.length; }

    public void ensureCapacity(int capacity) {
        if (contents.length < capacity) {
            ItemStack[] resized = new ItemStack[capacity];
            System.arraycopy(contents, 0, resized, 0, contents.length);
            contents = resized;
        }
    }

    /**
     * Shrinks the backpack to {@code rows} rows. Items in now-unreachable slots
     * are packed into empty slots in the remaining rows; whatever can't fit is
     * dropped at {@code dropAt}. After the call {@link #contents} is exactly
     * {@code rows * 9} long. No-op when the current array is already at or below
     * the target size.
     */
    public void shrinkTo(int rows, Location dropAt) {
        int target = Math.max(0, rows * 9);
        if (contents.length <= target) return;

        ItemStack[] kept = new ItemStack[target];
        System.arraycopy(contents, 0, kept, 0, target);

        World world = dropAt != null ? dropAt.getWorld() : null;
        for (int i = target; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            int empty = firstEmptySlot(kept);
            if (empty >= 0) {
                kept[empty] = item;
            } else if (world != null) {
                world.dropItem(dropAt, item);
            }
        }
        contents = kept;
    }

    private static int firstEmptySlot(ItemStack[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null || arr[i].getType().isAir()) return i;
        }
        return -1;
    }

    // -----------------------------------------------------------------------
    // Legacy integration: transient CustomInventory view
    // -----------------------------------------------------------------------

    /**
     * Returns a transient {@link CustomInventory} populated from the current
     * {@link #contents} array. Used by drop-on-death and self-feeding code
     * that has not yet been migrated to the new menu system.
     * <p>
     * Mutations to items inside this view (e.g., self-feeding decrementing
     * a stack) are NOT automatically written back to {@link #contents}; callers
     * that need write-back must call {@link #writeContents(ItemStack[])} after.
     */
    @Override
    public CustomInventory getInventory() {
        int capacity = Math.max(9, rows.getValue().intValue() * 9);
        CustomInventory inv = new CustomInventory();
        inv.setSize(capacity);
        for (int i = 0; i < capacity && i < contents.length; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i]);
            }
        }
        return inv;
    }

    /**
     * Drops all non-null, non-air items from {@link #contents} at the given location
     * and clears those slots. Callers should prefer this over
     * {@code getInventory().dropContentAt(loc)} because that method operates on a
     * transient view and does not write mutations back to {@link #contents}.
     *
     * @param loc the world location where items should be dropped
     */
    public void dropContents(Location loc) {
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                world.dropItem(loc, item);
                contents[i] = null;
            }
        }
    }

    /**
     * Closes the backpack. No-op in the new GUI system; open menus are managed
     * by the GuiService / MenuDispatcher lifecycle.
     */
    public void closeInventory() {
        // Menu lifecycle is now owned by GuiService; nothing to close here.
    }

    // -----------------------------------------------------------------------
    // Skill interface
    // -----------------------------------------------------------------------

    /**
     * Player-friendly string describing the number of rows, localized for the
     * provided locale code.
     *
     * @param locale the language/region code used for translations
     * @return a pretty, localized representation of the row count
     */
    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(rows.getValue().toString()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Rows", locale))
                .build();
    }

    /**
     * Message presented to the player when the backpack has been upgraded.
     * Includes the new slot count (rows * 9).
     *
     * @return an array of lines forming the upgrade message
     */
    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Locale.getFormattedComponent("Message.Skill.Inventory.Upgrade", pet.getOwner().getLanguage(), pet.getDisplayName(), getRows().getValue().intValue() * 9)
        };
    }

    /**
     * Validates environmental constraints before the caller opens the backpack
     * menu. Returns {@code true} if the conditions are met; the caller is then
     * responsible for opening the menu via {@code GuiService.openMenu(...)}.
     * <p>
     * Guards: rows > 0, owner not sleeping, creative restrictions, and the
     * {@link PetInventoryActionEvent} must not be cancelled.
     *
     * @return true if the backpack may be opened; false if a guard blocked it
     */
    @Override
    public boolean activate() {
        if (rows.getValue().intValue() > 0) {
            if (pet.getOwner().getPlayer().isSleeping()) {
                pet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", pet.getOwner()));
                return false;
            }
            if (pet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !Configuration.Skilltree.Skill.Backpack.OPEN_IN_CREATIVE && !Permissions.has(pet.getOwner().getPlayer(), "MyPet.admin")) {
                pet.getOwner().sendMessage(Locale.getComponent("Message.Skill.Inventory.Creative", pet.getOwner()));
                return false;
            }
            PetInventoryActionEvent event = new PetInventoryActionEvent(pet, PetInventoryActionEvent.Action.OPEN);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", pet.getOwner(), pet.getDisplayName()));
                return false;
            }
            if (pet.getLocation().isPresent()) {
                Location petLoc = pet.getLocation().get();
                // Reading the block at the pet's location requires owning that region on Folia.
                // If the player issued the command from a different region, skip the swim check
                // and allow opening — the "pet is swimming" guard is cosmetic, not a hard rule.
                boolean inLiquid = Bukkit.isOwnedByCurrentRegion(petLoc) && petLoc.getBlock().isLiquid();
                if (!inLiquid) {
                    return true;
                } else {
                    pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skill.Inventory.Swimming", pet.getOwner(), pet.getDisplayName()));
                    return false;
                }
            } else {
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skill.Inventory.Swimming", pet.getOwner(), pet.getDisplayName()));
                return false;
            }
        } else {
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skill.Inventory.NotAvailable", pet.getOwner(), pet.getDisplayName()));
            return false;
        }
    }

    /**
     * Indicates whether this skill is currently usable (i.e., at least one row
     * is available).
     *
     * @return true if the backpack has capacity; false otherwise
     */
    public boolean isActive() {
        return rows.getValue().intValue() > 0;
    }

    /**
     * Resets all upgrades applied to this skill (row count and drop-on-death).
     * Does not clear the inventory contents.
     */
    @Override
    public void reset() {
        rows.removeAllUpgrades();
        dropOnDeath.removeAllUpgrades();
    }

    @Override
    public Optional<State> getState() {
        return Optional.of(new State(contents.clone()));
    }

    @Override
    public void applyState(SkillState state) {
        if (state instanceof State bp) {
            contents = bp.contents() != null ? bp.contents().clone() : new ItemStack[0];
        }
    }
}
