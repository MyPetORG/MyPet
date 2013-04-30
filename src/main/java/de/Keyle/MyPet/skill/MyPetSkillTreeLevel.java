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

import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.logger.MyPetLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyPetSkillTreeLevel
{
    int level;
    List<ISkillInfo> skillList = new ArrayList<ISkillInfo>();

    public MyPetSkillTreeLevel(int level)
    {
        this.level = level;
    }

    public int getLevel()
    {
        return level;
    }

    public void addSkill(ISkillInfo skill)
    {
        if (skill == null)
        {
            MyPetLogger.write("Skills->null: ende");
            MyPetLogger.write(Arrays.toString(Thread.currentThread().getStackTrace()));
        }
        skillList.add(skill);
    }

    public void removeSkill(int index)
    {
        skillList.remove(index);
    }

    public List<ISkillInfo> getSkills()
    {
        return skillList;
    }

    public MyPetSkillTreeLevel clone()
    {
        MyPetSkillTreeLevel newLevel = new MyPetSkillTreeLevel(level);

        for (ISkillInfo skill : skillList)
        {
            newLevel.addSkill(skill.cloneSkill());
        }

        return newLevel;
    }
}
