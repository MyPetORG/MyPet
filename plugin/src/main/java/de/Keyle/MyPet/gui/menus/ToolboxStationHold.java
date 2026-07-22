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

package de.Keyle.MyPet.gui.menus;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.skills.Toolbox.Station;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Makes a Toolbox pet visibly hold the station's block while its GUI is open — a crafting
 * table for the crafting view, an anvil for the anvil, and so on — restoring the pet's hand
 * as soon as the view closes.
 *
 * <p>Station views are plain vanilla inventories (no {@code MenuInstanceImpl} holder), so
 * {@link de.Keyle.MyPet.gui.MenuDispatcher} skips them. This listener instead keys the hold
 * to the <em>viewer</em>, so any {@link InventoryCloseEvent} for a player who is holding a
 * station open puts the pet's hand back — whichever station it was. A singleton because
 * {@link ToolboxMenuHandler}'s open path is static.
 */
public final class ToolboxStationHold implements Listener {

    private static ToolboxStationHold instance;

    /** viewer id → the pet whose hand we borrowed and the item we stashed from it. */
    private final ConcurrentMap<UUID, Held> holds = new ConcurrentHashMap<>();
    private final Plugin plugin;

    private record Held(Mob mob, ItemStack stashed) {}

    private ToolboxStationHold(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Creates and registers the singleton; called once when the GUI service enables. */
    public static void register(Plugin plugin) {
        instance = new ToolboxStationHold(plugin);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
    }

    public static ToolboxStationHold get() {
        return instance;
    }

    /** Show {@code station}'s block in {@code pet}'s hand while {@code viewer} has its GUI open. */
    public void begin(Player viewer, Pet pet, Station station) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.getEquipment() == null) {
            return;
        }
        UUID id = viewer.getUniqueId();
        end(id); // clear any stale hold for this viewer before borrowing the hand again
        ItemStack display = new ItemStack(station.getIcon());
        if (Bukkit.isOwnedByCurrentRegion(mob)) {
            applyBegin(id, mob, display);
        } else {
            mob.getScheduler().run(plugin, task -> applyBegin(id, mob, display), null);
        }
    }

    private void applyBegin(UUID id, Mob mob, ItemStack display) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }
        holds.put(id, new Held(mob, equipment.getItemInMainHand()));
        equipment.setItemInMainHand(display);
    }

    /** Restores the pet's hand for {@code viewer} if it was holding a station open; else a no-op. */
    private void end(UUID viewer) {
        Held held = holds.remove(viewer);
        if (held == null) {
            return;
        }
        Mob mob = held.mob();
        if (!mob.isValid()) {
            return; // gone — a fresh entity gets fresh equipment on respawn anyway
        }
        if (Bukkit.isOwnedByCurrentRegion(mob)) {
            applyEnd(mob, held.stashed());
        } else {
            mob.getScheduler().run(plugin, task -> applyEnd(mob, held.stashed()), null);
        }
    }

    private static void applyEnd(Mob mob, ItemStack stashed) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(stashed);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            end(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // The pet despawns with its owner, so just drop the leaked hold entry.
        holds.remove(event.getPlayer().getUniqueId());
    }
}
