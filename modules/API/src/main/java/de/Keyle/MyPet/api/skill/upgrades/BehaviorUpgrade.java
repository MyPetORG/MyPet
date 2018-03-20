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
import de.Keyle.MyPet.api.skill.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@SkillName("Behavior")
public class BehaviorUpgrade implements Upgrade<Behavior> {
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier aggroModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier duelModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier farmModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier friendModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier raidModifier = UpgradeBooleanModifier.DontChange;

    @Override
    public void apply(Behavior skill) {
        if (aggroModifier != UpgradeBooleanModifier.DontChange) {
            if (aggroModifier.getBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Aggressive);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Aggressive);
            }
        }
        if (duelModifier != UpgradeBooleanModifier.DontChange) {
            if (duelModifier.getBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Duel);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Duel);
            }
        }
        if (farmModifier != UpgradeBooleanModifier.DontChange) {
            if (farmModifier.getBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Farm);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Farm);
            }
        }
        if (friendModifier != UpgradeBooleanModifier.DontChange) {
            if (friendModifier.getBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Friendly);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Friendly);
            }
        }
        if (raidModifier != UpgradeBooleanModifier.DontChange) {
            if (raidModifier.getBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Raid);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Raid);
            }
        }
    }

    @Override
    public void invert(Behavior skill) {
        if (aggroModifier != UpgradeBooleanModifier.DontChange) {
            if (aggroModifier.getInvertedBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Aggressive);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Aggressive);
            }
        }
        if (duelModifier != UpgradeBooleanModifier.DontChange) {
            if (duelModifier.getInvertedBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Duel);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Duel);
            }
        }
        if (farmModifier != UpgradeBooleanModifier.DontChange) {
            if (farmModifier.getInvertedBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Farm);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Farm);
            }
        }
        if (friendModifier != UpgradeBooleanModifier.DontChange) {
            if (friendModifier.getInvertedBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Friendly);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Friendly);
            }
        }
        if (raidModifier != UpgradeBooleanModifier.DontChange) {
            if (raidModifier.getInvertedBoolean()) {
                skill.enableBehavior(Behavior.BehaviorMode.Raid);
            } else {
                skill.disableBehavior(Behavior.BehaviorMode.Raid);
            }
        }
    }
}
