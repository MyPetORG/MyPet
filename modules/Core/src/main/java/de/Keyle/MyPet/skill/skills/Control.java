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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.ControlInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.Material;

public class Control extends ControlInfo implements SkillInstance {
    private Location moveTo;
    private Location prevMoveTo;
    private boolean active = false;
    private MyPet myPet;

    public Control(boolean addedByInheritance) {
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
        if (upgrade instanceof ControlInfo) {
            if (!quiet && !active) {
                String controlItemName;
                if (Configuration.Skilltree.Skill.CONTROL_ITEM.getItem().getType() == Material.AIR) {
                    controlItemName = Translation.getString("Name.EmptyHand", myPet.getOwner());
                } else {
                    controlItemName = WordUtils.capitalizeFully(Configuration.Skilltree.Skill.CONTROL_ITEM.getItem().getType().name().replace("_", " "));
                }
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Control.Upgrade", myPet.getOwner()), myPet.getPetName(), controlItemName));

            }
            active = true;
        }
    }

    public String getFormattedValue() {
        return "";
    }

    public Location getLocation() {
        Location tmpMoveTo = moveTo;
        moveTo = null;
        return tmpMoveTo;
    }

    public Location getLocation(boolean delete) {
        Location tmpMoveTo = moveTo;
        if (delete) {
            moveTo = null;
        }
        return tmpMoveTo;
    }

    public void setMoveTo(Location loc) {
        if (!active) {
            return;
        }
        if (prevMoveTo != null) {
            if (loc.distance(prevMoveTo) > 1) {
                moveTo = loc;
                prevMoveTo = loc;
            }
        } else {
            moveTo = loc;
        }
    }

    public void reset() {
        active = false;
        moveTo = null;
        prevMoveTo = null;
    }

    @Override
    public SkillInstance cloneSkill() {
        Control newSkill = new Control(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}