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
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 * An experience modifier that grants bonus experience based on the pet owner's permissions.
 *
 * <p>When the permission-based modifier feature is enabled in configuration
 * ({@link Modifier#PERMISSION}), this modifier scans the pet owner's effective
 * permissions for nodes of the form {@code MyPet.experience.multiplier.<percent>}
 * where {@code <percent>} is any positive integer. The highest matching value is
 * applied as {@code percent / 100.0}:
 *
 * <ul>
 *   <li>{@code MyPet.experience.multiplier.125} → 1.25x</li>
 *   <li>{@code MyPet.experience.multiplier.183} → 1.83x</li>
 *   <li>{@code MyPet.experience.multiplier.250} → 2.5x</li>
 *   <li>{@code MyPet.experience.multiplier.50} → 0.5x (halved)</li>
 * </ul>
 *
 * <p>If the feature is disabled, the owner is offline, or no permission node matches,
 * the experience passes through unchanged.
 */
public class PermissionModifier extends ExperienceModifier {

    private static final String PREFIX = "mypet.experience.multiplier.";

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
        if (!Modifier.PERMISSION) return experience;

        Player owner = myPet.getOwner().getPlayer();
        if (owner == null) return experience;

        int highest = 0;
        for (PermissionAttachmentInfo pai : owner.getEffectivePermissions()) {
            if (!pai.getValue()) continue;
            String perm = pai.getPermission().toLowerCase();
            if (!perm.startsWith(PREFIX)) continue;

            try {
                int value = Integer.parseInt(perm.substring(PREFIX.length()));
                if (value > highest) highest = value;
            } catch (NumberFormatException ignored) {
            }
        }

        if (highest > 0) {
            experience *= highest / 100.0;
        }
        return experience;
    }
}
