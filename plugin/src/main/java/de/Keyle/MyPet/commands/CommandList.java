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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petlist} command (alias: {@code /plist}).
 *
 * <p>Lists all stored pets belonging to the sender, or to another player when an admin
 * provides a target name. Each pet's display name is shown as a comma-separated list
 * with hover tooltips containing pet details (via {@link de.Keyle.MyPet.api.Util#myPetToItemHover}).</p>
 *
 * <p>This command is restricted to in-game players only (no console support).</p>
 *
 * <p><b>Usage:</b> {@code /petlist [player]}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.admin} -- required to view another player's pet list</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandList {

    /**
     * Registers the {@code /petlist} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petlist")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender(), null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    execute((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "player"));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Lists your pets",
                List.of("plist")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.List",
                "/petlist",
                CommandCategory.PET,
                125,
                player -> MyPetApi.getPlayerManager().isMyPetPlayer(player)
        ));
    }

    /**
     * Executes the petlist command logic. Resolves the target pet owner, fetches all
     * their stored pets from the repository asynchronously, and sends a formatted
     * comma-separated list with hover events to the sender.
     *
     * @param sender     the player who executed the command
     * @param targetName the name of the target player whose pets to list,
     *                   or {@code null} to list the sender's own pets
     */
    private void execute(Player sender, String targetName) {
        final String lang = MyPetApi.getPlatformHelper().getPlayerLanguage(sender);

        final Player petOwner;
        if (targetName == null) {
            petOwner = sender;
        } else {
            if (Permissions.has(sender, "MyPet.admin", false)) {
                petOwner = Bukkit.getPlayer(targetName);
            } else {
                petOwner = sender;
            }
        }

        if (petOwner == null || !petOwner.isOnline()) {
            sender.sendMessage(Translation.getComponent("Message.No.PlayerOnline", lang));
            return;
        }
        final MyPetPlayer owner;
        if (MyPetApi.getPlayerManager().isMyPetPlayer(petOwner)) {
            owner = MyPetApi.getPlayerManager().getMyPetPlayer(petOwner);
        } else {
            sender.sendMessage(Translation.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName()));
            return;
        }

        if (owner != null) {
            MyPetApi.getRepository().getPets(owner).thenAccept(value -> {
                Runnable listBody = () -> {
                    if (petOwner == sender) {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.List.Yours", lang, owner.getName()));
                    } else {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.List.Player", lang, owner.getName()));
                    }
                    boolean doComma = false;
                    TextComponent.Builder messageBuilder = Component.text();
                    for (StoredMyPet mypet : value) {
                        if (doComma) {
                            messageBuilder.append(Component.text(", "));
                        }
                        messageBuilder.append(
                                mypet.getDisplayName()
                                        .hoverEvent(Util.myPetToItemHover(mypet, lang))
                        );
                        if (!doComma) {
                            doComma = true;
                        }
                    }
                    sender.sendMessage(messageBuilder.build());
                };
                if (sender instanceof Player senderPlayer) {
                    senderPlayer.getScheduler().run(MyPetApi.getPlugin(), schedTask -> listBody.run(), null);
                } else {
                    Bukkit.getServer().getGlobalRegionScheduler().run(MyPetApi.getPlugin(), schedTask -> listBody.run());
                }
            });
        }
    }
}
