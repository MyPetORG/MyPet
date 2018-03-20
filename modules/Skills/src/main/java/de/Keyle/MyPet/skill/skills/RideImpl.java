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

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

public class RideImpl implements Ride {
    protected int speedPercent = 0;
    protected double jumpHeight = 0D;
    protected float flyRegenRate = 0F;
    protected float flyLimit = 0F;
    protected boolean canFly = false;
    private boolean active = false;
    private MyPet myPet;

    public RideImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void reset() {
        speedPercent = 0;
        jumpHeight = 0;
        flyRegenRate = 0;
        flyLimit = 0;
        canFly = false;
    }

    public String toPrettyString() {
        return Translation.getString("Name.Speed", myPet.getOwner().getLanguage()) + " +" + ChatColor.GOLD + speedPercent + ChatColor.RESET + "%";
    }

    public int getSpeedIncrease() {
        return speedPercent;
    }

    public double getJumpHeight() {
        return jumpHeight;
    }

    public float getFlyLimit() {
        return flyLimit;
    }

    public float getFlyRegenRate() {
        return flyRegenRate;
    }

    public boolean canFly() {
        return canFly;
    }

    public void setSpeedIncrease(int speedPercent) {
        this.speedPercent = speedPercent;
    }

    public void setJumpHeight(double jumpHeigth) {
        this.jumpHeight = jumpHeigth;
    }

    public void setFlyRegenRate(float flyRegenRate) {
        this.flyRegenRate = flyRegenRate;
    }

    public void setFlyLimit(float flyLimit) {
        this.flyLimit = flyLimit;
    }

    public void setCanFly(boolean canFly) {
        this.canFly = canFly;
    }

    @Override
    public String toString() {
        return "RideImpl{" +
                "speedPercent=" + speedPercent +
                ", jumpHeight=" + jumpHeight +
                ", flyRegenRate=" + flyRegenRate +
                ", flyLimit=" + flyLimit +
                ", canFly=" + canFly +
                ", active=" + active +
                '}';
    }
}