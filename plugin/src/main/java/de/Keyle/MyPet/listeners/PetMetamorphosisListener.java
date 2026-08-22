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
import de.Keyle.MyPet.api.entity.PetMetamorphic;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.repository.PetManager;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tadpole;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * Keeps a metamorphosing pet — vanilla's Tadpole → Frog timer is the only case —
 * from being silently destroyed when it matures.
 *
 * <p>Without this handler the transform runs unhandled: vanilla discards the
 * tadpole entity and adds a plain, unowned Frog that inherits the custom name
 * (vanilla's {@code convertTo} copies it), leaving the owner with a mob wearing
 * their pet's name and a Pet whose entity vanished.
 *
 * <p>Two paths, gated on {@link PetMetamorphic#allowMetamorphosis()}:
 * <ul>
 *   <li><b>Allowed</b> — the event is left uncancelled and the Pet domain object
 *       is re-typed onto the entity vanilla just produced, via
 *       {@link PetManager#convertPetType}. UUID, name, XP, skilltree, skill
 *       state, health and the database row all carry across, so a matured pet
 *       is the same pet in a new body.</li>
 *   <li><b>Disallowed</b> (the default) — cancel and re-assert the age lock.</li>
 * </ul>
 *
 * <p><b>Why re-assert the lock after cancelling:</b> the lock is normally set at
 * spawn by {@code VanillaMobSpawner}, so reaching this handler at all means
 * something unlocked the timer (a third-party plugin, or a pet spawned before
 * this version). Vanilla only skips its {@code age++} tick while the lock is
 * set, so a bare cancel would let the event re-fire on the very next tick,
 * forever — the same trap {@code PetZombificationListener} closes with its
 * immunity flip. The age counter itself is left where it is: if an admin later
 * flips {@code AllowMetamorphosis} to {@code true}, an already-overdue tadpole
 * should mature promptly rather than serve another 20 minutes.
 */
public class PetMetamorphosisListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPetMetamorphosis(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.METAMORPHOSIS) return;
        if (!PetEntityMarker.isMarked(event.getEntity())) return;

        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getEntity());
        if (!(pet instanceof PetMetamorphic metamorphic)) return;

        if (metamorphic.allowMetamorphosis() && tryRetype(event, pet)) {
            return;
        }

        // Trap path: admin opted out, or the re-type failed because the new
        // entity didn't map to a registered PetType.
        event.setCancelled(true);
        if (event.getEntity() instanceof Tadpole tadpole) {
            tadpole.setAgeLock(true);
        }
    }

    /**
     * Attempts the re-type path. Returns {@code true} if the conversion was
     * accepted (event left uncancelled, Pet domain object swapped); {@code false}
     * if the new type couldn't be resolved or the new entity isn't a {@link Mob}
     * — in which case the caller falls back to the trap path.
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

        // Cast is safe: the active concrete manager is always plugin-side.
        PetManager manager = (PetManager) MyPetApi.getPetManager();
        return manager.convertPetType(oldPet, newType, newEntity).isPresent();
    }
}
