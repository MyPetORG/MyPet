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
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

public class RangedImpl implements Ranged {

    protected UpgradeComputer<Number> damage = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> rateOfFire = new UpgradeComputer<>(1);
    protected UpgradeComputer<Projectile> projectile = new UpgradeComputer<>(Projectile.Arrow);
    private MyPet myPet;

    public RangedImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return damage.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        damage.removeAllUpgrades();
        rateOfFire.removeAllUpgrades();
        projectile.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return Util.formatText(Translation.getString("Message.Skill.Ranged.RoundsPerMinute", locale), String.format("%1.2f", (1. / ((rateOfFire.getValue() * 50.) / 1000.)) * 60.))
                + " -> " + ChatColor.GOLD + damage.getValue().doubleValue() + ChatColor.RESET
                + " " + Translation.getString("Name.Damage", locale);
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Ranged.Upgrade", myPet.getOwner()), myPet.getPetName(), Translation.getString("Name." + getProjectile().getValue().name(), myPet.getOwner()), damage, String.format("%1.2f", (1. / ((getRateOfFire().getValue() * 50.) / 1000.)) * 60.))
        };
    }

    public UpgradeComputer<Integer> getRateOfFire() {
        return rateOfFire;
    }

    public UpgradeComputer<Number> getDamage() {
        return damage;
    }

    public UpgradeComputer<Projectile> getProjectile() {
        return projectile;
    }

    @Override
    public String toString() {
        return "RangedImpl{" +
                "projectile=" + projectile +
                ", damage=" + damage +
                ", rateOfFire=" + rateOfFire +
                '}';
    }
}