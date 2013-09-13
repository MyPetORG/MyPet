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


import de.Keyle.MyPet.skill.skills.SkillsInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.configuration.ConfigurationNBT;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.spout.nbt.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SkillTreeLoaderNBT extends SkillTreeLoader {
    public static SkillTreeLoaderNBT getSkilltreeLoader() {
        return new SkillTreeLoaderNBT();
    }

    private SkillTreeLoaderNBT() {
    }

    public void loadSkillTrees(String configPath, String[] mobtypes) {
        ConfigurationNBT skilltreeConfig;
        DebugLogger.info("Loading nbt skill configs in: " + configPath);
        File skillFile;

        for (String mobType : mobtypes) {
            skillFile = new File(configPath + File.separator + mobType.toLowerCase() + ".st");

            SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(mobType);

            if (!skillFile.exists()) {
                continue;
            }

            skilltreeConfig = new ConfigurationNBT(skillFile);
            if (skilltreeConfig.load()) {
                try {
                    loadSkillTree(skilltreeConfig, skillTreeMobType);
                    DebugLogger.info("  " + mobType.toLowerCase() + ".st");
                } catch (Exception e) {
                    MyPetLogger.write(ChatColor.RED + "  Error while loading skilltrees from: " + mobType.toLowerCase() + ".st");
                    e.printStackTrace();
                }
            }
            skillTreeMobType.cleanupPlaces();
        }
    }

    protected void loadSkillTree(ConfigurationNBT nbtConfiguration, SkillTreeMobType skillTreeMobType) {
        IntTag intTag;
        ListTag skilltreeList = (ListTag) nbtConfiguration.getNBTCompound().getValue().get("Skilltrees");
        for (int i_skilltree = 0; i_skilltree < skilltreeList.getValue().size(); i_skilltree++) {
            CompoundTag skilltreeCompound = (CompoundTag) skilltreeList.getValue().get(i_skilltree);
            SkillTree skillTree = new SkillTree(((StringTag) skilltreeCompound.getValue().get("Name")).getValue());

            intTag = (IntTag) skilltreeCompound.getValue().get("Place");
            int place = intTag.getValue();

            if (skilltreeCompound.getValue().containsKey("Inherits")) {
                skillTree.setInheritance(((StringTag) skilltreeCompound.getValue().get("Inherits")).getValue());
            }
            if (skilltreeCompound.getValue().containsKey("Permission")) {
                skillTree.setPermission(((StringTag) skilltreeCompound.getValue().get("Permission")).getValue());
            }
            if (skilltreeCompound.getValue().containsKey("Display")) {
                skillTree.setDisplayName(((StringTag) skilltreeCompound.getValue().get("Display")).getValue());
            }
            if (skilltreeCompound.getValue().containsKey("IconItem")) {
                skillTree.setIconItem(((CompoundTag) skilltreeCompound.getValue().get("IconItem")));
            }
            if (skilltreeCompound.getValue().containsKey("Description")) {
                ListTag descriptionTagList = (ListTag) skilltreeCompound.getValue().get("Description");
                for (int i = 0; i < descriptionTagList.getValue().size(); i++) {
                    StringTag line = (StringTag) descriptionTagList.getValue().get(i);
                    skillTree.addDescriptionLine(line.getValue());
                }
            }

            if (skilltreeCompound.getValue().containsKey("Level")) {
                ListTag levelList = (ListTag) skilltreeCompound.getValue().get("Level");
                for (int i_level = 0; i_level < levelList.getValue().size(); i_level++) {
                    CompoundTag levelCompound = (CompoundTag) levelList.getValue().get(i_level);
                    int thisLevel;
                    if (levelCompound.getValue().get("Level").getType() == TagType.TAG_INT) {
                        thisLevel = ((IntTag) levelCompound.getValue().get("Level")).getValue();
                    } else {
                        thisLevel = ((ShortTag) levelCompound.getValue().get("Level")).getValue();
                    }

                    SkillTreeLevel newLevel = skillTree.addLevel(thisLevel);
                    if (levelCompound.getValue().containsKey("Message")) {
                        String message = ((StringTag) levelCompound.getValue().get("Message")).getValue();
                        newLevel.setLevelupMessage(message);
                    }

                    ListTag skillList = (ListTag) levelCompound.getValue().get("Skills");
                    for (int i_skill = 0; i_skill < skillList.getValue().size(); i_skill++) {
                        CompoundTag skillCompound = (CompoundTag) skillList.getValue().get(i_skill);
                        String skillName = ((StringTag) skillCompound.getValue().get("Name")).getValue();
                        if (SkillsInfo.getSkillInfoClass(skillName) != null) {
                            CompoundTag skillPropertyCompound = (CompoundTag) skillCompound.getValue().get("Properties");
                            ISkillInfo skill = SkillsInfo.getNewSkillInfoInstance(skillName);
                            if (skill != null) {
                                skill.setProperties(skillPropertyCompound);
                                skill.setDefaultProperties();
                                skillTree.addSkillToLevel(thisLevel, skill);
                            }
                        }
                    }
                }
            }
            skillTreeMobType.addSkillTree(skillTree, place);
        }
    }

    public List<String> saveSkillTrees(String configPath, String[] mobtypes) {
        ConfigurationNBT nbtConfig;
        File skillFile;
        List<String> savedPetTypes = new ArrayList<String>();

        for (String petType : mobtypes) {
            skillFile = new File(configPath + File.separator + petType.toLowerCase() + ".st");
            nbtConfig = new ConfigurationNBT(skillFile);
            if (saveSkillTree(nbtConfig, petType)) {
                savedPetTypes.add(petType);
            }
        }
        return savedPetTypes;
    }

    protected boolean saveSkillTree(ConfigurationNBT nbtConfiguration, String petTypeName) {
        boolean saveMobType = false;

        if (SkillTreeMobType.getMobTypeByName(petTypeName).getSkillTreeNames().size() != 0) {
            SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByName(petTypeName);
            mobType.cleanupPlaces();

            List<CompoundTag> skilltreeList = new ArrayList<CompoundTag>();
            for (SkillTree skillTree : mobType.getSkillTrees()) {
                CompoundTag skilltreeCompound = new CompoundTag(skillTree.getName(), new CompoundMap());
                skilltreeCompound.getValue().put("Name", new StringTag("Name", skillTree.getName()));
                skilltreeCompound.getValue().put("Place", new IntTag("Place", mobType.getSkillTreePlace(skillTree)));
                if (skillTree.hasInheritance()) {
                    skilltreeCompound.getValue().put("Inherits", new StringTag("Inherits", skillTree.getInheritance()));
                }
                if (skillTree.hasCustomPermissions()) {
                    skilltreeCompound.getValue().put("Permission", new StringTag("Permission", skillTree.getPermission()));
                }
                if (skillTree.hasDisplayName()) {
                    skilltreeCompound.getValue().put("Display", new StringTag("Display", skillTree.getDisplayName()));
                }
                if (skillTree.getDescription().size() > 0) {
                    List<StringTag> descriptionTagList = new ArrayList<StringTag>();
                    for (String line : skillTree.getDescription()) {
                        descriptionTagList.add(new StringTag(null, line));
                    }
                    skilltreeCompound.getValue().put("Description", new ListTag<StringTag>("Description", StringTag.class, descriptionTagList));
                }
                skilltreeCompound.getValue().put("IconItem", skillTree.getIconItem());

                List<CompoundTag> levelList = new ArrayList<CompoundTag>();
                for (SkillTreeLevel level : skillTree.getLevelList()) {
                    CompoundTag levelCompound = new CompoundTag("" + level.getLevel(), new CompoundMap());
                    levelCompound.getValue().put("Level", new IntTag("Level", level.getLevel()));
                    if (level.hasLevelupMessage()) {
                        levelCompound.getValue().put("Message", new StringTag("Message", level.getLevelupMessage()));
                    }

                    List<CompoundTag> skillList = new ArrayList<CompoundTag>();
                    for (ISkillInfo skill : skillTree.getLevel(level.getLevel()).getSkills()) {
                        if (!skill.isAddedByInheritance()) {
                            CompoundTag skillCompound = new CompoundTag(skill.getName(), new CompoundMap());
                            skillCompound.getValue().put("Name", new StringTag("Name", skill.getName()));
                            skillCompound.getValue().put("Properties", skill.getProperties());

                            skillList.add(skillCompound);
                        }
                    }
                    levelCompound.getValue().put("Name", new ListTag<CompoundTag>("Skills", CompoundTag.class, skillList));
                    levelList.add(levelCompound);
                }
                skilltreeCompound.getValue().put("Level", new ListTag<CompoundTag>("Level", CompoundTag.class, levelList));
                skilltreeList.add(skilltreeCompound);
            }
            nbtConfiguration.getNBTCompound().getValue().put("Skilltrees", new ListTag<CompoundTag>("Skilltrees", CompoundTag.class, skilltreeList));

            if (mobType.getSkillTreeNames().size() > 0) {
                nbtConfiguration.save();
                saveMobType = true;
            }
        }
        return saveMobType;
    }
}