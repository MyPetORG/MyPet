/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.compat.SoundCompat;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Shield;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

public class ShieldImpl implements Shield {

    private static Random random = new Random();

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> redirectedDamage = new UpgradeComputer<>(0);
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
        return chance.getValue() > 0 && redirectedDamage.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        redirectedDamage.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return Util.formatText(Translation.getString("Message.Skill.Shield.Format", locale), myPet.getPetName(), chance.getValue(), redirectedDamage.getValue().doubleValue());
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Shield.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), getChance().getValue(), getRedirectedDamage().getValue())
        };
    }

    public boolean trigger() {
        return random.nextDouble() < chance.getValue() / 100.;
    }

    protected double calculateRedirectedDamage(double damage) {
        return damage * redirectedDamage.getValue() / 100.;
    }

    public void apply(EntityDamageEvent event) {
        double redirectedDamage = calculateRedirectedDamage(event.getFinalDamage());
        if (myPet.getStatus() == PetState.Here && myPet.getHealth() - redirectedDamage > 0) {
            myPet.getEntity().ifPresent(myPetBukkitEntity -> {
                myPetBukkitEntity.damage(redirectedDamage);
                event.setDamage(event.getDamage() - redirectedDamage);
                myPetBukkitEntity.getHandle().makeSound(SoundCompat.ENDERMAN_TELEPORT.get(), 0.2F, 1.0F);
                MyPetApi.getPlatformHelper().playParticleEffect(myPet.getOwner().getPlayer().getLocation().add(0, 1, 0), ParticleCompat.CRIT_MAGIC.get(), 0.5F, 0.5F, 0.5F, 0.1F, 20, 20);
                MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, 1, 0), ParticleCompat.CRIT.get(), 0.5F, 0.5F, 0.5F, 0.1F, 10, 20);
            });
        }
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public UpgradeComputer<Integer> getRedirectedDamage() {
        return redirectedDamage;
    }

    @Override
    public String toString() {
        return "ShieldImpl{" +
                "chance=" + chance +
                ", redirectedDamage=" + redirectedDamage.getValue() +
                '}';
    }
}