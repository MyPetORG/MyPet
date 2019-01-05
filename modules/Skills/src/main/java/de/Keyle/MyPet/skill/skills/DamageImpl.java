/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Damage;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

public class DamageImpl implements Damage {
    protected MyPet myPet;
    protected UpgradeComputer<Number> damage = new UpgradeComputer<>(0);

    public DamageImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return damage.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        damage.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return "" + ChatColor.GOLD + damage.getValue().doubleValue() + ChatColor.RESET + " " + Translation.getString("Name.Damage", locale);
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Damage.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), getDamage().getValue().doubleValue())
        };
    }

    public UpgradeComputer<Number> getDamage() {
        return damage;
    }

    @Override
    public String toString() {
        return "DamageImpl{" +
                "damage=" + damage.getValue().doubleValue() +
                '}';
    }
}