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

package de.Keyle.MyPet.commands.mypet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.logger.DebugLogHandler;
import de.Keyle.MyPet.util.ConfigurationLoader;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.util.shop.ShopManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.util.Optional;

/**
 * Provides the {@code /mypet reload} subcommand, enabling hot-reload of plugin resources.
 *
 * <p>This command is restricted to the console and players with the {@code MyPet.admin}
 * permission. It supports four reload targets:</p>
 *
 * <h3>Command tree</h3>
 * <pre>
 *   /mypet reload             - shows usage hint (missing parameter)
 *   /mypet reload all         - reloads config, skilltrees, and shops
 *   /mypet reload config      - reloads config.yml, translations, hook configs, and
 *                                recalculates pet-storage permissions and XP calculator
 *   /mypet reload skilltrees  - reloads skilltree JSON files from the skilltrees/
 *                                directory and reassigns trees to all active pets
 *   /mypet reload shops       - reloads shop definitions via the {@link ShopManager}
 * </pre>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionReload {

    /**
     * Builds the {@code reload} literal command node to be mounted under {@code /mypet}.
     *
     * <p>The node requires {@code MyPet.admin} permission (or console) and defines four
     * sub-literals ({@code all}, {@code config}, {@code skilltrees}, {@code shops}). The
     * bare {@code /mypet reload} execution (without a target) sends a usage hint listing
     * the available targets.</p>
     *
     * @return the built {@link LiteralCommandNode} for the {@code reload} subtree
     */
    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("reload")
                .requires(ctx -> {
                    var sender = ctx.getSender();
                    return !(sender instanceof Player p) || Permissions.has(p, "MyPet.admin", false);
                })
                .then(Commands.literal("all")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadConfig(sender);
                            reloadSkilltrees(sender);
                            reloadShops(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("config")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadConfig(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("skilltrees")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadSkilltrees(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("shops")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadShops(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Locale.getComponent("Message.Command.Help.MissingParameter", sender));
                    sender.sendMessage(Component.text(" -> ")
                            .append(Component.text("/mypet reload ").color(NamedTextColor.DARK_AQUA))
                            .append(Component.text("<all|config|shops|skilltrees>").color(NamedTextColor.RED)));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /**
     * Reloads the plugin configuration from {@code config.yml} and applies side effects.
     *
     * <p>This method performs the following steps in order:</p>
     * <ol>
     *   <li>Reloads the main configuration and version-specific compatibility config via
     *       {@link ConfigurationLoader}</li>
     *   <li>Updates the debug log level</li>
     *   <li>Re-initializes the translation/locale system</li>
     *   <li>Adjusts {@code MyPet.petstorage.limit.*} permissions if the maximum stored
     *       pet count changed (registers new permissions or removes excess ones)</li>
     *   <li>Switches the experience calculator to match the configured calculation mode</li>
     *   <li>Reloads configuration for all registered {@link PluginHook} integrations</li>
     * </ol>
     *
     * @param sender the command sender to receive a confirmation message (console senders
     *               receive only a log entry, not a chat message)
     */
    protected void reloadConfig(CommandSender sender) {
        int oldMaxPetCount = Configuration.Misc.MAX_STORED_PET_COUNT;
        ConfigurationLoader.loadConfiguration();
        ConfigurationLoader.loadCompatConfiguration();

        DebugLogHandler.updateLogLevel();

        Locale.init();

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
            sender.sendMessage(MessageUtil.prefixed(Component.text("config reloaded!")));
        }
        MyPetApi.getLogger().info("Config reloaded!");
    }

    /**
     * Reloads all skilltree definitions from the {@code skilltrees/} data folder.
     *
     * <p>After clearing and re-loading the skilltree JSON files, every currently active
     * pet is checked: if its previously assigned skilltree still exists and is compatible
     * with the pet's mob type, the reference is updated to the newly loaded instance;
     * otherwise the pet's skilltree is set to {@code null}.</p>
     *
     * @param sender the command sender to receive a confirmation message
     */
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
        sender.sendMessage(MessageUtil.prefixed(Component.text("skilltrees reloaded!")));
        MyPetApi.getLogger().info("Skilltrees reloaded!");
    }

    /**
     * Reloads shop definitions by re-running the {@link ShopManager#onEnable()} lifecycle
     * method, which re-reads shop configuration files.
     *
     * <p>If no {@link ShopManager} service is registered (e.g. shops are disabled), the
     * reload is silently skipped but the confirmation message is still sent.</p>
     *
     * @param sender the command sender to receive a confirmation message
     */
    protected void reloadShops(CommandSender sender) {
        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            MyPetApi.getServiceManager().getService(ShopManager.class).get().onEnable(); //TODO reload method?
        }

        sender.sendMessage(MessageUtil.prefixed(Component.text("shops reloaded!")));
        MyPetApi.getLogger().info("Shops reloaded!");
    }
}
