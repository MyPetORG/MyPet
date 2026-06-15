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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.repository.PetManager;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Wires up MyPet's bStats charts so that opt-in servers report aggregate usage telemetry to
 * <a href="https://bstats.org/">bStats</a>.
 *
 * <p>The bStats client honors the server's {@code plugins/bStats/config.yml} opt-out, so this
 * class submits charts only when {@link Metrics#isEnabled()} returns {@code true} and the
 * running plugin is not a local dev build (see {@link VersionUtil#isLocalBuild()}). Any
 * failure during setup is captured and forwarded to Sentry rather than propagated, so
 * telemetry hiccups cannot prevent the plugin from finishing enabling.</p>
 *
 * <p>Submitted charts: active pet count, build identifier, update-checker mode, activated
 * third-party hooks, distribution of pet types, database backend, and distribution of active
 * skills across all live pets.</p>
 *
 * <p>Invoked once during plugin enable, after plugin hooks have been activated so the
 * "hooks" chart reports accurate values.</p>
 */
public final class MyPetMetrics {

    /** bStats project identifier for MyPet (see <a href="https://bstats.org/plugin/bukkit/MyPet/778">bstats.org/plugin/bukkit/MyPet/778</a>). */
    private static final int BSTATS_PLUGIN_ID = 778;

    private MyPetMetrics() {
    }

    /**
     * Initializes bStats and registers all MyPet charts. Returns silently and reports the
     * exception to Sentry on any failure (including bStats network/IO errors at construction).
     *
     * @param plugin         the plugin instance bStats associates the metrics with
     * @param petManager   used by the active-pets and pet-types charts to enumerate live pets
     * @param errorReporter  destination for any uncaught initialization error
     */
    public static void register(@NotNull JavaPlugin plugin,
                                @NotNull PetManager petManager,
                                @NotNull SentryErrorReporter errorReporter) {
        try {
            if (VersionUtil.isLocalBuild()) {
                return;
            }
            // bStats 3.x has no isEnabled(); the Metrics object respects the global bStats
            // opt-out internally, so charts can be registered unconditionally.
            Metrics metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
            metrics.addCustomChart(new SingleLineChart("active_pets", petManager::countActivePets));
            metrics.addCustomChart(new SimplePie("build", VersionUtil::getBuild));
            metrics.addCustomChart(new SimplePie("update_mode", MyPetMetrics::updateMode));
            metrics.addCustomChart(new AdvancedPie("hooks", MyPetMetrics::activatedHooks));
            metrics.addCustomChart(new AdvancedPie("pet_types", () -> petTypes(petManager)));
            metrics.addCustomChart(new SimplePie("database_type", MyPetMetrics::databaseType));
            metrics.addCustomChart(new AdvancedPie("active_skills", () -> activeSkills(petManager)));
        } catch (Throwable e) {
            errorReporter.sendError(e, "Init Metrics failed");
        }
    }

    private static String updateMode() {
        if (!MyPetGlobal.Update.CHECK.get()) {
            return "Disabled";
        }
        return MyPetGlobal.Update.DOWNLOAD.get() ? "Check & Download" : "Check";
    }

    private static Map<String, Integer> activatedHooks() {
        Map<String, Integer> hooks = new HashMap<>();
        for (ServiceContainer hook : MyPetApi.getServiceManager().getServices(ServiceContainer.class).stream()
                .filter(s -> s.getClass().isAnnotationPresent(RequiresPlugin.class))
                .toList()) {
            hooks.put(hook.getServiceName(), 1);
        }
        return hooks;
    }

    private static Map<String, Integer> petTypes(PetManager petManager) {
        Map<String, Integer> types = new HashMap<>();
        for (Pet pet : petManager.getAllActivePets()) {
            types.merge(pet.getPetType().name(), 1, Integer::sum);
        }
        return types;
    }

    private static String databaseType() {
        if (MyPetGlobal.Repository.REPOSITORY_TYPE.get().equalsIgnoreCase("SQLite")) {
            return "SQLite";
        }
        if (MyPetGlobal.Repository.REPOSITORY_TYPE.get().equalsIgnoreCase("MySQL")) {
            return "MySQL";
        }
        return null;
    }

    private static Map<String, Integer> activeSkills(PetManager petManager) {
        Map<String, Integer> counts = new HashMap<>();
        for (Pet pet : petManager.getAllActivePets()) {
            for (Skill skill : pet.getSkills().all()) {
                if (skill.isActive()) {
                    counts.merge(skill.getName(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }
}
