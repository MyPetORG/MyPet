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
import de.Keyle.MyPet.api.skill.skills.Slow;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.SlowUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class SlowImpl extends AbstractSkill implements Slow {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Slow.class,
            UpgradeSchema.builder()
                    .integer("chance").label("Chance %").suffix("%").cumulative()
                    .integer("duration").label("Duration (ticks)").cumulative()
                    .build(), json -> new SlowUpgrade()
            .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
            .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration"))));

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> duration = new UpgradeComputer<>(0);

    public SlowImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return chance.getValue() > 0 && duration.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        duration.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(chance.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text("% -> "))
                .append(Component.text(duration.getValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Slow.Upgrade", getChance().getValue(), getDuration().getValue())
        };
    }

    public boolean trigger() {
        return ThreadLocalRandom.current().nextDouble() <= chance.getValue() / 100.;
    }

    public UpgradeComputer<Integer> getDuration() {
        return duration;
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public void apply(LivingEntity target) {
        PotionEffect effect = new PotionEffect(PotionEffectType.SLOWNESS, duration.getValue() * 20, 1, false);
        target.addPotionEffect(effect);
    }

}
