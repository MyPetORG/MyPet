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
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

public class ShieldImpl implements Shield {

    private static Random random = new Random();

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> redirectedDamage = new UpgradeComputer<>(0);
    private Pet pet;

    public ShieldImpl(Pet pet) {
        this.pet = pet;
    }

    public Pet getPet() {
        return pet;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
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
                Locale.getFormattedComponent("Message.Skill.Shield.Upgrade", pet.getOwner().getLanguage(), pet.getDisplayName(), getChance().getValue(), getRedirectedDamage().getValue())
        };
    }

    public boolean trigger() {
        return random.nextDouble() < chance.getValue() / 100.;
    }

    protected double calculateRedirectedDamage(double damage) {
        return damage * redirectedDamage.getValue() / 100.;
    }

    public void apply(EntityDamageEvent event) {
        double redirectedDamage = calculateRedirectedDamage(event.getFinalDamage());
        if (pet.getStatus() == PetState.Here && pet.getHealth() - redirectedDamage > 0) {
            pet.getEntity().ifPresent(pet -> {
                pet.damage(redirectedDamage);
                event.setDamage(event.getDamage() - redirectedDamage);
                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.2F, 1.0F);
                this.pet.getOwner().getPlayer().getWorld().spawnParticle(Particle.ENCHANTED_HIT, this.pet.getOwner().getPlayer().getLocation().add(0, 1, 0), 20, 0.5F, 0.5F, 0.5F, 0.1F);
                this.pet.getLocation().get().getWorld().spawnParticle(Particle.CRIT, this.pet.getLocation().get().add(0, 1, 0), 10, 0.5F, 0.5F, 0.5F, 0.1F);
            });
        }
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public UpgradeComputer<Integer> getRedirectedDamage() {
        return redirectedDamage;
    }

}
