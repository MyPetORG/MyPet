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
import de.Keyle.MyPet.util.MyPetConfig;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.NBTConfiguration;
import net.minecraft.server.v1_4_6.NBTTagCompound;
import net.minecraft.server.v1_4_6.NBTTagList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyPetSkillTreeLoader
{
    public static void loadSkillTrees(String configPath)
    {
        loadSkillTrees(configPath, true);
    }

    public static void loadSkillTrees(String configPath, boolean applyDefaultAndInheritance)
    {
        NBTConfiguration MWConfig;
        if (MyPetUtil.getDebugLogger() != null)
        {
            MyPetUtil.getDebugLogger().info("Loading skill configs in: " + configPath);
        }
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.st");
        MyPetSkillTreeMobType skillTreeMobType = new MyPetSkillTreeMobType("default");
        if (skillFile.exists())
        {
            MWConfig = new NBTConfiguration(skillFile);
            loadSkillTree(MWConfig, skillTreeMobType, applyDefaultAndInheritance);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  default.st");
            }
        }

        for (MyPetType mobType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + mobType.getTypeName().toLowerCase() + ".st");

            skillTreeMobType = new MyPetSkillTreeMobType(mobType.getTypeName());

            if (!skillFile.exists())
            {
                if (applyDefaultAndInheritance)
                {
                    if (!skillTreeMobType.getMobTypeName().equals("default"))
                    {
                        addDefault(skillTreeMobType);
                    }
                    manageInheritance(skillTreeMobType);
                }
                continue;
            }

            MWConfig = new NBTConfiguration(skillFile);
            loadSkillTree(MWConfig, skillTreeMobType, applyDefaultAndInheritance);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  " + mobType.getTypeName().toLowerCase() + ".st");
            }
            skillTreeMobType.cleanupPlaces();
        }
    }

    private static void loadSkillTree(NBTConfiguration nbtConfiguration, MyPetSkillTreeMobType skillTreeMobType, boolean applyDefaultAndInheritance)
    {
        nbtConfiguration.load();
        NBTTagList skilltreeList = nbtConfiguration.getNBTTagCompound().getList("Skilltrees");
        for (int i_skilltree = 0 ; i_skilltree < skilltreeList.size() ; i_skilltree++)
        {
            NBTTagCompound skilltreeCompound = (NBTTagCompound) skilltreeList.get(i_skilltree);
            MyPetSkillTree skillTree = new MyPetSkillTree(skilltreeCompound.getString("Name"));
            int place = skilltreeCompound.getInt("Place");

            if (skilltreeCompound.hasKey("Inherits"))
            {
                skillTree.setInheritance(skilltreeCompound.getString("Inherits"));
            }
            if (skilltreeCompound.hasKey("Permission"))
            {
                skillTree.setPermission(skilltreeCompound.getString("Permission"));
            }
            if (skilltreeCompound.hasKey("Display"))
            {
                skillTree.setDisplayName(skilltreeCompound.getString("Display"));
            }
            NBTTagList levelList = skilltreeCompound.getList("Level");
            for (int i_level = 0 ; i_level < levelList.size() ; i_level++)
            {
                NBTTagCompound levelCompound = (NBTTagCompound) levelList.get(i_level);
                short thisLevel = levelCompound.getShort("Level");

                NBTTagList skillList = levelCompound.getList("Skills");
                for (int i_skill = 0 ; i_skill < skillList.size() ; i_skill++)
                {
                    NBTTagCompound skillCompound = (NBTTagCompound) skillList.get(i_skill);
                    String skillName = skillCompound.getString("Name");
                    if (MyPetSkills.isValidSkill(skillName))
                    {
                        MyPetSkillTreeSkill skill = MyPetSkills.getNewSkillInstance(skillName);
                        skill.setProperties(skillCompound.getCompound("Properties"));
                        skillTree.addSkillToLevel(thisLevel, skill);
                    }
                }
            }
            skillTreeMobType.addSkillTree(skillTree, place);
        }
        if (applyDefaultAndInheritance)
        {
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
            if (!skillTreeMobType.hasSkillTree(skillTreeName) && defaultSkillTreeMobType.hasSkillTree(skillTreeName))
            {
                MyPetSkillTree newSkillTree = defaultSkillTreeMobType.getSkillTree(skillTreeName).clone();
                for (MyPetSkillTreeLevel level : newSkillTree.getLevelList())
                {
                    for (MyPetSkillTreeSkill skill : level.getSkills())
                    {
                        skill.setIsInherited(true);
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
            if (!skillTreeMobType.hasSkillTree(skillTreeName))
            {
                continue;
            }
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
                            if (!skill.isAddedByInheritance() || MyPetConfig.inheritAlreadyInheritedSkills)
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

    public static List<String> saveSkillTrees(String configPath)
    {
        NBTConfiguration nbtConfig;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<String>();

        for (MyPetType petType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + petType.getTypeName().toLowerCase() + ".st");
            nbtConfig = new NBTConfiguration(skillFile);
            if (saveSkillTree(nbtConfig, petType.getTypeName()))
            {
                savedPetTypes.add(petType.getTypeName());
            }
        }

        skillFile = new File(configPath + File.separator + "default.st");
        nbtConfig = new NBTConfiguration(skillFile);
        if (saveSkillTree(nbtConfig, "default"))
        {
            savedPetTypes.add("default");
        }

        return savedPetTypes;
    }

    private static boolean saveSkillTree(NBTConfiguration nbtConfiguration, String petTypeName)
    {
        boolean saveMobType = false;

        if (MyPetSkillTreeMobType.getMobTypeByName(petTypeName).getSkillTreeNames().size() != 0)
        {
            MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByName(petTypeName);
            mobType.cleanupPlaces();

            NBTTagList skilltreeTagList = new NBTTagList();
            for (MyPetSkillTree skillTree : mobType.getSkillTrees())
            {
                NBTTagCompound skilltreeCompound = new NBTTagCompound();
                skilltreeCompound.setString("Name", skillTree.getName());
                skilltreeCompound.setInt("Place", mobType.getSkillTreePlace(skillTree));
                if (skillTree.hasInheritance())
                {
                    skilltreeCompound.setString("Inherits", skillTree.getInheritance());
                }
                if (skillTree.hasCustomPermissions())
                {
                    skilltreeCompound.setString("Permission", skillTree.getPermission());
                }
                if (skillTree.hasDisplayName())
                {
                    skilltreeCompound.setString("Display", skillTree.getDisplayName());
                }

                NBTTagList levelTagList = new NBTTagList();
                for (MyPetSkillTreeLevel level : skillTree.getLevelList())
                {
                    NBTTagCompound levelCompound = new NBTTagCompound();
                    levelCompound.setShort("Level", level.getLevel());

                    NBTTagList skillTagList = new NBTTagList();
                    for (MyPetSkillTreeSkill skill : skillTree.getLevel(level.getLevel()).getSkills())
                    {
                        if (!skill.isAddedByInheritance())
                        {
                            NBTTagCompound skillCompound = new NBTTagCompound();
                            skillCompound.setString("Name", skill.getName());
                            skillCompound.set("Properties", skill.getProperties());

                            skillTagList.add(skillCompound);
                        }
                    }
                    levelCompound.set("Skills", skillTagList);
                    levelTagList.add(levelCompound);
                }
                skilltreeCompound.set("Level", levelTagList);
                skilltreeTagList.add(skilltreeCompound);
            }
            nbtConfiguration.getNBTTagCompound().set("Skilltrees", skilltreeTagList);

            if (mobType.getSkillTreeNames().size() > 0)
            {
                nbtConfiguration.save();
                saveMobType = true;
            }
        }
        return saveMobType;
    }
}