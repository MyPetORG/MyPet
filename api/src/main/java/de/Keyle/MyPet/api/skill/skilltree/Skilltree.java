/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class Skilltree {

    protected String skilltreeName;
    @Getter @Setter protected String inheritedSkilltreeName;
    protected List<String> description = new ArrayList<>();
    protected SkilltreeIcon icon = null;
    protected String displayName = null;
    protected int maxLevel = 0;
    protected int requiredLevel = 0;
    protected int order = 0;
    @Getter @Setter protected double weight = 1;
    protected Set<MyPetType> mobTypes = new HashSet<>();
    protected Map<LevelRule, Upgrade> upgrades = new HashMap<>();
    protected Map<LevelRule, String> notifications = new HashMap<>();
    protected @Getter List<Settings> requirementSettings = new ArrayList<>();

    public Skilltree(String name) {
        this.skilltreeName = name;
    }

    public String getName() {
        return skilltreeName;
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
            return skilltreeName;
        }
        return displayName;
    }

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasInheritance() {
        return inheritedSkilltreeName != null
                && !inheritedSkilltreeName.isEmpty()
                && MyPetApi.getSkilltreeManager().hasSkilltree(inheritedSkilltreeName);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<Upgrade> getUpgrades(int level) {
        return getUpgrades(level, new HashSet<>());
    }

    protected List<Upgrade> getUpgrades(int level, Set<String> computedSkilltrees) {
        List<Upgrade> upgrades = new ArrayList<>();
        computedSkilltrees.add(this.skilltreeName);
        if (inheritedSkilltreeName != null && !inheritedSkilltreeName.isEmpty() && !computedSkilltrees.contains(inheritedSkilltreeName)) {
            if (MyPetApi.getSkilltreeManager().hasSkilltree(inheritedSkilltreeName)) {
                upgrades.addAll(MyPetApi
                        .getSkilltreeManager()
                        .getSkilltree(inheritedSkilltreeName)
                        .getUpgrades(level, computedSkilltrees));
            }
        }

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

    public void addRequirementSettings(Settings settings) {
        this.requirementSettings.add(settings);
    }

    public boolean checkRequirements(MyPet pet) {
        boolean usable = true;
        for (Settings flagSettings : requirementSettings) {
            String reqName = flagSettings.getName();
            Requirement requirement = MyPetApi.getSkilltreeManager().getRequirement(reqName);
            if (requirement == null) {
                MyPetApi.getLogger().warning("\"" + reqName + "\" is not a valid skilltree requirement!");
                continue;
            }
            if (!requirement.check(this, pet, flagSettings)) {
                usable = false;
            }
            if (!usable) {
                break;
            }
        }
        return usable;
    }

    @Override
    public String toString() {
        return "Skilltree{" +
                "skilltreeName='" + skilltreeName + '\'' +
                ", displayName='" + displayName + '\'' +
                (maxLevel > 0 ? ", maxLevel=" + maxLevel : "") +
                (requiredLevel > 0 ? ", requiredLevel=" + requiredLevel : "") +
                '}';
    }
}