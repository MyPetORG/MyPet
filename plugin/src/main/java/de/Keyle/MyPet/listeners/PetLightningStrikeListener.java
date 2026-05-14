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
import de.Keyle.MyPet.api.entity.PetLightningConvertible;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.repository.PetManager;
import org.bukkit.entity.Mob;
import org.bukkit.entity.MushroomCow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * Suppresses (or properly handles) vanilla lightning-bolt species conversions
 * on pet entities.
 *
 * <p>Vanilla {@code Mob#thunderHit} runs species-specific logic when a
 * {@code LightningBolt} intersects an entity — Creeper toggles to powered,
 * Pig converts to ZombifiedPiglin, Villager converts to Witch, red Mooshroom
 * flips to brown variant. Because v4 pets are real vanilla mobs (no NMS
 * wrappers), those paths now run against pets. Each path is gated by an
 * admin-configurable flag (default {@code false}, i.e. suppress); when the
 * flag is enabled, the pet is properly re-typed instead of leaving an
 * orphaned vanilla mob behind.
 *
 * <h2>The three shapes</h2>
 *
 * <ul>
 *   <li><b>Creeper power</b> ({@link CreeperPowerEvent} with
 *       {@code PowerCause.LIGHTNING}): vanilla mutates the {@code dataPowered}
 *       flag on the same entity. {@code PetEntitySnapshot} captures that
 *       state, so when admins opt in via
 *       {@code Configuration.MyPet.Creeper.ALLOW_LIGHTNING_POWER}, no rebind
 *       is needed — let the event proceed.</li>
 *
 *   <li><b>Pig / Villager type conversion</b> ({@link EntityTransformEvent}
 *       with {@code TransformReason.LIGHTNING}): vanilla discards the source
 *       entity and spawns a new entity of a different species. Handled by the
 *       {@link PetLightningConvertible} marker + the
 *       {@link PetZombificationListener}-style {@code convertPetType} flow.
 *       When admins opt in via
 *       {@code MyPet.Pets.<Type>.AllowLightningConversion}, the pet's domain
 *       object is re-typed to the new species, preserving UUID, name, XP,
 *       skills, and owner.</li>
 *
 *   <li><b>Mooshroom variant flip</b> ({@link EntityTransformEvent} where
 *       the source and target are both {@code MushroomCow}): {@code
 *       convertPetType} short-circuits on equal types, so the variant flip
 *       is replicated manually via {@code MushroomCow#setVariant} on the
 *       existing entity. Admins opt in via
 *       {@code Configuration.MyPet.Mooshroom.ALLOW_LIGHTNING_VARIANT_FLIP};
 *       the event is cancelled either way (the vanilla discard-and-respawn
 *       path is never desirable for a pet — we just optionally apply its
 *       observable effect).</li>
 * </ul>
 *
 * <p>For any future lightning-conversion entity Mojang adds that doesn't
 * match one of the cases above, the marked-pet check + transform handler
 * defaults to cancellation so pets stay protected.
 */
public class PetLightningStrikeListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetCreeperPower(CreeperPowerEvent event) {
        if (event.getCause() != CreeperPowerEvent.PowerCause.LIGHTNING) {
            return;
        }
        if (!PetEntityMarker.isMarked(event.getEntity())) {
            return;
        }
        if (!Configuration.MyPet.Creeper.ALLOW_LIGHTNING_POWER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetLightningTransform(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.LIGHTNING) {
            return;
        }
        if (!PetEntityMarker.isMarked(event.getEntity())) {
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getEntity());

        if (pet instanceof PetLightningConvertible convertible) {
            if (convertible.allowLightningConversion() && tryRetype(event, pet)) {
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof MushroomCow cow) {
            event.setCancelled(true);
            if (Configuration.MyPet.Mooshroom.ALLOW_LIGHTNING_VARIANT_FLIP) {
                cow.setVariant(cow.getVariant() == MushroomCow.Variant.RED
                        ? MushroomCow.Variant.BROWN
                        : MushroomCow.Variant.RED);
            }
            return;
        }

        // Unknown lightning-transform path on a marked pet. Suppress by
        // default so a future Mojang conversion mob doesn't orphan a wild
        // copy or destroy the pet's entity binding.
        event.setCancelled(true);
    }

    /**
     * Attempts the re-type path. Returns {@code true} if the conversion was
     * accepted (event left uncancelled, Pet domain object swapped to the new
     * species); {@code false} if the new type couldn't be resolved or the new
     * entity isn't a {@link Mob} — in which case the caller cancels.
     */
    private boolean tryRetype(EntityTransformEvent event, Pet oldPet) {
        if (!(event.getTransformedEntity() instanceof Mob newEntity)) {
            return false;
        }
        PetType newType;
        try {
            newType = PetType.byEntityTypeName(newEntity.getType().name());
        } catch (PetTypeNotFoundException e) {
            return false;
        }
        if (newType == null) return false;

        PetManager manager = (PetManager) MyPetApi.getPetManager();
        return manager.convertPetType(oldPet, newType, newEntity).isPresent();
    }
}
