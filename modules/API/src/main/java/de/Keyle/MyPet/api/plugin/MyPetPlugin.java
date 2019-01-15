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

import de.Keyle.MyPet.api.PlatformHelper;
import de.Keyle.MyPet.api.entity.EntityRegistry;
import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.repository.MyPetManager;
import de.Keyle.MyPet.api.repository.PlayerManager;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.util.CompatUtil;
import de.Keyle.MyPet.api.util.ErrorReporter;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import org.bukkit.plugin.Plugin;

import java.io.File;

public interface MyPetPlugin extends Plugin {
    Repository getRepository();

    PlatformHelper getPlatformHelper();

    File getFile();

    MyPetInfo getMyPetInfo();

    EntityRegistry getEntityRegistry();

    CompatUtil getCompatUtil();

    PlayerManager getPlayerManager();

    MyPetManager getMyPetManager();

    HookHelper getHookHelper();

    PluginHookManager getPluginHookManager();

    ServiceManager getServiceManager();

    ErrorReporter getErrorReporter();
}