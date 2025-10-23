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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.ConfigurationLoader;
import de.Keyle.MyPet.util.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandOptionReload implements CommandOptionTabCompleter {

    public static final List<String> COMMAND_OPTIONS = new ArrayList<>();

    static {
        COMMAND_OPTIONS.add("all");
        COMMAND_OPTIONS.add("config");
        COMMAND_OPTIONS.add("skilltrees");
        COMMAND_OPTIONS.add("shops");
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin reload " + ChatColor.RED + "<what to reload?>");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "all":
                reloadConfig(sender);
                reloadSkilltrees(sender);
                reloadShops(sender);
                break;
            case "config":
                reloadConfig(sender);
                break;
            case "skilltrees":
                reloadSkilltrees(sender);
                break;
            case "shops":
                reloadShops(sender);
                break;
            default:
                sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin reload " + ChatColor.RED + "<what to reload?>");
        }
        return true;
    }

    protected void reloadConfig(CommandSender sender) {
        int oldMaxPetCount = Configuration.Misc.MAX_STORED_PET_COUNT;
        ConfigurationLoader.loadConfiguration();
        ConfigurationLoader.loadCompatConfiguration();

        if (MyPetApi.getLogger() instanceof MyPetLogger) {
            ((MyPetLogger) MyPetApi.getLogger()).updateDebugLoggerLogLevel();
        }

        Translation.init();

        if (Configuration.Misc.MAX_STORED_PET_COUNT > oldMaxPetCount) {
            for (int i = oldMaxPetCount + 1; i <= Configuration.Misc.MAX_STORED_PET_COUNT; i++) {
                try {
                    Bukkit.getPluginManager().addPermission(new Permission("MyPet.petstorage.limit." + i));
                } catch (Exception ignored) {
                }
            }
        } else if (oldMaxPetCount > Configuration.Misc.MAX_STORED_PET_COUNT) {
            for (int i = oldMaxPetCount; i > Configuration.Misc.MAX_STORED_PET_COUNT; i--) {
                try {
                    Bukkit.getPluginManager().removePermission("MyPet.petstorage.limit." + i);
                } catch (Exception ignored) {
                }
            }
        }

        ExperienceCalculatorManager calculatorManager = MyPetApi.getServiceManager().getService(ExperienceCalculatorManager.class).get();
        calculatorManager.switchCalculator(Configuration.LevelSystem.CALCULATION_MODE);

        MyPetApi.getPluginHookManager().getConfig().loadConfig();

        for (PluginHook hook : MyPetApi.getPluginHookManager().getHooks()) {
            ConfigurationSection pluginSection = MyPetApi.getPluginHookManager().getConfig().getConfig().getConfigurationSection(hook.getPluginName());
            if (pluginSection != null) {
                hook.loadConfig(pluginSection);
            }
        }
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("[" + ChatColor.DARK_GREEN + "MyPet" + ChatColor.RESET + "] config reloaded!");
        }
        MyPetApi.getLogger().info("Config reloaded!");
    }

    protected void reloadSkilltrees(CommandSender sender) {
        MyPetApi.getSkilltreeManager().clearSkilltrees();

        SkillTreeLoaderJSON.loadSkilltrees(new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"));

        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            Skilltree skilltree = myPet.getSkilltree();
            if (skilltree != null) {
                String skilltreeName = skilltree.getName();
                if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
                    skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                    if (!skilltree.getMobTypes().contains(myPet.getPetType())) {
                        skilltree = null;
                    }
                } else {
                    skilltree = null;
                }
            }
            myPet.setSkilltree(skilltree);
        }
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] skilltrees reloaded!");
        MyPetApi.getLogger().info("Skilltrees reloaded!");
    }

    protected void reloadShops(CommandSender sender) {
        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            MyPetApi.getServiceManager().getService(ShopManager.class).get().onEnable(); //TODO reload method?
        }

        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] shops reloaded!");
        MyPetApi.getLogger().info("Shops reloaded!");
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length > 2) {
            return Collections.emptyList();
        } else {
            return filterTabCompletionResults(COMMAND_OPTIONS, strings[1]);
        }
    }
}