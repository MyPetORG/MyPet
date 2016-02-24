/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.skill.skilltreeloader;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SkillTreeLoader {
    protected static List<SkillTree> alreadyLoadedInheritance = new ArrayList<>();

    public static void addDefault(SkillTreeMobType skillTreeMobType) {
        SkillTreeMobType defaultSkillTreeMobType = SkillTreeMobType.DEFAULT;
        for (String skillTreeName : defaultSkillTreeMobType.getSkillTreeNames()) {
            if (!skillTreeMobType.hasSkillTree(skillTreeName) && defaultSkillTreeMobType.hasSkillTree(skillTreeName)) {
                SkillTree newSkillTree = defaultSkillTreeMobType.getSkillTree(skillTreeName).clone();
                for (SkillTreeLevel level : newSkillTree.getLevelList()) {
                    for (SkillInfo skill : level.getSkills()) {
                        skill.setIsInherited(true);
                    }
                }
                skillTreeMobType.addSkillTree(newSkillTree);
            }
        }
    }

    public static void manageInheritance(SkillTreeMobType skillTreeMobType) {
        Map<SkillTree, SkillTree> skillTreeClones = new HashMap<>();
        for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
            skillTreeClones.put(skillTree, skillTree.clone());
        }
        for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
            alreadyLoadedInheritance.clear();
            if (skillTree.hasInheritance()) {
                alreadyLoadedInheritance.add(skillTree);
                manageInheritance(skillTreeMobType, skillTree, skillTree, skillTreeClones, 0);
            }
        }
    }

    protected static void manageInheritance(SkillTreeMobType skillTreeMobType, SkillTree startSkillTree, SkillTree skillTree, Map<SkillTree, SkillTree> clones, int depth) {
        if (skillTree.hasInheritance() && depth < 20) {
            if (skillTreeMobType.hasSkillTree(skillTree.getInheritance())) {
                SkillTree skillTreeInherit = skillTreeMobType.getSkillTree(skillTree.getInheritance());
                if (!alreadyLoadedInheritance.contains(skillTreeInherit)) {
                    if (skillTreeInherit.hasInheritance() && Configuration.Skilltree.INHERIT_ALREADY_INHERITED_SKILLS) {
                        alreadyLoadedInheritance.add(skillTreeInherit);
                        manageInheritance(skillTreeMobType, startSkillTree, skillTreeInherit, clones, depth + 1);
                    } else {
                        alreadyLoadedInheritance.add(skillTreeInherit);
                    }
                    SkillTree skillTreeClone = clones.get(skillTreeInherit);
                    for (SkillTreeLevel level : skillTreeClone.getLevelList()) {
                        for (SkillInfo skill : level.getSkills()) {
                            SkillInfo skillClone = skill.cloneSkill();
                            if (skillClone != null) {
                                skillClone.setIsInherited(true);
                                startSkillTree.addSkillToLevel(level.getLevel(), skillClone, true);
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract void loadSkillTrees(String configPath, String[] mobtypes);
}