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
import de.Keyle.MyPet.api.skill.skills.Knockback;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Random;

public class KnockbackImpl implements Knockback {

    private static Random random = new Random();

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    private MyPet myPet;

    public KnockbackImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return "" + ChatColor.GOLD + chance.getValue() + ChatColor.RESET + "%";
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Knockback.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), getChance().getValue())
        };
    }

    public boolean trigger() {
        return random.nextDouble() < chance.getValue() / 100.;
    }

    public void apply(LivingEntity target) {
        double yaw = myPet.getLocation().get().getYaw() % 360;
        target.setVelocity(new Vector(
                -Math.sin(yaw * Math.PI / 180.0F) * 2 * 0.5F,
                0.1D,
                Math.cos(yaw * Math.PI / 180.0F) * 2 * 0.5F
        ));
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    @Override
    public String toString() {
        return "KnockbackImpl{" +
                "chance=" + chance +
                '}';
    }
}