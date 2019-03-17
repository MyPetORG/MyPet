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

package de.Keyle.MyPet.api.util.hooks;

import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.configuration.ConfigurationYAML;
import lombok.Getter;
import org.apache.commons.lang.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * The {@link PluginHookManager} manages all interactions with other plugins. Hooks are stored by class and by the
 * interfaces they implement so they can also be retrieved by them. You can get instances of other plugins and check if
 * other plugins are active.
 */
public class PluginHookManager {

    protected ArrayListMultimap<Class<? extends PluginHook>, PluginHook> hooks = ArrayListMultimap.create();
    protected Map<String, PluginHook> hookByName = new HashMap<>();
    protected Map<Class<? extends PluginHook>, PluginHook> hookByClass = new HashMap<>();
    protected Queue<PluginHook> registeredHooks = new ArrayDeque<>();
    @Getter public ConfigurationYAML config;

    public PluginHookManager() {
        File hookConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "hooks-config.yml");
        config = new ConfigurationYAML(hookConfigFile);
        config.getConfig().options().header("" +
                "#######################################################################\n" +
                "               This is the hook configuration of MyPet                #\n" +
                "                 You can find more info on the wiki:                  #\n" +
                "  https://wiki.mypet-plugin.de/setup/configurations/hooks-config.yml  #\n" +
                "#######################################################################\n");
        config.getConfig().options().copyHeader(true);
        config.getConfig().options().copyDefaults(true);
    }

    /**
     * Register new hooks here. A hook needs the {@link PluginHookName} annotation to be accepted.
     * Registered hooks will not enabled after MyPet has loaded. Use enableHook instead.
     *
     * @param hookClass the hook class
     */
    public void registerHook(Class<? extends PluginHook> hookClass) {
        if (hookClass.isAnnotationPresent(PluginHookName.class)) {
            PluginHookName hookNameAnnotation = hookClass.getAnnotation(PluginHookName.class);

            String pluginName = hookNameAnnotation.value();
            if (!hookNameAnnotation.classPath().equalsIgnoreCase("")) {
                if (!isPluginAvailable(pluginName, hookNameAnnotation.classPath())) {
                    return;
                }
            } else {
                if (!isPluginAvailable(pluginName)) {
                    return;
                }
            }
            try {
                PluginHook hook = hookClass.newInstance();
                registeredHooks.add(hook);
            } catch (Throwable e) {
                MyPetApi.getLogger().warning("Error occured while enabling " + pluginName + " (" + Bukkit.getPluginManager().getPlugin(pluginName).getDescription().getVersion() + ") hook.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a hook for MyPet.
     * A hook needs the {@link PluginHookName} annotation to be accepted.
     *
     * @param hook the hook
     * @return true if the hook was enabled successfully
     */
    @SuppressWarnings("unchecked")
    public boolean enableHook(PluginHook hook) {
        try {
            PluginHookName hookNameAnnotation = hook.getClass().getAnnotation(PluginHookName.class);
            if (!hookNameAnnotation.classPath().equalsIgnoreCase("")) {
                if (!isPluginUsable(hook.getPluginName(), hookNameAnnotation.classPath())) {
                    return false;
                }
            } else if (!isPluginUsable(hook.getPluginName())) {
                return false;
            }

            boolean enable = loadHookConfig(hook);
            config.saveConfig();

            if (enable && hook.onEnable()) {
                boolean genericHook = true;
                for (Object o : ClassUtils.getAllInterfaces(hook.getClass())) {
                    if (o != PluginHook.class && PluginHook.class.isAssignableFrom((Class) o)) {
                        hooks.put((Class) o, hook);
                        genericHook = false;
                    }
                }
                if (genericHook) {
                    hooks.put(PluginHook.class, hook);
                }
                hookByName.put(hook.getPluginName(), hook);
                hookByClass.put(hook.getClass(), hook);

                String message = hook.getPluginName();
                message += " (" + Bukkit.getPluginManager().getPlugin(hook.getPluginName()).getDescription().getVersion() + ")";
                if (!hookNameAnnotation.classPath().equalsIgnoreCase("")) {
                    message += " (" + hookNameAnnotation.classPath() + ")";
                }
                message += hook.getActivationMessage();
                MyPetApi.getLogger().info(message + " hook activated.");
                return true;
            }
        } catch (Throwable e) {
            MyPetApi.getLogger().warning("Error occured while enabling " + hook.getPluginName() + " (" + Bukkit.getPluginManager().getPlugin(hook.getPluginName()).getDescription().getVersion() + ") hook.");
            e.printStackTrace();
        }
        return false;
    }

    protected boolean loadHookConfig(PluginHook hook) {
        boolean enable = true;
        FileConfiguration config = this.config.getConfig();
        if (config.contains(hook.getPluginName())) {
            enable = config.getBoolean(hook.getPluginName(), true);
        } else {
            config.addDefault(hook.getPluginName() + ".Enabled", true);
        }
        ConfigurationSection pluginSection = config.getConfigurationSection(hook.getPluginName());
        if (pluginSection != null) {
            hook.loadConfig(pluginSection);
        }

        return enable;
    }

    /**
     * Enables all registered hooks.
     * Not needed after MyPet has loaded.
     */
    public void enableHooks() {
        while (!registeredHooks.isEmpty()) {
            PluginHook hook = registeredHooks.poll();
            enableHook(hook);
        }
    }

    /**
     * Disables all enabled hooks.
     * Only used to disable MyPet.
     */
    public void disableHooks() {
        for (PluginHook hook : hooks.values()) {
            hook.onDisable();
        }
        hooks.clear();
        hookByName.clear();
        hookByClass.clear();
    }

    /**
     * Disables one specific enabled hook.
     *
     * @param hook the hook to be disabled
     */
    public void disableHook(PluginHook hook) {
        hook.onDisable();
        for (Object o : ClassUtils.getAllInterfaces(hook.getClass())) {
            if (o != PluginHook.class && PluginHook.class.isAssignableFrom((Class) o)) {
                hooks.remove(o, hook);
            }
        }
        hooks.removeAll(hook.getClass());
        hookByName.remove(hook.getPluginName());
        hookByClass.remove(hook.getClass());
    }

    /**
     * returns all enabled hooks
     *
     * @return list of instances of the hook class
     */
    @SuppressWarnings("unchecked")
    public <T extends PluginHook> List<T> getHooks() {
        return (List<T>) new ArrayList<>(hookByName.values());
    }

    /**
     * returns all hooks that inherit from a specific class/interface
     *
     * @param hookClass class that implements from {@link PluginHook}
     * @return list of instances of the hook class
     */
    @SuppressWarnings("unchecked")
    public <T extends PluginHook> List<T> getHooks(Class<? extends T> hookClass) {
        return (List<T>) hooks.get(hookClass);
    }

    /**
     * returns if hooks that inherit from a specific class/interface are available
     *
     * @param hookClass class that implements from {@link PluginHook}
     * @return if any hook was found
     */
    public boolean hasHooks(Class<? extends PluginHook> hookClass) {
        return hooks.containsKey(hookClass);
    }

    /**
     * returns the hooks of a specific class
     *
     * @param hookClass class that implements from {@link PluginHook}
     * @return instance of the hook class
     */
    @SuppressWarnings("unchecked")
    public <T extends PluginHook> T getHook(Class<? extends T> hookClass) {
        return (T) hookByClass.get(hookClass);
    }

    /**
     * returns the hooks with a specific {@link PluginHookName}
     *
     * @param name name of the plugin
     * @return instance of a hook class associated with the plugin name
     */
    public PluginHook getHook(String name) {
        return hookByName.get(name);
    }

    /**
     * returns if a hooks with a specific {@link PluginHookName} is available
     *
     * @param name name of the plugin
     * @return if any hook was found
     */
    public boolean isHookActive(String name) {
        return hookByName.containsKey(name);
    }

    /**
     * returns if a hook that inherit from a specific class/interface is available
     *
     * @param hookClass class that implements from {@link PluginHook}
     * @return if any hook was found
     */
    public boolean isHookActive(Class<? extends PluginHook> hookClass) {
        return hookByClass.containsKey(hookClass);
    }

    /**
     * searches for an instance of a plugin
     *
     * @param clazz class of the plugin
     * @return instance of the plugin
     */
    public <T extends JavaPlugin> Optional<T> getPluginInstance(Class<T> clazz) {
        try {
            T plugin = JavaPlugin.getPlugin(clazz);
            if (plugin != null) {
                return Optional.of(plugin);
            }
        } catch (NoSuchMethodError e) {
            for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                if (clazz.isInstance(p)) {
                    T plugin = clazz.cast(p);
                    return Optional.of(plugin);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * searches for an instance of a plugin
     *
     * @param pluginName name of the plugin
     * @return instance of the plugin
     */
    @SuppressWarnings("unchecked")
    public <T extends JavaPlugin> Optional<T> getPluginInstance(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                if (p.getName().equalsIgnoreCase(pluginName)) {
                    return Optional.of((T) p);
                }
            }
            return Optional.empty();
        }
        return Optional.of((T) plugin);
    }

    /**
     * checks if a plugin is enabled
     *
     * @param pluginName name of the plugin
     * @return if the plugin is enabled
     */
    public boolean isPluginUsable(String pluginName) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    /**
     * checks if a plugin with a specific class name is enabled
     *
     * @param pluginName name of the plugin
     * @param className  class name
     * @return if the plugin is enabled
     */
    public static boolean isPluginUsable(String pluginName, String className) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled() && plugin.getClass().getName().equals(className);
    }

    /**
     * checks if a plugin is available
     *
     * @param pluginName name of the plugin
     * @return if the plugin is available
     */
    public boolean isPluginAvailable(String pluginName) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null;
    }

    /**
     * checks if a plugin with a specific class name is available
     *
     * @param pluginName name of the plugin
     * @param className  class name
     * @return if the plugin is available
     */
    public static boolean isPluginAvailable(String pluginName, String className) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.getClass().getName().equals(className);
    }
}