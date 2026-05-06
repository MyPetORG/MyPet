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
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Represents a named skilltree that defines how a pet's skills progress as it levels up.
 *
 * <p>A skilltree contains a set of {@link Upgrade} entries gated by {@link LevelRule}s. When a pet
 * reaches a new level, each rule is checked and the matching upgrades are applied to the pet's
 * skills. Skilltrees may also inherit from another skilltree so that common progressions can be
 * shared without duplication.
 *
 * <p>Additional metadata includes:
 * <ul>
 *   <li><b>Display name / description</b> -- shown to the player in the skilltree selection GUI</li>
 *   <li><b>Icon</b> -- the {@link SkilltreeIcon} rendered in inventory menus</li>
 *   <li><b>Mob types</b> -- which pet types can use this skilltree</li>
 *   <li><b>Weight</b> -- relative probability when randomly assigning a skilltree</li>
 *   <li><b>Order</b> -- display ordering in lists and menus</li>
 *   <li><b>Requirements</b> -- conditions (permissions, economy, etc.) the pet must satisfy</li>
 *   <li><b>Max level / required level</b> -- bounds on when this tree is available and active</li>
 * </ul>
 *
 * <p>Skilltrees are loaded from {@code .st.json} files by the plugin's loader and registered with
 * the {@link SkilltreeManager}.
 */
public class Skilltree {

    protected final String skilltreeName;
    @Getter
    @Setter
    protected String inheritedSkilltreeName;
    protected final List<String> description = new ArrayList<>();
    @Setter
    protected SkilltreeIcon icon = null;
    @Setter
    protected String displayName = null;
    protected int maxLevel = 0;
    @Getter
    protected int requiredLevel = 0;
    @Setter
    @Getter
    protected int order = 0;
    @Getter
    @Setter
    protected double weight = 1;
    @Getter
    protected final Set<PetType> mobTypes = new HashSet<>();
    protected final Map<LevelRule, Upgrade<?>> upgrades = new HashMap<>();
    protected final Map<LevelRule, String> notifications = new HashMap<>();
    protected @Getter List<Settings> requirementSettings = new ArrayList<>();

    /**
     * Creates a new skilltree with the given internal name.
     *
     * @param name the unique identifier for this skilltree
     */
    public Skilltree(String name) {
        this.skilltreeName = name;
    }

    /** Returns the unique internal name of this skilltree. */
    public String getName() {
        return skilltreeName;
    }

    /** Returns an unmodifiable view of this skilltree's description lines. */
    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    /** Appends a single line to the description. */
    public void addDescriptionLine(String line) {
        description.add(line);
    }

    /** Appends multiple lines to the description. */
    public void addDescription(String[] lines) {
        for (String line : lines) {
            addDescriptionLine(line);
        }
    }

    /** Removes the description line at the specified index. */
    public void removeDescriptionLine(int index) {
        description.remove(index);
    }

    /** Removes all description lines. */
    public void clearDescription() {
        description.clear();
    }

    /** Returns the icon for this skilltree, creating a default one if not yet set. */
    public SkilltreeIcon getIcon() {
        if (icon == null) {
            icon = new SkilltreeIcon();
        }
        return icon;
    }

    /**
     * Returns the maximum level for this skilltree.
     *
     * <p>If not explicitly set (or set to 0), falls back to the global level cap from configuration.
     */
    public int getMaxLevel() {
        return maxLevel == 0 ? Configuration.LevelSystem.Experience.LEVEL_CAP : maxLevel;
    }

    /**
     * Sets the maximum level for this skilltree, clamped to the global level cap.
     * A value of 0 or below means "use the global cap".
     */
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel < 0 ? 0 : Math.min(maxLevel, Configuration.LevelSystem.Experience.LEVEL_CAP);
    }

    /**
     * Sets the minimum pet level required before this skilltree becomes available.
     * Clamped to a minimum of 1.
     */
    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = Math.max(requiredLevel, 1);
    }

    /** Returns the display name shown to players, falling back to the internal name if unset. */
    public String getDisplayName() {
        if (displayName == null) {
            return skilltreeName;
        }
        return displayName;
    }

    /** Returns {@code true} if a custom display name has been explicitly set. */
    public boolean hasDisplayName() {
        return displayName != null;
    }

    /** Returns {@code true} if this skilltree inherits from another registered skilltree. */
    public boolean hasInheritance() {
        return inheritedSkilltreeName != null
                && !inheritedSkilltreeName.isEmpty()
                && MyPetApi.getSkilltreeManager().hasSkilltree(inheritedSkilltreeName);
    }

    /**
     * Collects all upgrades that should be applied at the given level, including inherited ones.
     *
     * <p>Inherited skilltree upgrades are resolved first (depth-first), then this tree's own
     * upgrades are appended. Circular inheritance is detected and prevented.
     *
     * @param level the pet's current level
     * @return an ordered list of upgrades to apply
     */
    public List<Upgrade<?>> getUpgrades(int level) {
        return getUpgrades(level, new HashSet<>());
    }

    /**
     * Internal recursive method that collects upgrades while tracking already-visited trees
     * to avoid circular inheritance loops.
     */
    protected List<Upgrade<?>> getUpgrades(int level, Set<String> computedSkilltrees) {
        List<Upgrade<?>> upgrades = new ArrayList<>();
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

    /**
     * Registers an upgrade to be applied whenever the given level rule matches.
     *
     * @param levelRule the rule determining at which levels this upgrade activates
     * @param upgrade   the upgrade to apply
     */
    public void addUpgrade(LevelRule levelRule, Upgrade<?> upgrade) {
        this.upgrades.put(levelRule, upgrade);
    }

    /**
     * Collects all notification messages that should be displayed at the given level.
     *
     * @param level the pet's current level
     * @return the list of notification strings whose rules match this level
     */
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

    /**
     * Registers a notification message to be displayed when the given level rule matches.
     *
     * @param levelRule    the rule determining at which levels the notification fires
     * @param notification the message to display to the pet owner
     */
    public void addNotification(LevelRule levelRule, String notification) {
        this.notifications.put(levelRule, notification);
    }

    /**
     * Replaces the set of pet types that are allowed to use this skilltree.
     *
     * @param mobTypes the collection of allowed pet types
     */
    public void setMobTypes(Collection<PetType> mobTypes) {
        this.mobTypes.clear();
        this.mobTypes.addAll(mobTypes);
    }

    /** Adds a requirement configuration block that will be evaluated by the named {@link Requirement}. */
    public void addRequirementSettings(Settings settings) {
        this.requirementSettings.add(settings);
    }

    /**
     * Evaluates all configured requirements against the given pet.
     *
     * <p>Returns {@code true} only if every requirement passes. Short-circuits on the
     * first failure.
     *
     * @param pet the pet to check requirements against
     * @return {@code true} if the pet satisfies all requirements for this skilltree
     */
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

}
