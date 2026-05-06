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

import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.skills.*;

/**
 * Registers parsers for MyPet's bundled {@code *Upgrade} types with
 * {@link SkillManager#registerUpgradeParser}. Mirrors {@code BuiltInSkills.register()}
 * — invoked once during plugin enable, immediately after the built-in skill
 * classes themselves are registered, before skilltrees are loaded from disk.
 */
public final class BuiltInUpgradeParsers {

    private BuiltInUpgradeParsers() {
    }

    public static void register(SkillManager skillManager) {
        skillManager.registerUpgradeParser(Backpack.class, json -> new BackpackUpgrade()
                .setRowsModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "rows")))
                .setDropOnDeathModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "drop"))));

        skillManager.registerUpgradeParser(Beacon.class, json -> {
            JsonObject buffs = (JsonObject) UpgradeParsers.get(json, "buffs");
            return new BeaconUpgrade()
                    .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
                    .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration")))
                    .setNumberOfBuffsModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "count")))
                    .setAbsorptionModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "absorption")))
                    .setFireResistanceModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "fireresistance")))
                    .setHasteModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "haste")))
                    .setLuckModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "luck")))
                    .setNightVisionModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "nightvision")))
                    .setResistanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "resistance")))
                    .setSpeedModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "speed")))
                    .setStrengthModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "strength")))
                    .setWaterBreathingModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "waterbreathing")))
                    .setRegenerationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "regeneration")))
                    .setInvisibilityModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "invisibility")))
                    .setJumpBoostModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "jumpboost")));
        });

        skillManager.registerUpgradeParser(Behavior.class, json -> new BehaviorUpgrade()
                .setAggroModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "aggro")))
                .setDuelModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "duel")))
                .setFarmModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "farm")))
                .setFriendlyModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "friend")))
                .setRaidModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "raid"))));

        skillManager.registerUpgradeParser(Bleed.class, json -> new BleedUpgrade()
                .setDamageModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "damage")))
                .setIntervalModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "interval")))
                .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration")))
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance"))));

        skillManager.registerUpgradeParser(Control.class, json -> new ControlUpgrade()
                .setActiveModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "active"))));

        skillManager.registerUpgradeParser(Damage.class, json -> new DamageUpgrade()
                .setDamageModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "damage"))));

        skillManager.registerUpgradeParser(Fire.class, json -> new FireUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration"))));

        skillManager.registerUpgradeParser(Heal.class, json -> new HealUpgrade()
                .setHealModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "health")))
                .setTimerModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "timer"))));

        skillManager.registerUpgradeParser(Knockback.class, json -> new KnockbackUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance"))));

        skillManager.registerUpgradeParser(Life.class, json -> new LifeUpgrade()
                .setLifeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "health"))));

        skillManager.registerUpgradeParser(Lightning.class, json -> new LightningUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setDamageModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "damage"))));

        skillManager.registerUpgradeParser(Pickup.class, json -> new PickupUpgrade()
                .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
                .setPickupExpModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "exp"))));

        skillManager.registerUpgradeParser(Poison.class, json -> new PoisonUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration"))));

        skillManager.registerUpgradeParser(Ranged.class, json -> new RangedUpgrade()
                .setDamageModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "damage")))
                .setRateOfFireModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "rate")))
                .setProjectileModifier(UpgradeParsers.parseEnum(UpgradeParsers.get(json, "projectile"), Ranged.Projectile.class)));

        skillManager.registerUpgradeParser(Ride.class, json -> new RideUpgrade()
                .setActiveModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "active")))
                .setSpeedIncreaseModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "speed")))
                .setJumpHeightModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "jumpheight")))
                .setFlyLimitModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "flylimit")))
                .setFlyRegenRateModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "flyregenrate")))
                .setCanFlyModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "canfly"))));

        skillManager.registerUpgradeParser(Shield.class, json -> new ShieldUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setRedirectedDamageModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "redirect"))));

        skillManager.registerUpgradeParser(Slow.class, json -> new SlowUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration"))));

        skillManager.registerUpgradeParser(Sprint.class, json -> new SprintUpgrade()
                .setActiveModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "active"))));

        skillManager.registerUpgradeParser(Stomp.class, json -> new StompUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setDamageModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "damage"))));

        skillManager.registerUpgradeParser(Thorns.class, json -> new ThornsUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setReflectedDamageModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "reflection"))));

        skillManager.registerUpgradeParser(Wither.class, json -> new WitherUpgrade()
                .setChanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "chance")))
                .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration"))));
    }
}
