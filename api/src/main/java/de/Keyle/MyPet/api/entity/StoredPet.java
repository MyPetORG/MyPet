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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only view of a pet at rest. Sealed: the only permitted implementations are
 *
 * <ul>
 *   <li>{@link PersistedPet} — the immutable record that the repository
 *       loads from disk and that {@code PetManager} round-trips between
 *       active and inactive states. "Mutations" return new instances via
 *       {@code withX} or {@code Builder}.
 *   <li>{@link MyPet} — the active runtime pet. Extends this interface
 *       {@code non-sealed} so that plugin-side concretes can extend it without
 *       being named in the seal.
 * </ul>
 */
public sealed interface StoredPet permits PersistedPet, MyPet {

    /** Globally unique identifier for this pet instance (persisted). */
    UUID getUUID();

    /** The player who owns this pet. */
    MyPetPlayer getOwner();

    /** The mob type this pet represents (e.g., Wolf, Creeper). */
    PetType getPetType();

    /** Raw pet name in MiniMessage format. */
    String getPetName();

    /**
     * MiniMessage-rendered pet name. Default implementation deserializes
     * {@link #getPetName} via {@link Util#SANITIZED_MINIMESSAGE}; both
     * implementations were previously identical.
     */
    default Component getDisplayName() {
        return Util.SANITIZED_MINIMESSAGE.deserialize(getPetName());
    }

    /** The world-group this pet belongs to (multi-world isolation). */
    String getWorldGroup();

    /** Raw accumulated experience points. */
    double getExp();

    /**
     * Pet's experience level — derived from {@link #getExp()} and the
     * skilltree's XP curve via {@link ExperienceCache}. Returns {@code 0}
     * if the cache has no curve loaded for this pet's world group / type
     * (e.g., before skilltrees have finished loading, or for a pet whose
     * world group has no configured curve).
     */
    default int getLevel() {
        return MyPetApi.getServiceManager().getService(ExperienceCache.class)
                .map(cache -> cache.getLevel(getWorldGroup(), getPetType(), getExp()))
                .orElse(0);
    }

    /** Current health (half-hearts). Zero when dead. */
    double getHealth();

    /** Hunger level (1–100). Drains over time; restored by feeding. */
    double getSaturation();

    /** Seconds remaining on the respawn timer. Zero when alive. */
    int getRespawnTime();

    /** If {@code true}, the pet will auto-spawn on the owner's next login. */
    boolean wantsToRespawn();

    /** Epoch millis of the last time this pet was active in the world. */
    long getLastUsed();

    /** The assigned skilltree, or {@code null} if none is set. */
    Skilltree getSkilltree();

    /**
     * Typed access to a skill's persisted or live state. Replaces the
     * pre-4.0.0 raw {@code getSkillInfo()} blob
     *
     * <p>Dispatches over the sealed permits:
     * <ul>
     *   <li>{@link PersistedPet}: looks up the {@code SkillStateParser}
     *       registered for {@code skillClass} and parses the per-skill
     *       compound from {@code skillInfo}.</li>
     *   <li>{@link MyPet} (live): asks the live {@link Skill} instance for
     *       its current state via {@link Skill#getState()}.</li>
     * </ul>
     *
     * <p>Returns {@link Optional#empty()} if the skill isn't registered
     * with the pet, the skill has no state to expose, or (persisted only)
     * no parser is registered for {@code skillClass}.
     *
     * @param skillClass the skill api type, e.g. {@code Backpack.class}
     * @param stateClass the skill's nested {@code State} class — must
     *                   match what the skill's parser / {@code getState}
     *                   produces, otherwise returns empty
     */
    default <S extends Skill, T extends SkillState> Optional<T> skillState(
            Class<S> skillClass, Class<T> stateClass) {
        return switch (this) {
            case PersistedPet p -> {
                SkillManager mgr = MyPetApi.getSkillManager();
                String skillName = mgr.getSkillName(skillClass);
                if (skillName == null) yield Optional.empty();
                CompoundBinaryTag info = p.skillInfo();
                if (!info.keySet().contains(skillName)) yield Optional.empty();
                yield mgr.parseState(skillClass, stateClass, info.getCompound(skillName));
            }
            case MyPet live -> {
                Skill skill = live.getSkills().get(skillClass);
                if (skill == null) yield Optional.empty();
                yield skill.getState()
                        .filter(stateClass::isInstance)
                        .map(stateClass::cast);
            }
        };
    }
}
