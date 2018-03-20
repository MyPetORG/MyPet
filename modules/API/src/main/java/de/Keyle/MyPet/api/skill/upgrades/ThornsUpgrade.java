/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

package de.Keyle.MyPet.api.skill.upgrades;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.UpgradeNumberModifier;
import de.Keyle.MyPet.api.skill.skills.Thorns;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@SkillName("Thorns")
public class ThornsUpgrade implements Upgrade<Thorns> {
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier chanceModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier reflectedDamageModifier = null;

    @Override
    public void apply(Thorns skill) {
        if (chanceModifier != null) {
            skill.setChance(chanceModifier.modify(skill.getChance()).intValue());
        }
        if (reflectedDamageModifier != null) {
            skill.setReflectedDamage(reflectedDamageModifier.modify(skill.getReflectedDamage()).intValue());
        }
    }

    @Override
    public void invert(Thorns skill) {
        if (chanceModifier != null) {
            skill.setChance(chanceModifier.invert(skill.getChance()).intValue());
        }
        if (reflectedDamageModifier != null) {
            skill.setReflectedDamage(reflectedDamageModifier.invert(skill.getReflectedDamage()).intValue());
        }
    }
}
