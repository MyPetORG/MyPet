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
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.DamageInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;

public class Damage extends DamageInfo implements SkillInstance {
    protected MyPet myPet;

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

    public void upgrade(SkillInfo upgrade, boolean quiet) {
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
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Damage.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), damage));
                }
            }
            if (isPassive != (damage <= 0)) {
                if (myPet.getStatus() == PetState.Here) {
                    getMyPet().getEntity().get().getHandle().getPathfinder().clearGoals();
                    getMyPet().getEntity().get().getHandle().getTargetSelector().clearGoals();
                    getMyPet().getEntity().get().getHandle().setPathfinder();
                    if (damage == 0) {
                        getMyPet().getEntity().get().setTarget(null);
                    }
                }
            }
        }
    }

    public String getFormattedValue() {
        return " -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Translation.getString("Name.Damage", myPet.getOwner());
    }

    public void reset() {
        damage = 0;
        if (myPet.getStatus() == PetState.Here) {
            getMyPet().getEntity().get().getHandle().getPathfinder().clearGoals();
            getMyPet().getEntity().get().getHandle().getTargetSelector().clearGoals();
            getMyPet().getEntity().get().getHandle().setPathfinder();
            getMyPet().getEntity().get().setTarget(null);
        }
    }

    public double getDamage() {
        return damage;
    }

    public SkillInstance cloneSkill() {
        Damage newSkill = new Damage(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}