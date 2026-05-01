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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.NameFilter;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.arguments.MiniMessageSuggestions;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the {@code /petname <name>} command using Paper's Brigadier API.
 *
 * <p>This command allows a player to rename their active pet. The name argument accepts
 * MiniMessage formatting tags (e.g. {@code <gold>}, {@code <bold>}) when the player holds
 * the appropriate permissions. Tag suggestions are provided via
 * {@link MiniMessageSuggestions} while the player types.</p>
 *
 * <h3>Command tree</h3>
 * <pre>
 *   /petname &lt;name&gt;  - set the pet's display name
 * </pre>
 *
 * <h3>Validation</h3>
 * <ul>
 *   <li>Requires an active pet and {@code MyPet.command.name} permission</li>
 *   <li>The name is checked against {@link NameFilter} for forbidden words</li>
 *   <li>Colour/format tags are stripped unless the player has
 *       {@code MyPet.command.name.color}</li>
 *   <li>A {@code <reset>} tag is auto-appended if the name contains any MiniMessage tags,
 *       preventing formatting from leaking into subsequent chat text</li>
 *   <li>The name (after stripping tags) must not exceed
 *       {@link Configuration.Name#MAX_LENGTH}</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandName {

    /**
     * Registers the {@code /petname} Brigadier command and its help entry.
     *
     * <p>The tree consists of a single root literal {@code petname} restricted to players,
     * with a greedy-string argument {@code name} that provides MiniMessage tag suggestions
     * via {@link MiniMessageSuggestions#suggest}. A {@link HelpEntry} is registered under
     * the {@link CommandCategory#PET} category, visible only to players who have an active
     * pet and the {@code MyPet.command.name} permission.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petname")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    MiniMessageSuggestions.suggest(builder, ctx.getSource().getSender());
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getSender();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    execute(player, name);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Sets the name of your pet",
                List.of()
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Name",
                "/petname",
                CommandCategory.PET,
                90,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
                        && Permissions.has(player, "MyPet.command.name")
        ));
    }

    /**
     * Executes the pet-rename logic for the given player.
     *
     * <p>Validates world group, active pet ownership, permission, and name filter rules
     * before applying the new name. If MiniMessage tags are detected in the name, a
     * {@code <reset>} tag is appended to prevent formatting bleed. The display name
     * shown in the confirmation message respects the player's colour permission.</p>
     *
     * @param petOwner the player who issued the command
     * @param name     the raw name string (may contain MiniMessage tags)
     */
    private void execute(Player petOwner, String name) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        if (!Permissions.has(petOwner, "MyPet.command.name")) {
            myPet.getOwner().sendMessage(Translation.getComponent("Message.No.CanUse", petOwner));
            return;
        }

        if (!NameFilter.isClean(name)) {
            petOwner.sendMessage(Translation.getComponent("Message.Command.Name.Filter", petOwner));
            return;
        }

        Pattern regex = Pattern.compile("<[a-zA-Z_]+>");
        Matcher regexMatcher = regex.matcher(name);
        if (regexMatcher.find()) {
            name = name + "<reset>";
        }

        String nameWithoutColors = Util.SANITIZED_MINIMESSAGE.stripTags(name);

        if (nameWithoutColors.length() <= Configuration.Name.MAX_LENGTH) {
            myPet.setPetName(name);
            if (Permissions.has(petOwner, "MyPet.command.name.color")) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Name.New", petOwner, name));
            } else {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Name.New", petOwner, nameWithoutColors));
            }
        } else {
            petOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Name.ToLong", petOwner, name, Configuration.Name.MAX_LENGTH));
        }
    }
}
