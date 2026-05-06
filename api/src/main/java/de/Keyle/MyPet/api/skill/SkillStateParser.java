package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.api.entity.PersistedPet;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.Optional;

/**
 * Parses the persisted NBT compound for one skill into its typed
 * {@link SkillState} record. Registered with
 * {@link SkillManager#registerStateParser} once per skill at plugin enable;
 * looked up by {@code de.Keyle.MyPet.api.entity.StoredPet#skillState} when an
 * addon asks a {@link PersistedPet} for typed
 * state.
 *
 * <p>{@code compound} is the per-skill compound (the value stored under the
 * skill's name in the aggregate {@code skillInfo}), not the aggregate.
 * Implementations should return {@link Optional#empty()} when {@code compound}
 * is empty or missing required fields rather than throwing — the caller
 * treats both as "no persisted state for this skill".
 */
@FunctionalInterface
public interface SkillStateParser<T extends SkillState> {
    Optional<T> parse(CompoundBinaryTag compound);
}
