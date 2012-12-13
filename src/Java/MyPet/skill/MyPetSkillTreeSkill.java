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

package Java.MyPet.skill;

import java.util.HashMap;
import java.util.Map;

public class MyPetSkillTreeSkill
{
    String name;
    boolean addedByInheritance = false;
    Map<String, String> options = new HashMap<String, String>();

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

    public void addOption(String option, String value)
    {
        options.put(option, value);
    }

    public String getOption(String option)
    {
        if (options.containsKey(option))
        {
            return options.get(option);
        }
        return null;
    }

    public boolean isAddedByInheritance()
    {
        return addedByInheritance;
    }

    @Override
    public String toString()
    {
        return "MyPetSkillTreeSkill{name=" + this.name + "}";
    }
}
