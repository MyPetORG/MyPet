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
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Heal;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.Color;

public class HealImpl implements Heal {

    protected UpgradeComputer<Number> heal = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> timer = new UpgradeComputer<>(0);
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
        return heal.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        timer.removeAllUpgrades();
        heal.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return "+" + ChatColor.GOLD + heal.getValue().doubleValue() + ChatColor.RESET
                + Translation.getString("Name.HP", locale)
                + " -> " + ChatColor.GOLD + timer.getValue() + ChatColor.RESET + " " + Translation.getString("Name.Seconds", locale);
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.HpRegeneration.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), getHeal().getValue().doubleValue(), getTimer().getValue())
        };
    }

    public void schedule() {
        if (myPet.getStatus() == PetState.Here) {
            myPet.getEntity().ifPresent(entity -> {
                if (heal.getValue().doubleValue() > 0) {
                    if (timeCounter-- <= 0) {

                        if (myPet.getHealth() < myPet.getMaxHealth()) {
                            if (!particles) {
                                particles = true;
                                entity.getHandle().showPotionParticles(Color.LIME);
                            }
                            entity.setHealth(myPet.getHealth() + heal.getValue().doubleValue());
                        }
                        timeCounter = timer.getValue();
                    } else {
                        particles = false;
                    }
                }
                if (particles) {
                    particles = false;
                    entity.getHandle().hidePotionParticles();
                }
            });
        } else if (particles) {
            particles = false;
        }
    }

    public UpgradeComputer<Number> getHeal() {
        return heal;
    }

    public UpgradeComputer<Integer> getTimer() {
        return timer;
    }

    @Override
    public String toString() {
        return "HealImpl{" +
                "heal=" + heal.getValue().doubleValue() +
                ", timer=" + timer +
                '}';
    }
}