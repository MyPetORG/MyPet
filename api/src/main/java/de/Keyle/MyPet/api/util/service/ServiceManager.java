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

package de.Keyle.MyPet.api.util.service;

import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.configuration.ConfigurationYAML;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link ServiceManager} manages all interactions with other plugins. Services are stored by class and by the
 * interfaces they implement so they can also be retrieved by them. You can get instances of other plugins and check if
 * other plugins are active.
 */
public class ServiceManager {
    // Read by getService()/isServiceActive()/hasServices() — paths that may run off the main
    // thread (async repository callbacks, late hook registration) — so these must be concurrent.
    final Map<Class<? extends ServiceContainer>, ServiceContainer> services = new ConcurrentHashMap<>();
    final Map<String, ServiceContainer> serviceByName = new ConcurrentHashMap<>();

    // Only mutated on the main thread during registerService()/activate() lifecycle; never read
    // from the async path, so a plain multimap is sufficient.
    final ArrayListMultimap<Load.State, ServiceContainer> registeredServices = ArrayListMultimap.create();

    private ConfigurationYAML config;

    /**
     * Returns the hook/service configuration, lazily creating it on first access. Hooks may read
     * config from their constructor (during {@code onLoad}, before {@link #activate} runs), so this
     * must always return a live config rather than the possibly-null backing field.
     */
    public ConfigurationYAML getConfig() {
        if (config == null) {
            File hookConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "hooks-config.yml");
            config = new ConfigurationYAML(hookConfigFile);
            config.getConfig().options().setHeader(java.util.List.of(
                    "#######################################################################",
                    "          This is the hook/service configuration of MyPet           #",
                    "                 You can find more info on the wiki:                  #",
                    "  https://wiki.mypet-plugin.de/setup/configurations/hooks-config.yml  #",
                    "#######################################################################"
            ));
            config.getConfig().options().copyDefaults(true);
        }
        return config;
    }

    public void listServices() {
        MyPetApi.getLogger().info("Loaded services: " + serviceByName.keySet());
    }

    /**
     * register new services here. A service needs the {@link ServiceName} annotation to be accepted.
     *
     * @param serviceClass the service class
     */
    public void registerService(Class<? extends ServiceContainer> serviceClass) {
        RequiresPlugin requires = serviceClass.getAnnotation(RequiresPlugin.class);
        if (requires != null) {
            if (!requires.classPath().isEmpty()) {
                if (!isPluginAvailable(requires.value(), requires.classPath())) {
                    return;
                }
            } else if (!isPluginAvailable(requires.value())) {
                return;
            }
        }

        Load.State loadingState = Load.State.OnEnable;
        if (serviceClass.isAnnotationPresent(Load.class)) {
            loadingState = serviceClass.getAnnotation(Load.class).value();
        }
        try {
            ServiceContainer service = serviceClass.getDeclaredConstructor().newInstance();
            registeredServices.put(loadingState, service);
        } catch (Throwable e) {
            ErrorUtil.report("Error occurred while creating the " + serviceClass.getName() + " service.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerService(ServiceContainer service) {
        boolean genericService = true;
        for (Class<?> iface : Util.getAllInterfaces(service.getClass())) {
            if (iface != ServiceContainer.class && ServiceContainer.class.isAssignableFrom(iface)) {
                services.put((Class<? extends ServiceContainer>) iface, service);
                genericService = false;
            }
        }
        for (Class<?> superclass : Util.getAllSuperclasses(service.getClass())) {
            if (superclass != ServiceContainer.class && ServiceContainer.class.isAssignableFrom(superclass)) {
                services.put((Class<? extends ServiceContainer>) superclass, service);
                genericService = false;
            }
        }
        if (genericService) {
            services.put(ServiceContainer.class, service);
        }
        serviceByName.put(service.getServiceName(), service);
        services.put(service.getClass(), service);
    }

    public void activate(Load.State state) {
        List<ServiceContainer> services = registeredServices.get(state);

        for (ServiceContainer service : services) {
            RequiresPlugin requires = service.getClass().getAnnotation(RequiresPlugin.class);

            if (requires != null) {
                if (!requires.classPath().isEmpty()) {
                    if (!isPluginUsable(requires.value(), requires.classPath())) {
                        continue;
                    }
                } else if (!isPluginUsable(requires.value())) {
                    continue;
                }

                FileConfiguration cfg = getConfig().getConfig();
                if (cfg.contains(requires.value())) {
                    if (!cfg.getBoolean(requires.value() + ".Enabled", true)) {
                        continue;
                    }
                } else {
                    cfg.addDefault(requires.value() + ".Enabled", true);
                }
                ConfigurationSection pluginSection = cfg.getConfigurationSection(requires.value());
                if (pluginSection != null) {
                    service.loadConfig(pluginSection);
                }
            }

            try {
                if (service.onEnable()) {
                    registerService(service);
                    if (requires != null) {
                        String version = getPluginVersion(requires.value());
                        String msg = requires.value() + " (" + version + ")";
                        if (!requires.classPath().isEmpty()) {
                            msg += " (" + requires.classPath() + ")";
                        }
                        msg += service.getActivationMessage();
                        MyPetApi.getLogger().info(msg + " hook activated.");
                    }
                }
            } catch (Throwable e) {
                String label = requires != null ? requires.value() : service.getClass().getSimpleName();
                ErrorUtil.report("Error occurred while enabling " + label + " service.", e);
            }
        }

        if (config != null) {
            config.saveConfig();
        }

        registeredServices.removeAll(state);
    }

    public void disableServices() {
        for (ServiceContainer service : serviceByName.values()) {
            service.onDisable();
        }
    }

    /**
     * returns all services that inherit from a specific class/interface
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return list of instances of the service class
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceContainer> List<T> getServices(Class<? extends T> serviceClass) {
        List<T> list = new ArrayList<>();

        for (ServiceContainer service : services.values()) {
            if (serviceClass.isInstance(service)) {
                T typed = (T) service;
                if (!list.contains(typed)) {
                    list.add(typed);
                }
            }
        }

        return list;
    }

    /**
     * returns if services that inherit from a specific class/interface are available
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return if any service was found
     */
    public boolean hasServices(Class<? extends ServiceContainer> serviceClass) {
        return services.containsKey(serviceClass);
    }

    /**
     * returns the services of a specific class
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return instance of the service class
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceContainer> Optional<T> getService(Class<? extends T> serviceClass) {
        return Optional.ofNullable((T) services.get(serviceClass));
    }

    /**
     * returns the services with a specific {@link ServiceName}
     *
     * @param name name of the plugin
     * @return instance of a service class associated with the plugin name
     */
    public Optional<ServiceContainer> getService(String name) {
        return Optional.ofNullable(serviceByName.get(name));
    }

    /**
     * returns if a services with a specific {@link ServiceName} is available
     *
     * @param name name of the plugin
     * @return if any service was found
     */
    public boolean isServiceActive(String name) {
        return serviceByName.containsKey(name);
    }

    /**
     * returns if a service that inherit from a specific class/interface is available
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return if any service was found
     */
    public boolean isServiceActive(Class<? extends ServiceContainer> serviceClass) {
        return services.containsKey(serviceClass);
    }

    private static String getPluginVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) return "unknown";
        return plugin.getPluginMeta().getVersion();
    }

    public boolean isPluginUsable(String pluginName, String className) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled() && plugin.getClass().getName().equals(className);
    }

    public boolean isPluginAvailable(String pluginName, String className) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.getClass().getName().equals(className);
    }

    public boolean isPluginUsable(String pluginName) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    public boolean isPluginAvailable(String pluginName) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null;
    }
}