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
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.event.PetSendAwayEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petsendaway} command, which despawns (sends away) a player's active pet
 * without removing ownership. The pet can later be called back with {@code /petcall}.
 *
 * <p>When executed without arguments, sends away the executing player's own pet.
 * When an optional player name argument is provided (admin only), sends away the
 * specified player's pet.</p>
 *
 * <p><b>Usage:</b> {@code /petsendaway [player]}</p>
 * <p><b>Aliases:</b> {@code /petsa}, {@code /psa}</p>
 * <p><b>Permissions:</b> {@code MyPet.command.sendaway.other} — required to send away another player's pet (granted by the {@code MyPet.admin} bundle)</p>
 * <p><b>Help category:</b> {@link CommandCategory#PET PET} (priority 80)</p>
 *
 * @see PetSendAwayEvent
 */
public class CommandSendAway {

    /**
     * Registers the {@code /petsendaway} Brigadier command and its help entry.
     *
     * <p>The command tree consists of a base literal node (player-only) that sends away
     * the sender's own pet, plus an optional {@code player} argument restricted to admins
     * with tab-completion of online player names.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petsendaway")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            execute(player, player.getName(),
                                    Locale.getPlayerLanguage(player));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .requires(ctx -> {
                                    var sender = ctx.getSender();
                                    return !(sender instanceof Player p) || Permissions.has(p, AdminPermissions.SENDAWAY_OTHER);
                                })
                                .suggests((ctx, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    String targetName = StringArgumentType.getString(ctx, "player");
                                    String lang = sender instanceof Player p
                                            ? Locale.getPlayerLanguage(p)
                                            : "en_en";
                                    execute(sender, targetName, lang);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Sends your pet away",
                List.of("petsa", "psa")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.SendAway",
                "/petsendaway",
                CommandCategory.PET,
                80,
                player -> MyPetApi.getPetManager().hasActivePet(player)
        ));
    }

    /**
     * Executes the send-away logic for the specified player's pet.
     *
     * <p>Validates that the target player is a registered MyPet player, is online, and
     * has an active pet. Fires a {@link PetSendAwayEvent} which may be cancelled by
     * other plugins. Provides feedback for all states: success, already away, or dead.</p>
     *
     * @param sender     the command sender (player or console) to receive feedback messages
     * @param playerName the name of the player whose pet should be sent away
     * @param lang       the locale code for translating feedback messages
     */
    private void execute(CommandSender sender, String playerName, String lang) {
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(playerName)) {
            sender.sendMessage(Locale.getFormattedComponent("Message.No.UserHavePet", lang, playerName));
            return;
        }
        MyPetPlayer petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(playerName);
        if (petOwner != null && !petOwner.isOnline()) {
            sender.sendMessage(Locale.getComponent("Message.No.PlayerOnline", lang));
            return;
        }
        if (petOwner != null && petOwner.hasPet()) {
            Pet pet = petOwner.getPet();
            if (pet.getStatus() == PetState.Here) {
                PetSendAwayEvent event = new PetSendAwayEvent(pet);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    pet.removePet(false);
                    sender.sendMessage(MessageUtil.success(
                            Locale.getFormattedComponent(
                                    "Message.Command.SendAway.Success",
                                    petOwner,
                                    pet.getDisplayName()
                            ), false
                    ));
                }
            } else if (pet.getStatus() == PetState.Despawned) {
                sender.sendMessage(MessageUtil.info(
                        Locale.getFormattedComponent(
                                "Message.Command.SendAway.AlreadyAway",
                                petOwner,
                                pet.getDisplayName()
                        ), false
                ));
            } else if (pet.getStatus() == PetState.Dead) {
                sender.sendMessage(MessageUtil.info(
                        Locale.getFormattedComponent(
                                "Message.Action.Dead",
                                petOwner,
                                pet.getDisplayName()
                        ), false
                ));
            }
        } else {
            sender.sendMessage(Locale.getFormattedComponent("Message.No.UserHavePet", lang, playerName));
        }
    }
}
