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

import java.util.HashMap;
import java.util.Map;

@SkillName(value = "Beacon", translationNode = "Name.Skill.Beacon")
public interface Beacon extends Skill, Scheduler, NBTStorage, ActiveSkill {

    UpgradeComputer<Integer> getDuration();

    UpgradeComputer<Integer> getNumberOfBuffs();

    UpgradeComputer<Number> getRange();

    UpgradeComputer getBuff(Buff buff);

    enum Buff {
        Speed("Speed", 1, 0),
        Haste("Haste", 3, 9),
        Strength("Strength", 5, 18),
        JumpBoost("JumpBoost", 8, 1),
        Regeneration("Regeneration", 10, 10),
        Resistance("Resistance", 11, 19),
        FireResistance("FireResistance", 12, 7, false),
        WaterBreathing("WaterBreathing", 13, 16, false),
        Invisibility("Invisibility", 14, 25, false),
        NightVision("NightVision", 16, 8, false),
        Absorption("Absorption", 22, 26),
        Luck("Luck", 26, 17, false),
        HealthBoost("HealthBoost", -1, -1);

        private final String name;
        private final int id;
        private final int position;
        private final boolean moreThanOneLevel;
        protected static Map<Integer, Buff> buffPositions = new HashMap<>();
        protected static Map<Integer, Buff> buffIds = new HashMap<>();

        Buff(String name, int id, int position) {
            this.name = name;
            this.id = id;
            this.position = position;
            this.moreThanOneLevel = true;
        }

        Buff(String name, int id, int position, boolean moreThanOneLevel) {
            this.name = name;
            this.id = id;
            this.position = position;
            this.moreThanOneLevel = moreThanOneLevel;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public int getPosition() {
            return position;
        }

        public boolean hasMoreThanOneLevel() {
            return moreThanOneLevel;
        }

        public static Buff getBuffAtPosition(int positiion) {
            if (buffPositions.size() == 0) {
                for (Buff buff : values()) {
                    buffPositions.put(buff.position, buff);
                }
            }
            return buffPositions.get(positiion);
        }

        public static Buff getBuffByID(int id) {
            if (buffIds.size() == 0) {
                for (Buff buff : values()) {
                    buffIds.put(buff.id, buff);
                }
            }
            return buffIds.get(id);
        }
    }

    enum BuffReceiver {
        Owner, Party, Everyone
    }
}