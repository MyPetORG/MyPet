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
import de.Keyle.MyPet.entity.spawn.PetSpawnGuard;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPortalEvent;

/**
 * Vanilla-environment overrides for pet entities: spawn uncancel, portal
 * cancel, block-interaction cancel (farmland trampling, turtle egg
 * crushing).
 *
 * <p>These handlers are cross-pet (apply to every marked pet regardless of
 * type), so they live in a shared listener. SnowGolem-specific snow-track
 * suppression lives on {@code PetSnowGolem.SNOW_TRACK_SUPPRESS}.
 *
 * <p>These handlers have no dependencies on pet state, hooks, or skills —
 * they only need to know whether the entity is a pet via
 * {@link PetEntityMarker#isMarked}.
 */
public class PetEnvironmentListener implements Listener {

    /**
     * Force-uncancels pet spawn events that other plugins may have cancelled.
     *
     * <p><b>Note:</b> mutating event state at {@link EventPriority#MONITOR}
     * violates the Bukkit convention that MONITOR handlers are observe-only.
     * This is intentional — without it, protection plugins that blanket-cancel
     * mob spawns would prevent pets from appearing.
     *
     * <p>The {@link PetSpawnGuard} check covers a source-driven pet's provider
     * spawn (e.g. a MythicMob): that entity carries no {@link PetEntityMarker}
     * yet when this event fires, since MyPet has not adopted it. The guard is
     * only ever active around a summon, never a release — see its javadoc.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity()) || PetSpawnGuard.isActive()) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
            if (event.getBlock().getType() == Material.FARMLAND) {
                event.setCancelled(true);
            } else if ("TURTLE_EGG".equals(event.getBlock().getType().name())) {
                event.setCancelled(true);
            }
        }
    }

}
