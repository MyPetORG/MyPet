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

package de.Keyle.MyPet.skill.skilltreeloader;

import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeLevel;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MyPetSkillTreeLoader
{
    protected static List<MyPetSkillTree> alreadyLoadedInheritance = new ArrayList<MyPetSkillTree>();

    public static void addDefault(MyPetSkillTreeMobType skillTreeMobType)
    {
        if (!MyPetSkillTreeMobType.hasMobType("default"))
        {
            return;
        }
        MyPetSkillTreeMobType defaultSkillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        for (String skillTreeName : defaultSkillTreeMobType.getSkillTreeNames())
        {
            if (!skillTreeMobType.hasSkillTree(skillTreeName) && defaultSkillTreeMobType.hasSkillTree(skillTreeName))
            {
                MyPetSkillTree newSkillTree = defaultSkillTreeMobType.getSkillTree(skillTreeName).clone();
                for (MyPetSkillTreeLevel level : newSkillTree.getLevelList())
                {
                    for (ISkillInfo skill : level.getSkills())
                    {
                        skill.setIsInherited(true);
                    }
                }
                skillTreeMobType.addSkillTree(newSkillTree);
            }
        }
    }

    public static void manageInheritance(MyPetSkillTreeMobType skillTreeMobType)
    {
        Map<MyPetSkillTree, MyPetSkillTree> skillTreeClones = new HashMap<MyPetSkillTree, MyPetSkillTree>();
        for (MyPetSkillTree skillTree : skillTreeMobType.getSkillTrees())
        {
            skillTreeClones.put(skillTree, skillTree.clone());
        }
        for (MyPetSkillTree skillTree : skillTreeMobType.getSkillTrees())
        {
            alreadyLoadedInheritance.clear();
            if (skillTree.hasInheritance())
            {
                alreadyLoadedInheritance.add(skillTree);
                manageInheritance(skillTreeMobType, skillTree, skillTree, skillTreeClones, 0);
            }
        }
    }

    protected static void manageInheritance(MyPetSkillTreeMobType skillTreeMobType, MyPetSkillTree startSkillTree, MyPetSkillTree skillTree, Map<MyPetSkillTree, MyPetSkillTree> clones, int depth)
    {
        if (skillTree.hasInheritance() && depth < 20)
        {
            if (skillTreeMobType.hasSkillTree(skillTree.getInheritance()))
            {
                MyPetSkillTree skillTreeInherit = skillTreeMobType.getSkillTree(skillTree.getInheritance());
                if (!alreadyLoadedInheritance.contains(skillTreeInherit))
                {
                    if (skillTreeInherit.hasInheritance() && MyPetConfiguration.INHERIT_ALREADY_INHERITED_SKILLS)
                    {
                        alreadyLoadedInheritance.add(skillTreeInherit);
                        manageInheritance(skillTreeMobType, startSkillTree, skillTreeInherit, clones, depth + 1);
                    }
                    else
                    {
                        alreadyLoadedInheritance.add(skillTreeInherit);
                    }
                    MyPetSkillTree skillTreeClone = clones.get(skillTreeInherit);
                    for (MyPetSkillTreeLevel level : skillTreeClone.getLevelList())
                    {
                        for (ISkillInfo skill : level.getSkills())
                        {
                            ISkillInfo skillClone = skill.cloneSkill();
                            if (skillClone != null)
                            {
                                skillClone.setIsInherited(true);
                                startSkillTree.addSkillToLevel(level.getLevel(), skillClone);
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract void loadSkillTrees(String configPath, String[] mobtypes);

    public abstract List<String> saveSkillTrees(String configPath, String[] mobtypes);
}
