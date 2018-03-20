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
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.skills.Heal;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.Color;

public class HealImpl implements Heal {
    protected double increaseHpBy = 0;
    protected int regenTime = 0;
    private int timeCounter = 0;
    private MyPet myPet;
    protected boolean particles = false;

    public HealImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return increaseHpBy > 0;
    }

    @Override
    public void reset() {
        regenTime = 0;
        increaseHpBy = 0;
    }

    public String toPrettyString() {
        return "+" + ChatColor.GOLD + increaseHpBy + ChatColor.RESET + Translation.getString("Name.HP", myPet.getOwner().getLanguage()) + " ->" + ChatColor.GOLD + regenTime + ChatColor.RESET + "sec";
    }

    public void schedule() {
        if (myPet.getStatus() == PetState.Here) {
            if (increaseHpBy > 0) {
                if (timeCounter-- <= 0) {
                    if (myPet.getHealth() < myPet.getMaxHealth()) {
                        if (!particles) {
                            particles = true;
                            myPet.getEntity().get().getHandle().showPotionParticles(Color.LIME);
                        }
                        myPet.getEntity().get().setHealth(myPet.getHealth() + increaseHpBy);
                    }
                    timeCounter = regenTime;
                } else {
                    particles = false;
                }
            }
            if (particles) {
                particles = false;
                myPet.getEntity().get().getHandle().hidePotionParticles();
            }
        } else if (particles) {
            particles = false;
        }
    }

    public double getIncreaseHpBy() {
        return increaseHpBy;
    }

    public void setIncreaseHpBy(double increaseHpBy) {
        this.increaseHpBy = increaseHpBy;
    }

    public int getRegenTime() {
        return regenTime;
    }

    public void setRegenTime(int regenTime) {
        this.regenTime = regenTime;
    }

    @Override
    public String toString() {
        return "HealImpl{" +
                "increaseHpBy=" + increaseHpBy +
                ", regenTime=" + regenTime +
                '}';
    }
}