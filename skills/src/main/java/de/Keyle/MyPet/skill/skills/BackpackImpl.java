/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagCompound;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Backpack Pet Skill
 * <p>
 * This skill provides a per-Pet expandable inventory that can be opened by the
 * owner. The size of the inventory is driven by the number of configured rows
 * (each row represents 9 slots). Additional features include persisting the
 * inventory via NBT, reacting to environment and permission constraints when
 * opening the inventory, and optional drop behavior on player death.
 * <p>
 * Robustness: All open operations validate environmental constraints (sleeping,
 * creative-mode limitations, location restrictions) and honor plugin events so
 * other components can veto the action. The inventory size is automatically
 * recalculated when the row-upgrade changes via a callback.
 */
public class BackpackImpl implements de.Keyle.MyPet.api.skill.skills.Backpack {

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
     * Backing inventory implementation, provided by a version-dependent compat layer.
     * Returns the underlying CustomInventory for direct interactions (e.g., drop,
     * set/get items, or querying the Bukkit inventory).
     */
    @Getter
    protected CustomInventory inventory;

    /** The owning Pet. */
    @Getter
    protected MyPet myPet;

    /**
     * Creates a new Backpack skill instance for the given Pet and wires the size
     * callback so that inventory capacity follows the number of configured rows.
     *
     * @param myPet owning Pet
     */
    public BackpackImpl(MyPet myPet) {
        this.myPet = myPet;
        inventory = MyPetApi.getCompatUtil().getCompatInstance(CustomInventory.class, "util.inventory", "CustomInventory");
        // Ensure inventory reflects current row count immediately
        if(rows.getValue().intValue() > 0) {
            inventory.setSize(rows.getValue().intValue() * 9);
            // Keep size in sync with future row changes
            rows.addCallback((newValue, reason) -> this.inventory.setSize(newValue.intValue() * 9));
        }
    }

    /**
     * Player-friendly string describing the number of rows, localized for the
     * provided locale code.
     *
     * @param locale the language/region code used for translations
     * @return a pretty, localized representation of the row count
     */
    public String toPrettyString(String locale) {
        return "" + ChatColor.GOLD + rows.getValue() + ChatColor.RESET + " " + Translation.getString("Name.Rows", locale);
    }

    /**
     * Message presented to the player when the backpack has been upgraded.
     * Includes the new slot count (rows * 9).
     *
     * @return an array of lines forming the upgrade message
     */
    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Inventory.Upgrade", myPet.getOwner()), myPet.getPetName(), getRows().getValue().intValue() * 9)
        };
    }

    /**
     * Tries to open the backpack for the owning player. This performs multiple
     * checks:
     * - The backpack must have at least one row.
     * - The owner must not be sleeping.
     * - Creative mode may be restricted by configuration/permissions.
     * - The current location must allow opening (not inside liquid blocks).
     * - Other plugins may cancel via MyPetInventoryActionEvent.
     *
     * @return true if the backpack was opened; false otherwise
     */
    public boolean activate() {
        if (rows.getValue().intValue() > 0) {
            if (myPet.getOwner().getPlayer().isSleeping()) {
                myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner()));
                return false;
            }
            if (myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !Configuration.Skilltree.Skill.Backpack.OPEN_IN_CREATIVE && !Permissions.has(myPet.getOwner().getPlayer(), "MyPet.admin", false)) {
                myPet.getOwner().sendMessage(Translation.getString("Message.Skill.Inventory.Creative", myPet.getOwner()));
                return false;
            }
            MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Open);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", myPet.getOwner()), myPet.getPetName()));
                return false;
            }
            if (myPet.getLocation().isPresent() && !myPet.getLocation().get().getBlock().isLiquid()) {
                openInventory(myPet.getOwner().getPlayer());
                return true;
            } else {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Inventory.Swimming", myPet.getOwner()), myPet.getPetName()));
                return false;
            }
        } else {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Inventory.NotAvailable", myPet.getOwner()), myPet.getPetName()));
            return false;
        }
    }

    /**
     * Opens the underlying inventory GUI for the given player, after setting the
     * inventory title to the Pet's current name.
     *
     * @param p the player for whom to open the inventory
     */
    public void openInventory(Player p) {
        inventory.setName(myPet.getPetName());
        inventory.open(p);
    }

    /**
     * Closes the backpack inventory if it is currently open.
     */
    public void closeInventory() {
        inventory.close();
    }

    /**
     * Restores the inventory state from the provided NBT tag.
     *
     * @param tag the tag to load from
     */
    public void load(TagCompound tag) {
        // Ensure the underlying inventory exists with correct capacity before loading items
        inventory.setSize(rows.getValue().intValue() * 9);
        inventory.load(tag);
    }

    /**
     * Serializes the current inventory state into a new NBT tag.
     *
     * @return an NBT tag containing the inventory contents and metadata
     */
    public TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        inventory.save(nbtTagCompound);
        return nbtTagCompound;
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

    /**
     * Debug-friendly representation of the current skill state.
     */
    @Override
    public String toString() {
        return "BackpackImpl{" +
                "rows=" + rows +
                ", dropOnDeath=" + dropOnDeath +
                '}';
    }
}