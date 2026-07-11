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
import de.Keyle.MyPet.api.skill.skills.Shield;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

public class ShieldImpl extends AbstractSkill implements Shield {

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> redirectedDamage = new UpgradeComputer<>(0);

    public ShieldImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return chance.getValue() > 0 && redirectedDamage.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        redirectedDamage.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Locale.getFormattedComponent("Message.Skill.Shield.Format", locale, pet.getDisplayName(), chance.getValue(), redirectedDamage.getValue().doubleValue());
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Shield.Upgrade", getChance().getValue(), getRedirectedDamage().getValue())
        };
    }

    public boolean trigger() {
        return ThreadLocalRandom.current().nextDouble() < chance.getValue() / 100.;
    }

    protected double calculateRedirectedDamage(double damage) {
        return damage * redirectedDamage.getValue() / 100.;
    }

    public void apply(EntityDamageEvent event) {
        double redirectedDamage = calculateRedirectedDamage(event.getFinalDamage());
        if (pet.getStatus() == PetState.Here && pet.getHealth() - redirectedDamage > 0) {
            Mob entity = pet.getBukkitEntity();
            if (entity != null) {
                entity.damage(redirectedDamage);
                event.setDamage(event.getDamage() - redirectedDamage);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.2F, 1.0F);
                pet.getOwner().getPlayer().getWorld().spawnParticle(Particle.ENCHANTED_HIT, pet.getOwner().getPlayer().getLocation().add(0, 1, 0), 20, 0.5F, 0.5F, 0.5F, 0.1F);
                pet.getLocation().get().getWorld().spawnParticle(Particle.CRIT, pet.getLocation().get().add(0, 1, 0), 10, 0.5F, 0.5F, 0.5F, 0.1F);
            }
        }
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public UpgradeComputer<Integer> getRedirectedDamage() {
        return redirectedDamage;
    }

}
