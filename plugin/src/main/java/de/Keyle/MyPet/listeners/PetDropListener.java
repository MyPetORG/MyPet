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
import de.Keyle.MyPet.api.entity.PetNaturalDrop;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;

/**
 * Suppresses periodic vanilla item drops from MyPet pets when the pet's own
 * per-type config flag is disabled.
 *
 * <p>In v4 pets are real vanilla mobs, so behaviors that previously had to be
 * driven from NMS overrides — chickens laying eggs, armadillos shedding
 * scutes, etc. — now run for free from vanilla AI ticks. The flags that used
 * to *enable* those behaviors therefore flip role: they now *suppress* the
 * vanilla path when set to {@code false}.
 *
 * <p>The listener is pet-agnostic: it dispatches via the
 * {@link PetNaturalDrop} marker interface. Adding a new periodic drop to
 * suppress is one {@code extends PetNaturalDrop} clause plus two default
 * methods on the relevant {@code Pet<Type>} api interface — no listener
 * changes required.
 *
 * <p>Wild mobs are unaffected — the {@link PetEntityMarker} check ensures
 * suppression applies only to MyPet pets.
 */
public class PetDropListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) {
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getEntity());
        if (!(pet instanceof PetNaturalDrop dropper)) {
            return;
        }
        if (!dropper.naturalDropMaterials().contains(event.getItemDrop().getItemStack().getType())) {
            return;
        }
        if (dropper.isNaturalDropSuppressed()) {
            event.setCancelled(true);
        }
    }
}
