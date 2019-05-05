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

package de.Keyle.MyPet.compat.v1_14_R1.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.entity.EntityType;

@Compat("v1_14_R1")
public class MyPetInfo extends de.Keyle.MyPet.api.entity.MyPetInfo {

    @Override
    public boolean isLeashableEntityType(EntityType bukkitType) {
        if (bukkitType == EntityType.ENDER_DRAGON) {
            return MyPetApi.getPluginHookManager().isHookActive("ProtocolLib");
        }

        try {
            MyPetType type = MyPetType.byEntityTypeName(bukkitType.name());
            return type != null;
        } catch (MyPetTypeNotFoundException e) {
            return false;
        }
    }
}