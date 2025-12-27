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

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Poison;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class PoisonImpl implements Poison {

    private static Random random = new Random();

    private MyPet myPet;
    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> duration = new UpgradeComputer<>(0);

    public PoisonImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance.getValue() > 0 && duration.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        duration.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return "" + ChatColor.GOLD + chance.getValue() + ChatColor.RESET
                + "% -> " + ChatColor.GOLD + duration.getValue() + ChatColor.RESET + " " + Translation.getString("Name.Seconds", locale);
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Poison.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), getChance().getValue(), getDuration().getValue())
        };
    }

    public boolean trigger() {
        return random.nextDouble() <= chance.getValue() / 100.;
    }

    public UpgradeComputer<Integer> getDuration() {
        return duration;
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public void apply(LivingEntity target) {
        PotionEffect effect = new PotionEffect(PotionEffectType.POISON, duration.getValue() * 20, 1, false);
        target.addPotionEffect(effect);
    }

    @Override
    public String toString() {
        return "PoisonImpl{" +
                "chance=" + chance +
                ", duration=" + duration +
                '}';
    }
}