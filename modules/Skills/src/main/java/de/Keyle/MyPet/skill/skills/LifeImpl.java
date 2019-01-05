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
import de.Keyle.MyPet.api.skill.skills.Life;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

public class LifeImpl implements Life {

    protected UpgradeComputer<Number> life = new UpgradeComputer<>(0);
    private MyPet myPet;

    public LifeImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return life.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        life.removeAllUpgrades();
    }

    public String toPrettyString(String locale) {
        return "+" + ChatColor.GOLD + life.getValue().doubleValue();
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Hp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), myPet.getMaxHealth())
        };
    }

    public UpgradeComputer<Number> getLife() {
        return life;
    }

    @Override
    public String toString() {
        return "LifeImpl{" +
                "life=" + life.getValue().doubleValue() +
                '}';
    }
}