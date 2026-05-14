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

package de.Keyle.MyPet.api.skill.skilltree;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;

import java.util.Optional;

/**
 * Base interface for all pet skills in the MyPet skill system.
 *
 * <p>Each skill has a name (derived from the {@link SkillName} annotation on its implementation
 * class), belongs to a specific pet, and can be activated/deactivated as the pet gains levels
 * and receives upgrades from its {@link Skilltree}.
 *
 * <p>Skills report their current status through {@link #toPrettyComponent(String)} (for
 * display to the player) and may optionally expose internal state via {@link #getState()}.
 *
 * @see de.Keyle.MyPet.api.skill.Skills
 * @see SkillName
 */
public interface Skill {

    /**
     * Returns the internal name of this skill, as declared in its {@link SkillName} annotation.
     *
     * @return the skill name, or {@code null} if the annotation is missing
     */
    default String getName() {
        SkillName sn = Util.getClassAnnotation(this.getClass(), SkillName.class);
        if (sn != null) {
            return sn.value();
        }
        return null;
    }

    /**
     * Returns the localized display name of this skill for the given locale.
     *
     * <p>If the skill's {@link SkillName} annotation specifies a {@code translationNode},
     * the localized string for that node is returned. Falls back to the annotation's
     * {@code value()} if translation is unavailable or no node is defined.
     *
     * @param locale the locale code to translate into
     * @return the display name, or {@code null} if the annotation is missing
     */
    default String getName(String locale) {
        SkillName sn = Util.getClassAnnotation(this.getClass(), SkillName.class);
        if (sn != null) {
            if (sn.translationNode().equalsIgnoreCase("")) {
                return sn.value();
            } else {
                String translatedName = Locale.getString(sn.translationNode(), locale);
                if (translatedName.equals(sn.translationNode())) {
                    return sn.value();
                } else {
                    return translatedName;
                }
            }
        }
        return null;
    }

    /** Returns the pet that owns this skill instance. */
    Pet getPet();

    /** Returns {@code true} if this skill has been activated (i.e., at least one upgrade has been applied). */
    boolean isActive();

    /** Resets this skill to its initial inactive state, undoing all upgrades. */
    void reset();

    /**
     * Builds a human-readable component summarizing this skill's current state for display
     * in the pet's skill info panel.
     *
     * @param locale the player's locale code for translation
     * @return a formatted text component
     */
    Component toPrettyComponent(String locale);

    /**
     * Returns the message components to display to the player when this skill receives an upgrade.
     *
     * @return an array of text components (one per line), or an empty array if silent
     */
    Component[] getUpgradeMessage();

    /**
     * Returns this skill's current runtime state as a typed {@link SkillState}
     * record, or {@link Optional#empty()} if the skill has no persisted
     * state worth exposing (the default — most skills' behavior is fully
     * driven by skilltree upgrade levels).
     *
     * <p>Implementations should construct a fresh, immutable record each
     * call from their current fields. Used by {@code StoredPet#skillState}
     * on the live-pet branch (the persisted-pet branch consults the
     * registered {@link de.Keyle.MyPet.api.skill.SkillStateCodec} instead).
     * A skill that registers a codec must also implement this method —
     * MyPet calls {@code getState()} to obtain the state the codec
     * serializes to NBT.
     */
    default Optional<? extends SkillState> getState() {
        return Optional.empty();
    }

    /**
     * Absorbs a previously persisted {@link SkillState} back into the live
     * skill's mutable fields. Called by MyPet on activation (and on pet-type
     * change) when a {@link de.Keyle.MyPet.api.skill.SkillStateCodec} is
     * registered for this skill — the codec produces the state from NBT, this
     * method writes it onto the skill instance.
     *
     * <p>Codec-using skills should narrow {@code state} to their registered
     * state type and apply it. The default is a no-op so skills that don't
     * persist mutable runtime state can ignore it entirely.
     */
    default void applyState(SkillState state) {
    }
}