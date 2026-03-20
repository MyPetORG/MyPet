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

package de.Keyle.MyPet.api.skill.skills;

import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

@SkillName(value = "Beacon", translationNode = "Name.Skill.Beacon")
public interface Beacon extends Skill, Scheduler, NBTStorage, ActiveSkill {

    UpgradeComputer<Integer> getDuration();

    UpgradeComputer<Integer> getNumberOfBuffs();

    UpgradeComputer<Number> getRange();

    <T> UpgradeComputer<T> getBuff(Buff buff);

    enum Buff {
        Speed("Speed", 0, PotionEffectType.SPEED),
        Haste("Haste", 9, PotionEffectType.HASTE),
        Strength("Strength", 18, PotionEffectType.STRENGTH),
        JumpBoost("JumpBoost", 1, PotionEffectType.JUMP_BOOST),
        Regeneration("Regeneration", 10, PotionEffectType.REGENERATION),
        Resistance("Resistance", 19, PotionEffectType.RESISTANCE),
        FireResistance("FireResistance", 7, PotionEffectType.FIRE_RESISTANCE, false),
        WaterBreathing("WaterBreathing", 16, PotionEffectType.WATER_BREATHING, false),
        Invisibility("Invisibility", 25, PotionEffectType.INVISIBILITY, false),
        NightVision("NightVision", 8, PotionEffectType.NIGHT_VISION, false),
        Absorption("Absorption", 26, PotionEffectType.ABSORPTION),
        Luck("Luck", 17, PotionEffectType.LUCK, false),
        HealthBoost("HealthBoost", -1, PotionEffectType.HEALTH_BOOST);

        private static final Map<Integer, Buff> buffPositions = new HashMap<>();
        private final String name;
        private final int position;
        private final PotionEffectType potionEffectType;
        private final boolean moreThanOneLevel;

        Buff(String name, int position, PotionEffectType potionEffectType) {
            this.name = name;
            this.position = position;
            this.potionEffectType = potionEffectType;
            this.moreThanOneLevel = true;
        }

        Buff(String name, int position, PotionEffectType potionEffectType, boolean moreThanOneLevel) {
            this.name = name;
            this.position = position;
            this.potionEffectType = potionEffectType;
            this.moreThanOneLevel = moreThanOneLevel;
        }

        public static Buff getBuffAtPosition(int positiion) {
            if (buffPositions.isEmpty()) {
                for (Buff buff : values()) {
                    buffPositions.put(buff.position, buff);
                }
            }
            return buffPositions.get(positiion);
        }

        public static Buff getByName(String name) {
            for (Buff buff : values()) {
                if (buff.name.equals(name)) {
                    return buff;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public int getPosition() {
            return position;
        }

        public PotionEffectType getPotionEffectType() {
            return potionEffectType;
        }

        public boolean hasMoreThanOneLevel() {
            return moreThanOneLevel;
        }
    }

    enum BuffReceiver {
        Owner, Party, Everyone
    }
}