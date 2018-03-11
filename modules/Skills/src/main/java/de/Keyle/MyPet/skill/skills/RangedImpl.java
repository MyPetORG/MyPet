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

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

public class RangedImpl implements Ranged {
    protected Projectile selectedProjectile = Projectile.Arrow;
    protected double damage = 0;
    protected int rateOfFire = 0;
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
        return damage > 0;
    }

    public String toPrettyString() {
        return Util.formatText(Translation.getString("Message.Skill.Ranged.RoundsPerMinute", myPet.getOwner()), String.format("%1.2f", (1. / ((getRateOfFire() * 50.) / 1000.)) * 60.)) + " -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Translation.getString("Name.Damage", myPet.getOwner());
    }

    public int getRateOfFire() {
        if (rateOfFire == 0) {
            rateOfFire = 1;
        }
        return rateOfFire;
    }

    public double getDamage() {
        return damage;
    }

    public Projectile getProjectile() {
        return selectedProjectile;
    }

    public Projectile getSelectedProjectile() {
        return selectedProjectile;
    }

    public void setSelectedProjectile(Projectile selectedProjectile) {
        this.selectedProjectile = selectedProjectile;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setRateOfFire(int rateOfFire) {
        this.rateOfFire = rateOfFire;
    }

    @Override
    public String toString() {
        return "RangedImpl{" +
                "selectedProjectile=" + selectedProjectile +
                ", damage=" + damage +
                ", rateOfFire=" + rateOfFire +
                '}';
    }
}