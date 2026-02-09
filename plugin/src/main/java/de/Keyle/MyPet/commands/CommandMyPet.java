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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.commands.CommandAdmin;
import de.Keyle.MyPet.api.commands.HelpProvider;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.mypet.CommandOptionReload;
import de.Keyle.MyPet.commands.mypet.CommandOptionTicket;
import de.Keyle.MyPet.commands.mypet.CommandOptionUpdate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandMyPet implements CommandTabCompleter {

    private final Map<String, CommandOption> subcommands = new LinkedHashMap<>();

    {
        subcommands.put("reload", new CommandOptionReload());
        subcommands.put("ticket", new CommandOptionTicket());
        subcommands.put("update", new CommandOptionUpdate());
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("help")) {
                showHelp(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
            CommandOption option = subcommands.get(sub);
            if (option != null) {
                if (sender instanceof Player && !option.isVisibleTo((Player) sender)) {
                    sender.sendMessage(Translation.getString("Message.No.Allowed", sender));
                    return true;
                }
                return option.onCommandOption(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        showSplash(sender);
        return true;
    }

    private void showSplash(CommandSender sender) {
        String line = dashes(SEPARATOR_WIDTH);
        sender.sendMessage(line);
        String build = MyPetVersion.getBuild();
        boolean numericBuild = build.chars().allMatch(Character::isDigit);
        String version = MyPetVersion.isDevBuild()
                ? "v" + MyPetVersion.getVersion() + "-SNAPSHOT-" + (numericBuild ? "b" : "") + build
                : "v" + MyPetVersion.getVersion();
        sender.sendMessage(ChatColor.GOLD + "MyPet" + ChatColor.RESET + " " + version);
        sender.sendMessage(ChatColor.GRAY + "https://github.com/MyPetORG/MyPet");
        sender.sendMessage("");
        sender.sendMessage("Use " + ChatColor.GOLD + "/mypet help" + ChatColor.RESET
                + " to see available commands.");
        sender.sendMessage(line);
    }

    private void showHelp(CommandSender sender, String[] args) {
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        if (args.length == 0) {
            showCategoryListing(sender, player, isPlayer);
        } else if (args[0].equalsIgnoreCase("all")) {
            showGroupedHelp(sender, player, isPlayer, null);
        } else {
            CommandCategory match = matchCategory(args[0]);
            if (match != null) {
                showGroupedHelp(sender, player, isPlayer, match);
            } else {
                sender.sendMessage(ChatColor.RED + "Unknown category: " + args[0]);
                sender.sendMessage("Use " + ChatColor.GOLD + "/mypet help" + ChatColor.RESET
                        + " to see available categories.");
            }
        }
    }

    private void showCategoryListing(CommandSender sender, Player player, boolean isPlayer) {
        String title = "MyPet - " + Translation.getString("Name.Help", sender);
        sender.sendMessage(buildSeparator(title));
        sender.sendMessage("Use " + ChatColor.GOLD + "/mypet help <category>" + ChatColor.RESET
                + " to see commands.");
        sender.sendMessage("");

        Map<CommandCategory, List<HelpProvider>> grouped = collectGroupedHelp();

        for (CommandCategory category : CommandCategory.values()) {
            List<HelpProvider> providers = grouped.get(category);
            if (providers == null) {
                continue;
            }
            if (!hasVisibleCommands(providers, player, isPlayer)) {
                continue;
            }
            String descKey = "Message.Command.Help.Category." + category.getDisplayName();
            String description = Translation.getString(descKey, sender);
            sender.sendMessage("  " + ChatColor.GOLD + category.getDisplayName().toLowerCase()
                    + ChatColor.RESET + " - " + description);
        }

        sender.sendMessage("");
        sender.sendMessage("  " + ChatColor.GOLD + "all" + ChatColor.RESET + " - "
                + Translation.getString("Message.Command.Help.Category.All", sender));
        sender.sendMessage(dashes(SEPARATOR_WIDTH));
    }

    private void showGroupedHelp(CommandSender sender, Player player, boolean isPlayer, CommandCategory filter) {
        Map<CommandCategory, List<HelpProvider>> grouped = collectGroupedHelp();

        boolean first = true;
        for (CommandCategory category : CommandCategory.values()) {
            if (filter != null && category != filter) {
                continue;
            }
            List<HelpProvider> providers = grouped.get(category);
            if (providers == null) {
                continue;
            }
            if (!hasVisibleCommands(providers, player, isPlayer)) {
                continue;
            }

            if (!first) {
                sender.sendMessage("");
            }
            String title = "MyPet - " + Translation.getString("Name.Help", sender)
                    + " - " + category.getDisplayName();
            sender.sendMessage(buildSeparator(title));
            for (HelpProvider hp : providers) {
                if (!isPlayer || hp.isVisibleTo(player)) {
                    if (hp.getHelpTranslationKey() != null) {
                        sender.sendMessage("  " + Util.formatText(
                                Translation.getString(hp.getHelpTranslationKey(), sender),
                                hp.getHelpCommand()));
                    } else if (hp.getHelpDescription() != null) {
                        sender.sendMessage("  " + ChatColor.GOLD + hp.getHelpCommand()
                                + ChatColor.RESET + ": " + hp.getHelpDescription());
                    }
                }
            }
            first = false;
        }

        sender.sendMessage("");
        sender.sendMessage(Translation.getString("Message.Command.Help.MoreInfo", sender)
                + ChatColor.GOLD + " " + Configuration.Misc.WIKI_URL);
        sender.sendMessage(dashes(SEPARATOR_WIDTH));
    }

    private Map<CommandCategory, List<HelpProvider>> collectGroupedHelp() {
        List<HelpProvider> all = collectHelpProviders();
        Map<CommandCategory, List<HelpProvider>> grouped = new LinkedHashMap<>();
        for (HelpProvider hp : all) {
            grouped.computeIfAbsent(hp.getHelpCategory(), k -> new ArrayList<>()).add(hp);
        }
        return grouped;
    }

    private List<HelpProvider> collectHelpProviders() {
        JavaPlugin plugin = (JavaPlugin) MyPetApi.getPlugin();
        List<HelpProvider> result = new ArrayList<>();

        for (String name : plugin.getDescription().getCommands().keySet()) {
            PluginCommand pluginCmd = plugin.getCommand(name);
            if (pluginCmd == null) {
                continue;
            }
            CommandExecutor executor = pluginCmd.getExecutor();
            if (executor instanceof HelpProvider) {
                HelpProvider hp = (HelpProvider) executor;
                if (hp.getHelpTranslationKey() != null && hp.getHelpCommand() != null) {
                    result.add(hp);
                }
            }
        }

        for (CommandOption option : subcommands.values()) {
            if (option.getHelpTranslationKey() != null && option.getHelpCommand() != null) {
                result.add(option);
            }
        }

        PluginCommand adminCmd = plugin.getCommand("mypetadmin");
        if (adminCmd != null) {
            CommandExecutor adminExecutor = adminCmd.getExecutor();
            if (adminExecutor instanceof CommandAdmin) {
                for (CommandOption option : ((CommandAdmin) adminExecutor).getCommandOptions().values()) {
                    if (option.getHelpCommand() != null
                            && (option.getHelpTranslationKey() != null || option.getHelpDescription() != null)) {
                        result.add(option);
                    }
                }
            }
        }

        result.sort(Comparator.comparingInt(HelpProvider::getHelpOrder));
        return result;
    }

    private boolean hasVisibleCommands(List<HelpProvider> providers, Player player, boolean isPlayer) {
        if (!isPlayer) {
            return true;
        }
        for (HelpProvider hp : providers) {
            if (hp.isVisibleTo(player)) {
                return true;
            }
        }
        return false;
    }

    private CommandCategory matchCategory(String input) {
        String lower = input.toLowerCase();
        for (CommandCategory category : CommandCategory.values()) {
            if (category.getDisplayName().toLowerCase().equals(lower)) {
                return category;
            }
        }
        return null;
    }

    private static final int SEPARATOR_WIDTH = 52;

    private static String dashes(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append('-');
        }
        return sb.toString();
    }

    private static String buildSeparator(String title) {
        int contentWidth = title.length() + 2;
        int remaining = SEPARATOR_WIDTH - contentWidth;
        int side = Math.max(0, remaining / 2);

        return dashes(side) + " " + ChatColor.GOLD + title + ChatColor.RESET
                + " " + dashes(remaining - side);
    }

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.MyPet";
    }

    @Override
    public String getHelpCommand() {
        return "/mypet";
    }

    @Override
    public int getHelpOrder() {
        return 5;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String s, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if ("help".startsWith(prefix)) {
                options.add("help");
            }
            for (Map.Entry<String, CommandOption> entry : subcommands.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    boolean allowed = !(sender instanceof Player)
                            || entry.getValue().isVisibleTo((Player) sender);
                    if (allowed) {
                        options.add(entry.getKey());
                    }
                }
            }
            return options;
        }
        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("help")) {
                return completeHelpCategories(sender, args[1]);
            }
            CommandOption option = subcommands.get(sub);
            if (option instanceof CommandOptionTabCompleter) {
                boolean allowed = !(sender instanceof Player)
                        || option.isVisibleTo((Player) sender);
                if (allowed) {
                    return ((CommandOptionTabCompleter) option).onTabComplete(sender, args);
                }
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private List<String> completeHelpCategories(CommandSender sender, String prefix) {
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;
        String lower = prefix.toLowerCase();

        Map<CommandCategory, List<HelpProvider>> grouped = collectGroupedHelp();
        List<String> results = new ArrayList<>();

        for (CommandCategory category : CommandCategory.values()) {
            List<HelpProvider> providers = grouped.get(category);
            if (providers == null) {
                continue;
            }
            if (!hasVisibleCommands(providers, player, isPlayer)) {
                continue;
            }
            String name = category.getDisplayName().toLowerCase();
            if (name.startsWith(lower)) {
                results.add(name);
            }
        }

        if ("all".startsWith(lower)) {
            results.add("all");
        }

        return results;
    }
}
