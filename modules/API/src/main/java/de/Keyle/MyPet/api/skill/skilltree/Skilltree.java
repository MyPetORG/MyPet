/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;

import java.util.*;

public class Skilltree {

    protected String skillTreeName;
    protected List<String> description = new ArrayList<>();
    protected SkilltreeIcon icon = null;
    protected String permission = null;
    protected String displayName = null;
    protected int maxLevel = 0;
    protected int requiredLevel = 0;
    protected int order = 0;
    protected Set<MyPetType> mobTypes = new HashSet<>();
    protected Map<LevelRule, Upgrade> upgrades = new HashMap<>();
    protected Map<LevelRule, String> notifications = new HashMap<>();

    public Skilltree(String name) {
        this.skillTreeName = name;
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

    public void setIcon(SkilltreeIcon icon) {
        this.icon = icon;
    }

    public SkilltreeIcon getIcon() {
        if (icon == null) {
            icon = new SkilltreeIcon();
        }
        return icon;
    }

    public int getMaxLevel() {
        return maxLevel == 0 ? Configuration.LevelSystem.Experience.LEVEL_CAP : maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel < 0 ? 0 : Math.min(maxLevel, Configuration.LevelSystem.Experience.LEVEL_CAP);
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel < 1 ? 1 : requiredLevel;
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

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getFullPermission() {
        if (permission == null) {
            return "MyPet.skilltree." + skillTreeName;
        }
        return "MyPet.skilltree." + permission;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<Upgrade> getUpgrades(int level) {
        List<Upgrade> upgrades = new ArrayList<>();
        List<LevelRule> rules = new ArrayList<>(this.upgrades.keySet());
        rules.sort(Comparator.comparingInt(LevelRule::getPriority));
        for (LevelRule rule : rules) {
            if (rule.check(level)) {
                upgrades.add(this.upgrades.get(rule));
            }
        }
        return upgrades;
    }

    public void addUpgrade(LevelRule levelRule, Upgrade upgrade) {
        this.upgrades.put(levelRule, upgrade);
    }

    public List<String> getNotifications(int level) {
        List<String> notifications = new ArrayList<>();
        List<LevelRule> rules = new ArrayList<>(this.notifications.keySet());
        rules.sort(Comparator.comparingInt(LevelRule::getPriority));
        for (LevelRule rule : rules) {
            if (rule.check(level)) {
                notifications.add(this.notifications.get(rule));
            }
        }
        return notifications;
    }

    public void addNotification(LevelRule levelRule, String notification) {
        this.notifications.put(levelRule, notification);
    }

    public Set<MyPetType> getMobTypes() {
        return mobTypes;
    }

    public void setMobTypes(Collection<MyPetType> mobTypes) {
        this.mobTypes.clear();
        this.mobTypes.addAll(mobTypes);
    }

    @Override
    public String toString() {
        return "Skilltree{" +
                "skillTreeName='" + skillTreeName + '\'' +
                ", displayName='" + displayName + '\'' +
                (maxLevel > 0 ? ", maxLevel=" + maxLevel : "") +
                (requiredLevel > 0 ? ", requiredLevel=" + requiredLevel : "") +
                '}';
    }
}