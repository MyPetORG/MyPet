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
import de.Keyle.MyPet.api.skill.skills.Beacon;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@SuppressWarnings("ALL")
@ToString
@SkillName("Beacon")
public class BeaconUpgrade implements Upgrade<Beacon> {

    @Getter @Setter @Accessors(chain = true)
    protected UpgradeNumberModifier rangeModifier = null;
    @Getter @Setter
    @Accessors(chain = true)
    protected UpgradeIntegerModifier durationModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier numberOfBuffsModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier absorptionModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier hasteModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier healthBoostModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier jumpBoostModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier resistanceModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier speedModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier strengthModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeIntegerModifier regenerationModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier fireResistanceModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier luckModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier nightVisionModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier waterBreathingModifier = null;
    @Getter @Setter @Accessors(chain = true)
    protected UpgradeBooleanModifier invisibilityModifier = null;

    @Override
    public void apply(Beacon skill) {
        skill.getRange().addUpgrade(rangeModifier);
        skill.getDuration().addUpgrade(durationModifier);
        skill.getNumberOfBuffs().addUpgrade(numberOfBuffsModifier);

        skill.getBuff(Beacon.Buff.Absorption).addUpgrade(absorptionModifier);
        skill.getBuff(Beacon.Buff.FireResistance).addUpgrade(fireResistanceModifier);
        skill.getBuff(Beacon.Buff.Haste).addUpgrade(hasteModifier);
        skill.getBuff(Beacon.Buff.HealthBoost).addUpgrade(healthBoostModifier);
        skill.getBuff(Beacon.Buff.JumpBoost).addUpgrade(jumpBoostModifier);
        skill.getBuff(Beacon.Buff.Resistance).addUpgrade(resistanceModifier);
        skill.getBuff(Beacon.Buff.Speed).addUpgrade(speedModifier);
        skill.getBuff(Beacon.Buff.Strength).addUpgrade(strengthModifier);
        skill.getBuff(Beacon.Buff.Regeneration).addUpgrade(regenerationModifier);
        skill.getBuff(Beacon.Buff.Luck).addUpgrade(luckModifier);
        skill.getBuff(Beacon.Buff.NightVision).addUpgrade(nightVisionModifier);
        skill.getBuff(Beacon.Buff.WaterBreathing).addUpgrade(waterBreathingModifier);
        skill.getBuff(Beacon.Buff.Invisibility).addUpgrade(invisibilityModifier);
    }

    @Override
    public void invert(Beacon skill) {
        skill.getRange().removeUpgrade(rangeModifier);
        skill.getDuration().removeUpgrade(durationModifier);
        skill.getNumberOfBuffs().removeUpgrade(numberOfBuffsModifier);

        skill.getBuff(Beacon.Buff.Absorption).removeUpgrade(absorptionModifier);
        skill.getBuff(Beacon.Buff.FireResistance).removeUpgrade(fireResistanceModifier);
        skill.getBuff(Beacon.Buff.Haste).removeUpgrade(hasteModifier);
        skill.getBuff(Beacon.Buff.HealthBoost).removeUpgrade(healthBoostModifier);
        skill.getBuff(Beacon.Buff.JumpBoost).removeUpgrade(jumpBoostModifier);
        skill.getBuff(Beacon.Buff.Resistance).removeUpgrade(resistanceModifier);
        skill.getBuff(Beacon.Buff.Speed).removeUpgrade(speedModifier);
        skill.getBuff(Beacon.Buff.Strength).removeUpgrade(strengthModifier);
        skill.getBuff(Beacon.Buff.Regeneration).removeUpgrade(regenerationModifier);
        skill.getBuff(Beacon.Buff.Luck).removeUpgrade(luckModifier);
        skill.getBuff(Beacon.Buff.NightVision).removeUpgrade(nightVisionModifier);
        skill.getBuff(Beacon.Buff.WaterBreathing).removeUpgrade(waterBreathingModifier);
        skill.getBuff(Beacon.Buff.Invisibility).removeUpgrade(invisibilityModifier);
    }
}