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

package de.Keyle.MyPet.util;


import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.MyPetSkillSystem;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.util.configuration.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyPetSkillTreeConfigLoader
{
    //private static Map<String, String> Inheritances = new HashMap<String, String>();
    private static Map<String, MyPetSkillTreeMobType> skillTreeMobTypes = new HashMap<String, MyPetSkillTreeMobType>();

    private static String configPath;

    public static void setConfigPath(String path)
    {
        configPath = path;
    }

    public static void loadSkillTrees()
    {
        YamlConfiguration MWConfig;
        MyPetUtil.getDebugLogger().info("Loading skill configs in: " + configPath);
        File skillFile;
        for (MyPetType mobType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + mobType.getTypeName() + ".yml");

            MyPetSkillTreeMobType skillTreeMobType = new MyPetSkillTreeMobType(mobType.getTypeName().toLowerCase());
            skillTreeMobTypes.put(mobType.getTypeName().toLowerCase(), skillTreeMobType);

            if (!skillFile.exists())
            {
                continue;
            }

            MWConfig = new YamlConfiguration(skillFile);
            loadSkillTree(MWConfig, skillTreeMobType);
            MyPetUtil.getDebugLogger().info("  " + mobType.getTypeName().toLowerCase() + ".yml");
        }
        skillFile = new File(configPath + File.separator + "default.yml");
        MyPetSkillTreeMobType skillTreeMobType = new MyPetSkillTreeMobType("default");
        skillTreeMobTypes.put("default", skillTreeMobType);
        if (skillFile.exists())
        {
            MWConfig = new YamlConfiguration(skillFile);
            loadSkillTree(MWConfig, skillTreeMobType);
            MyPetUtil.getDebugLogger().info("  default.yml");
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
            //Map<String, String> Inheritances = new HashMap<String, String>();
            for (String skillTreeName : SkillTreeNames)
            {
                MyPetSkillTree skillTree = new MyPetSkillTree(skillTreeName);
                //String inherit = MWConfig.getConfig().getString("skilltrees." + skillTreeName + ".inherit", "%#_DeFaUlT_#%");
                //if (!inherit.equals("%#_DeFaUlT_#%"))
                //{
                //    Inheritances.put(skillTreeName, inherit);
                //}
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
                                    if (MyPetSkillSystem.isValidSkill(thisSkill))
                                    {
                                        MyPetSkillTreeSkill skillTreeSkill = new MyPetSkillTreeSkill(thisSkill);
                                        skillTree.addSkillToLevel(Integer.parseInt(thisLevel), skillTreeSkill);
                                    }
                                }
                            }
                        }
                    }
                    skillTreeMobType.addSkillTree(skillTree);
                }
            }
        }
    }

    /*
    public static MyPetSkillTree manageInheritance(MyPetSkillTreeMobType skillTreeMobType)
    {
        if (SkillTrees.containsKey(myPetType) && SkillTrees.get(myPetType).containsKey(name))
        {

            MyPetSkillTree MWST = new MyPetSkillTree(myPetType, SkillTrees.get(myPetType).get(name).getName());

            if (SkillTrees.get(myPetType).get(name).getLevelList() != null)
            {
                for (int level : SkillTrees.get(myPetType).get(name).getLevelList())
                {
                    MWST.addSkillToLevel(level, SkillTrees.get(myPetType).get(name).getSkills(level));
                }
            }

            if (Inheritances.containsKey(name))
            {
                String NextInheritance = Inheritances.get(name);
                while (!NextInheritance.isEmpty())
                {
                    if (SkillTrees.containsKey(name))
                    {
                        MyPetSkillTree nextMWST = SkillTrees.get(NextInheritance);
                        if (nextMWST.getLevelList() != null)
                        {
                            for (int level : nextMWST.getLevelList())
                            {
                                MWST.addSkillToLevel(level, nextMWST.getSkills(level));
                            }
                            if (Inheritances.containsKey(NextInheritance))
                            {
                                NextInheritance = getInheritance(NextInheritance);
                            }
                            else
                            {
                                NextInheritance = "";
                            }
                        }
                    }
                }
            }
            return MWST;
        }
        return null;
    }

    public static String getInheritance(String petName)
    {
        if (Inheritances.containsKey(petName))
        {
            return Inheritances.get(petName);
        }
        return null;
    }
    */

    public static List<String> getSkillTreeNames(MyPetType myPetType)
    {
        return skillTreeMobTypes.get(myPetType.getTypeName().toLowerCase()).getSkillTreeNames();
    }

    public static boolean containsSkillTree(String myPetTypeName, String name)
    {
        return skillTreeMobTypes.containsKey(myPetTypeName.toLowerCase()) && getMobType(myPetTypeName.toLowerCase()).getSkillTreeNames().indexOf(name) != -1;
    }

    public static boolean hasMobType(String mobTypeName)
    {
        return skillTreeMobTypes.containsKey(mobTypeName.toLowerCase());
    }

    public static MyPetSkillTreeMobType getMobType(String mobTypeName)
    {
        return skillTreeMobTypes.get(mobTypeName);
    }
}