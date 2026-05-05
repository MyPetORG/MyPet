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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.api.entity.MyPet;

/**
 * Strategy interface for computing the cumulative experience required at each pet level.
 *
 * <p>Implementations define the XP curve that determines how quickly a pet progresses
 * through levels. The plugin ships with {@link DefaultExperienceCalculator} as a built-in
 * implementation; server administrators can register custom calculators (for example,
 * Rhino-based JavaScript formulae) through the {@link ExperienceCalculatorManager}.
 *
 * <p>Each calculator is identified by a unique string ({@link #getIdentifier()}) and a
 * numeric version ({@link #getVersion()}). Together these allow the {@link ExperienceCache}
 * to detect when the formula has changed and invalidate stale cached values.
 */
public interface ExperienceCalculator {

    /**
     * Computes the total cumulative experience required to reach the specified level.
     *
     * @param myPet the pet whose level curve is being calculated (allows per-type curves)
     * @param level the target level (1-based; level 1 typically requires 0 experience)
     * @return the total experience required to reach {@code level}
     */
    double getExpByLevel(MyPet myPet, int level);

    /**
     * Returns a version number representing the current revision of this calculator's formula.
     *
     * <p>If the formula changes (for example, an admin edits the JavaScript source), the
     * version must increment so the {@link ExperienceCache} can detect staleness and
     * recalculate.
     */
    long getVersion();

    /**
     * Indicates whether this calculator is ready to produce results.
     *
     * <p>A calculator might be unusable if, for example, its backing script failed to compile
     * or a required dependency is missing.
     *
     * @return {@code true} if {@link #getExpByLevel} can be called safely
     */
    boolean isUsable();

    /**
     * Returns a unique identifier for this calculator (e.g. {@code "MyPet"}, {@code "JavaScript"}).
     *
     * <p>Used alongside {@link #getVersion()} to key the experience cache.
     */
    String getIdentifier();
}
