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
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Admin subcommand for forcibly changing the skilltree of another player's active pet.
 *
 * <p>Usage: {@code /petadmin skilltree <player> <skilltree>}
 *
 * <p>The skilltree argument provides tab completion that filters available skilltrees
 * based on compatibility with the target player's active pet type. Only skilltrees
 * whose {@link Skilltree#getMobTypes()} includes the pet's type are suggested and accepted.
 *
 * <p>Fires a {@link PetSelectSkilltreeEvent} with source
 * {@link PetSelectSkilltreeEvent.Source#ADMIN_COMMAND} when the skilltree is changed.
 *
 * <p>Requires the {@code MyPet.admin} permission.
 */
public class CommandOptionSkilltree {

    /**
     * Builds the Brigadier command node for the {@code skilltree} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   skilltree
     *     &lt;player: player_selector&gt;
     *       &lt;skilltree: word&gt;
     *         (executes) -- assign the named skilltree to the player's pet
     * </pre>
     *
     * <p>The {@code skilltree} argument's suggestion provider resolves the player selector,
     * looks up their active pet, and suggests only skilltrees compatible with that pet type.
     * If the player selector fails during suggestion (e.g., player not yet typed), the
     * exception is silently caught and no suggestions are provided.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code skilltree} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Skilltree",
                "/petadmin skilltree",
                CommandCategory.ADMIN,
                32,
                player -> Permissions.has(player, "MyPet.admin")
        ));

        return Commands.literal("skilltree")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("skilltree", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    try {
                                        List<Player> resolved = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                .resolve(ctx.getSource());
                                        if (!resolved.isEmpty()) {
                                            Player player = resolved.getFirst();
                                            if (MyPetApi.getPetManager().hasActivePet(player)) {
                                                Pet pet = MyPetApi.getPetManager().getPet(player);
                                                for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getSkilltrees()) {
                                                    if (skilltree.getMobTypes().contains(pet.getPetType())) {
                                                        builder.suggest(skilltree.getName());
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {
                                        // Player selector may fail during suggestion - silently skip
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    execute(ctx.getSource().getSender(), player,
                                            StringArgumentType.getString(ctx, "skilltree"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Assigns a skilltree to the specified player's active pet.
     *
     * <p>Validates that the player has an active pet, that the named skilltree exists,
     * and that it is compatible with the pet's type. If all checks pass, the skilltree
     * is set on the pet and a localized success message is sent. Otherwise, an appropriate
     * error message is sent.
     *
     * @param sender        the command sender (admin) to receive feedback messages
     * @param petOwner      the player whose active pet's skilltree will be changed
     * @param skilltreeName the name of the skilltree to assign
     */
    private void execute(CommandSender sender, Player petOwner, String skilltreeName) {
        String lang = Locale.getCommandSenderLanguage(sender);

        if (!MyPetApi.getPetManager().hasActivePet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPet(petOwner);

        if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
            Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
            if (skilltree.getMobTypes().contains(pet.getPetType()) && pet.setSkilltree(skilltree, PetSelectSkilltreeEvent.Source.ADMIN_COMMAND)) {
                sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.Skilltree.SwitchedToFor", lang, petOwner.getName(), Util.SANITIZED_MINIMESSAGE.deserialize(skilltree.getDisplayName()))));
            } else {
                sender.sendMessage(MessageUtil.prefixed(Locale.getComponent("Message.Skilltree.NotSwitched", lang)));
            }
        } else {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.Command.Skilltree.CantFindSkilltree", lang, skilltreeName)));
        }
    }
}
