/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.NBTTagCompound;

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

    public void setDefaultProperties()
    {
        SkillProperties sp = this.getClass().getAnnotation(SkillProperties.class);
        if (sp != null)
        {
            for (int i = 0 ; i < sp.parameterNames().length ; i++)
            {

                String propertyName = sp.parameterNames()[i];
                String defaultPropertyValue = sp.parameterDefaultValues()[i];
                NBTdatatypes propertyType = sp.parameterTypes()[i];
                if (!getProperties().hasKey(propertyName))
                {
                    switch (propertyType)
                    {
                        case Short:
                            propertiesCompound.setShort(propertyName, Short.parseShort(defaultPropertyValue));
                            break;
                        case Int:
                            propertiesCompound.setInt(propertyName, Integer.parseInt(defaultPropertyValue));
                            break;
                        case Long:
                            propertiesCompound.setLong(propertyName, Long.parseLong(defaultPropertyValue));
                            break;
                        case Float:
                            propertiesCompound.setFloat(propertyName, Float.parseFloat(defaultPropertyValue));
                            break;
                        case Double:
                            propertiesCompound.setDouble(propertyName, Double.parseDouble(defaultPropertyValue));
                            break;
                        case Byte:
                            propertiesCompound.setByte(propertyName, Byte.parseByte(defaultPropertyValue));
                            break;
                        case Boolean:
                            propertiesCompound.setBoolean(propertyName, Boolean.parseBoolean(defaultPropertyValue));
                            break;
                        case String:
                            propertiesCompound.setString(propertyName, defaultPropertyValue);
                            break;
                    }
                }
            }
        }
    }

    public boolean isAddedByInheritance()
    {
        return addedByInheritance;
    }

    public void setIsInherited(boolean flag)
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
