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

package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Material;

@ServiceName("EggIconService")
public class EggIconService implements ServiceContainer {
    /**
     * API implementation for choosing an icon Material for a Pet type.
     * <p>
     * NMS-specific subclasses may override this to fix egg identification.
     */
    public void updateIcon(MyPetType type, IconMenuItem icon) {
        // Clear any previous glow
        icon.setGlowing(false);

        // Try to resolve a Material like "PIGLIN_BRUTE_SPAWN_EGG" from enum name
        String upperSnake = toUpperSnake(type.name());
        String matName = upperSnake + "_SPAWN_EGG";

        Material material = Material.matchMaterial(matName);

        // Known exceptions or entities without official spawn eggs
        if (material == null) {
            switch (type) {
                case EnderDragon:
                    material = Material.DRAGON_EGG; // block egg
                    break;
                case Snowman:
                    material = Material.PUMPKIN; // snow golem has no egg
                    break;
                case Giant:
                    material = Material.ZOMBIE_SPAWN_EGG; // no official egg
                    break;
                case Illusioner:
                    material = Material.SQUID_SPAWN_EGG; // No official egg
                    icon.setGlowing(true);
                    break;
                case IronGolem:
                    material = Material.SKELETON_SPAWN_EGG; // Required for 1.19.2 and below
                    icon.setGlowing(true);
                    break;
                case Wither:
                    material = Material.ENDERMITE_SPAWN_EGG; // Required for 1.19.2 and below
                    icon.setGlowing(true);
                    break;
                default:
                    // Show barrier to point out issue
                    material = Material.BARRIER;
                    icon.setGlowing(true);
                    break;
            }
        }

        icon.setMaterial(material);
    }

    /**
     * Helper: convert PascalCase enum names (e.g., PiglinBrute) to UPPER_SNAKE (PIGLIN_BRUTE)
     */
    protected static String toUpperSnake(String in) {
        return in.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }
}