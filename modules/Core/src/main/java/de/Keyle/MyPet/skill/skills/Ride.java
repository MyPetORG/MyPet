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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.RideInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;

public class Ride extends RideInfo implements SkillInstance {
    private boolean active = false;
    private MyPet myPet;

    public Ride(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return active;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof RideInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("speed_percent")) {
                if (!upgrade.getProperties().containsKeyAs("addset_speed", TagString.class) || upgrade.getProperties().getAs("addset_speed", TagString.class).getStringData().equals("add")) {
                    speedPercent += upgrade.getProperties().getAs("speed_percent", TagInt.class).getIntData();
                } else {
                    speedPercent = upgrade.getProperties().getAs("speed_percent", TagInt.class).getIntData();
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("jump_height")) {
                if (upgrade.getProperties().containsKeyAs("addset_jump_height", TagString.class) && upgrade.getProperties().getAs("addset_jump_height", TagString.class).getStringData().equals("add")) {
                    jumpHeigth += upgrade.getProperties().getAs("jump_height", TagDouble.class).getDoubleData();
                } else {
                    jumpHeigth = upgrade.getProperties().getAs("jump_height", TagDouble.class).getDoubleData();
                }
            }
            if (!active && !quiet) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Ride.Receive", myPet.getOwner().getLanguage()), myPet.getPetName()));
            } else if (active && !quiet) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Ride.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), speedPercent));
            }
            active = true;
        }
    }

    public String getFormattedValue() {
        return Translation.getString("Name.Speed", myPet.getOwner().getLanguage()) + " +" + ChatColor.GOLD + speedPercent + "%" + ChatColor.RESET;
    }

    public void reset() {
        active = false;
        speedPercent = 0;
    }

    public int getSpeedPercent() {
        return speedPercent;
    }

    public double getJumpHeight() {
        return jumpHeigth;
    }

    @Override
    public SkillInstance cloneSkill() {
        Ride newSkill = new Ride(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}