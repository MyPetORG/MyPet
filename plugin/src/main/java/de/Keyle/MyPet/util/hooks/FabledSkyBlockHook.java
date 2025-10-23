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

package de.Keyle.MyPet.util.hooks;

import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.api.SkyBlockAPI;
import com.songoda.skyblock.config.FileManager;
import com.songoda.skyblock.island.IslandManager;
import com.songoda.skyblock.island.IslandRole;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;

@PluginHookName("FabledSkyBlock")
public class FabledSkyBlockHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook {

    SkyBlock skyblock;
    IslandManager islandManager;
    FileManager fileManager;

    @Override
    public boolean onEnable() {
        this.skyblock = SkyBlockAPI.getImplementation();
        islandManager = skyblock.getIslandManager();
        fileManager = skyblock.getFileManager();
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            if (skyblock.getWorldManager().isIslandWorld(defender.getWorld())) {
                FileManager.Config config = fileManager.getConfig(new File(skyblock.getDataFolder(), "config.yml"));
                FileConfiguration configLoad = config.getFileConfiguration();
                if (configLoad.getBoolean("Island.Settings.PvP.Enable")) {
                    if (!islandManager.hasSetting(defender.getLocation(), IslandRole.Owner, "PvP")) {
                        return false;
                    }
                } else if (!configLoad.getBoolean("Island.PvP.Enable")) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            if (skyblock.getWorldManager().isIslandWorld(defender.getWorld())) {
                if (!islandManager.hasPermission(attacker, defender.getLocation(), "MobHurting")) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}