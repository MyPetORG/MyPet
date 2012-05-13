/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.entity.types;

import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import org.bukkit.entity.EntityType;

public enum MyPetType
{
    Wolf(EntityType.WOLF, "Wolf", MyWolf.class);

    private EntityType entityType;
    private String typeName;
    private Class<? extends de.Keyle.MyWolf.entity.types.MyPet> clazz;

    private MyPetType(EntityType type, String name, Class<? extends de.Keyle.MyWolf.entity.types.MyPet> clazz)
    {
        this.entityType = type;
        this.typeName = name;
        this.clazz = clazz;
    }

    public EntityType getEntityType()
    {
        return entityType;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public de.Keyle.MyWolf.entity.types.MyPet getNewInstance()
    {
        return null;
    }
}
