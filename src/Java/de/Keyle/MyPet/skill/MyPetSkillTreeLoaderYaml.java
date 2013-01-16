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


import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.List;
import java.util.Set;

public class MyPetSkillTreeLoaderYaml
{
    public static void loadSkillTrees(String configPath)
    {
        YamlConfiguration MWConfig;
        if (MyPetUtil.getDebugLogger() != null)
        {
            MyPetUtil.getDebugLogger().info("Loading skill configs in: " + configPath);
        }
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.yml");
        MyPetSkillTreeMobType skillTreeMobType = new MyPetSkillTreeMobType("default");
        if (skillFile.exists())
        {
            MWConfig = new YamlConfiguration(skillFile);
            loadSkillTree(MWConfig, skillTreeMobType);
            skillFile.renameTo(new File(configPath + File.separator + "old_default.yml"));
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  default.yml");
            }
        }

        for (MyPetType mobType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + mobType.getTypeName().toLowerCase() + ".yml");

            skillTreeMobType = new MyPetSkillTreeMobType(mobType.getTypeName());

            if (!skillFile.exists())
            {
                if (!skillTreeMobType.getMobTypeName().equals("default"))
                {
                    addDefault(skillTreeMobType);
                }
                manageInheritance(skillTreeMobType);
                continue;
            }

            MWConfig = new YamlConfiguration(skillFile);
            loadSkillTree(MWConfig, skillTreeMobType);
            skillFile.renameTo(new File(configPath + File.separator + "old_" + mobType.getTypeName().toLowerCase() + ".yml"));
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  " + mobType.getTypeName().toLowerCase() + ".yml");
            }
        }

    }

    private static void loadSkillTree(YamlConfiguration MWConfig, MyPetSkillTreeMobType skillTreeMobType)
    {
        ConfigurationSection sec = MWConfig.getConfig().getConfigurationSection("skilltrees");
        if (sec == null)
        {
            return;
        }
        Set<String> SkillTreeNames = sec.getKeys(false);
        if (SkillTreeNames.size() > 0)
        {
            for (String skillTreeName : SkillTreeNames)
            {
                String inherit = MWConfig.getConfig().getString("skilltrees." + skillTreeName + ".inherit", "%#_DeFaUlT_#%");
                MyPetSkillTree skillTree;
                int place = skillTreeMobType.getNextPlace();
                if (!inherit.equals("%#_DeFaUlT_#%"))
                {
                    skillTree = new MyPetSkillTree(skillTreeName, inherit);
                }
                else
                {
                    skillTree = new MyPetSkillTree(skillTreeName);
                }

                Set<String> level = MWConfig.getConfig().getConfigurationSection("skilltrees." + skillTreeName).getKeys(false);
                if (level.size() > 0)
                {
                    for (String thisLevel : level)
                    {
                        if (MyPetUtil.isInt(thisLevel))
                        {
                            List<String> skillsOfThisLevel = MWConfig.getConfig().getStringList("skilltrees." + skillTreeName + "." + thisLevel);
                            if (skillsOfThisLevel.size() > 0)
                            {
                                for (String thisSkill : skillsOfThisLevel)
                                {
                                    if (MyPetSkills.isValidSkill(thisSkill))
                                    {
                                        MyPetSkillTreeSkill skillTreeSkill = MyPetSkills.getNewSkillInstance(thisSkill);
                                        skillTree.addSkillToLevel(Short.parseShort(thisLevel), skillTreeSkill);
                                    }
                                }
                            }
                        }
                    }
                    skillTreeMobType.addSkillTree(skillTree);
                }
            }
            if (!skillTreeMobType.getMobTypeName().equals("default"))
            {
                addDefault(skillTreeMobType);
            }
            manageInheritance(skillTreeMobType);
        }
    }

    private static void addDefault(MyPetSkillTreeMobType skillTreeMobType)
    {
        MyPetSkillTreeMobType defaultSkillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        for (String skillTreeName : defaultSkillTreeMobType.getSkillTreeNames())
        {
            if (!skillTreeMobType.hasSkillTree(skillTreeName))
            {
                MyPetSkillTree defaultSkillTree = defaultSkillTreeMobType.getSkillTree(skillTreeName);
                MyPetSkillTree newSkillTree = new MyPetSkillTree(skillTreeName);

                for (MyPetSkillTreeLevel level : defaultSkillTree.getLevelList())
                {
                    for (MyPetSkillTreeSkill skill : level.getSkills())
                    {
                        if (!skill.isAddedByInheritance())
                        {
                            MyPetSkillTreeSkill skillClone = skill.cloneSkill();
                            skillClone.setIsInherited(true);
                            newSkillTree.addSkillToLevel(level.getLevel(), skillClone);
                        }
                    }
                }
                skillTreeMobType.addSkillTree(newSkillTree);
            }

        }
    }

    private static void manageInheritance(MyPetSkillTreeMobType skillTreeMobType)
    {
        for (String skillTreeName : skillTreeMobType.getSkillTreeNames())
        {
            MyPetSkillTree skillTree = skillTreeMobType.getSkillTree(skillTreeName);
            if (skillTree.hasInheritance())
            {
                if (skillTreeMobType.hasSkillTree(skillTree.getInheritance()))
                {
                    MyPetSkillTree skillTreeInherit = skillTreeMobType.getSkillTree(skillTree.getInheritance());
                    for (MyPetSkillTreeLevel level : skillTreeInherit.getLevelList())
                    {
                        for (MyPetSkillTreeSkill skill : level.getSkills())
                        {
                            if (!skill.isAddedByInheritance())
                            {
                                MyPetSkillTreeSkill skillClone = skill.cloneSkill();
                                skillClone.setIsInherited(true);
                                skillTree.addSkillToLevel(level.getLevel(), skillClone);
                            }
                        }
                    }
                }
            }
        }
    }
}