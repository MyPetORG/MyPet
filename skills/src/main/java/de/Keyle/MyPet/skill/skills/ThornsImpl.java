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

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Thorns;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class ThornsImpl implements Thorns {

    private static Random random = new Random();

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> reflectedDamage = new UpgradeComputer<>(0);
    private MyPet myPet;

    public ThornsImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance.getValue() > 0 && reflectedDamage.getValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        reflectedDamage.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(chance.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text("% -> "))
                .append(Component.text(reflectedDamage.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text("% "))
                .append(Locale.getComponent("Name.Damage", locale))
                .build();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Locale.getFormattedComponent("Message.Skill.Thorns.Upgrade", myPet.getOwner().getLanguage(), myPet.getDisplayName(), getChance().getValue(), getReflectedDamage().getValue())
        };
    }

    protected double calculateReflectedDamage(double damage) {
        return damage * reflectedDamage.getValue() / 100.;
    }

    public UpgradeComputer<Integer> getReflectedDamage() {
        return reflectedDamage;
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    @Override
    public boolean trigger() {
        return random.nextDouble() < chance.getValue() / 100.;
    }

    @Override
    public void apply(LivingEntity damager, EntityDamageByEntityEvent event) {
        if (damager instanceof Creeper) {
            return;
        }
        myPet.getEntity().ifPresent(entity -> {
            damager.damage(calculateReflectedDamage(event.getDamage()), entity);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.2F, 1.0F);
            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1, 0), 20, 0.5F, 0.5F, 0.5F, 0.1F);
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 10, 0.5F, 0.5F, 0.5F, 0.1F);
        });
    }

}
