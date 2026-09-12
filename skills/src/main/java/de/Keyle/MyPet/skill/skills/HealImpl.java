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
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Heal;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.HealUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;

public class HealImpl extends AbstractSkill implements Heal {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Heal.class,
            UpgradeSchema.builder()
                    .number("health").label("Health (+X)").cumulative()
                    .integer("timer").label("Timer (s)").cumulative()
                    .build(), json -> new HealUpgrade()
            .setHealModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "health")))
            .setTimerModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "timer"))));

    protected UpgradeComputer<Number> heal = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> timer = new UpgradeComputer<>(0);
    protected boolean particles = false;
    private int timeCounter = 0;

    public HealImpl(Pet pet) {
        super(pet);
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
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.HpRegeneration.Upgrade", getHeal().getValue().doubleValue(), getTimer().getValue())
        };
    }

    public void schedule() {
        if (pet.getStatus() == PetState.Here && pet.getBukkitEntity() != null) {
            boolean healed = false;
            if (heal.getValue().doubleValue() > 0 && timeCounter-- <= 0) {
                if (pet.getHealth() < pet.getMaxHealth() - 0.01f) {
                    pet.setHealth(pet.getHealth() + heal.getValue().doubleValue());
                    healed = true;
                }
                timeCounter = timer.getValue();
            }
            // Show the green potion swirl on a heal tick and take it down again on
            // the next schedule() call (one second later) — the old code showed and
            // hid it inside the same call, so it was never visible.
            if (healed) {
                if (!particles) {
                    particles = true;
                    pet.showPotionParticles(Color.LIME);
                }
            } else if (particles) {
                particles = false;
                pet.hidePotionParticles();
            }
        } else if (particles) {
            // Entity is gone; nothing to hide, just forget the state.
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
