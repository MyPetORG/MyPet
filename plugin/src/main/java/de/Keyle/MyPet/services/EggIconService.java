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

package de.Keyle.MyPet.services;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Material;

@ServiceName("EggIconService")
public class EggIconService implements ServiceContainer {

    protected static String toUpperSnake(String in) {
        return in.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    public void updateIcon(PetType type, IconMenuItem icon) {
        icon.setGlowing(false);

        String matName = toUpperSnake(type.name()) + "_SPAWN_EGG";
        Material material = Material.matchMaterial(matName);
        if (material != null) {
            icon.setMaterial(material);
            return;
        }

        // No vanilla spawn-egg item — consult the pet class's @DefaultInfo
        // fallback so each PetXxx declares its own icon. Falls through to a
        // glowing BARRIER when no fallback is specified.
        DefaultInfo info = type.getPetClass() != null
                ? type.getPetClass().getAnnotation(DefaultInfo.class)
                : null;
        if (info != null && !info.fallbackIconMaterial().isEmpty()) {
            Material fallback = Material.matchMaterial(info.fallbackIconMaterial());
            if (fallback != null) {
                icon.setMaterial(fallback);
                icon.setGlowing(info.fallbackIconGlow());
                return;
            }
        }

        icon.setMaterial(Material.EGG);
        icon.setGlowing(true);
    }
}
