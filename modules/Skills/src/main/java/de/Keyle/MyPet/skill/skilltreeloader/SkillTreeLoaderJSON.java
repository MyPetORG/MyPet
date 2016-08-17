/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillProperties;
import de.Keyle.MyPet.api.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.api.skill.SkillsInfo;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoader;
import de.Keyle.MyPet.api.util.configuration.ConfigurationJSON;
import de.keyle.knbt.*;
import org.bukkit.ChatColor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SkillTreeLoaderJSON extends SkillTreeLoader {
    public static SkillTreeLoaderJSON getSkilltreeLoader() {
        return new SkillTreeLoaderJSON();
    }

    private SkillTreeLoaderJSON() {
    }

    public void loadSkillTrees(String configPath, List<String> mobtypes) {
        ConfigurationJSON skilltreeConfig;
        File skillFile;

        for (String mobType : mobtypes) {
            skillFile = new File(configPath + File.separator + mobType.toLowerCase() + ".json");

            SkillTreeMobType skillTreeMobType;
            if (mobType.equalsIgnoreCase("default")) {
                skillTreeMobType = SkillTreeMobType.DEFAULT;
            } else {
                skillTreeMobType = SkillTreeMobType.getMobTypeByName(mobType);
            }

            if (!skillFile.exists()) {
                continue;
            }
            skilltreeConfig = new ConfigurationJSON(skillFile);
            if (skilltreeConfig.load()) {
                try {
                    loadSkillTree(skilltreeConfig, skillTreeMobType);
                    MyPetApi.getLogger().info("Skilltrees from " + mobType.toLowerCase() + ".json loaded.");
                } catch (Exception e) {
                    MyPetApi.getLogger().warning(ChatColor.RED + "  Error while loading skilltrees from: " + mobType.toLowerCase() + ".json");
                    e.printStackTrace();
                }
            }
            skillTreeMobType.cleanupPlaces();
        }
    }

    protected void loadSkillTree(ConfigurationJSON jsonConfiguration, SkillTreeMobType skillTreeMobType) {
        JSONObject skilltreeList = jsonConfiguration.getJSONObject();
        for (Object o : skilltreeList.keySet()) {
            SkillTree skillTree;
            int place;
            try {
                JSONObject skilltreeObject = (JSONObject) skilltreeList.get(o);
                String skilltreeName = o.toString();

                if (skillTreeMobType.hasSkillTree(skilltreeName)) {
                    continue;
                }

                skillTree = new SkillTree(skilltreeName);
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
                if (skilltreeObject.containsKey("MaxLevel")) {
                    skillTree.setMaxLevel(((Number) skilltreeObject.get("MaxLevel")).intValue());
                }
                if (skilltreeObject.containsKey("RequiredLevel")) {
                    skillTree.setRequiredLevel(((Number) skilltreeObject.get("RequiredLevel")).intValue());
                }
                if (skilltreeObject.containsKey("IconItem")) {
                    skillTree.setIconItem(loadIcon((JSONObject) skilltreeObject.get("IconItem")));
                }
                if (skilltreeObject.containsKey("Description")) {
                    JSONArray descriptionArray = (JSONArray) skilltreeObject.get("Description");
                    for (Object lvl_object : descriptionArray) {
                        skillTree.addDescriptionLine(String.valueOf(lvl_object));
                    }
                }

                JSONObject levelList = (JSONObject) skilltreeObject.get("Level");
                for (Object oo : levelList.keySet()) {
                    JSONObject levelObject = (JSONObject) levelList.get(oo);
                    int thisLevel = Integer.parseInt(oo.toString());

                    SkillTreeLevel newLevel = skillTree.addLevel(thisLevel);
                    if (levelObject.containsKey("Message")) {
                        String message = (String) levelObject.get("Message");
                        newLevel.setLevelupMessage(message);
                    }

                    JSONObject skillList = (JSONObject) levelObject.get("Skills");
                    for (Object ooo : skillList.keySet()) {
                        JSONObject skillObject = (JSONObject) skillList.get(ooo);
                        String skillName = ooo.toString();

                        if (SkillsInfo.getSkillInfoClass(skillName) != null) {
                            SkillInfo skill = SkillsInfo.getNewSkillInfoInstance(skillName);

                            if (skill != null) {
                                SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                                if (sp != null) {
                                    TagCompound propertiesCompound = skill.getProperties();
                                    for (int i = 0; i < sp.parameterNames().length; i++) {
                                        String propertyName = sp.parameterNames()[i];
                                        NBTdatatypes propertyType = sp.parameterTypes()[i];
                                        if (!propertiesCompound.getCompoundData().containsKey(propertyName) && skillObject.containsKey(propertyName)) {
                                            String value = String.valueOf(skillObject.get(propertyName));
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
                                                    if (value != null && (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true"))) {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagByte(true));
                                                    } else {
                                                        propertiesCompound.getCompoundData().put(propertyName, new TagByte(false));
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
                e.printStackTrace();
                MyPetApi.getLogger().warning(ChatColor.RED + "Error in " + skillTreeMobType.getPetType().name() + ".json -> Skilltree not loaded.");
            }
        }
    }

    public List<String> saveSkillTrees(String configPath, String[] mobtypes) {
        ConfigurationJSON jsonConfig;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<>();

        for (String petType : mobtypes) {
            skillFile = new File(configPath + File.separator + petType.toLowerCase() + ".json");
            jsonConfig = new ConfigurationJSON(skillFile);
            if (saveSkillTree(jsonConfig, petType)) {
                savedPetTypes.add(petType);
            }
        }
        return savedPetTypes;
    }

    @SuppressWarnings("unchecked")
    protected boolean saveSkillTree(ConfigurationJSON jsonConfiguration, String petTypeName) {
        boolean saveMobType = false;

        SkillTreeMobType mobType;
        if (petTypeName.equalsIgnoreCase("default")) {
            mobType = SkillTreeMobType.DEFAULT;
        } else if (SkillTreeMobType.hasMobType(MyPetType.byName(petTypeName, false))) {
            mobType = SkillTreeMobType.getMobTypeByName(petTypeName);
        } else {
            return false;
        }
        if (mobType.getSkillTreeNames().size() != 0) {
            mobType.cleanupPlaces();

            JSONObject skilltreeList = jsonConfiguration.getJSONObject();
            for (SkillTree skillTree : mobType.getSkillTrees()) {
                JSONObject skilltreeObject = new JSONObject();
                skilltreeObject.put("Place", mobType.getSkillTreePlace(skillTree));
                if (skillTree.hasInheritance()) {
                    skilltreeObject.put("Inherits", skillTree.getInheritance());
                }
                if (skillTree.hasCustomPermissions()) {
                    skilltreeObject.put("Permission", skillTree.getPermission());
                }
                if (skillTree.hasDisplayName()) {
                    skilltreeObject.put("Display", skillTree.getDisplayName());
                }
                if (skillTree.getMaxLevel() > 0) {
                    skilltreeObject.put("MaxLevel", skillTree.getMaxLevel());
                }
                if (skillTree.getRequiredLevel() > 1) {
                    skilltreeObject.put("RequiredLevel", skillTree.getRequiredLevel());
                }
                if (skillTree.getDescription().size() > 0) {
                    JSONArray descriptionTagList = new JSONArray();
                    for (String line : skillTree.getDescription()) {
                        descriptionTagList.add(line);
                    }
                    skilltreeObject.put("Description", descriptionTagList);
                }
                skilltreeObject.put("IconItem", saveIcon(skillTree.getIconItem()));

                JSONObject levelList = new JSONObject();
                for (SkillTreeLevel level : skillTree.getLevelList()) {
                    JSONObject levelObject = new JSONObject();
                    if (level.hasLevelupMessage()) {
                        levelObject.put("Message", level.getLevelupMessage());
                    }

                    JSONObject skillList = new JSONObject();
                    for (SkillInfo skill : skillTree.getLevel(level.getLevel()).getSkills()) {
                        if (!skill.isAddedByInheritance()) {
                            JSONObject skillObject = new JSONObject();
                            SkillProperties sp = skill.getClass().getAnnotation(SkillProperties.class);
                            if (sp != null) {
                                for (int i = 0; i < sp.parameterNames().length; i++) {
                                    String propertyName = sp.parameterNames()[i];
                                    NBTdatatypes propertyType = sp.parameterTypes()[i];
                                    TagCompound propertiesCompound = skill.getProperties();
                                    if (propertiesCompound.getCompoundData().containsKey(propertyName)) {
                                        switch (propertyType) {
                                            case Short:
                                                skillObject.put(propertyName, ((TagShort) propertiesCompound.getCompoundData().get(propertyName)).getShortData());
                                                break;
                                            case Int:
                                                skillObject.put(propertyName, ((TagInt) propertiesCompound.getCompoundData().get(propertyName)).getIntData());
                                                break;
                                            case Long:
                                                skillObject.put(propertyName, ((TagLong) propertiesCompound.getCompoundData().get(propertyName)).getLongData());
                                                break;
                                            case Float:
                                                skillObject.put(propertyName, ((TagFloat) propertiesCompound.getCompoundData().get(propertyName)).getFloatData());
                                                break;
                                            case Double:
                                                skillObject.put(propertyName, ((TagDouble) propertiesCompound.getCompoundData().get(propertyName)).getDoubleData());
                                                break;
                                            case Byte:
                                                skillObject.put(propertyName, ((TagByte) propertiesCompound.getCompoundData().get(propertyName)).getByteData());
                                                break;
                                            case Boolean:
                                                skillObject.put(propertyName, ((TagByte) propertiesCompound.getCompoundData().get(propertyName)).getBooleanData());
                                                break;
                                            case String:
                                                skillObject.put(propertyName, ((TagString) propertiesCompound.getCompoundData().get(propertyName)).getStringData());
                                                break;
                                        }
                                    } else {
                                        skillObject.put(propertyName, sp.parameterDefaultValues()[i]);
                                    }
                                }
                            }

                            skillList.put(skill.getName(), skillObject);
                        }
                    }
                    levelObject.put("Skills", skillList);
                    levelList.put(level.getLevel(), levelObject);
                }
                skilltreeObject.put("Level", levelList);
                skilltreeList.put(skillTree.getName(), skilltreeObject);
            }

            if (mobType.getSkillTreeNames().size() > 0) {
                jsonConfiguration.save();
                saveMobType = true;
            }
        }
        return saveMobType;
    }

    @SuppressWarnings("unchecked")
    private JSONObject saveIcon(TagCompound itemTag) {
        JSONObject itemObject = new JSONObject();
        itemObject.put("Damage", itemTag.getAs("Damage", TagShort.class).getShortData());
        itemObject.put("ID", itemTag.getAs("id", TagShort.class).getShortData());
        if (itemTag.containsKeyAs("tag", TagCompound.class)) {
            TagCompound tag = itemTag.getAs("tag", TagCompound.class);
            if (tag.containsKey("ench")) {
                itemObject.put("Glowing", true);
            } else {
                itemObject.put("Glowing", false);
            }
        } else {
            itemObject.put("Glowing", false);
        }
        return itemObject;
    }

    private TagCompound loadIcon(JSONObject object) {
        TagCompound itemTag = new TagCompound();

        short itemID = object.containsKey("ID") ? ((Number) object.get("ID")).shortValue() : 6;
        short damage = object.containsKey("Damage") ? ((Number) object.get("Damage")).shortValue() : 0;

        itemTag.put("id", new TagShort(itemID));
        itemTag.put("Damage", new TagShort(damage));
        itemTag.put("Count", new TagByte(1));
        if (object.containsKey("Glowing") && object.get("Glowing").toString().equals("true")) {
            TagCompound tag = new TagCompound();
            tag.put("ench", new TagList());
            itemTag.put("tag", tag);
        }

        return itemTag;
    }
}