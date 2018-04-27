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
import de.Keyle.MyPet.api.skill.UpgradeNumberModifier;
import de.Keyle.MyPet.api.skill.skills.Ride;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@SkillName("Ride")
public class RideUpgrade implements Upgrade<Ride> {

    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier activeModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier speedIncreaseModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier jumpHeightModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier flyLimitModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier flyRegenRateModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier canFlyModifier = UpgradeBooleanModifier.DontChange;

    @Override
    public void apply(Ride skill) {
        if (activeModifier != UpgradeBooleanModifier.DontChange) {
            skill.setActive(activeModifier.getBoolean());
        }
        if (speedIncreaseModifier != null) {
            skill.setSpeedIncrease(speedIncreaseModifier.modify(skill.getSpeedIncrease()).intValue());
        }
        if (jumpHeightModifier != null) {
            skill.setJumpHeight(jumpHeightModifier.modify(skill.getJumpHeight()).doubleValue());
        }
        if (flyLimitModifier != null) {
            skill.setFlyLimit(flyLimitModifier.modify(skill.getFlyLimit()).floatValue());
        }
        if (flyRegenRateModifier != null) {
            skill.setFlyRegenRate(flyRegenRateModifier.modify(skill.getFlyRegenRate()).floatValue());
        }
        if (canFlyModifier != UpgradeBooleanModifier.DontChange) {
            skill.setCanFly(canFlyModifier.getBoolean());
        }
    }

    @Override
    public void invert(Ride skill) {
        if (activeModifier != UpgradeBooleanModifier.DontChange) {
            skill.setActive(activeModifier.getInvertedBoolean());
        }
        if (speedIncreaseModifier != null) {
            skill.setSpeedIncrease(speedIncreaseModifier.invert(skill.getSpeedIncrease()).intValue());
        }
        if (jumpHeightModifier != null) {
            skill.setJumpHeight(jumpHeightModifier.invert(skill.getJumpHeight()).doubleValue());
        }
        if (flyLimitModifier != null) {
            skill.setFlyLimit(flyLimitModifier.invert(skill.getFlyLimit()).floatValue());
        }
        if (flyRegenRateModifier != null) {
            skill.setFlyRegenRate(flyRegenRateModifier.invert(skill.getFlyRegenRate()).floatValue());
        }
        if (canFlyModifier != UpgradeBooleanModifier.DontChange) {
            skill.setCanFly(canFlyModifier.getInvertedBoolean());
        }
    }
}
