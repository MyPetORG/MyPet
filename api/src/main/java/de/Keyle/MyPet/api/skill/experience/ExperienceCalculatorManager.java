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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for managing available {@link ExperienceCalculator} implementations
 * and switching between them at runtime.
 *
 * <p>Calculator classes are registered by identifier (case-insensitive), and the active
 * calculator can be switched via {@link #switchCalculator(String)}. When a switch occurs
 * the {@link ExperienceCache} is checked for version compatibility, triggering a cache
 * invalidation if needed.
 *
 * <p>If the requested calculator cannot be instantiated or is not usable, the manager
 * falls back to the {@link DefaultExperienceCalculator}.
 *
 * <p>Loaded during {@link Load.State#OnEnable} because it depends on the
 * {@link ExperienceCache} service which loads earlier.
 */
@ServiceName("ExperienceCalculatorManager")
@Load(Load.State.OnEnable)
public class ExperienceCalculatorManager implements ServiceContainer {

    protected final Map<String, Class<? extends ExperienceCalculator>> calculators = new HashMap<>();
    @Getter()
    protected ExperienceCalculator defaultCalculator = new DefaultExperienceCalculator();
    @Getter()
    protected ExperienceCalculator calculator = defaultCalculator;
    protected ExperienceCache cache;

    @Override
    public boolean onEnable() {
        // Register the built-in default curve under its identifier and the "Default" config alias
        // so it lives in the same map as every other calculator (discoverable, no special-casing).
        registerCalculator(defaultCalculator.getIdentifier(), DefaultExperienceCalculator.class);
        registerCalculator("Default", DefaultExperienceCalculator.class);
        return MyPetApi.getServiceManager().getService(ExperienceCache.class)
                .map(experienceCache -> {
                    cache = experienceCache;
                    return true;
                })
                .orElse(false);
    }

    /**
     * Switches the active experience calculator to the one registered under the given identifier.
     *
     * <p>If the identifier does not match the current calculator, the manager attempts to
     * instantiate the registered class. On failure (instantiation error or
     * {@link ExperienceCalculator#isUsable()} returning {@code false}), the default calculator
     * is activated instead. After switching, the {@link ExperienceCache} is notified so it can
     * invalidate stale entries if necessary.
     *
     * @param calculator the case-insensitive identifier of the desired calculator
     */
    public void switchCalculator(@NonNull String calculator) {
        calculator = calculator.toLowerCase();
        if (!this.calculator.getIdentifier().toLowerCase().equals(calculator)) {
            if (calculators.containsKey(calculator)) {
                Class<? extends ExperienceCalculator> calculatorClass = calculators.get(calculator);
                try {
                    ExperienceCalculator newCalculator = calculatorClass.getDeclaredConstructor().newInstance();
                    if (newCalculator.isUsable()) {
                        this.calculator = newCalculator;
                    }
                } catch (Throwable e) {
                    MyPetApi.getLogger().warning("There was an error loading the experience calculator. Please check your setup.");
                    MyPetApi.getLogger().warning("  " + e.getMessage());
                    this.calculator = defaultCalculator;
                }
            } else {
                MyPetApi.getLogger().warning("Unknown experience CalculationMode '" + calculator + "'; using the default (MyPet) curve.");
                this.calculator = defaultCalculator;
            }
        }

        cache.checkVersion(this.calculator);
    }

    @Override
    public void onDisable() {
        calculators.clear();
        calculator = null;
        cache = null;
    }

    /**
     * Registers an experience calculator class under the given identifier.
     *
     * <p>The identifier is stored in lower-case for case-insensitive lookups. Registering
     * with an existing identifier silently replaces the previous registration.
     *
     * @param id              the unique identifier for the calculator (case-insensitive)
     * @param calculatorClass the class to instantiate when this calculator is activated
     */
    public void registerCalculator(@NonNull String id, @NonNull Class<? extends ExperienceCalculator> calculatorClass) {
        this.calculators.put(id.toLowerCase(), calculatorClass);
    }
}
