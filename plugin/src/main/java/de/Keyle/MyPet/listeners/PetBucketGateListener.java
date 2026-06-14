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

import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEntityEvent;

/**
 * Prevents players from scooping a Pet into a bucket.
 *
 * <p>In v4 pets are real vanilla mobs, so vanilla's bucketable-mob logic now
 * applies to pets: right-clicking the pet with a water bucket would capture
 * the live entity into a filled bucket, orphaning or destroying the pet
 * binding. Vanilla surfaces that as {@link PlayerBucketEntityEvent} before the
 * capture; cancelling it for any marked pet closes every bucketable pet type
 * (Axolotl, Cod, Salmon, Pufferfish, TropicalFish, Tadpole, and any future
 * bucketable mob Mojang adds) in one hook.
 */
public class PetBucketGateListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
