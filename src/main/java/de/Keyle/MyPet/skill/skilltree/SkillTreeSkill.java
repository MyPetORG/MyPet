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

package de.Keyle.MyPet.skill.skilltree;

import de.Keyle.MyPet.skill.skills.SkillName;
import de.Keyle.MyPet.skill.skills.SkillProperties;
import de.Keyle.MyPet.skill.skills.SkillProperties.NBTdatatypes;
import org.spout.nbt.*;

public abstract class SkillTreeSkill {
    private boolean addedByInheritance = false;
    private CompoundTag propertiesCompound;

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

    public void setProperties(CompoundTag propertiesCompound) {
        this.propertiesCompound = propertiesCompound.clone();
    }

    public CompoundTag getProperties() {
        if (propertiesCompound == null) {
            propertiesCompound = new CompoundTag("Properties", new CompoundMap());
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
                if (!getProperties().getValue().containsKey(propertyName)) {
                    switch (propertyType) {
                        case Short:
                            ShortTag shortTag = new ShortTag(propertyName, Short.parseShort(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, shortTag);
                            break;
                        case Int:
                            IntTag intTag = new IntTag(propertyName, Integer.parseInt(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, intTag);
                            break;
                        case Long:
                            LongTag longTag = new LongTag(propertyName, Long.parseLong(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, longTag);
                            break;
                        case Float:
                            FloatTag floatTag = new FloatTag(propertyName, Float.parseFloat(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, floatTag);
                            break;
                        case Double:
                            DoubleTag doubleTag = new DoubleTag(propertyName, Double.parseDouble(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, doubleTag);
                            break;
                        case Byte:
                            ByteTag byteTag = new ByteTag(propertyName, Byte.parseByte(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, byteTag);
                            break;
                        case Boolean:
                            ByteTag booleanTag = new ByteTag(propertyName, Boolean.parseBoolean(defaultPropertyValue));
                            propertiesCompound.getValue().put(propertyName, booleanTag);
                            break;
                        case String:
                            StringTag stringTag = new StringTag(propertyName, defaultPropertyValue);
                            propertiesCompound.getValue().put(propertyName, stringTag);
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