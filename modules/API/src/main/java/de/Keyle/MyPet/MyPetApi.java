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

package de.Keyle.MyPet;

import de.Keyle.MyPet.api.PlatformHelper;
import de.Keyle.MyPet.api.entity.EntityRegistry;
import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.plugin.MyPetPlugin;
import de.Keyle.MyPet.api.repository.MyPetManager;
import de.Keyle.MyPet.api.repository.PlayerManager;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.util.CompatUtil;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;

import java.util.logging.Logger;

public class MyPetApi {
    private static MyPetPlugin plugin;

    protected static void setPlugin(MyPetPlugin plugin) {
        if (MyPetApi.plugin != null) {
            return;
        }
        MyPetApi.plugin = plugin;
    }

    /**
     * @return the main plugin instance
     */
    public static MyPetPlugin getPlugin() {
        return plugin;
    }

    /**
     * @return the repository where pets and their owners are stored
     */
    public static Repository getRepository() {
        return plugin.getRepository();
    }

    public static Logger getLogger() {
        if (plugin != null) {
            return plugin.getLogger();
        } else {
            return Logger.getLogger("MyPet");
        }
    }

    public static PlatformHelper getPlatformHelper() {
        return plugin.getPlatformHelper();
    }

    public static MyPetInfo getMyPetInfo() {
        return plugin.getMyPetInfo();
    }

    public static EntityRegistry getEntityRegistry() {
        return plugin.getEntityRegistry();
    }

    public static CompatUtil getCompatUtil() {
        return plugin.getCompatUtil();
    }

    public static PlayerManager getPlayerManager() {
        return plugin.getPlayerManager();
    }

    public static MyPetManager getMyPetManager() {
        return plugin.getMyPetManager();
    }

    public static HookHelper getHookHelper() {
        return plugin.getHookHelper();
    }

    public static PluginHookManager getPluginHookManager() {
        return plugin.getPluginHookManager();
    }
}