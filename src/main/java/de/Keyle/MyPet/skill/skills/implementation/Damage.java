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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.info.DamageInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;

public class Damage extends DamageInfo implements ISkillInstance {
    private MyPet myPet;

    public Damage(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return damage > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof DamageInfo) {
            boolean isPassive = damage <= 0;
            if (upgrade.getProperties().getCompoundData().containsKey("damage")) {
                int damage = upgrade.getProperties().getAs("damage", TagInt.class).getIntData();
                upgrade.getProperties().getCompoundData().remove("damage");
                TagDouble TagDouble = new TagDouble(damage);
                upgrade.getProperties().getCompoundData().put("damage_double", TagDouble);
            }
            if (upgrade.getProperties().getCompoundData().containsKey("damage_double")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_damage") || upgrade.getProperties().getAs("addset_damage", TagString.class).getStringData().equals("add")) {
                    damage += upgrade.getProperties().getAs("damage_double", TagDouble.class).getDoubleData();
                } else {
                    damage = upgrade.getProperties().getAs("damage_double", TagDouble.class).getDoubleData();
                }
                if (!quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Damage.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), damage));
                }
            }
            if (isPassive != (damage <= 0)) {
                if (myPet.getStatus() == PetState.Here) {
                    getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                    getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                    getMyPet().getCraftPet().getHandle().setPathfinder();
                    if (damage == 0) {
                        getMyPet().getCraftPet().getHandle().setGoalTarget(null);
                    }
                }
            }
        }
    }

    public String getFormattedValue() {
        return " -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Locales.getString("Name.Damage", myPet.getOwner());
    }

    public void reset() {
        damage = 0;
        if (myPet.getStatus() == PetState.Here) {
            getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().setPathfinder();
            getMyPet().getCraftPet().getHandle().setGoalTarget(null);
        }
    }

    public double getDamage() {
        return damage;
    }

    public ISkillInstance cloneSkill() {
        Damage newSkill = new Damage(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}