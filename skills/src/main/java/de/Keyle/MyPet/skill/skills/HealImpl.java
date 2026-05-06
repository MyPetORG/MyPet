/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Heal;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;

public class HealImpl implements Heal {

    protected UpgradeComputer<Number> heal = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> timer = new UpgradeComputer<>(0);
    protected boolean particles = false;
    private int timeCounter = 0;
    private Pet pet;

    public HealImpl(Pet pet) {
        this.pet = pet;
    }

    public Pet getPet() {
        return pet;
    }

    public boolean isActive() {
        return heal.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        timer.removeAllUpgrades();
        heal.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text("+"))
                .append(Component.text(heal.getValue().doubleValue()).color(NamedTextColor.GOLD))
                .append(Locale.getComponent("Name.HP", locale))
                .append(Component.text(" -> "))
                .append(Component.text(timer.getValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .build();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Locale.getFormattedComponent("Message.Skill.HpRegeneration.Upgrade", pet.getOwner().getLanguage(), pet.getDisplayName(), getHeal().getValue().doubleValue(), getTimer().getValue())
        };
    }

    public void schedule() {
        if (pet.getStatus() == PetState.Here) {
            if (pet.getBukkitEntity() != null) {
                if (heal.getValue().doubleValue() > 0) {
                    if (timeCounter-- <= 0) {
                        if (pet.getHealth() < pet.getMaxHealth() - 0.01f) {
                            if (!particles) {
                                particles = true;
                                pet.showPotionParticles(Color.LIME);
                            }
                            pet.setHealth(pet.getHealth() + heal.getValue().doubleValue());
                        }
                        timeCounter = timer.getValue();
                    } else {
                        particles = false;
                    }
                }
                if (particles) {
                    particles = false;
                    pet.hidePotionParticles();
                }
            }
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

}
