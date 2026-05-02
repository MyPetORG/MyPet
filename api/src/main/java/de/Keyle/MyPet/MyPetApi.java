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

package de.Keyle.MyPet;

import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagManager;
import de.Keyle.MyPet.api.plugin.MyPetPlugin;
import de.Keyle.MyPet.api.repository.MyPetManager;
import de.Keyle.MyPet.api.repository.PlayerManager;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeManager;
import de.Keyle.MyPet.api.util.ErrorReporter;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceManager;

import java.util.logging.Logger;

public class MyPetApi {

    private static MyPetPlugin plugin;

    /**
     * @return the main plugin instance
     */
    public static MyPetPlugin getPlugin() {
        return plugin;
    }

    protected static void setPlugin(MyPetPlugin plugin) {
        if (MyPetApi.plugin != null) {
            return;
        }
        MyPetApi.plugin = plugin;
    }

    /**
     * @return {@code true} if MyPet has loaded and the API is safe to call
     */
    public static boolean isReady() {
        return plugin != null;
    }

    private static MyPetPlugin requirePlugin() {
        if (plugin == null) {
            throw new IllegalStateException(
                    "MyPetApi accessed before MyPet has loaded. Call from onEnable (after MyPet's onEnable runs), " +
                            "not from onLoad or constructors. Add 'softdepend: [MyPet]' to plugin.yml or guard with MyPetApi.isReady().");
        }
        return plugin;
    }

    private static <T extends ServiceContainer> T requireService(Class<T> serviceClass) {
        return getServiceManager().getService(serviceClass).orElseThrow(() ->
                new IllegalStateException(
                        "MyPetApi: " + serviceClass.getSimpleName() + " service not yet registered. " +
                                "Call from onEnable (after MyPet's onEnable runs) or guard with MyPetApi.isReady()."));
    }

    /**
     * @return the pluginlogger or a logger instance called MyPet
     */
    public static Logger getLogger() {
        if (plugin != null) {
            return plugin.getLogger();
        } else {
            return Logger.getLogger("MyPet");
        }
    }

    /**
     * @return instance of the error reporter
     */
    public static ErrorReporter getErrorReporter() {
        return requirePlugin().getErrorReporter();
    }

    /**
     * @return you can find info about pet types here
     */
    public static MyPetInfo getMyPetInfo() {
        return requirePlugin().getMyPetInfo();
    }

    /**
     * @return MyPet player manager
     */
    public static PlayerManager getPlayerManager() {
        return requirePlugin().getPlayerManager();
    }

    /**
     * @return MyPet manager
     */
    public static MyPetManager getMyPetManager() {
        return requirePlugin().getMyPetManager();
    }

    /**
     * @return you can find plugin hook helper functions here
     */
    public static HookHelper getHookHelper() {
        return requirePlugin().getHookHelper();
    }

    /**
     * @return instance of the plugin hook manager
     */
    public static PluginHookManager getPluginHookManager() {
        return requirePlugin().getPluginHookManager();
    }

    /**
     * @return instance of the plugin hook manager
     */
    public static ServiceManager getServiceManager() {
        return requirePlugin().getServiceManager();
    }

    /**
     * @return instance of the skilltree manager
     */
    public static SkilltreeManager getSkilltreeManager() {
        return requireService(SkilltreeManager.class);
    }

    /**
     * @return instance of the skill manager
     */
    public static SkillManager getSkillManager() {
        return requireService(SkillManager.class);
    }

    /**
     * @return instance of the leashflag manager
     */
    public static LeashFlagManager getLeashFlagManager() {
        return requireService(LeashFlagManager.class);
    }
}