/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill;

import net.minecraft.server.v1_4_6.NBTTagCompound;

public class MyPetSkillTreeSkill
{
    String name;
    boolean addedByInheritance = false;
    NBTTagCompound propertiesCompound;

    public MyPetSkillTreeSkill(String name)
    {
        this.name = name;
    }

    public MyPetSkillTreeSkill(String name, boolean addedByInheritance)
    {
        this.name = name;
        this.addedByInheritance = addedByInheritance;
    }

    public String getName()
    {
        return name;
    }

    public void setProperties(NBTTagCompound propertiesCompound)
    {
        this.propertiesCompound = (NBTTagCompound) propertiesCompound.clone();
    }

    public NBTTagCompound getProperties()
    {
        if (propertiesCompound == null)
        {
            return new NBTTagCompound("Properties");
        }
        return propertiesCompound;
    }

    public boolean isAddedByInheritance()
    {
        return addedByInheritance;
    }

    public MyPetSkillTreeSkill clone()
    {
        MyPetSkillTreeSkill newSkill = new MyPetSkillTreeSkill(name, addedByInheritance);

        newSkill.setProperties(getProperties());

        return newSkill;
    }

    @Override
    public String toString()
    {
        return "MyPetSkillTreeSkill{name=" + this.name + "}";
    }
}
