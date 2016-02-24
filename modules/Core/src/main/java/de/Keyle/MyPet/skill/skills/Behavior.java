/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import com.google.common.collect.Iterables;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.BehaviorInfo;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;

import java.util.Iterator;
import java.util.Random;

public class Behavior extends BehaviorInfo implements SkillInstance, Scheduler, NBTStorage, ActiveSkill {
    private static Random random = new Random();

    private BehaviorState behavior = BehaviorState.Normal;
    private boolean active = false;
    private ActiveMyPet myPet;
    Iterator<BehaviorState> behaviorCycler;

    public Behavior(boolean addedByInheritance) {
        super(addedByInheritance);
        activeBehaviors.add(BehaviorState.Normal);
        behaviorCycler = Iterables.cycle(activeBehaviors).iterator();
    }

    public void setMyPet(ActiveMyPet myPet) {
        this.myPet = myPet;
    }

    public ActiveMyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isModeUsable(BehaviorState mode) {
        return activeBehaviors.contains(mode);
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof BehaviorInfo) {
            active = true;
            boolean valuesEdit = false;
            String activeModes = "";
            if (upgrade.getProperties().getCompoundData().containsKey("friend")) {
                if (upgrade.getProperties().getAs("friend", TagByte.class).getBooleanData()) {
                    activeBehaviors.add(BehaviorState.Friendly);
                    activeModes = ChatColor.GOLD + Translation.getString("Name.Friendly", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Friendly);
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("aggro")) {
                if (upgrade.getProperties().getAs("aggro", TagByte.class).getBooleanData()) {
                    activeBehaviors.add(BehaviorState.Aggressive);
                    if (!activeModes.equalsIgnoreCase("")) {
                        activeModes += ", ";
                    }
                    activeModes += ChatColor.GOLD + Translation.getString("Name.Aggressive", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Aggressive);
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("farm")) {
                if (upgrade.getProperties().getAs("farm", TagByte.class).getBooleanData()) {
                    activeBehaviors.add(BehaviorState.Farm);
                    if (!activeModes.equalsIgnoreCase("")) {
                        activeModes += ", ";
                    }
                    activeModes += ChatColor.GOLD + Translation.getString("Name.Farm", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Farm);
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("raid")) {
                if (upgrade.getProperties().getAs("raid", TagByte.class).getBooleanData()) {
                    activeBehaviors.add(BehaviorState.Raid);
                    activeModes += ChatColor.GOLD + Translation.getString("Name.Raid", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Raid);
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("duel")) {
                if (upgrade.getProperties().getAs("duel", TagByte.class).getBooleanData()) {
                    activeBehaviors.add(BehaviorState.Duel);
                    activeModes += ChatColor.GOLD + Translation.getString("Name.Duel", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Duel);
                }
                valuesEdit = true;
            }
            if (valuesEdit) {
                behaviorCycler = Iterables.cycle(activeBehaviors).iterator();
                if (!activeBehaviors.contains(behavior)) {
                    behavior = behaviorCycler.next();
                } else {
                    while (behaviorCycler.next() != behavior) {
                        if (!behaviorCycler.hasNext()) {
                            break;
                        }
                    }
                }
                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Behavior.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName()));
                    myPet.getOwner().sendMessage("  " + activeModes);
                }
            }
        }
    }

    public String getFormattedValue() {
        String activeModes = ChatColor.GOLD + Translation.getString("Name.Normal", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        if (activeBehaviors.contains(BehaviorState.Friendly)) {
            activeModes += ", " + ChatColor.GOLD + Translation.getString("Name.Friendly", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Aggressive)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Aggressive", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Farm)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Farm", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Raid)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Raid", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Duel)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Duel", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        return Translation.getString("Name.Modes", myPet.getOwner().getLanguage()) + ": " + activeModes;
    }

    public void reset() {
        behavior = BehaviorState.Normal;
        activeBehaviors.clear();
        activeBehaviors.add(BehaviorState.Normal);
        active = false;
    }

    public void setBehavior(BehaviorState behaviorState) {
        behavior = behaviorState;
        myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner().getLanguage()), myPet.getPetName(), Translation.getString("Name." + behavior.name(), myPet.getOwner().getLanguage())));
        if (behavior == BehaviorState.Friendly) {
            myPet.getEntity().setTarget(null);
        }
    }

    public void activateBehavior(BehaviorState behaviorState) {
        if (active) {
            if (activeBehaviors.contains(behaviorState)) {
                behavior = behaviorState;
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner().getLanguage()), myPet.getPetName(), Translation.getString("Name." + behavior.name(), myPet.getOwner().getPlayer())));
                if (behavior == BehaviorState.Friendly) {
                    myPet.getEntity().setTarget(null);
                }
            }
        } else {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.Skill", myPet.getOwner().getLanguage()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
        }
    }

    public BehaviorState getBehavior() {
        return behavior;
    }

    public boolean activate() {
        if (active) {
            while (true) {
                behavior = behaviorCycler.next();
                if (behavior != BehaviorState.Normal) {
                    if (Permissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Behavior." + behavior.name())) {
                        break;
                    }
                } else {
                    break;
                }
            }
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner().getLanguage()), myPet.getPetName(), Translation.getString("Name." + behavior.name(), myPet.getOwner().getPlayer())));
            return true;
        } else {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.Skill", myPet.getOwner().getLanguage()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
            return false;
        }
    }

    public void load(TagCompound compound) {
        if (compound.getCompoundData().containsKey("Mode")) {
            behavior = BehaviorState.valueOf(compound.getAs("Mode", TagString.class).getStringData());
        }
    }

    public TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        nbtTagCompound.getCompoundData().put("Mode", new TagString(behavior.name()));
        return nbtTagCompound;
    }

    public void schedule() {
        if (behavior == BehaviorState.Aggressive && random.nextBoolean()) {
            MyPetApi.getBukkitHelper().playParticleEffect(myPet.getLocation().add(0, myPet.getEntity().getEyeHeight(), 0), "VILLAGER_ANGRY", 0.2F, 0.2F, 0.2F, 0.5F, 1, 20);
        }
    }

    @Override
    public SkillInstance cloneSkill() {
        Behavior newSkill = new Behavior(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}