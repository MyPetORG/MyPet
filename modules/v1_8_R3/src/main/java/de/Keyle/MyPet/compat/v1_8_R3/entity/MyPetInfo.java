/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_8_R3.entity;

import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.bukkit.entity.EntityType;

public class MyPetInfo extends de.Keyle.MyPet.api.entity.MyPetInfo {
    @Override
    public boolean isLeashableEntityType(EntityType type) {
        if (type == EntityType.ENDER_DRAGON) {
            return PluginHookManager.isPluginUsable("ProtocolLib"); //ToDo & active
        }
        return true;
    }
}