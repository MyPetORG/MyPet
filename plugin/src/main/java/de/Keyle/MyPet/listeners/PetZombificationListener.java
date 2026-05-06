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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.MyPetZombifiable;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.repository.PetManager;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * Handles the Overworld conversion of nether-native pet types
 * (Hoglin → Zoglin, Piglin → ZombifiedPiglin, PiglinBrute → ZombifiedPiglin).
 *
 * <p>Two paths, gated on {@link MyPetZombifiable#allowZombification()}:
 * <ul>
 *   <li><b>{@code true} (admin opted in):</b> let vanilla complete the
 *       conversion and re-type the MyPet domain object via
 *       {@link PetManager#convertPetType}. The original instance is
 *       discarded; a fresh instance of the new type takes its place with
 *       the same UUID, name, XP, skill state, and owner. If the re-type
 *       fails (no PetType registered for the target Bukkit type, or the
 *       transformed entity isn't a {@link Mob}), falls through to the trap
 *       path so the pet at least doesn't disappear.</li>
 *   <li><b>{@code false} (default):</b> shouldn't fire — the pet was made
 *       immune at spawn by
 *       {@link de.Keyle.MyPet.entity.visual.PetVisualSyncer}, so the
 *       conversion timer never reached threshold. Defensive cancel + flip
 *       immunity in case a third-party plugin flipped the flag mid-life.</li>
 * </ul>
 *
 * <p><b>Why the post-cancel immunity flip:</b> vanilla's
 * {@code timeInOverworld} counter keeps incrementing past the conversion
 * threshold once cancellation aborts {@code convertToZoglin} /
 * {@code finishConversion}, so the next tick fires
 * {@link EntityTransformEvent} again. The vanilla {@code aiStep} check
 * {@code if (!isImmuneToZombification() && ...) timeInOverworld++; else
 * timeInOverworld = 0;} only resets the counter when the immunity bit is
 * set — flipping it after the cancel stops the loop on first fire.
 */
public class PetZombificationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPetTransform(EntityTransformEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) return;

        MyPet pet = MyPetApi.getPetManager().getMyPetFromEntity(event.getEntity());
        if (!(pet instanceof MyPetZombifiable zombifiable)) return;

        if (zombifiable.allowZombification() && tryRetype(event, pet)) {
            return;
        }

        // Trap path: cancel + flip immunity so the conversion doesn't repeat.
        // Reaches here when the admin disallowed zombification (race with a
        // third-party plugin flipping immunity off mid-life), or when the
        // re-type failed because the new entity didn't map to a registered
        // PetType.
        event.setCancelled(true);
        if (event.getEntity() instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        } else if (event.getEntity() instanceof PiglinAbstract piglin) {
            piglin.setImmuneToZombification(true);
        }
    }

    /**
     * Attempts the re-type path. Returns {@code true} if the conversion
     * was accepted (event left uncancelled, MyPet domain object swapped);
     * {@code false} if the new type couldn't be resolved or the new entity
     * isn't a {@link Mob} — in which case the caller falls back to the
     * trap path.
     */
    private boolean tryRetype(EntityTransformEvent event, MyPet oldPet) {
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
