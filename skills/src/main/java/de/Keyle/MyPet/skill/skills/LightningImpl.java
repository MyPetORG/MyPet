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
import de.Keyle.MyPet.api.skill.skills.Lightning;
import de.Keyle.MyPet.api.util.locale.Locale;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class LightningImpl extends AbstractSkill implements Lightning {

    @Getter
    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    @Getter
    protected UpgradeComputer<Number> damage = new UpgradeComputer<>(0);
    private boolean isStriking = false;

    public LightningImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return chance.getValue() > 0 && damage.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        damage.removeAllUpgrades();
        chance.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(chance.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text("% -> "))
                .append(Component.text(damage.getValue().doubleValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Damage", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Lightning.Upgrade", getChance().getValue(), getDamage().getValue().doubleValue())
        };
    }

    public boolean trigger() {
        return !isStriking && ThreadLocalRandom.current().nextDouble() <= chance.getValue() / 100.;
    }

    public void apply(LivingEntity target) {
        isStriking = true;
        Mob petEntity = pet.getBukkitEntity();
        if (petEntity != null) {
            Player owner = pet.getOwner().getPlayer();
            World world = target.getLocation().getWorld();
            if (world != null) {
                world.strikeLightningEffect(target.getLocation());
            }
            target.damage(damage.getValue().doubleValue(), petEntity);
            for (Entity entity : target.getNearbyEntities(1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity living && living != owner && living != petEntity) {
                    living.damage(damage.getValue().doubleValue(), petEntity);
                }
            }
        }
        isStriking = false;
    }

}
