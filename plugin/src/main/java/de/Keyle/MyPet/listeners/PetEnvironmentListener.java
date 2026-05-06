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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPortalEvent;

/**
 * Vanilla-environment overrides for pet entities: spawn uncancel, portal
 * cancel, block-interaction cancel (farmland trampling, turtle egg
 * crushing), and snow-track suppression for SnowGolem pets.
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
     * mob spawns would prevent pets from appearing. Preserved verbatim from
     * the original {@code MyPetEntityListener}.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
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

    /**
     * Cancels snow-block placement by SnowGolem pets when
     * {@code MyPet.Pets.SnowGolem.DisableSnowTrack} is on. Vanilla
     * {@code SnowGolem#aiStep} places top-snow on grass/dirt in cold biomes
     * via {@code level().setBlockAndUpdate(...)}, which fires this event —
     * SnowGolem is the only vanilla mob that uses
     * {@link EntityBlockFormEvent}, so the marker check alone is sufficient.
     * The block-type guard is defensive in case Mojang ever extends the
     * event to other entities.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) {
            return;
        }
        if (event.getNewState().getType() == Material.SNOW
                && Configuration.MyPet.SnowGolem.DISABLE_SNOW_TRACK) {
            event.setCancelled(true);
        }
    }
}
