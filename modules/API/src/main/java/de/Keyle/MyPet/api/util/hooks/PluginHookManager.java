/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.MyPetApi;
import org.apache.commons.lang.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginHookManager {
    ArrayListMultimap<Class<? extends PluginHook>, PluginHook> hooks = ArrayListMultimap.create();
    Map<String, PluginHook> hookByName = new HashMap<>();
    Map<Class<? extends PluginHook>, PluginHook> hookByClass = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void registerHook(Class<? extends PluginHook> hookClass) {
        if (hookClass.isAnnotationPresent(PluginHookName.class)) {
            PluginHookName hookNameAnnotation = hookClass.getAnnotation(PluginHookName.class);

            String pluginName = hookNameAnnotation.value();
            if (!hookNameAnnotation.classPath().equalsIgnoreCase("")) {
                if (!isPluginUsable(pluginName, hookNameAnnotation.classPath())) {
                    return;
                }
            } else {
                if (!isPluginUsable(pluginName)) {
                    return;
                }
            }
            try {
                PluginHook hook = hookClass.newInstance();

                if (hook.onEnable()) {

                    boolean genericHook = true;
                    for (Object o : ClassUtils.getAllInterfaces(hookClass)) {
                        if (o != PluginHook.class && PluginHook.class.isAssignableFrom((Class) o)) {
                            hooks.put((Class) o, hook);
                            genericHook = false;
                        }
                    }
                    if (genericHook) {
                        hooks.put(PluginHook.class, hook);
                    }
                    hookByName.put(pluginName, hook);
                    hookByClass.put(hookClass, hook);

                    String message = pluginName;
                    if (!hookNameAnnotation.classPath().equalsIgnoreCase("")) {
                        message += "(" + hookNameAnnotation.classPath() + ")";
                    }
                    MyPetApi.getLogger().info(message + " hook activated.");
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginHook> List<T> getHooks(Class<? extends T> hookClass) {
        return (List<T>) hooks.get(hookClass);
    }

    public boolean hasHooks(Class<? extends PluginHook> hookClass) {
        return hooks.containsKey(hookClass);
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginHook> T getHook(Class<? extends T> hookClass) {
        return (T) hookByClass.get(hookClass);
    }

    public PluginHook getHook(String name) {
        return hookByName.get(name);
    }

    public boolean isHookActive(String name) {
        return hookByName.containsKey(name);
    }

    public boolean isHookActive(Class<? extends PluginHook> hookClass) {
        return hookByClass.containsKey(hookClass);
    }

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
        return Optional.absent();
    }

    public boolean isPluginUsable(String pluginName) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    public static boolean isPluginUsable(String pluginName, String className) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled() && plugin.getClass().getName().equals(className);
    }
}