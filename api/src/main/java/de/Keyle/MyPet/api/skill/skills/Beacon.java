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
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import lombok.Getter;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill that allows the pet to act as a mobile beacon, periodically applying configurable
 * potion-effect buffs to nearby players. The owner can select which buffs are active,
 * choose the receiver scope (owner only, party, or everyone), and toggle the beacon on/off.
 *
 * <p>The number of simultaneously active buffs, effect duration, and range all scale with
 * the pet's skilltree level via {@link UpgradeComputer} values. The skill ticks via
 * {@link Scheduler} to reapply effects and persists selected buffs via {@link NBTStorage}.
 *
 * @see ActiveSkill#activate()
 * @see Buff
 * @see BuffReceiver
 */
@SkillName(value = "Beacon", translationNode = "Name.Skill.Beacon")
public interface Beacon extends Skill, Scheduler, NBTStorage, ActiveSkill {

    /** Returns the upgrade computer controlling the potion-effect duration in ticks. */
    UpgradeComputer<Integer> getDuration();

    /** Returns the upgrade computer controlling the maximum number of buffs the owner can select. */
    UpgradeComputer<Integer> getNumberOfBuffs();

    /** Returns the upgrade computer controlling the beacon's effect radius in blocks. */
    UpgradeComputer<Number> getRange();

    /**
     * Returns the upgrade computer for a specific buff's amplifier level.
     *
     * @param <T>  the value type of the buff's upgrade computer
     * @param buff the buff to query
     * @return the upgrade computer for the given buff
     */
    <T> UpgradeComputer<T> getBuff(Buff buff);

    /**
     * Enumerates the potion-effect buffs that the Beacon skill can apply. Each entry
     * maps a human-readable name and a GUI slot position to the corresponding
     * {@link PotionEffectType}.
     */
    enum Buff {
        Speed("Speed", 0, PotionEffectType.SPEED),
        Haste("Haste", 9, PotionEffectType.HASTE),
        Strength("Strength", 18, PotionEffectType.STRENGTH),
        JumpBoost("JumpBoost", 1, PotionEffectType.JUMP_BOOST),
        Regeneration("Regeneration", 10, PotionEffectType.REGENERATION),
        Resistance("Resistance", 19, PotionEffectType.RESISTANCE),
        FireResistance("FireResistance", 7, PotionEffectType.FIRE_RESISTANCE),
        WaterBreathing("WaterBreathing", 16, PotionEffectType.WATER_BREATHING),
        Invisibility("Invisibility", 25, PotionEffectType.INVISIBILITY),
        NightVision("NightVision", 8, PotionEffectType.NIGHT_VISION),
        Absorption("Absorption", 26, PotionEffectType.ABSORPTION),
        Luck("Luck", 17, PotionEffectType.LUCK),
        HealthBoost("HealthBoost", -1, PotionEffectType.HEALTH_BOOST);

        private static final Map<Integer, Buff> buffPositions = new HashMap<>();
        @Getter
        private final String name;
        @Getter
        private final int position;
        @Getter
        private final PotionEffectType potionEffectType;

        Buff(String name, int position, PotionEffectType potionEffectType) {
            this.name = name;
            this.position = position;
            this.potionEffectType = potionEffectType;
        }

        /**
         * Returns the buff occupying the given GUI slot position, or {@code null} if
         * no buff is assigned to that position.
         *
         * @param position the GUI slot index
         * @return the matching {@code Buff}, or {@code null}
         */
        public static Buff getBuffAtPosition(int position) {
            if (buffPositions.isEmpty()) {
                for (Buff buff : values()) {
                    buffPositions.put(buff.position, buff);
                }
            }
            return buffPositions.get(position);
        }

        /**
         * Looks up a buff by its canonical name (case-sensitive).
         *
         * @param name the buff name (e.g. {@code "Speed"}, {@code "Regeneration"})
         * @return the matching {@code Buff}, or {@code null} if not found
         */
        public static Buff getByName(String name) {
            for (Buff buff : values()) {
                if (buff.name.equals(name)) {
                    return buff;
                }
            }
            return null;
        }
    }

    /**
     * Determines which players receive the beacon's potion-effect buffs.
     */
    enum BuffReceiver {
        /** Only the pet's owner receives buffs. */
        Owner,
        /** All members of the owner's party receive buffs. */
        Party,
        /** All players within range receive buffs. */
        Everyone
    }

    /**
     * Snapshot of a Beacon skill's persisted or live state — the player-chosen buffs, the toggle, and the receiver scope.
     */
    record State(List<Buff> buffs, boolean active, BuffReceiver receiver) implements SkillState {}
}