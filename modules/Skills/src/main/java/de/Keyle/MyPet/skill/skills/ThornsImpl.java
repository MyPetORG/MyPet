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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Thorns;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class ThornsImpl implements Thorns {

    private static Random random = new Random();

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> reflectedDamage = new UpgradeComputer<>(0);
    private MyPet myPet;

    public ThornsImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance.getValue() > 0 && reflectedDamage.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        reflectedDamage.removeAllUpgrades();
    }

    public String toPrettyString() {
        return "" + ChatColor.GOLD + chance.getValue() + ChatColor.RESET
                + "% -> " + ChatColor.GOLD + reflectedDamage.getValue() + ChatColor.RESET
                + "% " + Translation.getString("Name.Damage", myPet.getOwner().getLanguage());
    }

    protected double calculateReflectedDamage(double damage) {
        return damage * reflectedDamage.getValue() / 100.;
    }

    public UpgradeComputer<Integer> getReflectedDamage() {
        return reflectedDamage;
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    @Override
    public boolean trigger() {
        return random.nextDouble() < chance.getValue() / 100.;
    }

    @Override
    public void apply(LivingEntity damager, EntityDamageByEntityEvent event) {
        if (damager instanceof Creeper) {
            return;
        }
        if (myPet.getEntity().isPresent()) {
            MyPetBukkitEntity entity = myPet.getEntity().get();
            damager.damage(calculateReflectedDamage(event.getDamage()), entity);
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                entity.getHandle().makeSound("enchant.thorns.hit", 0.2F, 1.0F);
            } else {
                entity.getHandle().makeSound("damage.thorns", 0.5F, 1.0F);
            }

            MyPetApi.getPlatformHelper().playParticleEffect(entity.getLocation().add(0, 1, 0), "CRIT_MAGIC", 0.5F, 0.5F, 0.5F, 0.1F, 20, 20);
            MyPetApi.getPlatformHelper().playParticleEffect(entity.getLocation().add(0, 1, 0), "CRIT", 0.5F, 0.5F, 0.5F, 0.1F, 10, 20);
        }
    }

    @Override
    public String toString() {
        return "ThornsImpl{" +
                "chance=" + chance +
                ", reflectedDamage=" + reflectedDamage +
                '}';
    }
}