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
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.RangedInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;

public class Ranged extends RangedInfo implements ISkillInstance {
    private MyPet myPet;

    public Ranged(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public Projectiles getProjectile() {
        return selectedProjectile;
    }

    public boolean isActive() {
        return damage > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof RangedInfo) {
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
            }
            if (upgrade.getProperties().getCompoundData().containsKey("projectile")) {
                String projectileName = upgrade.getProperties().getAs("projectile", TagString.class).getStringData();
                for (Projectiles projectile : Projectiles.values()) {
                    if (projectile.name().equalsIgnoreCase(projectileName)) {
                        selectedProjectile = projectile;
                    }
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("rateoffire")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_rateoffire") || upgrade.getProperties().getAs("addset_rateoffire", TagString.class).getStringData().equals("add")) {
                    rateOfFire += upgrade.getProperties().getAs("rateoffire", TagInt.class).getIntData();
                } else {
                    rateOfFire = upgrade.getProperties().getAs("rateoffire", TagInt.class).getIntData();
                }
            }
            if (!quiet) {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Ranged.Upgrade", myPet.getOwner()), myPet.getPetName(), Locales.getString("Name." + getProjectile().name(), myPet.getOwner()), damage, String.format("%1.2f", (1. / ((getRateOfFire() * 50.) / 1000.)) * 60.)));
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
        return Util.formatText(Locales.getString("Message.Skill.Ranged.RoundsPerMinute", myPet.getOwner()), String.format("%1.2f", (1. / ((getRateOfFire() * 50.) / 1000.)) * 60.)) + " -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Locales.getString("Name.Damage", myPet.getOwner());
    }

    public void reset() {
        damage = 0;
        rateOfFire = 0;
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

    public int getRateOfFire() {
        if (rateOfFire == 0) {
            rateOfFire = 1;
        }
        return rateOfFire;
    }

    public ISkillInstance cloneSkill() {
        Ranged newSkill = new Ranged(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}