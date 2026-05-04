package de.Keyle.MyPet.api.skill;

/**
 * Marker interface for the typed, immutable state record exposed by a
 * {@link de.Keyle.MyPet.api.skill.skilltree.Skill}. Each stateful skill defines
 * its own nested {@code record State(...) implements SkillState} and
 * registers a {@link SkillStateParser} via
 * {@link SkillManager#registerStateParser} so persisted pets can hand callers
 * a typed snapshot without parsing raw NBT.
 *
 * <p>Replaces the pre-4.0.0 {@code StoredMyPet.getSkillInfo()} escape hatch:
 * callers ask for {@code MyPet.State.class} and get a
 * shape the skill author owns — not a vendor-shaped {@code CompoundBinaryTag}.
 */
public interface SkillState {
}
