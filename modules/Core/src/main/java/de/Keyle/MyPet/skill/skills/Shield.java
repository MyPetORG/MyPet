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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.ShieldInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

import java.util.Random;

public class Shield extends ShieldInfo implements SkillInstance, ActiveSkill {
    private static Random random = new Random();
    private MyPet myPet;

    public Shield(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance > 0;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof ShieldInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("chance")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_chance") || upgrade.getProperties().getAs("addset_chance", TagString.class).getStringData().equals("add")) {
                    chance += upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                } else {
                    chance = upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                }
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_redirection") || upgrade.getProperties().getAs("addset_redirection", TagString.class).getStringData().equals("add")) {
                    redirectedDamagePercent += upgrade.getProperties().getAs("redirection", TagInt.class).getIntData();
                } else {
                    redirectedDamagePercent = upgrade.getProperties().getAs("redirection", TagInt.class).getIntData();
                }
                redirectedDamagePercent = Math.min(redirectedDamagePercent, 100);
                chance = Math.min(chance, 100);
                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Shield.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, redirectedDamagePercent));
                }
            }
        }
    }

    public String getFormattedValue() {
        return Util.formatText(Translation.getString("Message.Skill.Shield.Format", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, redirectedDamagePercent);
    }

    public void reset() {
        chance = 0;
        redirectedDamagePercent = 0;
    }

    public boolean activate() {
        return random.nextDouble() < chance / 100.;
    }

    public double getRedirectedDamage(double damage) {
        return damage * redirectedDamagePercent / 100.;
    }

    public double redirectDamage(double damage) {
        double redirectedDamage = getRedirectedDamage(damage);
        if (myPet.getStatus() == PetState.Here && myPet.getHealth() - redirectedDamage > 0) {
            myPet.getEntity().damage(redirectedDamage);
            myPet.getEntity().getHandle().makeSound("mob.endermen.portal", 1F, 2F);
            MyPetApi.getBukkitHelper().playParticleEffect(myPet.getOwner().getPlayer().getLocation().add(0, 1, 0), "CRIT_MAGIC", 0.5F, 0.5F, 0.5F, 0.1F, 20, 20);
            MyPetApi.getBukkitHelper().playParticleEffect(myPet.getLocation().add(0, 1, 0), "CRIT", 0.5F, 0.5F, 0.5F, 0.1F, 10, 20);
            return redirectedDamage;
        } else {
            return 0;
        }
    }

    @Override
    public SkillInstance cloneSkill() {
        Shield newSkill = new Shield(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}