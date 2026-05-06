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

package de.Keyle.MyPet.services;

import de.Keyle.MyPet.api.entity.leashing.LeashFlagManager;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeManager;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import de.Keyle.MyPet.migration.MigrationService;
import de.Keyle.MyPet.util.shop.ShopManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers MyPet's bundled service classes with the {@link ServiceManager}.
 *
 * <p>A {@link ServiceContainer} is a long-lived component (skill manager, skilltree manager,
 * shop manager, migration runner, etc.) that is later activated in lifecycle phases via
 * {@link de.Keyle.MyPet.api.util.service.Load.State}. Registration here only adds the class
 * to the manager's catalog; activation happens later in plugin enable.</p>
 *
 * <p>{@link de.Keyle.MyPet.services.EggIconService} is intentionally registered
 * separately from this list because it is owned by {@code api} and is registered inline by
 * {@code MyPetPlugin.onLoad} alongside the {@link Load.State#OnLoad} activation.</p>
 *
 * <p>Invoked once during plugin load.</p>
 */
public final class BuiltInServices {

    private static final List<Class<? extends ServiceContainer>> SERVICES = List.of(
            LeashFlagManager.class,
            ExperienceCache.class,
            ExperienceCalculatorManager.class,
            SkillManager.class,
            SkilltreeManager.class,
            ShopManager.class,
            DefaultCreakingService.class,
            MigrationService.class
    );

    private BuiltInServices() {
    }

    /**
     * Registers every built-in service class with the supplied {@link ServiceManager}.
     * Activation is deferred and is the caller's responsibility — see
     * {@link ServiceManager#activate(Load.State)}.
     *
     * @param serviceManager the manager to populate; not retained by this class
     */
    public static void register(@NotNull ServiceManager serviceManager) {
        for (Class<? extends ServiceContainer> service : SERVICES) {
            serviceManager.registerService(service);
        }
    }
}
