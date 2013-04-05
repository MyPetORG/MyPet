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
import de.Keyle.MyPet.util.configuration.JSON_Configuration;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.spout.nbt.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyPetSkillTreeLoaderJSON extends MyPetSkillTreeLoader
{
    public static MyPetSkillTreeLoaderJSON getSkilltreeLoader()
    {
        return new MyPetSkillTreeLoaderJSON();
    }

    private MyPetSkillTreeLoaderJSON()
    {
    }

    @Override
    public void loadSkillTrees(String configPath, String[] mobtypes)
    {
        loadSkillTrees(configPath, mobtypes, true);
    }

    public void loadSkillTrees(String configPath, String[] mobtypes, boolean applyDefaultAndInheritance)
    {
        JSON_Configuration skilltreeConfig;
        DebugLogger.info("Loading json skill configs in: " + configPath);
        File skillFile;

        skillFile = new File(configPath + File.separator + "default.json");
        MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName("default");
        if (skillFile.exists())
        {
            skilltreeConfig = new JSON_Configuration(skillFile);
            if (skilltreeConfig.load())
            {
                loadSkillTree(skilltreeConfig, skillTreeMobType, applyDefaultAndInheritance);
                DebugLogger.info("  default.json");
            }
        }

        for (String mobType : mobtypes)
        {
            skillFile = new File(configPath + File.separator + mobType.toLowerCase() + ".json");

            skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(mobType);

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
            skilltreeConfig = new JSON_Configuration(skillFile);
            if (skilltreeConfig.load())
            {
                loadSkillTree(skilltreeConfig, skillTreeMobType, applyDefaultAndInheritance);
                DebugLogger.info("  " + mobType.toLowerCase() + ".json");
            }
            skillTreeMobType.cleanupPlaces();
        }
    }

    protected void loadSkillTree(JSON_Configuration jsonConfiguration, MyPetSkillTreeMobType skillTreeMobType, boolean applyDefaultAndInheritance)
    {
        JSONArray skilltreeList = (JSONArray) jsonConfiguration.getJSONObject().get("Skilltrees");
        for (Object st_object : skilltreeList)
        {
            MyPetSkillTree skillTree;
            int place;
            try
            {
                JSONObject skilltreeObject = (JSONObject) st_object;
                skillTree = new MyPetSkillTree((String) skilltreeObject.get("Name"));
                place = Integer.parseInt(String.valueOf(skilltreeObject.get("Place")));

                if (skilltreeObject.containsKey("Inherits"))
                {
                    skillTree.setInheritance((String) skilltreeObject.get("Inherits"));
                }
                if (skilltreeObject.containsKey("Permission"))
                {
                    skillTree.setPermission((String) skilltreeObject.get("Permission"));
                }
                if (skilltreeObject.containsKey("Display"))
                {
                    skillTree.setDisplayName((String) skilltreeObject.get("Display"));
                }
                JSONArray levelList = (JSONArray) skilltreeObject.get("Level");
                for (Object lvl_object : levelList)
                {
                    JSONObject levelObject = (JSONObject) lvl_object;
                    short thisLevel = Short.parseShort(String.valueOf(levelObject.get("Level")));
                    skillTree.addLevel(thisLevel);

                    JSONArray skillList = (JSONArray) levelObject.get("Skills");
                    for (Object skill_object : skillList)
                    {
                        JSONObject skillObject = (JSONObject) skill_object;
                        String skillName = (String) skillObject.get("Name");
                        JSONObject skillPropertyObject = (JSONObject) skillObject.get("Properties");

                        if (MyPetSkillsInfo.isValidSkill(skillName))
                        {
                            ISkillInfo skill = MyPetSkillsInfo.getNewSkillInfoInstance(skillName);

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
                                        if (!propertiesCompound.getValue().containsKey(propertyName) && skillPropertyObject.containsKey(propertyName))
                                        {
                                            String value = String.valueOf(skillPropertyObject.get(propertyName));
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
                                    skillTree.addSkillToLevel(thisLevel, skill);
                                }
                            }
                        }
                    }
                }
                skillTreeMobType.addSkillTree(skillTree, place);
            }
            catch (Exception ignored)
            {
                DebugLogger.info("Problem in" + skillTreeMobType.getMobTypeName());
                DebugLogger.info(Arrays.toString(ignored.getStackTrace()));
                ignored.printStackTrace();
                MyPetLogger.write(ChatColor.RED + "Error in " + skillTreeMobType.getMobTypeName().toLowerCase() + ".json -> Skilltree not loaded.");
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
    public List<String> saveSkillTrees(String configPath, String[] mobtypes)
    {
        JSON_Configuration jsonConfig;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<String>();

        for (String petType : mobtypes)
        {
            skillFile = new File(configPath + File.separator + petType.toLowerCase() + ".json");
            jsonConfig = new JSON_Configuration(skillFile);
            if (saveSkillTree(jsonConfig, petType))
            {
                savedPetTypes.add(petType);
            }
        }

        skillFile = new File(configPath + File.separator + "default.json");
        jsonConfig = new JSON_Configuration(skillFile);
        if (saveSkillTree(jsonConfig, "default"))
        {
            savedPetTypes.add("default");
        }

        return savedPetTypes;
    }

    @SuppressWarnings("unchecked")
    private boolean saveSkillTree(JSON_Configuration jsonConfiguration, String petTypeName)
    {
        boolean saveMobType = false;

        if (MyPetSkillTreeMobType.getMobTypeByName(petTypeName).getSkillTreeNames().size() != 0)
        {
            MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByName(petTypeName);
            mobType.cleanupPlaces();

            JSONArray skilltreeList = new JSONArray();
            for (MyPetSkillTree skillTree : mobType.getSkillTrees())
            {
                JSONObject skilltreeObject = new JSONObject();
                skilltreeObject.put("Name", skillTree.getName());
                skilltreeObject.put("Place", mobType.getSkillTreePlace(skillTree));
                if (skillTree.hasInheritance())
                {
                    skilltreeObject.put("Inherits", skillTree.getInheritance());
                }
                if (skillTree.hasCustomPermissions())
                {
                    skilltreeObject.put("Permission", skillTree.getPermission());
                }
                if (skillTree.hasDisplayName())
                {
                    skilltreeObject.put("Display", skillTree.getDisplayName());
                }

                JSONArray levelList = new JSONArray();
                for (MyPetSkillTreeLevel level : skillTree.getLevelList())
                {
                    JSONObject levelObject = new JSONObject();
                    levelObject.put("Level", level.getLevel());

                    JSONArray skillList = new JSONArray();
                    for (ISkillInfo skill : skillTree.getLevel(level.getLevel()).getSkills())
                    {
                        if (!skill.isAddedByInheritance())
                        {
                            JSONObject skillObject = new JSONObject();
                            skillObject.put("Name", skill.getName());
                            JSONObject skillProperties = new JSONObject();
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
                                                skillProperties.put(propertyName, ((ShortTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Int:
                                                skillProperties.put(propertyName, ((IntTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Long:
                                                skillProperties.put(propertyName, ((LongTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Float:
                                                skillProperties.put(propertyName, ((FloatTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Double:
                                                skillProperties.put(propertyName, ((DoubleTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Byte:
                                                skillProperties.put(propertyName, ((ByteTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                            case Boolean:
                                                skillProperties.put(propertyName, ((ByteTag) propertiesCompound.getValue().get(propertyName)).getBooleanValue());
                                                break;
                                            case String:
                                                skillProperties.put(propertyName, ((StringTag) propertiesCompound.getValue().get(propertyName)).getValue());
                                                break;
                                        }
                                    }
                                    else
                                    {
                                        skillProperties.put(propertyName, sp.parameterDefaultValues()[i]);
                                    }
                                }
                            }
                            skillObject.put("Properties", skillProperties);

                            skillList.add(skillObject);
                        }
                    }
                    levelObject.put("Skills", skillList);
                    levelList.add(levelObject);
                }
                skilltreeObject.put("Level", levelList);
                skilltreeList.add(skilltreeObject);
            }
            jsonConfiguration.getJSONObject().put("Skilltrees", skilltreeList);
            if (mobType.getSkillTreeNames().size() > 0)
            {
                jsonConfiguration.save();
                saveMobType = true;
            }
        }
        return saveMobType;
    }
}
