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

package de.Keyle.MyPet.skill.skills.info;

import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;

@SkillName("HPregeneration")
@SkillProperties(
        parameterNames = {"hp", "time", "addset_hp", "addset_time"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.String, NBTdatatypes.String},
        parameterDefaultValues = {"1", "60", "add", "add"})
public class HPregenerationInfo extends MyPetSkillTreeSkill implements ISkillInfo
{
    protected int increaseHpBy = 0;
    protected int regenTime = 0;

    public HPregenerationInfo(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public ISkillInfo cloneSkill()
    {
        HPregenerationInfo newSkill = new HPregenerationInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}
