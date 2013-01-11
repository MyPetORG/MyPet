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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.NBTTagCompound;

import java.io.InputStream;

public abstract class MyPetSkillTreeSkill
{
    private boolean addedByInheritance = false;
    private NBTTagCompound propertiesCompound;

    public MyPetSkillTreeSkill(boolean addedByInheritance)
    {
        this.addedByInheritance = addedByInheritance;
    }

    public String getName()
    {
        SkillName sn = this.getClass().getAnnotation(SkillName.class);
        if (sn != null)
        {
            return sn.value();
        }
        return "BadSkillClass";
    }

    public void setProperties(NBTTagCompound propertiesCompound)
    {
        this.propertiesCompound = (NBTTagCompound) propertiesCompound.clone();
    }

    public NBTTagCompound getProperties()
    {
        if (propertiesCompound == null)
        {
            propertiesCompound = new NBTTagCompound("Properties");
            return propertiesCompound;
        }
        return propertiesCompound;
    }

    public boolean isAddedByInheritance()
    {
        return addedByInheritance;
    }

    protected void setIsInherited(boolean flag)
    {
        addedByInheritance = flag;
    }

    public abstract MyPetSkillTreeSkill cloneSkill();

    public String getHtml()
    {
        InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("html/skills/" + getName() + ".html");
        if (htmlStream == null)
        {
            htmlStream = MyPetPlugin.class.getClassLoader().getResourceAsStream("html/skills/_default.html");
            if (htmlStream == null)
            {
                return "NoSkillPropertieViewFoundError";
            }
        }
        return MyPetUtil.convertStreamToString(htmlStream).replace("#Skillname#", getName());
    }

    @Override
    public String toString()
    {
        return "MyPetSkillTreeSkill{name=" + getName() + "}";
    }
}
