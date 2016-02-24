/*
 * This file is part of mypet-core
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-core is licensed under the GNU Lesser General Public License.
 *
 * mypet-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.KnockbackInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Random;

public class Knockback extends KnockbackInfo implements SkillInstance, ActiveSkill {
    private static Random random = new Random();
    private ActiveMyPet myPet;

    public Knockback(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(ActiveMyPet myPet) {
        this.myPet = myPet;
    }

    public ActiveMyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance > 0;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof KnockbackInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("chance")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_chance") || upgrade.getProperties().getAs("addset_chance", TagString.class).getStringData().equals("add")) {
                    chance += upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                } else {
                    chance = upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                }
                chance = Math.min(chance, 100);
                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Knockback.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance));
                }
            }
        }
    }

    public String getFormattedValue() {
        return chance + "%";
    }

    public void reset() {
        chance = 0;
    }

    public boolean activate() {
        return random.nextDouble() < chance / 100.;
    }

    public void knockbackTarget(LivingEntity target) {
        target.setVelocity(new Vector(
                -Math.sin(myPet.getLocation().getYaw() * 3.141593F / 180.0F) * 2 * 0.5F,
                0.1D,
                Math.cos(myPet.getLocation().getYaw() * 3.141593F / 180.0F) * 2 * 0.5F
        ));
    }

    @Override
    public SkillInstance cloneSkill() {
        Knockback newSkill = new Knockback(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}