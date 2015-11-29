/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skilltree;

import de.Keyle.MyPet.skill.Experience;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagShort;

import java.util.*;

public class SkillTree {
    private String skillTreeName;
    private List<String> description = new ArrayList<String>();
    private TagCompound iconItem = null;
    protected String inheritance = null;
    private String permission = null;
    private String displayName = null;
    private int maxLevel = 0;
    private int requiredLevel = 0;
    private SortedMap<Integer, SkillTreeLevel> skillsPerLevel = new TreeMap<Integer, SkillTreeLevel>();
    private int lastLevelWithSkills = 1;

    public SkillTree(String name) {
        this.skillTreeName = name;
    }

    public SkillTree(String name, String inheritance) {
        this.skillTreeName = name;
        this.inheritance = inheritance;
    }

    public String getName() {
        return skillTreeName;
    }

    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public void addDescriptionLine(String line) {
        description.add(line);
    }

    public void addDescription(String[] lines) {
        for (String line : lines) {
            addDescriptionLine(line);
        }
    }

    public void removeDescriptionLine(int index) {
        description.remove(index);
    }

    public void clearDescription() {
        description.clear();
    }

    public void setIconItem(TagCompound iconItem) {
        iconItem = new TagCompound(iconItem.getCompoundData());
        this.iconItem = iconItem;
        getIconItem();
    }

    public void setIconItem(short id, short damage, boolean enchantetGlow) {
        getIconItem();

        if (id > 0) {
            iconItem.getCompoundData().put("id", new TagShort(id));
        }
        if (damage >= 0) {
            iconItem.getCompoundData().put("Damage", new TagShort(damage));
        }
        if (!iconItem.getCompoundData().containsKey("tag")) {
            iconItem.getCompoundData().put("tag", new TagCompound());
        }
        TagCompound tagCompound = iconItem.getAs("tag", TagCompound.class);
        if (enchantetGlow) {
            tagCompound.getCompoundData().put("ench", new TagList());
        } else {
            tagCompound.getCompoundData().remove("ench");
        }
    }

    public TagCompound getIconItem() {
        if (iconItem == null) {
            iconItem = new TagCompound();
        }
        iconItem.getCompoundData().put("Count", new TagByte((byte) 1));
        if (!iconItem.getCompoundData().containsKey("id")) {
            iconItem.getCompoundData().put("id", new TagShort((short) 6));
        }
        if (!iconItem.getCompoundData().containsKey("Damage")) {
            iconItem.getCompoundData().put("Damage", new TagShort((short) 0));
        }
        return iconItem;
    }

    public int getLastLevelWithSkills() {
        return lastLevelWithSkills;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel < 0 ? 0 : Math.min(maxLevel, Experience.LEVEL_CAP);
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel < 1 ? 1 : requiredLevel;
    }

    public boolean hasLevel(int level) {
        return skillsPerLevel.containsKey(level);
    }

    public SkillTreeLevel getLevel(int level) {
        return skillsPerLevel.get(level);
    }

    public SkillTreeLevel addLevel(int level) {
        if (level > lastLevelWithSkills) {
            lastLevelWithSkills = level;
        }
        if (!skillsPerLevel.containsKey(level)) {
            SkillTreeLevel newLevel = new SkillTreeLevel(level);
            skillsPerLevel.put(level, newLevel);
            return newLevel;
        }
        return skillsPerLevel.get(level);
    }

    public SkillTreeLevel addLevel(SkillTreeLevel level) {
        if (level.getLevel() > lastLevelWithSkills) {
            lastLevelWithSkills = level.getLevel();
        }
        if (!skillsPerLevel.containsKey(level.getLevel())) {
            skillsPerLevel.put(level.getLevel(), level);
            return level;
        }
        return skillsPerLevel.get(level.getLevel());
    }

    public void removeLevel(int level) {
        if (skillsPerLevel.containsKey(level)) {
            skillsPerLevel.remove(level);
            lastLevelWithSkills = 1;
            for (int l : skillsPerLevel.keySet()) {
                if (l > lastLevelWithSkills) {
                    lastLevelWithSkills = l;
                }
            }
        }
    }

    public void addSkillToLevel(int level, ISkillInfo skill) {
        addSkillToLevel(level, skill, false);
    }

    public void addSkillToLevel(int level, ISkillInfo skill, boolean top) {
        if (skill == null) {
            MyPetLogger.write("Skills->null:level " + level);
        }
        addLevel(level).addSkill(skill, top);
    }

    public void addSkillToLevel(int level, List<ISkillInfo> skillList) {
        SkillTreeLevel myPetSkillTreeLevel = addLevel(level);
        for (ISkillInfo skill : skillList) {
            myPetSkillTreeLevel.addSkill(skill);
        }
    }

    public List<SkillTreeLevel> getLevelList() {
        List<SkillTreeLevel> levelList = new ArrayList<SkillTreeLevel>();
        if (skillsPerLevel.size() > 0) {
            for (int level : skillsPerLevel.keySet()) {
                levelList.add(skillsPerLevel.get(level));
            }
        }
        return levelList;
    }

    public String getDisplayName() {
        if (displayName == null) {
            return skillTreeName;
        }
        return displayName;
    }

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPermission() {
        if (permission == null) {
            return skillTreeName;
        }
        return permission;
    }

    public boolean hasCustomPermissions() {
        return permission != null;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getInheritance() {
        return inheritance;
    }

    public void setInheritance(String inheritance) {
        this.inheritance = inheritance;
    }

    public boolean hasInheritance() {
        return inheritance != null;
    }

    public SkillTree clone() {
        return clone(skillTreeName);
    }

    public SkillTree clone(String toName) {
        SkillTree newSkillTree = new SkillTree(toName);
        newSkillTree.setInheritance(inheritance);
        newSkillTree.setDisplayName(displayName);
        newSkillTree.setRequiredLevel(requiredLevel);
        newSkillTree.setMaxLevel(maxLevel);
        newSkillTree.setPermission(permission);
        newSkillTree.description = new ArrayList<String>(description);
        newSkillTree.iconItem = getIconItem().clone();

        for (int level : skillsPerLevel.keySet()) {
            newSkillTree.addLevel(skillsPerLevel.get(level).clone());
        }

        return newSkillTree;
    }
}