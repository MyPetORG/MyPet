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

package de.Keyle.MyPet.api.plugin;

import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.repository.MyPetManager;
import de.Keyle.MyPet.api.repository.PlayerManager;
import de.Keyle.MyPet.api.util.ErrorReporter;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * API-facing contract for the MyPet plugin instance. Provides access to
 * all top-level managers and services without depending on the concrete
 * plugin-module class. Obtained globally via {@code MyPetApi.getPlugin()}.
 * <p>
 * Extends Bukkit {@link Plugin} so callers can register listeners, schedule
 * tasks, and access the data folder without a separate plugin reference.
 */
public interface MyPetPlugin extends Plugin {

    /** Returns the plugin JAR file on disk. */
    File getFile();

    /** Returns the per-type metadata registry (HP, speed, food, leash flags). */
    MyPetInfo getMyPetInfo();

    /** Returns the manager for {@link de.Keyle.MyPet.api.player.MyPetPlayer} instances. */
    PlayerManager getPlayerManager();

    /** Returns the manager for active and inactive pet instances. */
    MyPetManager getMyPetManager();

    /** Returns the utility for querying third-party hook state. */
    HookHelper getHookHelper();

    /** Returns the registry for third-party plugin integrations (WorldGuard, Vault, etc.). */
    PluginHookManager getPluginHookManager();

    /** Returns the central service registry for lifecycle-managed services. */
    ServiceManager getServiceManager();

    /** Returns the error reporter (Sentry in non-release builds, no-op otherwise). */
    ErrorReporter getErrorReporter();

    /**
     * Returns {@code true} during {@code onDisable()} execution. Used to
     * suppress async operations that would race against shutdown.
     */
    boolean isDisabling();

    /** Returns the shared MiniMessage instance used for all text serialization. */
    MiniMessage getMiniMessage();
}