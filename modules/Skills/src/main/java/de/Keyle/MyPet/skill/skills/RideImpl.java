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

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

public class RideImpl implements Ride {

    protected UpgradeComputer<Integer> speed = new UpgradeComputer<>(0);
    protected UpgradeComputer<Number> jumpHeight = new UpgradeComputer<>(0);
    protected UpgradeComputer<Number> flyRegenRate = new UpgradeComputer<>(0);
    protected UpgradeComputer<Number> flyLimit = new UpgradeComputer<>(0);
    protected UpgradeComputer<Boolean> canFly = new UpgradeComputer<>(false);
    protected UpgradeComputer<Boolean> active = new UpgradeComputer<>(false);
    private MyPet myPet;

    public RideImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    @Override
    public void reset() {
        active.removeAllUpgrades();
        speed.removeAllUpgrades();
        jumpHeight.removeAllUpgrades();
        flyRegenRate.removeAllUpgrades();
        flyLimit.removeAllUpgrades();
        canFly.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return Translation.getString("Name.Speed", locale)
                + " +" + ChatColor.GOLD + speed.getValue() + ChatColor.RESET + "%";
    }

    @Override
    public String[] getUpgradeMessage() {
        return null;
    }

    public boolean isActive() {
        return active.getValue();
    }

    public UpgradeComputer<Boolean> getActive() {
        return active;
    }

    public UpgradeComputer<Integer> getSpeedIncrease() {
        return speed;
    }

    public UpgradeComputer<Number> getJumpHeight() {
        return jumpHeight;
    }

    public UpgradeComputer<Number> getFlyLimit() {
        return flyLimit;
    }

    public UpgradeComputer<Number> getFlyRegenRate() {
        return flyRegenRate;
    }

    public UpgradeComputer<Boolean> getCanFly() {
        return canFly;
    }

    @Override
    public String toString() {
        return "RideImpl{" +
                "speed=" + speed +
                ", jumpHeight=" + jumpHeight.getValue().doubleValue() +
                ", flyRegenRate=" + flyRegenRate.getValue().doubleValue() +
                ", flyLimit=" + flyLimit.getValue().doubleValue() +
                ", getCanFly=" + canFly +
                ", active=" + active +
                '}';
    }
}