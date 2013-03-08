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
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.SnakeYAML_Configuration;
import org.spout.nbt.*;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        SnakeYAML_Configuration skilltreeConfig;
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.yml");
        MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        if (skillFile.exists())
        {
            skilltreeConfig = new SnakeYAML_Configuration(skillFile);
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

            skilltreeConfig = new SnakeYAML_Configuration(skillFile);
            loadSkillTree(skilltreeConfig, skillTreeMobType, applyDefaultAndInheritance);
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("  " + mobType.getTypeName().toLowerCase() + ".yml");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSkillTree(SnakeYAML_Configuration yamlConfiguration, MyPetSkillTreeMobType skillTreeMobType, boolean applyDefaultAndInheritance)
    {
        yamlConfiguration.load();
        Map<String, Object> config = yamlConfiguration.getConfig();
        if (config == null || config.size() == 0)
        {
            return;
        }
        Map<String, Object> skilltrees = (Map<String, Object>) config.get("Skilltrees");
        for (String skillTreeName : skilltrees.keySet())
        {
            Integer place = null;
            //System.out.println(skillTreeName);
            MyPetSkillTree skillTree;
            Map<String, Object> skilltreeMap = (Map<String, Object>) skilltrees.get(skillTreeName);
            if (skilltreeMap.containsKey("Inherit"))
            {
                String inherit = (String) skilltreeMap.get("Inherit");
                skillTree = new MyPetSkillTree(skillTreeName, inherit);
            }
            else
            {
                skillTree = new MyPetSkillTree(skillTreeName);
            }
            if (skilltreeMap.containsKey("Permission"))
            {
                String permission = (String) skilltreeMap.get("Permission");
                skillTree.setPermission(permission);
            }
            if (skilltreeMap.containsKey("Display"))
            {
                String display = (String) skilltreeMap.get("Display");
                skillTree.setDisplayName(display);
            }
            if (skilltreeMap.containsKey("Place"))
            {
                if (skilltreeMap.get("Place") instanceof Integer)
                {
                    place = (Integer) skilltreeMap.get("Place");
                }
                else if (skilltreeMap.get("Place") instanceof String)
                {
                    if (MyPetUtil.isInt((String) skilltreeMap.get("Place")))
                    {
                        place = Integer.parseInt((String) skilltreeMap.get("Place"));
                    }
                }
            }

            if (skilltreeMap.containsKey("Level"))
            {
                Map<String, Object> levelMap = (Map<String, Object>) skilltreeMap.get("Level");
                for (String thisLevel : levelMap.keySet())
                {
                    //System.out.println("  " + thisLevel);
                    if (MyPetUtil.isInt(thisLevel))
                    {
                        short shortLevel = Short.parseShort(thisLevel);

                        Map<String, Object> skillMap = (Map<String, Object>) levelMap.get(thisLevel);

                        if (skillMap.size() == 0)
                        {
                            skillTree.addLevel(shortLevel);
                            continue;
                        }
                        for (String thisSkill : skillMap.keySet())
                        {
                            //System.out.println("    " + thisSkill);
                            if (MyPetSkills.isValidSkill(thisSkill))
                            {
                                Map<String, Object> propertyMap = (Map<String, Object>) skillMap.get(thisSkill);
                                ISkillInfo skill = MyPetSkillsInfo.getNewSkillInfoInstance(thisSkill);

                                if (skill != null)
                                {
                                    SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                                    if (sp != null)
                                    {
                                        CompoundTag propertiesCompound = skill.getProperties();
                                        for (int i = 0 ; i < sp.parameterNames().length ; i++)
                                        {
                                            String propertyName = sp.parameterNames()[i];
                                            NBTdatatypes propertyType = sp.parameterTypes()[i];
                                            if (!propertiesCompound.getValue().containsKey(propertyName) && propertyMap.containsKey(propertyName))
                                            {
                                                String value = String.valueOf(propertyMap.get(propertyName));
                                                //System.out.println("      " + propertyName + " : " + value);
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
                                                        if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false"))
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
                                    }
                                    skillTree.addSkillToLevel(shortLevel, skill);
                                }
                            }
                        }
                    }
                }
            }
            if (place != null)
            {
                skillTreeMobType.addSkillTree(skillTree, place);
            }
            else
            {
                skillTreeMobType.addSkillTree(skillTree);
            }
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
        SnakeYAML_Configuration yamlConfiguration;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<String>();

        for (MyPetType petType : MyPetType.values())
        {
            skillFile = new File(configPath + File.separator + petType.getTypeName().toLowerCase() + ".yml");
            yamlConfiguration = new SnakeYAML_Configuration(skillFile);
            if (saveSkillTree(yamlConfiguration, petType.getTypeName()))
            {
                savedPetTypes.add(petType.getTypeName());
            }
        }

        skillFile = new File(configPath + File.separator + "default.yml");
        yamlConfiguration = new SnakeYAML_Configuration(skillFile);
        if (saveSkillTree(yamlConfiguration, "default"))
        {
            savedPetTypes.add("default");
        }

        return savedPetTypes;
    }

    protected boolean saveSkillTree(SnakeYAML_Configuration yamlConfiguration, String petTypeName)
    {
        boolean saveMobType = false;

        yamlConfiguration.clearConfig();
        Map<String, Object> config = yamlConfiguration.getConfig();
        Map<String, Object> skilltreesMap = new LinkedHashMap<String, Object>();
        config.put("Skilltrees", skilltreesMap);

        if (MyPetSkillTreeMobType.getMobTypeByName(petTypeName).getSkillTreeNames().size() != 0)
        {
            MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByName(petTypeName);
            mobType.cleanupPlaces();

            for (MyPetSkillTree skillTree : mobType.getSkillTrees())
            {
                Map<String, Object> skilltreeMap = new LinkedHashMap<String, Object>();
                skilltreesMap.put(skillTree.getName(), skilltreeMap);

                skilltreeMap.put("Place", mobType.getSkillTreePlace(skillTree));
                if (skillTree.hasInheritance())
                {
                    skilltreeMap.put("Inherits", skillTree.getInheritance());
                }
                if (skillTree.hasCustomPermissions())
                {
                    skilltreeMap.put("Permission", skillTree.getPermission());
                }
                if (skillTree.hasDisplayName())
                {
                    skilltreeMap.put("Display", skillTree.getDisplayName());
                }

                Map<String, Object> levelsMap = new LinkedHashMap<String, Object>();
                skilltreeMap.put("Level", levelsMap);
                for (MyPetSkillTreeLevel level : skillTree.getLevelList())
                {
                    Map<String, Object> skillMap = new LinkedHashMap<String, Object>();
                    levelsMap.put("" + level.getLevel(), skillMap);
                    for (ISkillInfo skill : skillTree.getLevel(level.getLevel()).getSkills())
                    {
                        if (!skill.isAddedByInheritance())
                        {
                            SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                            if (sp != null)
                            {
                                Map<String, Object> propertyMap = new LinkedHashMap<String, Object>();
                                skillMap.put(skill.getName(), propertyMap);
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
                                                propertyMap.put(propertyName, ((ShortTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Int:
                                                propertyMap.put(propertyName, ((IntTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Long:
                                                propertyMap.put(propertyName, ((LongTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Float:
                                                propertyMap.put(propertyName, ((FloatTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Double:
                                                propertyMap.put(propertyName, ((DoubleTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Byte:
                                                propertyMap.put(propertyName, ((ByteTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Boolean:
                                                propertyMap.put(propertyName, ((ByteTag) propertiesCompound.getValue().get(propertyName)).getBooleanValue());
                                                break;
                                            case String:
                                                propertyMap.put(propertyName, ((StringTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                        }
                                    }
                                    else
                                    {
                                        propertyMap.put(propertyName, sp.parameterDefaultValues()[i]);
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
                yamlConfiguration.save();
                saveMobType = true;
            }
        }
        return saveMobType;
    }
}