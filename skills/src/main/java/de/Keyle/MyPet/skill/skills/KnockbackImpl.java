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
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Knockback;
import de.Keyle.MyPet.skill.upgrades.KnockbackUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class KnockbackImpl extends AbstractSkill implements Knockback {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Knockback.class,
            UpgradeSchema.builder()
                    .integer("chance").label("Chance %").suffix("%").cumulative()
                    .build(), json -> new KnockbackUpgrade()
            .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance"))));

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);

    public KnockbackImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return chance.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(chance.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text("%"))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Knockback.Upgrade", getChance().getValue())
        };
    }

    public boolean trigger() {
        return ThreadLocalRandom.current().nextDouble() < chance.getValue() / 100.;
    }

    public void apply(LivingEntity target) {
        double yaw = pet.getLocation().get().getYaw() % 360;
        target.setVelocity(new Vector(
                -Math.sin(yaw * Math.PI / 180.0F) * 2 * 0.5F,
                0.1D,
                Math.cos(yaw * Math.PI / 180.0F) * 2 * 0.5F
        ));
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

}
