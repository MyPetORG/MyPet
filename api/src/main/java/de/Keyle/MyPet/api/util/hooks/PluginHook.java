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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

/**
 * Plugin hooks are used to create a wrapper for functionality used from another plugin.
 */
public interface PluginHook extends Listener {

    /**
     * Method that is called when the hook gets enabled
     *
     * @return if activation was successfull
     */
    default boolean onEnable() {
        return true;
    }

    /**
     * Method that is called when the hook gets disabled
     */
    default void onDisable() {
    }

    /**
     * Method that is called before the hook gets enabled to load the config
     */
    default void loadConfig(ConfigurationSection config) {
    }

    /**
     * Returns the name of the hooked plugin
     *
     * @return name of the plugin
     */
    default String getPluginName() {
        if (this.getClass().isAnnotationPresent(PluginHookName.class)) {
            PluginHookName hookNameAnnotation = this.getClass().getAnnotation(PluginHookName.class);
            return hookNameAnnotation.value();
        }
        return "INVALID HOOK";
    }

    default String getActivationMessage() {
        return "";
    }
}