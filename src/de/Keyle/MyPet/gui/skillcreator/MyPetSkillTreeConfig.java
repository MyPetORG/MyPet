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

package de.Keyle.MyPet.gui.skillcreator;


import de.Keyle.MyPet.skill.MyPetSkillTreeLevel;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import javax.swing.*;
import java.io.File;
import java.util.*;

public class MyPetSkillTreeConfig
{
    private static Map<String, MyPetSkillTreeMobType> skillTreeMobTypes = new HashMap<String, MyPetSkillTreeMobType>();

    private static String configPath;
    private static String[] myPetTypes = {"default", "CaveSpider", "Chicken", "Cow", "Creeper", "IronGolem", "Mooshroom", "Ocelot", "Pig", "PigZombie", "Sheep", "Slime", "Spider", "Skeleton", "Silverfish", "Villager", "Wolf", "Zombie"};

    public static void setConfigPath(String path)
    {
        configPath = path;
    }

    public static void loadSkillTrees()
    {
        YamlConfiguration yamlConfig;
        File skillFile;
        for (String mobType : myPetTypes)
        {
            skillFile = new File(configPath + File.separator + mobType + ".yml");

            MyPetSkillTreeMobType skillTreeMobType = new MyPetSkillTreeMobType(mobType.toLowerCase());
            skillTreeMobTypes.put(mobType.toLowerCase(), skillTreeMobType);
            if (!skillFile.exists())
            {
                continue;
            }

            yamlConfig = new YamlConfiguration(skillFile);
            loadSkillTree(yamlConfig, skillTreeMobType);
        }
    }

    public static void saveSkillTrees()
    {
        YamlConfiguration yamlConfig;
        File skillFile;
        boolean save;
        String savedFiles = "";
        for (String mobType : myPetTypes)
        {
            save = false;
            mobType = mobType.toLowerCase();
            skillTreeMobTypes.get(mobType);
            skillTreeMobTypes.get(mobType).getSkillTreeNames();
            skillTreeMobTypes.get(mobType).getSkillTreeNames().size();
            if (skillTreeMobTypes.get(mobType).getSkillTreeNames().size() != 0)
            {
                skillFile = new File(configPath + File.separator + mobType + ".yml");


                yamlConfig = new YamlConfiguration(skillFile);
                yamlConfig.clearConfig();

                for (String skillTreeNames : skillTreeMobTypes.get(mobType).getSkillTreeNames())
                {
                    MyPetSkillTree skillTree = (MyPetSkillTree) skillTreeMobTypes.get(mobType).getSkillTree(skillTreeNames);
                    for (MyPetSkillTreeLevel skillTreeLevel : skillTree.getLevelList())
                    {
                        List<String> skillList = new ArrayList<String>();
                        if (skillTree.getLevel(skillTreeLevel.getLevel()).getSkills().size() > 0)
                        {
                            for (MyPetSkillTreeSkill skill : skillTree.getLevel(skillTreeLevel.getLevel()).getSkills())
                            {
                                skillList.add(skill.getName());
                            }
                            yamlConfig.getConfig().set("skilltrees." + skillTree.getName() + '.' + skillTreeLevel.getLevel(), skillList);
                            save = true;
                        }
                    }
                    if (skillTree.getInheritance() != null)
                    {
                        yamlConfig.getConfig().set("skilltrees." + skillTree.getName() + ".inherit", skillTree.getInheritance());
                        save = true;
                    }
                }
                if (save)
                {
                    yamlConfig.saveConfig();
                    savedFiles += "\n   " + mobType + ".yml";
                }
            }
        }
        JOptionPane.showMessageDialog(null, "Saved to:\n" + configPath + File.separator + savedFiles, "Saved following configs", JOptionPane.INFORMATION_MESSAGE);
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
                MyPetSkillTree skillTree;
                String inherit = MWConfig.getConfig().getString("skilltrees." + skillTreeName + ".inherit", "%#_DeFaUlT_#%");
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
                            skillTree.addLevel(Integer.parseInt(thisLevel));
                            List<String> skillsOfThisLevel = MWConfig.getConfig().getStringList("skilltrees." + skillTreeName + "." + thisLevel);
                            if (skillsOfThisLevel.size() > 0)
                            {
                                for (String thisSkill : skillsOfThisLevel)
                                {
                                    MyPetSkillTreeSkill skillTreeSkill = new MyPetSkillTreeSkill(thisSkill);
                                    skillTree.addSkillToLevel(Integer.parseInt(thisLevel), skillTreeSkill);
                                }
                            }
                        }
                    }
                    skillTreeMobType.addSkillTree(skillTree);
                }
            }
        }
    }

    public static MyPetSkillTreeMobType getMobType(String mobTypeName)
    {
        return skillTreeMobTypes.get(mobTypeName.toLowerCase());
    }

    public static class MyPetSkillTree extends de.Keyle.MyPet.skill.MyPetSkillTree
    {
        public MyPetSkillTree(String name)
        {
            super(name);
        }

        public MyPetSkillTree(String name, String inheritance)
        {
            super(name, inheritance);
        }

        public void setInheritance(String inheritance)
        {
            this.inheritance = inheritance;
        }
    }
}