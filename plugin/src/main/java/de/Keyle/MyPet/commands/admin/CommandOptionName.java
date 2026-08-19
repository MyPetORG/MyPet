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

package de.Keyle.MyPet.commands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.commands.arguments.MiniMessageSuggestions;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin subcommand for forcibly renaming another player's active pet.
 *
 * <p>Usage: {@code /petadmin name <player> <name>}
 *
 * <p>The name argument supports MiniMessage formatting tags (e.g., {@code <red>}, {@code <bold>}).
 * If MiniMessage tags are detected in the name, a {@code <reset>} tag is automatically appended
 * to prevent formatting from leaking into subsequent text. Tab completion suggests available
 * MiniMessage tags via {@link MiniMessageSuggestions}.
 *
 * <p>Requires the {@code MyPet.admin.name} permission (or the {@code MyPet.admin} bundle).
 */
/*
 * Multi-Pet Phase 2 (MyPetORG/MyPet#1435): this command resolves the player to a
 * single Pet via the manager. That has no unambiguous answer once a player can
 * have several out -- it needs the optional pet-name argument the issue calls for,
 * so it is deliberately left alone until that argument exists.
 */
public class CommandOptionName {

    /**
     * Builds the Brigadier command node for the {@code name} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   name
     *     &lt;player: player_selector&gt;
     *       &lt;name: greedy_string&gt;
     *         (executes) -- rename the player's active pet
     * </pre>
     *
     * <p>The {@code name} argument uses a greedy string type to capture the entire remaining
     * input, allowing pet names to contain spaces and MiniMessage tags.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code name} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Name",
                "/petadmin name",
                CommandCategory.ADMIN,
                24,
                player -> Permissions.has(player, AdminPermissions.NAME)
        ));

        return Commands.literal("name")
                .requires(AdminPermissions.requiresNode(AdminPermissions.NAME))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    MiniMessageSuggestions.suggest(builder, ctx.getSource().getSender());
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    execute(ctx.getSource().getSender(), player,
                                            StringArgumentType.getString(ctx, "name"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Renames the active pet of the specified player.
     *
     * <p>If the name contains MiniMessage formatting tags (matched by the pattern
     * {@code <[a-zA-Z_]+>}), a {@code <reset>} tag is appended to prevent formatting
     * from leaking beyond the pet's name. The new name is set on the pet and a
     * confirmation message is sent to the admin, showing the rendered name.
     *
     * @param sender   the command sender (admin) who issued the rename
     * @param petOwner the player whose active pet will be renamed
     * @param name     the new name for the pet, potentially containing MiniMessage tags
     */
    private void execute(CommandSender sender, Player petOwner, String name) {
        String lang = Locale.getCommandSenderLanguage(sender);

        if (!MyPetApi.getPetManager().hasActivePet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPet(petOwner);

        StringBuilder nameBuilder = new StringBuilder(name);
        Pattern regex = Pattern.compile("<[a-zA-Z_]+>");
        Matcher regexMatcher = regex.matcher(nameBuilder.toString());
        if (regexMatcher.find()) {
            nameBuilder.append("<reset>");
        }

        pet.setPetName(nameBuilder.toString());
        sender.sendMessage(MessageUtil.prefixed(Component.text("new name is now: ").append(Util.SANITIZED_MINIMESSAGE.deserialize(nameBuilder.toString()))));
    }
}
