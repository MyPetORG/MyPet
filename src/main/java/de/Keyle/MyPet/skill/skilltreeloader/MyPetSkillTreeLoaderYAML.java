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

import de.Keyle.MyPet.skill.*;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.configuration.SnakeYAML_Configuration;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.spout.nbt.*;

import java.io.File;
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

    public void loadSkillTrees(String configPath, String[] mobtypes)
    {
        SnakeYAML_Configuration skilltreeConfig;
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.yml");
        MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        if (skillFile.exists())
        {
            skilltreeConfig = new SnakeYAML_Configuration(skillFile);
            if (skilltreeConfig.load())
            {
                try
                {
                    loadSkillTree(skilltreeConfig, skillTreeMobType);
                    DebugLogger.info("  default.yml");
                }
                catch (Exception e)
                {
                    MyPetLogger.write(ChatColor.RED + "  Error while loading skilltrees from: default.yml");
                    e.printStackTrace();
                    MyPetLogger.write(ChatColor.RED + "  Error while loading skilltrees from: default.yml");
                }
            }
        }

        for (String mobType : mobtypes)
        {
            skillFile = new File(configPath + File.separator + mobType.toLowerCase() + ".yml");

            skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(mobType);

            if (!skillFile.exists())
            {
                continue;
            }

            skilltreeConfig = new SnakeYAML_Configuration(skillFile);
            if (skilltreeConfig.load())
            {
                try
                {
                    loadSkillTree(skilltreeConfig, skillTreeMobType);
                    DebugLogger.info("  " + mobType.toLowerCase() + ".yml");
                }
                catch (Exception e)
                {
                    MyPetLogger.write(ChatColor.RED + "  Error while loading skilltrees from: " + mobType.toLowerCase() + ".yml");
                    e.printStackTrace();
                    MyPetLogger.write(ChatColor.RED + "  Error while loading skilltrees from: " + mobType.toLowerCase() + ".yml");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSkillTree(SnakeYAML_Configuration yamlConfiguration, MyPetSkillTreeMobType skillTreeMobType)
    {
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
            if (skilltreeMap == null)
            {
                continue;
            }
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
                if (levelMap == null)
                {
                    continue;
                }
                for (String thisLevel : levelMap.keySet())
                {
                    //System.out.println("  " + thisLevel);
                    if (MyPetUtil.isInt(thisLevel))
                    {
                        int lvl = Integer.parseInt(thisLevel);

                        Map<String, Object> skillMap = (Map<String, Object>) levelMap.get(thisLevel);

                        if (skillMap == null)
                        {
                            continue;
                        }

                        MyPetSkillTreeLevel newLevel = skillTree.addLevel(lvl);
                        if (levelMap.containsKey("Message"))
                        {
                            String message = (String) levelMap.get("Message");
                            newLevel.setLevelupMessage(message);
                        }

                        if (skillMap.size() == 0)
                        {
                            continue;
                        }
                        for (String thisSkill : skillMap.keySet())
                        {
                            //System.out.println("    " + thisSkill);
                            if (MyPetSkillsInfo.isValidSkill(thisSkill))
                            {
                                Map<String, Object> propertyMap = (Map<String, Object>) skillMap.get(thisSkill);
                                if (propertyMap == null)
                                {
                                    continue;
                                }
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
                                    skillTree.addSkillToLevel(lvl, skill);
                                }
                                else
                                {
                                    System.out.println("null: " + thisSkill);
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
    }
}