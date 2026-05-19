/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import com.mojang.brigadier.Command;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.util.VersionUtil;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.commands.mypet.CommandOptionReload;
import de.Keyle.MyPet.commands.mypet.CommandOptionTicket;
import de.Keyle.MyPet.commands.mypet.CommandOptionUpdate;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /mypet} base command using Paper's Brigadier API.
 *
 * <p>This is the plugin's primary top-level command. When invoked without arguments it
 * displays a splash banner with plugin version and a link to the project. Sub-literals
 * are mounted for the {@code help}, {@code reload}, {@code ticket}, and {@code update}
 * subcommands.</p>
 *
 * <h3>Command tree</h3>
 * <pre>
 *   /mypet              - show splash banner
 *   /mypet help         - list available help categories
 *   /mypet help all     - show all commands across every category
 *   /mypet help pet     - show commands in the "Pet" category
 *   /mypet help skills  - show commands in the "Skills" category
 *   /mypet help admin   - show commands in the "Admin" category
 *   /mypet reload ...   - reload plugin resources (see {@link CommandOptionReload})
 *   /mypet ticket       - open a support ticket link
 *   /mypet update       - check for plugin updates
 * </pre>
 *
 * <p>The help system groups entries from the {@link HelpRegistry} by
 * {@link CommandCategory} and filters visibility based on the player's permissions.</p>
 */
public class CommandMyPet {

    /** Character width of the dash separators used in help output. */
    private static final int SEPARATOR_WIDTH = 52;

    /** Registry used to look up help entries by category and visibility. */
    private HelpRegistry helpRegistry;

