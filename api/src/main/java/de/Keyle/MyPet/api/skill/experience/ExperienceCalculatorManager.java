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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@ServiceName("ExperienceCalculatorManager")
@Load(Load.State.OnEnable)
public class ExperienceCalculatorManager implements ServiceContainer {

    protected Map<String, Class<? extends ExperienceCalculator>> calculators = new HashMap<>();
    @Getter() protected ExperienceCalculator defaultCalculator = new DefaultExperienceCalculator();
    @Getter() protected ExperienceCalculator calculator = defaultCalculator;
    protected ExperienceCache cache;

    @Override
    public boolean onEnable() {
        cache = MyPetApi.getServiceManager().getService(ExperienceCache.class).get();
        return true;
    }

    public void switchCalculator(@NonNull String calculator) {
        if (calculator == null) {
            calculator = "Default";
        }
        calculator = calculator.toLowerCase();
        if (!this.calculator.getIdentifier().toLowerCase().equals(calculator)) {
            if (calculators.containsKey(calculator)) {
                Class<? extends ExperienceCalculator> calculatorClass = calculators.get(calculator);
                try {
                    ExperienceCalculator newCalculator = calculatorClass.newInstance();
                    if (newCalculator.isUsable()) {
                        this.calculator = newCalculator;
                    }
                } catch (Throwable e) {
                    MyPetApi.getLogger().warning("There was an error loading the experience calculator. Please check your setup.");
                    MyPetApi.getLogger().warning("  " + e.getMessage());
                    this.calculator = defaultCalculator;
                }
            } else {
                this.calculator = defaultCalculator;
            }
            MyPetApi.getLogger().info("Exp calculation mode: " + this.calculator.getIdentifier());
        }

        cache.checkVersion(this.calculator);
    }

    @Override
    public void onDisable() {
        calculators.clear();
        calculator = null;
        cache = null;
    }

    public void registerCalculator(@NonNull String id, @NonNull Class<? extends ExperienceCalculator> calculatorClass) {
        this.calculators.put(id.toLowerCase(), calculatorClass);
    }
}
