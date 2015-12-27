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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.ThornsInfo;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class Thorns extends ThornsInfo implements ISkillInstance, ISkillActive {
    private static Random random = new Random();
    private MyPet myPet;

    public Thorns(boolean addedByInheritance) {
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

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof ThornsInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("chance")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_chance") || upgrade.getProperties().getAs("addset_chance", TagString.class).getStringData().equals("add")) {
                    chance += upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                } else {
                    chance = upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                }
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_reflection") || upgrade.getProperties().getAs("addset_reflection", TagString.class).getStringData().equals("add")) {
                    reflectedDamagePercent += upgrade.getProperties().getAs("reflection", TagInt.class).getIntData();
                } else {
                    reflectedDamagePercent = upgrade.getProperties().getAs("reflection", TagInt.class).getIntData();
                }
                reflectedDamagePercent = Math.min(reflectedDamagePercent, 100);
                chance = Math.min(chance, 100);
                if (!quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Thorns.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, reflectedDamagePercent));
                }
            }
        }
    }

    public String getFormattedValue() {
        return chance + "% -> " + reflectedDamagePercent + "% " + Locales.getString("Name.Damage", myPet.getOwner().getLanguage());
    }

    public void reset() {
        chance = 0;
        reflectedDamagePercent = 0;
    }

    public boolean activate() {
        return random.nextDouble() < chance / 100.;
    }

    public double getReflectedDamage(double damage) {
        return damage * reflectedDamagePercent / 100.;
    }

    public void reflectDamage(LivingEntity damager, double damage) {
        damager.damage(getReflectedDamage(damage), myPet.getCraftPet());
        myPet.getCraftPet().getHandle().makeSound("damage.thorns", 0.5F, 1.0F);
        BukkitUtil.playParticleEffect(myPet.getLocation().add(0, 1, 0), EnumParticle.CRIT_MAGIC, 0.5F, 0.5F, 0.5F, 0.1F, 20, 20);
        BukkitUtil.playParticleEffect(myPet.getLocation().add(0, 1, 0), EnumParticle.CRIT, 0.5F, 0.5F, 0.5F, 0.1F, 10, 20);
    }

    @Override
    public ISkillInstance cloneSkill() {
        Thorns newSkill = new Thorns(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}