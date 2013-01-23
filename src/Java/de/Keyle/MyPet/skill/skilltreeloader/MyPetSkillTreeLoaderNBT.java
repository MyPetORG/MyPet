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

package de.Keyle.MyPet.skill.skilltreeloader;


import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.*;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.NBT_Configuration;
import net.minecraft.server.v1_4_R1.NBTTagCompound;
import net.minecraft.server.v1_4_R1.NBTTagList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyPetSkillTreeLoaderNBT extends MyPetSkillTreeLoader
{
    public static MyPetSkillTreeLoaderNBT getSkilltreeLoader()
    {
        return new MyPetSkillTreeLoaderNBT();
    }

    private MyPetSkillTreeLoaderNBT()
    {
    }

    public void loadSkillTrees(String configPath)
    {
        loadSkillTrees(configPath, true);
    }

    public void loadSkillTrees(String configPath, boolean applyDefaultAndInheritance)
    {
        NBT_Configuration skilltreeConfig;
        if (MyPetUtil.getDebugLogger() != null)
        {
            MyPetUtil.getDebugLogger().info("Loading nbt skill configs in: " + configPath);
        }
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.st");
        MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        if (skillFile.exists())
        {
            skilltreeConfig = new NBT_Configuration(skillFile);
            loadSkillTree(skilltreeConfig, skillTreeMobType, applyDefaultAndInheritance);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  default.st");
            }
        }

        for (MyPetType mobType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + mobType.getTypeName().toLowerCase() + ".st");

            skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(mobType.getTypeName());

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

            skilltreeConfig = new NBT_Configuration(skillFile);
            loadSkillTree(skilltreeConfig, skillTreeMobType, applyDefaultAndInheritance);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  " + mobType.getTypeName().toLowerCase() + ".st");
            }
            skillTreeMobType.cleanupPlaces();
        }
    }

    protected void loadSkillTree(NBT_Configuration nbtConfiguration, MyPetSkillTreeMobType skillTreeMobType, boolean applyDefaultAndInheritance)
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
                skillTree.addLevel(thisLevel);

                NBTTagList skillList = levelCompound.getList("Skills");
                for (int i_skill = 0 ; i_skill < skillList.size() ; i_skill++)
                {
                    NBTTagCompound skillCompound = (NBTTagCompound) skillList.get(i_skill);
                    String skillName = skillCompound.getString("Name");
                    if (MyPetSkills.isValidSkill(skillName))
                    {
                        NBTTagCompound skillPropertyCompound = skillCompound.getCompound("Properties");
                        MyPetSkillTreeSkill skill = MyPetSkills.getNewSkillInstance(skillName);
                        skill.setProperties(skillPropertyCompound);
                        skill.setDefaultProperties();
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

    public List<String> saveSkillTrees(String configPath)
    {
        NBT_Configuration nbtConfig;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<String>();

        for (MyPetType petType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + petType.getTypeName().toLowerCase() + ".st");
            nbtConfig = new NBT_Configuration(skillFile);
            if (saveSkillTree(nbtConfig, petType.getTypeName()))
            {
                savedPetTypes.add(petType.getTypeName());
            }
        }

        skillFile = new File(configPath + File.separator + "default.st");
        nbtConfig = new NBT_Configuration(skillFile);
        if (saveSkillTree(nbtConfig, "default"))
        {
            savedPetTypes.add("default");
        }

        return savedPetTypes;
    }

    protected boolean saveSkillTree(NBT_Configuration nbtConfiguration, String petTypeName)
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