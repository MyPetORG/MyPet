/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation;

import com.google.common.collect.Iterables;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.enderman.MyEnderman;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.locale.Translation;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.ChatColor;

import java.util.Iterator;
import java.util.Random;

public class Behavior extends BehaviorInfo implements ISkillInstance, Scheduler, ISkillStorage, ISkillActive {
    private static Random random = new Random();

    private BehaviorState behavior = BehaviorState.Normal;
    private boolean active = false;
    private MyPet myPet;
    private double height;
    Iterator<BehaviorState> behaviorCycler;

    public Behavior(boolean addedByInheritance) {
        super(addedByInheritance);
        activeBehaviors.add(BehaviorState.Normal);
        behaviorCycler = Iterables.cycle(activeBehaviors).iterator();
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
        height = MyPet.getEntitySize(myPet.getPetType().getEntityClass())[0];
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isModeUsable(BehaviorState mode) {
        return mode.isActive() && activeBehaviors.contains(mode);
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof BehaviorInfo) {
            active = true;
            boolean valuesEdit = false;
            String activeModes = "";
            if (upgrade.getProperties().getCompoundData().containsKey("friend")) {
                if (upgrade.getProperties().getAs("friend", TagByte.class).getBooleanData() && BehaviorState.Friendly.isActive()) {
                    activeBehaviors.add(BehaviorState.Friendly);
                    activeModes = ChatColor.GOLD + Translation.getString("Name.Friendly", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Friendly);
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("aggro")) {
                if (upgrade.getProperties().getAs("aggro", TagByte.class).getBooleanData() && BehaviorState.Aggressive.isActive()) {
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
                if (upgrade.getProperties().getAs("farm", TagByte.class).getBooleanData() && BehaviorState.Farm.isActive()) {
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
                if (upgrade.getProperties().getAs("raid", TagByte.class).getBooleanData() && BehaviorState.Raid.isActive()) {
                    activeBehaviors.add(BehaviorState.Raid);
                    activeModes += ChatColor.GOLD + Translation.getString("Name.Raid", myPet.getOwner().getLanguage()) + ChatColor.RESET;
                } else {
                    activeBehaviors.remove(BehaviorState.Raid);
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("duel")) {
                if (upgrade.getProperties().getAs("duel", TagByte.class).getBooleanData() && BehaviorState.Duel.isActive()) {
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
                        ;
                    }
                }
                if (!quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skill.Behavior.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName()));
                    myPet.sendMessageToOwner("  " + activeModes);
                }
            }
        }
    }

    public String getFormattedValue() {
        String activeModes = ChatColor.GOLD + Translation.getString("Name.Normal", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        if (activeBehaviors.contains(BehaviorState.Friendly) && BehaviorState.Friendly.isActive()) {
            activeModes += ", " + ChatColor.GOLD + Translation.getString("Name.Friendly", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Aggressive) && BehaviorState.Aggressive.isActive()) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Aggressive", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Farm) && BehaviorState.Farm.isActive()) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Farm", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Raid) && BehaviorState.Raid.isActive()) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Raid", myPet.getOwner().getLanguage()) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(BehaviorState.Duel) && BehaviorState.Duel.isActive()) {
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
        myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner().getLanguage()), myPet.getPetName(), Translation.getString("Name." + behavior.name(), myPet.getOwner().getLanguage())));
        if (behavior == BehaviorState.Friendly) {
            myPet.getCraftPet().setTarget(null);
        }
    }

    public void activateBehavior(BehaviorState behaviorState) {
        if (active) {
            if (activeBehaviors.contains(behaviorState)) {
                behavior = behaviorState;
                myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner().getLanguage()), myPet.getPetName(), Translation.getString("Name." + behavior.name(), myPet.getOwner().getPlayer())));
                if (behavior == BehaviorState.Friendly) {
                    myPet.getCraftPet().getHandle().setGoalTarget(null);
                }
            }
        } else {
            myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.No.Skill", myPet.getOwner().getLanguage()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
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
            myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner().getLanguage()), myPet.getPetName(), Translation.getString("Name." + behavior.name(), myPet.getOwner().getPlayer())));
            return true;
        } else {
            myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.No.Skill", myPet.getOwner().getLanguage()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
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
            BukkitUtil.playParticleEffect(myPet.getLocation().add(0, height, 0), EnumParticle.VILLAGER_ANGRY, 0.2F, 0.2F, 0.2F, 0.5F, 1, 20);
        }
        if (myPet instanceof MyEnderman) {
            MyEnderman myEnderman = (MyEnderman) myPet;
            if (behavior == BehaviorState.Aggressive) {
                if (!myEnderman.isScreaming()) {
                    myEnderman.setScreaming(true);
                }
            } else {
                if (myEnderman.isScreaming()) {
                    myEnderman.setScreaming(false);
                }
            }
        }
    }

    @Override
    public ISkillInstance cloneSkill() {
        Behavior newSkill = new Behavior(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}