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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.util.PetInfoBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.suggestion.Suggestions;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Admin subcommand for switching which stored pet is active for a given player.
 *
 * <p>This command provides two modes of operation:
 * <ul>
 *   <li>{@code /petadmin switch <player>} -- displays a list of all the player's stored pets.
 *       When run by a player, clickable pet names are shown that auto-execute the switch command.
 *       When run from console, plain text with suggested commands is displayed.</li>
 *   <li>{@code /petadmin switch <player> <petname>} -- switches the player's active pet to the
 *       one matching the given name. The current active pet is deactivated and saved, and the
 *       new pet is activated and spawned in the world.</li>
 * </ul>
 *
 * <p>Pet names are matched case-insensitively after stripping MiniMessage formatting tags.
 * Tab completion suggests the names of all stored pets for the target player by querying
 * the repository asynchronously.
 *
 * <p>After switching, the pet's world group is updated to match the owner's current world,
 * and the entity is spawned. Various spawn result states (success, canceled, no space,
 * not allowed, dead, flying) are handled with appropriate localized messages.
 *
 * <p>Requires the {@code MyPet.admin} permission.
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionSwitch {

    /**
     * Builds the Brigadier command node for the {@code switch} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   switch
     *     &lt;player: player_selector&gt;
     *       (executes) -- show list of stored pets
     *       &lt;petname: greedy_string&gt;
     *         (executes) -- switch to the named pet
     * </pre>
     *
     * <p>The {@code petname} argument's suggestion provider asynchronously queries the
     * pet repository for the target player's stored pets and suggests their stripped names.
     * The greedy string type allows pet names containing spaces.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code switch} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Switch",
                "/petadmin switch",
                CommandCategory.ADMIN,
                36,
                player -> Permissions.has(player, "MyPet.admin", false)
        ));

        return Commands.literal("switch")
                .then(Commands.argument("player", ArgumentTypes.player())
                        // /petadmin switch <player> (show pet list)
                        .executes(ctx -> {
                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                    .resolve(ctx.getSource()).getFirst();
                            executeShowList(ctx.getSource().getSender(), player);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("petname", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    CompletableFuture<Suggestions> future = new CompletableFuture<>();
                                    try {
                                        Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                .resolve(ctx.getSource()).getFirst();
                                        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                                            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
                                            MyPetPlugin.getInstance().getRepository().getPets(petPlayer).thenAccept(pets -> {
                                                try {
                                                    for (StoredMyPet pet : pets) {
                                                        String name = Util.SANITIZED_MINIMESSAGE.stripTags(pet.getPetName());
                                                        builder.suggest(name);
                                                    }
                                                    future.complete(builder.build());
                                                } catch (Exception e) {
                                                    future.complete(builder.build());
                                                }
                                            });
                                            return future;
                                        }
                                    } catch (Exception ignored) {
                                    }
                                    future.complete(builder.build());
                                    return future;
                                })
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    String petName = StringArgumentType.getString(ctx, "petname");
                                    executeSwitch(ctx.getSource().getSender(), player, petName);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Displays a list of all stored pets for the specified player.
     *
     * <p>Validates that the player is a registered MyPet player, then delegates to
     * {@link #showPetList} for the actual display logic.
     *
     * @param sender the command sender (admin) to receive the pet list
     * @param player the target player whose stored pets will be listed
     */
    private void executeShowList(CommandSender sender, Player player) {
        String lang = Locale.getCommandSenderLanguage(sender);
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, player.getName())));
            return;
        }
        MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
        showPetList(sender, owner, player.getName());
    }

    /**
     * Asynchronously retrieves and displays the stored pets for a given pet owner.
     *
     * <p>When the sender is a {@link Player}, pet names are rendered as clickable components
     * that execute the switch command when clicked, with hover tooltips showing pet details.
     * When the sender is the console, plain text lines with command suggestions are shown.
     *
     * @param sender     the command sender to receive the pet list output
     * @param owner      the {@link MyPetPlayer} whose stored pets will be retrieved
     * @param playerName the display name of the player, used in click commands
     */
    private void showPetList(CommandSender sender, MyPetPlayer owner, String playerName) {
        String lang = Locale.getCommandSenderLanguage(sender);
        MyPetPlugin.getInstance().getRepository().getPets(owner).thenAccept(value -> {
            Runnable listBody = () -> {
                sender.sendMessage("Select the MyPet you want the player to switch to:");
                if (sender instanceof Player) {
                    TextComponent.Builder messageBuilder = Component.text();
                    boolean first = true;
                    for (StoredMyPet mypet : value) {
                        if (!first) {
                            messageBuilder.append(Component.text(", "));
                        }
                        String strippedName = Util.SANITIZED_MINIMESSAGE.stripTags(mypet.getPetName());
                        messageBuilder.append(
                                mypet.getDisplayName()
                                        .clickEvent(ClickEvent.runCommand("/petadmin switch " + playerName + " " + strippedName))
                                        .hoverEvent(PetInfoBuilder.myPetToItemHover(mypet, lang))
                        );
                        first = false;
                    }
                    sender.sendMessage(messageBuilder.build());
                } else {
                    for (StoredMyPet mypet : value) {
                        String strippedName = Util.SANITIZED_MINIMESSAGE.stripTags(mypet.getPetName());
                        sender.sendMessage(strippedName + " (" + mypet.getPetType().name() + ") -> /petadmin switch " + playerName + " " + strippedName);
                    }
                }
            };
            if (sender instanceof Player senderPlayer) {
                senderPlayer.getScheduler().run(MyPetApi.getPlugin(), folaTask -> listBody.run(), null);
            } else {
                Bukkit.getServer().getGlobalRegionScheduler().run(MyPetApi.getPlugin(), folaTask -> listBody.run());
            }
        });
    }

    /**
     * Switches the specified player's active pet to the one matching the given name.
     *
     * <p>This method asynchronously queries the repository for the player's stored pets,
     * finds one matching the given name (case-insensitive, MiniMessage tags stripped),
     * deactivates the currently active pet (if any), activates the new pet, updates the
     * world group association, and attempts to spawn the pet entity. The spawn result
     * is communicated to the sender via localized messages.
     *
     * @param sender  the command sender (admin) to receive feedback messages
     * @param player  the target player whose active pet will be switched
     * @param petName the name of the pet to switch to (matched after stripping MiniMessage tags)
     */
    private void executeSwitch(CommandSender sender, Player player, String petName) {
        String lang = Locale.getCommandSenderLanguage(sender);
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, player.getName())));
            return;
        }
        MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);

        MyPetPlugin.getInstance().getRepository().getPets(owner).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                // Find pet by name (stripped of MiniMessage tags)
                StoredMyPet newPet = null;
                for (StoredMyPet pet : pets) {
                    String strippedName = Util.SANITIZED_MINIMESSAGE.stripTags(pet.getPetName());
                    if (strippedName.equalsIgnoreCase(petName)) {
                        newPet = pet;
                        break;
                    }
                }

                if (newPet == null) {
                    sender.sendMessage(MessageUtil.prefixed(Component.text("Can't find a pet named \"" + petName + "\" for " + player.getName())));
                    return;
                }

                if (owner.hasMyPet()) {
                    MyPetApi.getMyPetManager().deactivateMyPet(owner, true);
                }

                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(newPet);
                sender.sendMessage(Locale.getComponent("Message.Command.Success", sender));
                if (myPet.isPresent()) {
                    WorldGroup worldGroup = WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName());
                    // The active world-group binding lives in the player→UUID index,
                    // not on the snapshot. activateMyPet does not persist
                    // StoredMyPet#worldGroup, so updating the snapshot here would
                    // be a no-op.
                    newPet.getOwner().setMyPetForWorldGroup(worldGroup, newPet.getUUID());

                    owner.sendMessage(Locale.getFormattedComponent("Message.MultiWorld.NowActivePet", owner, myPet.get().getDisplayName()));
                    switch (myPet.get().createEntity()) {
                        case Success:
                            sender.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", owner, myPet.get().getDisplayName()));
                            break;
                        case Canceled:
                            sender.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", owner, myPet.get().getDisplayName()));
                            break;
                        case NoSpace:
                            sender.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", owner, myPet.get().getDisplayName()));
                            break;
                        case NotAllowed:
                            sender.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", owner, myPet.get().getDisplayName()));
                            break;
                        case Dead:
                            if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                sender.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", owner, myPet.get().getDisplayName()));
                            } else {
                                sender.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", owner, myPet.get().getDisplayName(), myPet.get().getRespawnTime()));
                            }
                            break;
                        case Flying:
                            sender.sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", owner, myPet.get().getDisplayName()));
                            break;
                    }
                }
        }, null));
    }
}
