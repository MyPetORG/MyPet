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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.entity.EntityType;

public enum MyPetType
{
    Wolf(EntityType.WOLF, "Wolf", EntityMyWolf.class, MyWolf.class);

    private EntityType entityType;
    private String typeName;
    private Class<? extends EntityMyPet> entityClazz;
    private Class<? extends MyPet> myPetClazz;

    private MyPetType(EntityType type, String name, Class<? extends EntityMyPet> entityClazz, Class<? extends MyPet> myPetClazz)
    {
        this.entityType = type;
        this.typeName = name;
        this.entityClazz = entityClazz;
        this.myPetClazz = myPetClazz;
    }

    public EntityType getEntityType()
    {
        return entityType;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public EntityMyPet getNewEntityInstance()
    {
        EntityMyPet pet = null;

        try
        {
            Object obj = entityClazz.newInstance();
            if (obj instanceof EntityMyPet)
            {
                pet = (EntityMyPet) obj;
            }
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().warning(entityClazz.getName() + "is no valid MyPet(Entity)!");
        }
        return pet;
    }

    public MyPet getNewMyPetInstance()
    {
        MyPet pet = null;

        try
        {
            Object obj = myPetClazz.newInstance();
            if (obj instanceof MyPet)
            {
                pet = (MyPet) obj;
            }
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().warning(myPetClazz.getName() + "is no valid MyPet!");
        }
        return pet;
    }
}
