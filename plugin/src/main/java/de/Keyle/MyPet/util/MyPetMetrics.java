package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.repository.MyPetManager;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import org.bstats.bukkit.Metrics;
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
     * @param myPetManager   used by the active-pets and pet-types charts to enumerate live pets
     * @param errorReporter  destination for any uncaught initialization error
     */
    public static void register(@NotNull JavaPlugin plugin,
                                @NotNull MyPetManager myPetManager,
                                @NotNull SentryErrorReporter errorReporter) {
        try {
            Metrics metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
            if (!metrics.isEnabled() || VersionUtil.isLocalBuild()) {
                return;
            }
            metrics.addCustomChart(new Metrics.SingleLineChart("active_pets", myPetManager::countActiveMyPets));
            metrics.addCustomChart(new Metrics.SimplePie("build", VersionUtil::getBuild));
            metrics.addCustomChart(new Metrics.SimplePie("update_mode", MyPetMetrics::updateMode));
            metrics.addCustomChart(new Metrics.AdvancedPie("hooks", MyPetMetrics::activatedHooks));
            metrics.addCustomChart(new Metrics.AdvancedPie("pet_types", () -> petTypes(myPetManager)));
            metrics.addCustomChart(new Metrics.SimplePie("database_type", MyPetMetrics::databaseType));
            metrics.addCustomChart(new Metrics.AdvancedPie("active_skills", () -> activeSkills(myPetManager)));
        } catch (Throwable e) {
            errorReporter.sendError(e, "Init Metrics failed");
        }
    }

    private static String updateMode() {
        if (!Configuration.Update.CHECK) {
            return "Disabled";
        }
        return Configuration.Update.DOWNLOAD ? "Check & Download" : "Check";
    }

    private static Map<String, Integer> activatedHooks() {
        Map<String, Integer> hooks = new HashMap<>();
        for (PluginHook hook : MyPetApi.getPluginHookManager().getHooks()) {
            hooks.put(hook.getPluginName(), 1);
        }
        return hooks;
    }

    private static Map<String, Integer> petTypes(MyPetManager myPetManager) {
        Map<String, Integer> types = new HashMap<>();
        for (MyPet pet : myPetManager.getAllActiveMyPets()) {
            types.merge(pet.getPetType().name(), 1, Integer::sum);
        }
        return types;
    }

    private static String databaseType() {
        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("SQLite")) {
            return "SQLite";
        }
        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            return "MySQL";
        }
        return null;
    }

    private static Map<String, Integer> activeSkills(MyPetManager myPetManager) {
        Map<String, Integer> counts = new HashMap<>();
        for (MyPet pet : myPetManager.getAllActiveMyPets()) {
            for (Skill skill : pet.getSkills().all()) {
                if (skill.isActive()) {
                    counts.merge(skill.getName(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }
}
