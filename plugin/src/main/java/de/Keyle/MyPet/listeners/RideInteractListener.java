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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handles riding pets when players interact with them using the configured ride item.
 */
public class RideInteractListener implements Listener {

    private static boolean isOwner(Player player, Pet pet) {
        return pet != null && pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRideWithConfiguredItem(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!PetEntityMarker.isMarked(event.getRightClicked())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Player player = event.getPlayer();
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getRightClicked());
        if (pet == null) return;

        if (!isOwner(player, pet)) {
            return;
        }

        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (!pet.canMove() || !pet.getSkills().isActive(Ride.class)) {
            return;
        }
        if (!Permissions.hasExtended(player, "MyPet.extended.ride")) {
            pet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", pet.getOwner()), 2000);
            return;
        }

        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        if (!mob.getPassengers().contains(player)) {
            boolean mounted = mob.addPassenger(player);
            if (mounted) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonitorRideFinisher(PlayerInteractEntityEvent event) {
        if (!PetEntityMarker.isMarked(event.getRightClicked())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getRightClicked());
        if (pet == null) return;
        final Player player = event.getPlayer();
        if (!isOwner(player, pet)) {
            return;
        }
        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        if (event.isCancelled() && !mob.getPassengers().contains(player)) {
            event.setCancelled(false);
        }
    }
}