    /**
     * Registers the {@code /mypet} Brigadier command and its help entry.
     *
     * <p>The tree consists of a root literal {@code mypet} whose default execution shows
     * the splash banner, a {@code help} literal with sub-literals for each category (plus
     * {@code all}), and delegated subtrees for {@code reload}, {@code ticket}, and
     * {@code update}.</p>
     *
     * <p>A {@link HelpEntry} for the base command itself is also registered under the
     * {@link CommandCategory#PET} category.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        this.helpRegistry = helpRegistry;

        commands.register(
                Commands.literal("mypet")
                        .executes(ctx -> {
                            showSplash(ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("help")
                                .executes(ctx -> {
                                    showHelp(ctx.getSource().getSender(), null);
                                    return Command.SINGLE_SUCCESS;
                                })
                                        .then(Commands.literal("all")
                                        .executes(ctx -> {
                                            showHelp(ctx.getSource().getSender(), "all");
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .then(Commands.literal("pet")
                                        .executes(ctx -> {
                                            showHelp(ctx.getSource().getSender(), "pet");
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .then(Commands.literal("skills")
                                        .executes(ctx -> {
                                            showHelp(ctx.getSource().getSender(), "skills");
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .then(Commands.literal("admin")
                                        .executes(ctx -> {
                                            showHelp(ctx.getSource().getSender(), "admin");
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(new CommandOptionReload().buildNode())
                        .then(new CommandOptionTicket().buildNode())
                        .then(new CommandOptionUpdate().buildNode())
                        .build(),
                "MyPet plugin commands",
                List.of()
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.MyPet",
                "/mypet",
                CommandCategory.PET,
                5,
                null
        ));
    }

    /**
     * Displays the plugin splash banner showing the name, version, and project URL.
     *
     * @param sender the command sender to receive the splash output
     */
    private void showSplash(CommandSender sender) {
        String line = dashes(SEPARATOR_WIDTH);
        sender.sendMessage(line);
        sender.sendMessage(Component.text("MyPet").color(NamedTextColor.GOLD).append(Component.text(" " + VersionUtil.getFormattedVersion()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("https://github.com/MyPetORG/MyPet").color(NamedTextColor.GRAY));
        sender.sendMessage("");
        sender.sendMessage(Component.text("Use ").append(Component.text("/mypet help").color(NamedTextColor.GOLD)).append(Component.text(" to see available commands.")));
        sender.sendMessage(line);
    }

    /**
     * Routes the {@code /mypet help} command to the appropriate handler.
     *
     * <ul>
     *   <li>{@code null} category -- shows the category listing overview</li>
     *   <li>{@code "all"} -- shows all commands grouped by every category</li>
     *   <li>A valid category name -- shows commands for that single category</li>
     *   <li>An unrecognized name -- sends an error message</li>
     * </ul>
     *
     * @param sender      the command sender requesting help
     * @param categoryArg the category argument, or {@code null} if none was provided
     */
    private void showHelp(CommandSender sender, String categoryArg) {
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        if (categoryArg == null) {
            showCategoryListing(sender, player, isPlayer);
        } else if (categoryArg.equalsIgnoreCase("all")) {
            showGroupedHelp(sender, player, isPlayer, null);
        } else {
            CommandCategory match = matchCategory(categoryArg);
            if (match != null) {
                showGroupedHelp(sender, player, isPlayer, match);
            } else {
                sender.sendMessage(Component.text("Unknown category: " + categoryArg).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Use ").append(Component.text("/mypet help").color(NamedTextColor.GOLD)).append(Component.text(" to see available categories.")));
            }
        }
    }

    /**
     * Displays the help category overview, listing each {@link CommandCategory} that has
     * at least one visible entry for the sender, along with its localized description.
     *
     * <p>An additional {@code "all"} pseudo-category is always appended at the bottom.</p>
     *
     * @param sender   the command sender to receive the category listing
     * @param player   the {@link Player} instance if the sender is a player, otherwise {@code null}
     * @param isPlayer {@code true} if the sender is a player (used for permission filtering)
     */
    private void showCategoryListing(CommandSender sender, Player player, boolean isPlayer) {
        Component titleComponent = Component.text("MyPet - ").append(Locale.getComponent("Name.Help", sender));
        sender.sendMessage(buildSeparator(titleComponent));
        sender.sendMessage(Component.text("Use ").append(Component.text("/mypet help <category>").color(NamedTextColor.GOLD)).append(Component.text(" to see commands.")));
        sender.sendMessage("");

        for (CommandCategory category : CommandCategory.values()) {
            List<HelpEntry> entries = isPlayer
                    ? helpRegistry.getEntriesByCategoryVisibleTo(category, player)
                    : helpRegistry.getEntriesByCategory(category);
            if (entries.isEmpty()) {
                continue;
            }
            String descKey = "Message.Command.Help.Category." + category.getDisplayName();
            sender.sendMessage(Component.text("  ")
                    .append(Component.text(category.getDisplayName().toLowerCase()).color(NamedTextColor.GOLD))
                    .append(Component.text(" - "))
                    .append(Locale.getComponent(descKey, sender)));
        }

        sender.sendMessage("");
        sender.sendMessage(Component.text("  ")
                .append(Component.text("all").color(NamedTextColor.GOLD))
                .append(Component.text(" - "))
                .append(Locale.getComponent("Message.Command.Help.Category.All", sender)));
        sender.sendMessage(dashes(SEPARATOR_WIDTH));
    }

    /**
     * Renders help entries grouped under their {@link CommandCategory} headings.
     *
     * <p>When {@code filter} is {@code null}, all categories are shown (the "all" view).
     * When a specific category is given, only entries belonging to that category are
     * displayed. Each group is preceded by a titled separator line, and a wiki URL is
     * appended at the end.</p>
     *
     * @param sender   the command sender to receive the help output
     * @param player   the {@link Player} instance if the sender is a player, otherwise {@code null}
     * @param isPlayer {@code true} if the sender is a player (used for permission filtering)
     * @param filter   the category to restrict output to, or {@code null} to show all categories
     */
    private void showGroupedHelp(CommandSender sender, Player player, boolean isPlayer, CommandCategory filter) {
        boolean first = true;
        for (CommandCategory category : CommandCategory.values()) {
            if (filter != null && category != filter) {
                continue;
            }
            List<HelpEntry> entries = isPlayer
                    ? helpRegistry.getEntriesByCategoryVisibleTo(category, player)
                    : helpRegistry.getEntriesByCategory(category);
            if (entries.isEmpty()) {
                continue;
            }

            if (!first) {
                sender.sendMessage("");
            }
            Component titleComp = Component.text("MyPet - ")
                    .append(Locale.getComponent("Name.Help", sender))
                    .append(Component.text(" - " + category.getDisplayName()));
            sender.sendMessage(buildSeparator(titleComp));
            for (HelpEntry entry : entries) {
                sender.sendMessage(Component.text("  ").append(
                        Locale.getFormattedComponent(entry.translationKey(), sender,
                                entry.command())));
            }
            first = false;
        }

        sender.sendMessage("");
        sender.sendMessage(Locale.getComponent("Message.Command.Help.MoreInfo", sender)
                .append(Component.text(" " + Configuration.Misc.WIKI_URL).color(NamedTextColor.GOLD)));
        sender.sendMessage(dashes(SEPARATOR_WIDTH));
    }

    /**
     * Performs a case-insensitive lookup of a {@link CommandCategory} by its display name.
     *
     * @param input the category name typed by the user
     * @return the matching {@link CommandCategory}, or {@code null} if no match was found
     */
    private CommandCategory matchCategory(String input) {
        String lower = input.toLowerCase();
        for (CommandCategory category : CommandCategory.values()) {
            if (category.getDisplayName().toLowerCase().equals(lower)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Creates a string of dash ({@code -}) characters of the specified length.
     *
     * @param count the number of dashes to produce
     * @return a string containing exactly {@code count} dash characters
     */
    private static String dashes(int count) {
        return "-".repeat(count);
    }

    /**
     * Builds a centered separator line with a gold-colored title flanked by dashes.
     * Width is computed from the plain-text serialization of {@code titleComponent}.
     *
     * @param titleComponent the styled Adventure {@link Component} to render as the title
     * @return a {@link Component} of the form {@code "--- Title ---"}
     */
    private static Component buildSeparator(Component titleComponent) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(titleComponent);
        int contentWidth = plainTitle.length() + 2;
        int remaining = SEPARATOR_WIDTH - contentWidth;
        int side = Math.max(0, remaining / 2);

        return Component.text(dashes(side) + " ")
                .append(titleComponent.color(NamedTextColor.GOLD))
                .append(Component.text(" " + dashes(remaining - side)));
    }
}
