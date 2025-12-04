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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Handles riding pets when players interact with them using the configured ride item.
 */
public class RideInteractListener implements Listener {

    private static boolean isOwner(Player player, MyPetBukkitEntity petEntity) {
        MyPet apiPet = petEntity.getMyPet();
        return apiPet != null && apiPet.getOwner() != null && apiPet.getOwner().getPlayer() != null
                && apiPet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRideWithConfiguredItem(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getRightClicked() instanceof MyPetBukkitEntity petEntity)) {
            return;
        }
        // Only consider main-hand interactions
        try {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                return;
            }
        } catch (NoSuchMethodError ignored) {
            // Fallback for versions without getHand() method (should not happen on 1.17+)
        }

        final Player player = event.getPlayer();

        if (!isOwner(player, petEntity)) {
            return;
        }

        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }

        MyPet myPet = petEntity.getMyPet();
        if (myPet == null || !petEntity.canMove() || !myPet.getSkills().isActive(Ride.class)) {
            return;
        }
        if (!Permissions.hasExtended(player, "MyPet.extended.ride")) {
            myPet.getOwner().sendMessage(Translation.getComponent("Message.No.CanUse", myPet.getOwner()), 2000);
            return;
        }

        // Use Bukkit API for riding (available since 1.11)
        if (!petEntity.getPassengers().contains(player)) {
            boolean mounted = petEntity.addPassenger(player);
            if (mounted) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonitorRideFinisher(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof MyPetBukkitEntity petEntity)) {
            return;
        }
        try {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                return;
            }
        } catch (NoSuchMethodError ignored) {
        }
        final Player player = event.getPlayer();
        if (!isOwner(player, petEntity)) {
            return;
        }
        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (event.isCancelled() && !petEntity.getPassengers().contains(player)) {
            event.setCancelled(false);
        }
    }
}
