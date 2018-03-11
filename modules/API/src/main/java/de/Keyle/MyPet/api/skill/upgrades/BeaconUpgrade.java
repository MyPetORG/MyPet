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
import de.Keyle.MyPet.api.skill.skills.Beacon;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@SkillName("Beacon")
public class BeaconUpgrade implements Upgrade<Beacon> {
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier rangeModifier = null;
    @Getter @Setter
    @Accessors(chain = true)
    protected UpgradeNumberModifier durationModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier numberOfBuffsModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier absorptionModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier fireResistanceModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier hasteModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier healthBoostModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier jumpBoostModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier resistanceModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier speedModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier strengthModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier luckModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier nightVisionModifier = UpgradeBooleanModifier.DontChange;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier waterBreathingModifier = UpgradeBooleanModifier.DontChange;

    @Override
    public void apply(Beacon skill) {
        if (rangeModifier != null) {
            skill.setRange(rangeModifier.modify(skill.getRange()).intValue());
        }
        if (durationModifier != null) {
            skill.setDuration(durationModifier.modify(skill.getDuration()).intValue());
        }
        if (numberOfBuffsModifier != null) {
            skill.setNumberOfBuffs(numberOfBuffsModifier.modify(skill.getNumberOfBuffs()).intValue());
        }

        if (absorptionModifier != null) {
            skill.setBuffLevel(Beacon.Buff.Absorption, absorptionModifier.modify(skill.getBuffLevel(Beacon.Buff.Absorption)).intValue());
        }
        if (fireResistanceModifier != null) {
            skill.setBuffLevel(Beacon.Buff.FireResistance, fireResistanceModifier.modify(skill.getBuffLevel(Beacon.Buff.FireResistance)).intValue());
        }
        if (hasteModifier != null) {
            skill.setBuffLevel(Beacon.Buff.Haste, hasteModifier.modify(skill.getBuffLevel(Beacon.Buff.Haste)).intValue());
        }
        if (healthBoostModifier != null) {
            skill.setBuffLevel(Beacon.Buff.HealthBoost, healthBoostModifier.modify(skill.getBuffLevel(Beacon.Buff.HealthBoost)).intValue());
        }
        if (jumpBoostModifier != null) {
            skill.setBuffLevel(Beacon.Buff.JumpBoost, jumpBoostModifier.modify(skill.getBuffLevel(Beacon.Buff.JumpBoost)).intValue());
        }
        if (resistanceModifier != null) {
            skill.setBuffLevel(Beacon.Buff.Resistance, resistanceModifier.modify(skill.getBuffLevel(Beacon.Buff.Resistance)).intValue());
        }
        if (speedModifier != null) {
            skill.setBuffLevel(Beacon.Buff.Speed, speedModifier.modify(skill.getBuffLevel(Beacon.Buff.Speed)).intValue());
        }
        if (strengthModifier != null) {
            skill.setBuffLevel(Beacon.Buff.Strength, strengthModifier.modify(skill.getBuffLevel(Beacon.Buff.Strength)).intValue());
        }
        if (luckModifier != UpgradeBooleanModifier.DontChange) {
            skill.setBuffLevel(Beacon.Buff.Luck, luckModifier.getBoolean() ? 1 : 0);
        }
        if (nightVisionModifier != UpgradeBooleanModifier.DontChange) {
            skill.setBuffLevel(Beacon.Buff.NightVision, nightVisionModifier.getBoolean() ? 1 : 0);
        }
        if (waterBreathingModifier != UpgradeBooleanModifier.DontChange) {
            skill.setBuffLevel(Beacon.Buff.WaterBreathing, waterBreathingModifier.getBoolean() ? 1 : 0);
        }
    }
}