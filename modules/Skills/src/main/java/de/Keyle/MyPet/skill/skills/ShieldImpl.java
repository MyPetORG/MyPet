/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.skills.Shield;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

public class ShieldImpl implements Shield {
    protected int chance = 0;
    protected int redirectedDamage = 0;
    private static Random random = new Random();
    private MyPet myPet;

    public ShieldImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance > 0 && redirectedDamage > 0;
    }

    @Override
    public void reset() {
        chance = 0;
        redirectedDamage = 0;
    }

    public String toPrettyString() {
        return Util.formatText(Translation.getString("Message.Skill.Shield.Format", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, redirectedDamage);
    }

    public boolean trigger() {
        return random.nextDouble() < chance / 100.;
    }

    protected double calculateRedirectedDamage(double damage) {
        return damage * redirectedDamage / 100.;
    }

    public void apply(EntityDamageEvent event) {
        double redirectedDamage = calculateRedirectedDamage(event.getFinalDamage());
        if (myPet.getStatus() == PetState.Here && myPet.getHealth() - redirectedDamage > 0) {
            myPet.getEntity().get().damage(redirectedDamage);
            event.setDamage(event.getDamage() - redirectedDamage);
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                myPet.getEntity().get().getHandle().makeSound("entity.endermen.teleport", 0.2F, 1.0F);
            } else {
                myPet.getEntity().get().getHandle().makeSound("mob.endermen.portal", 1F, 2F);
            }
            MyPetApi.getPlatformHelper().playParticleEffect(myPet.getOwner().getPlayer().getLocation().add(0, 1, 0), "CRIT_MAGIC", 0.5F, 0.5F, 0.5F, 0.1F, 20, 20);
            MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, 1, 0), "CRIT", 0.5F, 0.5F, 0.5F, 0.1F, 10, 20);
        }
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public int getRedirectedDamage() {
        return redirectedDamage;
    }

    public void setRedirectedDamage(int redirectedDamage) {
        this.redirectedDamage = redirectedDamage;
    }

    @Override
    public String toString() {
        return "ShieldImpl{" +
                "chance=" + chance +
                ", redirectedDamage=" + redirectedDamage +
                '}';
    }
}