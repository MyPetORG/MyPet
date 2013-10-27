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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.RideInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.itemstringinterpreter.ConfigItem;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.ChatColor;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

public class Ride extends RideInfo implements ISkillInstance {
    public static ConfigItem RIDE_ITEM;
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

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof RideInfo) {
            if (!active && !quiet) {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Ride.Receive", myPet.getOwner().getLanguage()), myPet.getPetName()));
            }
            if (upgrade.getProperties().getValue().containsKey("speed_percent")) {
                if (!upgrade.getProperties().getValue().containsKey("addset_speed") || ((StringTag) upgrade.getProperties().getValue().get("addset_speed")).getValue().equals("add")) {
                    speedPercent += ((IntTag) upgrade.getProperties().getValue().get("speed_percent")).getValue();
                } else {
                    speedPercent = ((IntTag) upgrade.getProperties().getValue().get("speed_percent")).getValue();
                }
                if (active && !quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Ride.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), speedPercent));
                }
            }
            active = true;
        }
    }

    public String getFormattedValue() {
        return Locales.getString("Name.Speed", myPet.getOwner().getLanguage()) + " +" + ChatColor.GOLD + speedPercent + "%" + ChatColor.RESET;
    }

    public void reset() {
        active = false;
        speedPercent = 0;
    }

    public int getSpeedPercent() {
        return speedPercent;
    }

    @Override
    public ISkillInstance cloneSkill() {
        Ride newSkill = new Ride(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}