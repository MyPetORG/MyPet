/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skilltree;

import de.Keyle.MyPet.skill.skills.SkillName;
import de.Keyle.MyPet.skill.skills.SkillProperties;
import de.Keyle.MyPet.skill.skills.SkillProperties.NBTdatatypes;
import de.keyle.knbt.*;

public abstract class SkillTreeSkill {
    private boolean addedByInheritance = false;
    private TagCompound propertiesCompound;

    public SkillTreeSkill(boolean addedByInheritance) {
        this.addedByInheritance = addedByInheritance;
    }

    public String getName() {
        SkillName sn = this.getClass().getAnnotation(SkillName.class);
        if (sn != null) {
            return sn.value();
        }
        return null;
    }

    public void setProperties(TagCompound propertiesCompound) {
        this.propertiesCompound = propertiesCompound.clone();
    }

    public TagCompound getProperties() {
        if (propertiesCompound == null) {
            propertiesCompound = new TagCompound();
            return propertiesCompound;
        }
        return propertiesCompound;
    }

    public void setDefaultProperties() {
        SkillProperties sp = this.getClass().getAnnotation(SkillProperties.class);
        if (sp != null) {
            for (int i = 0; i < sp.parameterNames().length; i++) {

                String propertyName = sp.parameterNames()[i];
                String defaultPropertyValue = sp.parameterDefaultValues()[i];
                NBTdatatypes propertyType = sp.parameterTypes()[i];
                if (!getProperties().getCompoundData().containsKey(propertyName)) {
                    switch (propertyType) {
                        case Short:
                            TagShort TagShort = new TagShort(Short.parseShort(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, TagShort);
                            break;
                        case Int:
                            TagInt TagInt = new TagInt(Integer.parseInt(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, TagInt);
                            break;
                        case Long:
                            TagLong TagLong = new TagLong(Long.parseLong(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, TagLong);
                            break;
                        case Float:
                            TagFloat TagFloat = new TagFloat(Float.parseFloat(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, TagFloat);
                            break;
                        case Double:
                            TagDouble TagDouble = new TagDouble(Double.parseDouble(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, TagDouble);
                            break;
                        case Byte:
                            TagByte TagByte = new TagByte(Byte.parseByte(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, TagByte);
                            break;
                        case Boolean:
                            TagByte booleanTag = new TagByte(Boolean.parseBoolean(defaultPropertyValue));
                            propertiesCompound.getCompoundData().put(propertyName, booleanTag);
                            break;
                        case String:
                            TagString TagString = new TagString(defaultPropertyValue);
                            propertiesCompound.getCompoundData().put(propertyName, TagString);
                            break;
                    }
                }
            }
        }
    }

    public boolean isAddedByInheritance() {
        return addedByInheritance;
    }

    public void setIsInherited(boolean flag) {
        addedByInheritance = flag;
    }

    @Override
    public String toString() {
        return "SkillTreeSkill{name=" + getName() + "}";
    }
}