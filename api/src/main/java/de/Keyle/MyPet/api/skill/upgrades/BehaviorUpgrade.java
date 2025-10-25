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

package de.Keyle.MyPet.api.skill.upgrades;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.modifier.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@SkillName("Behavior")
public class BehaviorUpgrade implements Upgrade<Behavior> {
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier aggroModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier duelModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier farmModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier friendlyModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier raidModifier = null;

    @Override
    public void apply(Behavior skill) {
        skill.getAggressiveBehavior().addUpgrade(aggroModifier);
        skill.getDuelBehavior().addUpgrade(duelModifier);
        skill.getFarmBehavior().addUpgrade(farmModifier);
        skill.getFriendlyBehavior().addUpgrade(friendlyModifier);
        skill.getRaidBehavior().addUpgrade(raidModifier);
    }

    @Override
    public void invert(Behavior skill) {
        skill.getAggressiveBehavior().removeUpgrade(aggroModifier);
        skill.getDuelBehavior().removeUpgrade(duelModifier);
        skill.getFarmBehavior().removeUpgrade(farmModifier);
        skill.getFriendlyBehavior().removeUpgrade(friendlyModifier);
        skill.getRaidBehavior().removeUpgrade(raidModifier);
    }
}
