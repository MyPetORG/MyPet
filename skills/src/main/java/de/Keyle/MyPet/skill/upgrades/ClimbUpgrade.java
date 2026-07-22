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

package de.Keyle.MyPet.skill.upgrades;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.modifier.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.skills.Climb;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@SkillName("Climb")
public class ClimbUpgrade implements Upgrade<Climb> {
    @Getter
    @Setter
    @Accessors(chain = true)
    protected UpgradeBooleanModifier activeModifier = null;

    @Override
    public void apply(Climb skill) {
        skill.getActive().addUpgrade(activeModifier);
    }

    @Override
    public void invert(Climb skill) {
        skill.getActive().removeUpgrade(activeModifier);
    }
}
