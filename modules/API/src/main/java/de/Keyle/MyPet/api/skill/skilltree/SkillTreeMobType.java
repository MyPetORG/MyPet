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

package de.Keyle.MyPet.api.skill.skilltree;

import de.Keyle.MyPet.api.entity.MyPetType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeMobType {
    private Map<String, SkillTree> skillTrees = new HashMap<>();
    private List<String> skillTreeList = new ArrayList<>();
    private MyPetType petType;

    private static Map<MyPetType, SkillTreeMobType> mobTypes = new HashMap<>();
    public static final SkillTreeMobType DEFAULT = new SkillTreeMobType(null);

    private SkillTreeMobType(MyPetType petType) {
        this.petType = petType;
        if (petType != null) {
            mobTypes.put(this.petType, this);
        }
    }

    public MyPetType getPetType() {
        return petType;
    }

    public void addSkillTree(SkillTree skillTree) {
        addSkillTree(skillTree, getNextPlace());
    }

    public void addSkillTree(SkillTree skillTree, int place) {
        if (!skillTrees.containsKey(skillTree.getName())) {
            skillTrees.put(skillTree.getName(), skillTree);
            if (skillTreeList.size() - 1 < place) {
                for (int x = skillTreeList.size(); x <= place; x++) {
                    skillTreeList.add(x, null);
                }
            }
            if (skillTreeList.get(place) != null) {
                place = getNextPlace();
            }
            skillTreeList.set(place, skillTree.getName());
        }
    }

    public void removeSkillTree(String skillTreeName) {
        if (skillTrees.containsKey(skillTreeName)) {
            skillTrees.remove(skillTreeName);
            skillTreeList.remove(skillTreeName);
        }
    }

    public void moveSkillTreeUp(SkillTree skillTree) {
        moveSkillTreeUp(skillTree.getName());
    }

    public void moveSkillTreeUp(String skillTreeName) {
        int index = skillTreeList.indexOf(skillTreeName);
        if (index != -1 && index > 0 && index < skillTreeList.size()) {
            String skillTree = skillTreeList.get(index - 1);
            skillTreeList.set(index - 1, skillTreeName);
            skillTreeList.set(index, skillTree);
        }
    }

    public void moveSkillTreeDown(SkillTree skillTree) {
        moveSkillTreeDown(skillTree.getName());
    }

    public void moveSkillTreeDown(String skillTreeName) {
        int index = skillTreeList.indexOf(skillTreeName);
        if (index != -1 && index >= 0 && index < skillTreeList.size()) {
            String skillTree = skillTreeList.get(index + 1);
            skillTreeList.set(index + 1, skillTreeName);
            skillTreeList.set(index, skillTree);
        }
    }

    public int getSkillTreePlace(String skillTreeName) {
        if (skillTrees.containsKey(skillTreeName)) {
            return skillTreeList.indexOf(skillTreeName);
        }
        return -1;
    }

    public int getSkillTreePlace(SkillTree skillTree) {
        return getSkillTreePlace(skillTree.getName());
    }

    public SkillTree getSkillTree(String skillTreeName) {
        return skillTrees.get(skillTreeName);
    }

    public boolean hasSkillTree(String skillTreeName) {
        return skillTrees.containsKey(skillTreeName);
    }

    public List<String> getSkillTreeNames() {
        List<String> skilltreeNames = new ArrayList<>();
        for (String name : skillTreeList) {
            if (name != null) {
                skilltreeNames.add(name);
            }
        }
        return skilltreeNames;
    }

    public List<SkillTree> getSkillTrees() {
        cleanupPlaces();
        List<SkillTree> skilltreeNames = new ArrayList<>();
        for (String name : skillTreeList) {
            skilltreeNames.add(getSkillTree(name));
        }
        return skilltreeNames;
    }

    public short getNextPlace() {
        for (int i = 0; i < skillTreeList.size(); i++) {
            if (skillTreeList.get(i) == null) {
                return (short) i;
            }
        }
        short place = (short) skillTreeList.size();
        skillTreeList.add(null);
        return place;
    }

    public void cleanupPlaces() {
        while (skillTreeList.indexOf(null) != -1) {
            skillTreeList.remove(null);
        }
    }

    public static SkillTreeMobType getMobTypeByName(String typeName) {
        if (typeName == null) {
            return DEFAULT;
        }
        if (!mobTypes.containsKey(MyPetType.byName(typeName))) {
            return new SkillTreeMobType(MyPetType.byName(typeName));
        }
        return mobTypes.get(MyPetType.byName(typeName));
    }

    public static SkillTreeMobType byPetType(MyPetType type) {
        if (type == null) {
            return DEFAULT;
        }
        if (!mobTypes.containsKey(type)) {
            return new SkillTreeMobType(type);
        }
        return mobTypes.get(type);
    }

    public static boolean hasMobType(MyPetType mobType) {
        return mobTypes.containsKey(mobType);
    }

    public static List<String> getSkillTreeNames(MyPetType type) {
        List<String> skillTreeNames;
        if (mobTypes.containsKey(type)) {
            skillTreeNames = mobTypes.get(type).getSkillTreeNames();
        } else {
            skillTreeNames = new ArrayList<>();
        }
        return skillTreeNames;
    }

    public static List<SkillTree> getSkillTrees(MyPetType type) {
        if (mobTypes.containsKey(type)) {
            return mobTypes.get(type).getSkillTrees();
        } else {
            return new ArrayList<>();
        }
    }

    public static void clearMobTypes() {
        mobTypes.clear();
        DEFAULT.skillTreeList.clear();
        DEFAULT.skillTrees.clear();
    }

    public static boolean containsSkillTree(MyPetType type, String name) {
        return getSkillTreeNames(type).contains(name);
    }
}