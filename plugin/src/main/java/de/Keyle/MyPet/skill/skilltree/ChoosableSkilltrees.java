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

package de.Keyle.MyPet.skill.skilltree;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;

import java.util.ArrayList;
import java.util.List;

/**
 * The skilltrees a pet may see in the choose-skilltree menu, split into those it can pick right
 * now ({@link #available}) and those teased as locked ({@link #locked}) — a tree the pet fails
 * only on soft gates (a level requirement not yet reached, or a pending ascension whose
 * prerequisite tree the pet already sits on). Trees the pet's species can't use, or that fail a
 * hard requirement, are omitted entirely. Shared by the command and the pet-menu paths so both
 * surfaces classify identically.
 */
public record ChoosableSkilltrees(List<Skilltree> available, List<Skilltree> locked) {

    public static ChoosableSkilltrees forPet(Pet pet) {
        List<Skilltree> available = new ArrayList<>();
        List<Skilltree> locked = new ArrayList<>();
        int level = pet.getExperience().getLevel();
        for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
            if (!skilltree.getMobTypes().contains(pet.getPetType())) {
                continue;
            }
            List<String> failed = skilltree.getFailedRequirements(pet);
            boolean levelLocked = level < skilltree.getRequiredLevel();
            if (failed.isEmpty() && !levelLocked) {
                available.add(skilltree);
                continue;
            }
            // Teased-as-locked only when every unmet requirement is soft: a PetLevel gate, or a
            // Skilltree gate that IS an ascension for this pet (a failing Skilltree requirement
            // can only be soft when the pet already sits on the named prerequisite tree). Any
            // other failed requirement is a hard lock and the tree is hidden.
            boolean onlySoftLocks = failed.stream().allMatch(name ->
                    name.equalsIgnoreCase("PetLevel")
                    || (name.equalsIgnoreCase("Skilltree") && skilltree.isAscensionFor(pet)));
            if (onlySoftLocks) {
                locked.add(skilltree);
            }
        }
        return new ChoosableSkilltrees(available, locked);
    }

    public boolean isEmpty() {
        return available.isEmpty() && locked.isEmpty();
    }
}
