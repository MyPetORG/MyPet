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

import de.Keyle.MyPet.skill.skills.SkillProperties;
import de.Keyle.MyPet.skill.skills.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skills.SkillsInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.configuration.ConfigurationJSON;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.keyle.knbt.*;
import org.bukkit.ChatColor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.Arrays;

public class SkillTreeLoaderJSON extends SkillTreeLoader {
    public static SkillTreeLoaderJSON getSkilltreeLoader() {
        return new SkillTreeLoaderJSON();
    }

    private SkillTreeLoaderJSON() {
    }

    public void loadSkillTrees(String configPath, String[] mobtypes) {
        ConfigurationJSON skilltreeConfig;
        DebugLogger.info("Loading json skill configs in: " + configPath);
        File skillFile;

        for (String mobType : mobtypes) {
            skillFile = new File(configPath + File.separator + mobType.toLowerCase() + ".json");

            SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(mobType);

            if (!skillFile.exists()) {
                continue;
            }
            skilltreeConfig = new ConfigurationJSON(skillFile);
            if (skilltreeConfig.load()) {
                try {
                    loadSkillTree(skilltreeConfig, skillTreeMobType);
                    DebugLogger.info("  " + mobType.toLowerCase() + ".json");
                } catch (Exception e) {
                    MyPetLogger.write(ChatColor.RED + "  Error while loading skilltrees from: " + mobType.toLowerCase() + ".json");
                    e.printStackTrace();
                    DebugLogger.printThrowable(e);
                }
            }
            skillTreeMobType.cleanupPlaces();
        }
    }

    protected void loadSkillTree(ConfigurationJSON jsonConfiguration, SkillTreeMobType skillTreeMobType) {
        JSONArray skilltreeList = (JSONArray) jsonConfiguration.getJSONObject().get("Skilltrees");
        for (Object st_object : skilltreeList) {
            SkillTree skillTree;
            int place;
            try {
                JSONObject skilltreeObject = (JSONObject) st_object;
                skillTree = new SkillTree((String) skilltreeObject.get("Name"));
                place = Integer.parseInt(String.valueOf(skilltreeObject.get("Place")));

                if (skilltreeObject.containsKey("Inherits")) {
                    skillTree.setInheritance((String) skilltreeObject.get("Inherits"));
                }
                if (skilltreeObject.containsKey("Permission")) {
                    skillTree.setPermission((String) skilltreeObject.get("Permission"));
                }
                if (skilltreeObject.containsKey("Display")) {
                    skillTree.setDisplayName((String) skilltreeObject.get("Display"));
                }
                if (skilltreeObject.containsKey("Description")) {
                    JSONArray descriptionArray = (JSONArray) skilltreeObject.get("Description");
                    for (Object lvl_object : descriptionArray) {
                        skillTree.addDescriptionLine(String.valueOf(lvl_object));
                    }
                }

                JSONArray levelList = (JSONArray) skilltreeObject.get("Level");
                for (Object lvl_object : levelList) {
                    JSONObject levelObject = (JSONObject) lvl_object;
                    int thisLevel = Integer.parseInt(String.valueOf(levelObject.get("Level")));

                    SkillTreeLevel newLevel = skillTree.addLevel(thisLevel);
                    if (levelObject.containsKey("Message")) {
                        String message = (String) levelObject.get("Message");
                        newLevel.setLevelupMessage(message);
                    }

                    JSONArray skillList = (JSONArray) levelObject.get("Skills");
                    for (Object skill_object : skillList) {
                        JSONObject skillObject = (JSONObject) skill_object;
                        String skillName = (String) skillObject.get("Name");
                        JSONObject skillPropertyObject = (JSONObject) skillObject.get("Properties");

                        if (SkillsInfo.getSkillInfoClass(skillName) != null) {
                            ISkillInfo skill = SkillsInfo.getNewSkillInfoInstance(skillName);

                            if (skill != null) {
                                SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                                if (sp != null) {
                                    TagCompound propertiesCompound = skill.getProperties();
                                    for (int i = 0; i < sp.parameterNames().length; i++) {
                                        String propertyName = sp.parameterNames()[i];
                                        NBTdatatypes propertyType = sp.parameterTypes()[i];
                                        if (!propertiesCompound.getCompoundData().containsKey(propertyName) && skillPropertyObject.containsKey(propertyName)) {
                                            String value = String.valueOf(skillPropertyObject.get(propertyName));
                                            switch (propertyType) {
                                                case Short:
                                                    if (Util.isShort(value)) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagShort(Short.parseShort(value)));
                                                    }
                                                    break;
                                                case Int:
                                                    if (Util.isInt(value)) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagInt(Integer.parseInt(value)));
                                                    }
                                                    break;
                                                case Long:
                                                    if (Util.isLong(value)) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagLong(Long.parseLong(value)));
                                                    }
                                                    break;
                                                case Float:
                                                    if (Util.isFloat(value)) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagFloat(Float.parseFloat(value)));
                                                    }
                                                    break;
                                                case Double:
                                                    if (Util.isDouble(value)) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagDouble(Double.parseDouble(value)));
                                                    }
                                                    break;
                                                case Byte:
                                                    if (Util.isByte(value)) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagByte(Byte.parseByte(value)));
                                                    }
                                                    break;
                                                case Boolean:
                                                    if (value == null || value.equalsIgnoreCase("") || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false")) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagByte(false));
                                                    } else if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true")) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagByte(true));
                                                    }
                                                    break;
                                                case String:
                                                    propertiesCompound.getCompoundData().put(propertyName, new TagString(value));
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
            } catch (Exception e) {
                DebugLogger.info("Problem in" + skillTreeMobType.getMobTypeName());
                DebugLogger.info(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                MyPetLogger.write(ChatColor.RED + "Error in " + skillTreeMobType.getMobTypeName().toLowerCase() + ".json -> Skilltree not loaded.");
            }
        }
    }
}