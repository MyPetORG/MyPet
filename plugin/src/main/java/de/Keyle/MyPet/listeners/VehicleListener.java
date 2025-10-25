/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import org.bukkit.Location;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class VehicleListener implements Listener {

    @EventHandler
    public void on(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && !(event.getVehicle() instanceof Horse)) {
            Player player = (Player) event.getEntered();
            if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                MyPet pet = MyPetApi.getMyPetManager().getMyPet(player);
                if (pet.getStatus() == MyPet.PetState.Here) {
                    pet.removePet(true);
                }
            }
        }
        if (event.getEntered() instanceof MyPetBukkitEntity) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && event.getEntity() instanceof Player) {
            if (event.getEntity().getVehicle() instanceof MyPetBukkitEntity) {
                Location loc = event.getEntity().getLocation();
                if (MyPetApi.getCompatUtil().isCompatible("1.12")) {
                    if (loc.getWorld().getWorldBorder().isInside(loc)) {
                        event.setCancelled(true);
                    }
                } else {
                    double locX = loc.getX();
                    double centerX = loc.getWorld().getWorldBorder().getCenter().getX();
                    double locZ = loc.getZ();
                    double centerZ = loc.getWorld().getWorldBorder().getCenter().getZ();
                    double borderSize = loc.getWorld().getWorldBorder().getSize();
                    if (locX < centerX + borderSize &&
                            locX > centerX - borderSize &&
                            locZ < centerZ + borderSize &&
                            locZ > centerZ - borderSize
                    ) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}