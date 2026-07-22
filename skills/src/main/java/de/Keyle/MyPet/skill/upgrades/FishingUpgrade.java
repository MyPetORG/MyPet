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
import de.Keyle.MyPet.api.skill.modifier.UpgradeIntegerModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeNumberModifier;
import de.Keyle.MyPet.api.skill.skills.Fishing;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@SkillName("Fishing")
public class FishingUpgrade implements Upgrade<Fishing> {
    @Getter
    @Setter
    @Accessors(chain = true)
    protected UpgradeNumberModifier rangeModifier = null;
    @Getter
    @Setter
    @Accessors(chain = true)
    protected UpgradeIntegerModifier intervalModifier = null;
    @Getter
    @Setter
    @Accessors(chain = true)
    protected UpgradeBooleanModifier toollessModifier = null;

    @Override
    public void apply(Fishing skill) {
        skill.getRange().addUpgrade(rangeModifier);
        skill.getInterval().addUpgrade(intervalModifier);
        skill.getToolless().addUpgrade(toollessModifier);
    }

    @Override
    public void invert(Fishing skill) {
        skill.getRange().removeUpgrade(rangeModifier);
        skill.getInterval().removeUpgrade(intervalModifier);
        skill.getToolless().removeUpgrade(toollessModifier);
    }
}
