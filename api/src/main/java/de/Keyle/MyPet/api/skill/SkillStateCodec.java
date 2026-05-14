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

package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.api.skill.skilltree.Skill;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.Optional;

/**
 * Bundles the write and read halves of a skill's persisted state into one
 * object. Replaces the older pattern of implementing {@code NBTStorage} on the
 * live skill (write) and registering a separate {@link SkillStateParser}
 * (read): the two halves agreed on NBT keys only by convention, so a typo in
 * one silently broke persistence with no compiler check.
 *
 * <p>With a codec, both directions share one set of key/type literals. The
 * codec is registered once per skill via
 * {@link SkillManager#registerCodec(Class, Class, SkillStateCodec)}; MyPet
 * then drives all NBT round-trips through it:
 *
 * <ul>
 *   <li><b>Persist</b> — MyPet calls {@link Skill#getState()}, narrows the
 *       result to the codec's state type, and calls {@link #write(SkillState)}
 *       to produce the per-skill compound stored under {@code skillInfo}.</li>
 *   <li><b>Activate</b> — MyPet calls {@link #read(CompoundBinaryTag)} and
 *       hands the resulting state to {@link Skill#applyState(SkillState)} so
 *       the live skill can rebuild its mutable fields.</li>
 *   <li><b>Query a stored pet</b> — {@code StoredPet#skillState} consults the
 *       codec instead of a {@link SkillStateParser}.</li>
 * </ul>
 *
 * <p>{@link #read(CompoundBinaryTag)} should return {@link Optional#empty()}
 * when the compound is empty or missing required fields rather than throwing
 * — callers treat both as "no persisted state for this skill".
 */
public interface SkillStateCodec<T extends SkillState> {
    CompoundBinaryTag write(T state);

    Optional<T> read(CompoundBinaryTag compound);
}
