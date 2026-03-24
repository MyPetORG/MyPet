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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Admin subcommand that clones an existing pet from one player to another.
 *
 * <p>Usage: {@code /petadmin clone <player> <target>}</p>
 *
 * <p>Copies all pet data from the source player's active pet (type, name, experience, health,
 * saturation, respawn time, NBT info, skilltree, and skill data) to a new pet owned by the
 * target player. The target player must not already have an active pet.</p>
 *
 * <p>Requires the {@code MyPet.admin} permission.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionClone {

    /**
     * Builds the Brigadier command tree for the {@code clone} subcommand.
     *
     * <p>Tree structure: {@code clone <player> <target>} where both arguments are
     * single-player selectors resolved at execution time.</p>
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code clone} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Clone",
                "/petadmin clone",
                CommandCategory.ADMIN,
                34,
                player -> Permissions.has(player, "MyPet.admin", false)
        ));

        return Commands.literal("clone")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(ctx -> {
                                    List<Player> source = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource());
                                    List<Player> target = ctx.getArgument("target", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource());
                                    execute(ctx.getSource().getSender(), source.getFirst(), target.getFirst());
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Executes the pet cloning logic.
     *
     * <p>Validates that the source player has an active pet and that the target player does not.
     * Creates a new {@link InactiveMyPet} with all properties copied from the source pet,
     * persists it to the repository, activates it for the target player, and assigns it to
     * the target's current world group.</p>
     *
     * @param sender   the command sender (for feedback messages)
     * @param oldOwner the player whose active pet will be cloned
     * @param newOwner the player who will receive the cloned pet
     */
    private void execute(CommandSender sender, Player oldOwner, Player newOwner) {
        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);

        if (!MyPetApi.getPlayerManager().isMyPetPlayer(oldOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Translation.getFormattedComponent("Message.No.UserHavePet", lang, oldOwner.getName())));
            return;
        }

        MyPetPlayer oldPetOwner = MyPetApi.getPlayerManager().getMyPetPlayer(oldOwner);

        if (!oldPetOwner.hasMyPet()) {
            sender.sendMessage(MessageUtil.prefixed(Translation.getFormattedComponent("Message.No.UserHavePet", lang, oldOwner.getName())));
            return;
        }

        final MyPetPlayer newPetOwner;
        if (MyPetApi.getPlayerManager().isMyPetPlayer(newOwner)) {
            newPetOwner = MyPetApi.getPlayerManager().getMyPetPlayer(newOwner);
        } else {
            newPetOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(newOwner);
        }

        if (newPetOwner.hasMyPet()) {
            sender.sendMessage(MessageUtil.prefixed(Component.text(newOwner.getName() + " has already an active MyPet!")));
            return;
        }

        MyPet oldPet = oldPetOwner.getMyPet();
        final InactiveMyPet newPet = new InactiveMyPet(newPetOwner);
        newPet.setPetName(oldPet.getPetName());
        newPet.setWorldGroup(oldPet.getWorldGroup());
        newPet.setExp(oldPet.getExperience().getExp());
        newPet.setHealth(oldPet.getHealth());
        newPet.setSaturation(oldPet.getSaturation());
        newPet.setRespawnTime(oldPet.getRespawnTime());
        newPet.setInfo(oldPet.getInfo());
        newPet.setPetType(oldPet.getPetType());
        newPet.setSkilltree(oldPet.getSkilltree());
        newPet.setSkills(oldPet.getSkillInfo());

        MyPetSaveEvent event = new MyPetSaveEvent(newPet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        MyPetApi.getRepository().addMyPet(newPet, new RepositoryCallback<>() {
            @Override
            public void callback(Boolean added) {
                if (!added) {
                    sender.sendMessage(MessageUtil.prefixed(Component.text("Failed to clone MyPet!")));
                    return;
                }
                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(newPet);
                if (myPet.isPresent()) {
                    WorldGroup worldGroup = WorldGroup.getGroupByWorld(newPet.getOwner().getPlayer().getWorld().getName());
                    newPet.setWorldGroup(worldGroup.getName());
                    newPet.getOwner().setMyPetForWorldGroup(worldGroup, newPet.getUUID());
                    MyPetApi.getRepository().updateMyPetPlayer(newPetOwner, null);

                    sender.sendMessage(MessageUtil.prefixed(Component.text("MyPet successfully cloned to " + newPetOwner.getName() + "!")));
                }
            }
        });
    }
}
