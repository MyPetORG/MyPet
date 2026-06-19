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
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RangedImpl extends AbstractSkill implements Ranged {

    protected UpgradeComputer<Number> damage = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> rateOfFire = new UpgradeComputer<>(1);
    protected UpgradeComputer<Projectile> projectile = new UpgradeComputer<>(Projectile.Arrow);

    public RangedImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return damage.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        damage.removeAllUpgrades();
        rateOfFire.removeAllUpgrades();
        projectile.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Locale.getFormattedComponent("Message.Skill.Ranged.RoundsPerMinute", locale, String.format("%1.2f", (1. / ((rateOfFire.getValue() * 50.) / 1000.)) * 60.)))
                .append(Component.text(" -> "))
                .append(Component.text(damage.getValue().doubleValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Damage", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Locale.getFormattedComponent("Message.Skill.Ranged.Upgrade", pet.getOwner().getLanguage(), pet.getDisplayName(), Locale.getComponent("Name." + getProjectile().getValue().name(), pet.getOwner()), damage, String.format("%1.2f", (1. / ((getRateOfFire().getValue() * 50.) / 1000.)) * 60.))
        };
    }

    public UpgradeComputer<Integer> getRateOfFire() {
        return rateOfFire;
    }

    public UpgradeComputer<Number> getDamage() {
        return damage;
    }

    public UpgradeComputer<Projectile> getProjectile() {
        return projectile;
    }

}
