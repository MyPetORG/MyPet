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
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Suppresses block destruction caused by a pet EnderDragon brushing terrain.
 *
 * <p>Vanilla {@code EnderDragon#aiStep} runs {@code checkWalls(box)} for every
 * sub-entity (head, neck, body, three tails, two wings) every tick and removes
 * any non-{@code DRAGON_IMMUNE} block their bounding boxes overlap. Paper's
 * patch on that method (see
 * {@code paper-server/patches/sources/.../EnderDragon.java.patch}) collects the
 * candidate blocks into a list and fires
 * {@link EntityExplodeEvent} with the dragon as source — cancelling the event
 * causes Paper to {@code return} without removing any block, which is exactly
 * what we want when {@link Configuration.MyPet.EnderDragon#ALLOW_BLOCK_DAMAGE}
 * is {@code false}.
 *
 * <p>Pet-scoped via {@link PetEntityMarker#isMarked} so vanilla End-fight
 * dragons (and anything else summoning an EnderDragon) keep their normal
 * brush-destruction behavior.
 */
public class PetEnderDragonBlockDamageListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPetDragonBrushDestruction(EntityExplodeEvent event) {
        if (Configuration.MyPet.EnderDragon.ALLOW_BLOCK_DAMAGE) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;
        if (!PetEntityMarker.isMarked(event.getEntity())) return;
        event.setCancelled(true);
    }
}
