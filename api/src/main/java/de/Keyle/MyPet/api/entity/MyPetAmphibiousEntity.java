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

package de.Keyle.MyPet.api.entity;

/**
 * Marker for pet types whose underlying vanilla mob swims in water but also
 * survives on land indefinitely — axolotl, drowned, frog, turtle. Sibling
 * to {@link MyPetAquaticEntity} under the shared {@link MyPetSwimmingEntity}
 * base.
 *
 * <p>No additional config gates today — this is the architectural anchor
 * for a future "dynamic land/water goal switching" pass (see the Frog
 * "doesn't move on land" entry in {@code docs/pet-type-issue-tracker.md}).
 * Reclassifying these four pets out of {@link MyPetAquaticEntity} into this
 * marker is the prerequisite for {@link PetType#isSwimmingPet()} and
 * {@code PetGoalInstaller} to grant them ground stroll / mêlée goals
 * alongside the swim path.
 *
 * <p>Drowned is not biologically amphibious — it's an undead variant that
 * happens to share the gameplay shape (swims naturally, doesn't suffocate
 * on land). Grouping it here matches the per-pet behavior even if the name
 * is a stretch.
 */
public interface MyPetAmphibiousEntity extends MyPetSwimmingEntity {
}
