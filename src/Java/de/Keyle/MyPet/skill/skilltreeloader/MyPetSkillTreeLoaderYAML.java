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

import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.*;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.YAML_Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.spout.nbt.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyPetSkillTreeLoaderYAML extends MyPetSkillTreeLoader
{
    public static MyPetSkillTreeLoaderYAML getSkilltreeLoader()
    {
        return new MyPetSkillTreeLoaderYAML();
    }

    private MyPetSkillTreeLoaderYAML()
    {
    }

    @Override
    public void loadSkillTrees(String configPath)
    {
        loadSkillTrees(configPath, true);
    }

    public void loadSkillTrees(String configPath, boolean applyDefaultAndInheritance)
    {
        YAML_Configuration skilltreeConfig;
        if (MyPetUtil.getDebugLogger() != null)
        {
            MyPetUtil.getDebugLogger().info("Loading yaml skill configs in: " + configPath);
        }
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.yml");
        MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        if (skillFile.exists())
        {
            skilltreeConfig = new YAML_Configuration(skillFile);
            loadSkillTree(skilltreeConfig, skillTreeMobType, false);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  default.yml");
            }
        }

        for (MyPetType mobType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + mobType.getTypeName().toLowerCase() + ".yml");

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

            skilltreeConfig = new YAML_Configuration(skillFile);
            loadSkillTree(skilltreeConfig, skillTreeMobType, applyDefaultAndInheritance);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  " + mobType.getTypeName().toLowerCase() + ".yml");
            }
        }
    }

    private void loadSkillTree(YAML_Configuration yamlConfiguration, MyPetSkillTreeMobType skillTreeMobType, boolean applyDefaultAndInheritance)
    {
        FileConfiguration config = yamlConfiguration.getConfig();
        ConfigurationSection sec = config.getConfigurationSection("Skilltrees");
        if (sec == null)
        {
            return;
        }
        for (String skillTreeName : sec.getKeys(false))
        {
            MyPetSkillTree skillTree;
            if (config.contains("Skilltrees." + skillTreeName + ".Inherit"))
            {
                String inherit = config.getString("Skilltrees." + skillTreeName + ".Inherit");
                skillTree = new MyPetSkillTree(skillTreeName, inherit);
            }
            else
            {
                skillTree = new MyPetSkillTree(skillTreeName);
            }
            if (config.contains("Skilltrees." + skillTreeName + ".Permission"))
            {
                String permission = config.getString("Skilltrees." + skillTreeName + ".Permission");
                skillTree.setPermission(permission);
            }
            if (config.contains("Skilltrees." + skillTreeName + ".Display"))
            {
                String display = config.getString("Skilltrees." + skillTreeName + ".Display");
                skillTree.setDisplayName(display);
            }

            Set<String> level = config.getConfigurationSection("Skilltrees." + skillTreeName + ".Level").getKeys(false);
            for (String thisLevel : level)
            {
                if (MyPetUtil.isInt(thisLevel))
                {
                    short shortLevel = Short.parseShort(thisLevel);

                    Set<String> skillsOfThisLevel = config.getConfigurationSection("Skilltrees." + skillTreeName + ".Level." + thisLevel).getKeys(false);
                    for (String thisSkill : skillsOfThisLevel)
                    {
                        if (MyPetSkills.isValidSkill(thisSkill))
                        {
                            MyPetSkillTreeSkill skill = MyPetSkills.getNewSkillInstance(thisSkill);

                            SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                            if (sp != null)
                            {
                                CompoundTag propertiesCompound = skill.getProperties();
                                for (int i = 0 ; i < sp.parameterNames().length ; i++)
                                {
                                    String propertyName = sp.parameterNames()[i];
                                    NBTdatatypes propertyType = sp.parameterTypes()[i];
                                    if (!propertiesCompound.getValue().containsKey(propertyName) && config.contains("Skilltrees." + skillTreeName + ".Level." + thisLevel + "." + thisSkill + "." + propertyName))
                                    {
                                        String value = String.valueOf(config.getString("Skilltrees." + skillTreeName + ".Level." + thisLevel + "." + thisSkill + "." + propertyName));
                                        switch (propertyType)
                                        {
                                            case Short:
                                                if (MyPetUtil.isShort(value))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new ShortTag(propertyName, Short.parseShort(value)));
                                                }
                                                break;
                                            case Int:
                                                if (MyPetUtil.isInt(value))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new IntTag(propertyName, Integer.parseInt(value)));
                                                }
                                                break;
                                            case Long:
                                                if (MyPetUtil.isLong(value))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new LongTag(propertyName, Long.parseLong(value)));
                                                }
                                                break;
                                            case Float:
                                                if (MyPetUtil.isFloat(value))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new FloatTag(propertyName, Float.parseFloat(value)));
                                                }
                                                break;
                                            case Double:
                                                if (MyPetUtil.isDouble(value))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new DoubleTag(propertyName, Double.parseDouble(value)));
                                                }
                                                break;
                                            case Byte:
                                                if (MyPetUtil.isByte(value))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new ByteTag(propertyName, Byte.parseByte(value)));
                                                }
                                                break;
                                            case Boolean:
                                                if (value == null || value.equalsIgnoreCase("") || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false"))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new ByteTag(propertyName, false));
                                                }
                                                else if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true"))
                                                {
                                                    propertiesCompound.getValue().put(propertyName, new ByteTag(propertyName, true));
                                                }
                                                break;
                                            case String:
                                                propertiesCompound.getValue().put(propertyName, new StringTag(propertyName, value));
                                                break;
                                        }
                                    }
                                }
                                skill.setProperties(propertiesCompound);
                                skill.setDefaultProperties();
                                skillTree.addSkillToLevel(shortLevel, skill);
                            }
                        }
                    }
                }
            }
            skillTreeMobType.addSkillTree(skillTree);
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

    @Override
    public List<String> saveSkillTrees(String configPath)
    {
        YAML_Configuration yamlConfiguration;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<String>();

        for (MyPetType petType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + petType.getTypeName().toLowerCase() + ".yml");
            yamlConfiguration = new YAML_Configuration(skillFile);
            if (saveSkillTree(yamlConfiguration, petType.getTypeName()))
            {
                savedPetTypes.add(petType.getTypeName());
            }
        }

        skillFile = new File(configPath + File.separator + "default.yml");
        yamlConfiguration = new YAML_Configuration(skillFile);
        if (saveSkillTree(yamlConfiguration, "default"))
        {
            savedPetTypes.add("default");
        }

        return savedPetTypes;
    }

    //"Skilltrees." + skillTreeName + ".Level." + thisLevel + "." + thisSkill + "." + propertyName
    protected boolean saveSkillTree(YAML_Configuration yamlConfiguration, String petTypeName)
    {
        boolean saveMobType = false;

        yamlConfiguration.clearConfig();
        FileConfiguration config = yamlConfiguration.getConfig();

        if (MyPetSkillTreeMobType.getMobTypeByName(petTypeName).getSkillTreeNames().size() != 0)
        {
            MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByName(petTypeName);
            mobType.cleanupPlaces();

            for (MyPetSkillTree skillTree : mobType.getSkillTrees())
            {
                config.set("Skilltrees." + skillTree.getName() + ".Place", mobType.getSkillTreePlace(skillTree));
                if (skillTree.hasInheritance())
                {
                    config.set("Skilltrees." + skillTree.getName() + ".Inherits", skillTree.getInheritance());
                }
                if (skillTree.hasCustomPermissions())
                {
                    config.set("Skilltrees." + skillTree.getName() + ".Permission", skillTree.getPermission());
                }
                if (skillTree.hasDisplayName())
                {
                    config.set("Skilltrees." + skillTree.getName() + ".Display", skillTree.getDisplayName());
                }

                for (MyPetSkillTreeLevel level : skillTree.getLevelList())
                {
                    for (MyPetSkillTreeSkill skill : skillTree.getLevel(level.getLevel()).getSkills())
                    {
                        if (!skill.isAddedByInheritance())
                        {
                            SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                            if (sp != null)
                            {
                                for (int i = 0 ; i < sp.parameterNames().length ; i++)
                                {
                                    String propertyName = sp.parameterNames()[i];
                                    NBTdatatypes propertyType = sp.parameterTypes()[i];
                                    CompoundTag propertiesCompound = skill.getProperties();
                                    if (propertiesCompound.getValue().containsKey(propertyName))
                                    {
                                        switch (propertyType)
                                        {
                                            case Short:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((ShortTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Int:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((IntTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Long:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((LongTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Float:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((FloatTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Double:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((DoubleTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Byte:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((ByteTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Boolean:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((ByteTag) propertiesCompound.getValue().get(propertyName)).getBooleanValue());
                                                break;
                                            case String:
                                                config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, ((StringTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                        }
                                    }
                                    else
                                    {
                                        config.set("Skilltrees." + skillTree.getName() + ".Level." + level.getLevel() + "." + skill.getName() + "." + propertyName, sp.parameterDefaultValues()[i]);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (mobType.getSkillTreeNames().size() > 0)
            {
                System.out.println("save");
                yamlConfiguration.saveConfig();
                saveMobType = true;
            }
        }
        return saveMobType;
    }
}