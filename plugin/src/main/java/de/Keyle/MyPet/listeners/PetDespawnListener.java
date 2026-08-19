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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Ensures Pet entities are cleanly removed from the world at the right times.
 *
 * <p>Pet entities are spawned with {@code setPersistent(false)} + {@code setRemoveWhenFarAway(false)},
 * which means they stay alive in loaded chunks but are not saved to disk on
 * chunk unload. This listener handles two edge cases that the flags alone
 * don't cover:
 * <ol>
 *   <li><b>Owner disconnects.</b> The pet's Bukkit entity is removed and the
 *       {@link Pet} domain object's state is preserved for respawn on relogin
 *       via {@link Pet#removePet}.</li>
 *   <li><b>Chunk unload safety sweep.</b> Redundant with {@code persistent=false}
 *       but guarantees in-memory cleanup if any pet entities remain in the
 *       chunk at unload time.</li>
 * </ol>
 *
 */
public class PetDespawnListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Every Pet, not just the primary one: a Pet left behind here is an entity
        // that stays in the world after its owner logs out.
        for (Pet pet : MyPetApi.getPetManager().getPets(event.getPlayer())) {
            pet.removePet(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Sweep unconditionally: an orphaned marked entity (a despawn that failed to
        // remove its Bukkit entity, or one another plugin flipped to persistent) can
        // exist precisely when no pet is active, so gating on countActivePets()==0
        // would skip cleanup exactly when it's needed. isMarked is a cheap PDC check.
        for (Entity entity : event.getChunk().getEntities()) {
            if (PetEntityMarker.isMarked(entity)) {
                entity.remove();
            }
        }
    }
}
