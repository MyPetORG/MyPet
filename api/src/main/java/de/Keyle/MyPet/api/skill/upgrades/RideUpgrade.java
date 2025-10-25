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
import de.Keyle.MyPet.api.skill.modifier.UpgradeIntegerModifier;
import de.Keyle.MyPet.api.skill.modifier.UpgradeNumberModifier;
import de.Keyle.MyPet.api.skill.skills.Ride;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@SkillName("Ride")
public class RideUpgrade implements Upgrade<Ride> {

    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier activeModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier speedIncreaseModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier jumpHeightModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier flyLimitModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier flyRegenRateModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier canFlyModifier = null;

    @Override
    public void apply(Ride skill) {
        skill.getActive().addUpgrade(activeModifier);
        skill.getSpeedIncrease().addUpgrade(speedIncreaseModifier);
        skill.getJumpHeight().addUpgrade(jumpHeightModifier);
        skill.getFlyLimit().addUpgrade(flyLimitModifier);
        skill.getFlyRegenRate().addUpgrade(flyRegenRateModifier);
        skill.getCanFly().addUpgrade(canFlyModifier);
    }

    @Override
    public void invert(Ride skill) {
        skill.getActive().removeUpgrade(activeModifier);
        skill.getSpeedIncrease().removeUpgrade(speedIncreaseModifier);
        skill.getJumpHeight().removeUpgrade(jumpHeightModifier);
        skill.getFlyLimit().removeUpgrade(flyLimitModifier);
        skill.getFlyRegenRate().removeUpgrade(flyRegenRateModifier);
        skill.getCanFly().removeUpgrade(canFlyModifier);
    }
}
