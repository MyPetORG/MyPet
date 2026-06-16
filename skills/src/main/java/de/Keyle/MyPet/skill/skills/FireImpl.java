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
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Fire;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class FireImpl extends AbstractSkill implements Fire {

    private static Random random = new Random();

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> duration = new UpgradeComputer<>(0);

    public FireImpl(Pet pet) {
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
                .append(Component.text(duration.getValue().doubleValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .build();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Fire.Upgrade", getChance().getValue(), getDuration().getValue())
        };
    }

    public boolean trigger() {
        return random.nextDouble() <= chance.getValue() / 100.;
    }

    public UpgradeComputer<Integer> getDuration() {
        return duration;
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public void apply(LivingEntity target) {
        target.setFireTicks(getDuration().getValue() * 20);
        target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation(), 20, .5f, .5f, .5f, 0.02f);
    }

}
