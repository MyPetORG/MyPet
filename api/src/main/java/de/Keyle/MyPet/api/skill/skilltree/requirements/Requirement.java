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

package de.Keyle.MyPet.api.skill.skilltree.requirements;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;

/**
 * A condition that must be satisfied before a pet can use a particular {@link Skilltree}.
 *
 * <p>Requirements are registered with the {@link de.Keyle.MyPet.api.skill.skilltree.SkilltreeManager}
 * and referenced by name in {@code .st.json} skilltree files. At runtime, when a player
 * attempts to select or is assigned a skilltree, each configured requirement is evaluated
 * via {@link #check(Skilltree, MyPet, Settings)}.
 *
 * <p>Implementations must be annotated with {@link RequirementName} so they can be discovered
 * and looked up by name.
 *
 * <p>Common implementations include permission checks, economy balance checks, and
 * pet-level thresholds.
 */
public interface Requirement {

    /**
     * Evaluates whether the given pet meets this requirement for the specified skilltree.
     *
     * @param skilltree the skilltree the pet wants to use
     * @param pet       the pet being evaluated
     * @param settings  the requirement-specific configuration from the skilltree definition
     * @return {@code true} if the requirement is satisfied
     */
    boolean check(Skilltree skilltree, MyPet pet, Settings settings);
}
