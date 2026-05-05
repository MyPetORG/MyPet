/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.skill.experience.modifier;

import de.Keyle.MyPet.api.Configuration.LevelSystem.Experience.Modifier;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Player;

/**
 * An experience modifier that grants bonus experience based on the pet owner's permissions.
 *
 * <p>When the permission-based modifier feature is enabled in configuration
 * ({@link Modifier#PERMISSION}), this modifier checks the pet owner's permissions for
 * tiered multiplier nodes of the form {@code MyPet.experience.multiplier.<percent>}.
 * The highest matching tier is applied (checked from 250 down to 125):
 *
 * <ul>
 *   <li>{@code MyPet.experience.multiplier.250} -- 2.5x</li>
 *   <li>{@code MyPet.experience.multiplier.225} -- 2.25x</li>
 *   <li>{@code MyPet.experience.multiplier.200} -- 2.0x</li>
 *   <li>{@code MyPet.experience.multiplier.175} -- 1.75x</li>
 *   <li>{@code MyPet.experience.multiplier.150} -- 1.5x</li>
 *   <li>{@code MyPet.experience.multiplier.125} -- 1.25x</li>
 * </ul>
 *
 * <p>If the feature is disabled, or the owner is offline, or no permission node matches,
 * the experience passes through unchanged.
 */
public class PermissionModifier extends ExperienceModifier {

    final MyPet myPet;

    /**
     * Creates a permission modifier bound to the specified pet.
     *
     * @param myPet the pet whose owner's permissions will be checked
     */
    public PermissionModifier(MyPet myPet) {
        this.myPet = myPet;
    }

    /** {@inheritDoc} Applies the highest applicable permission-based multiplier. */
    public double modify(double experience, double baseExperience) {
        if (Modifier.PERMISSION) {
            Player owner = myPet.getOwner().getPlayer();
            if (owner != null) {
                if (owner.hasPermission("MyPet.experience.multiplier.250")) {
                    experience *= 2.5;
                } else if (owner.hasPermission("MyPet.experience.multiplier.225")) {
                    experience *= 2.25;
                } else if (owner.hasPermission("MyPet.experience.multiplier.200")) {
                    experience *= 2;
                } else if (owner.hasPermission("MyPet.experience.multiplier.175")) {
                    experience *= 1.75;
                } else if (owner.hasPermission("MyPet.experience.multiplier.150")) {
                    experience *= 1.5;
                } else if (owner.hasPermission("MyPet.experience.multiplier.125")) {
                    experience *= 1.25;
                }
            }
        }
        return experience;
    }
}
