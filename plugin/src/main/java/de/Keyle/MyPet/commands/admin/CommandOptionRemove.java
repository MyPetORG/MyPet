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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.event.PetRemoveEvent;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin subcommand for permanently removing a player's active pet.
 *
 * <p>Usage: {@code /petadmin remove <player>}
 *
 * <p>This command performs the following steps:
 * <ol>
 *   <li>Fires a {@link PetRemoveEvent} with source {@link PetRemoveEvent.Source#ADMIN_COMMAND}</li>
 *   <li>Clears the pet association for the player's current world group</li>
 *   <li>Deactivates the pet entity in the world</li>
 *   <li>Deletes the pet data from the repository (database)</li>
 * </ol>
 *
 * <p>This operation is irreversible -- the pet and all its data (level, skills, etc.)
 * are permanently deleted.
 *
 * <p>Requires the {@code MyPet.admin} permission.
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionRemove {

    /**
     * Builds the Brigadier command node for the {@code remove} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   remove
     *     &lt;player: player_selector&gt;
     *       (executes) -- remove the player's active pet permanently
     * </pre>
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code remove} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Remove",
                "/petadmin remove",
                CommandCategory.ADMIN,
                22,
                player -> Permissions.has(player, "MyPet.admin")
        ));

        return Commands.literal("remove")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> {
                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                    .resolve(ctx.getSource()).getFirst();
                            execute(ctx.getSource().getSender(), player);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    /**
     * Removes the specified player's active pet permanently.
     *
     * <p>Validates that the player is a registered MyPet player with an active pet,
     * then fires a removal event, clears the world group association, deactivates
     * the pet entity, and deletes the pet from the persistence repository.
     *
     * @param sender the command sender (admin) who issued the removal
     * @param player the target player whose active pet will be removed
     */
    private void execute(CommandSender sender, Player player) {
        String lang = Locale.getCommandSenderLanguage(sender);
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, player.getName())));
            return;
        }
        MyPetPlayer petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
        if (!petOwner.hasMyPet()) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, player.getName())));
            return;
        }
        MyPet myPet = petOwner.getMyPet();

        PetRemoveEvent removeEvent = new PetRemoveEvent(myPet, PetRemoveEvent.Source.ADMIN_COMMAND);
        Bukkit.getServer().getPluginManager().callEvent(removeEvent);

        myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()), null);
        MyPetApi.getMyPetManager().deactivateMyPet(myPet.getOwner(), false);
        MyPetPlugin.getInstance().getRepository().removePet(myPet.getUUID());

        sender.sendMessage(MessageUtil.prefixed(Component.text("You removed the MyPet of: ").append(Component.text(petOwner.getName()).color(NamedTextColor.YELLOW))));
    }
}
