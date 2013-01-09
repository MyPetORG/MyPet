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

import java.util.ArrayList;
import java.util.List;

public class MyPetSkillTreeLevel
{
    short level;
    List<MyPetSkillTreeSkill> skillList = new ArrayList<MyPetSkillTreeSkill>();

    public MyPetSkillTreeLevel(short level)
    {
        this.level = level;
    }

    public short getLevel()
    {
        return level;
    }

    public void addSkill(MyPetSkillTreeSkill skill)
    {
        skillList.add(skill);
    }

    public void removeSkill(int index)
    {
        skillList.remove(index);
    }

    public List<MyPetSkillTreeSkill> getSkills()
    {
        return skillList;
    }

    public MyPetSkillTreeLevel clone()
    {
        MyPetSkillTreeLevel newLevel = new MyPetSkillTreeLevel(level);

        for (MyPetSkillTreeSkill skill : skillList)
        {
            newLevel.addSkill(skill.cloneSkill());
        }

        return newLevel;
    }
}
