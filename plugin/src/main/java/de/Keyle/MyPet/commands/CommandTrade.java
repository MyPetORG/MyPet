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
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.util.PetInfoBuilder;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.repository.Repository;
import de.Keyle.MyPet.api.util.locale.Locale;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles the {@code /pettrade} command, which allows players to trade their active pet
 * with another online player, optionally for a price.
 *
 * <p>Registered aliases: {@code /pett}, {@code /pt}</p>
 *
 * <h3>Subcommands</h3>
 * <ul>
 *   <li>{@code /pettrade <player> [price]} -- offer your active pet to another player, optionally setting an economy price</li>
 *   <li>{@code /pettrade accept} -- accept a pending trade offer directed at you</li>
 *   <li>{@code /pettrade reject} -- reject a pending trade offer directed at you</li>
 *   <li>{@code /pettrade cancel} -- cancel an outgoing trade offer you previously made</li>
 * </ul>
 *
 * <h3>Permissions</h3>
 * <ul>
 *   <li>{@code MyPet.command.trade.offer.<PetType>} -- required to offer a pet of the given type</li>
 *   <li>{@code MyPet.command.trade.receive.<PetType>} -- required to accept a pet of the given type</li>
 * </ul>
 *
 * <p>Both players must be in the same world and within 10 blocks of each other to complete
 * a trade. If a price is set, a Vault-compatible economy plugin must be available and the
 * receiver must have sufficient funds. The command is only usable by players (not the console)
 * and is disabled in worlds where MyPet is disabled.</p>
 */
/*
 * Multi-Pet Phase 2 (MyPetORG/MyPet#1435): this command resolves the player to a
 * single Pet via the manager. That has no unambiguous answer once a player can
 * have several out -- it needs the optional pet-name argument the issue calls for,
 * so it is deliberately left alone until that argument exists.
 */
public class CommandTrade {

    /**
     * Shared offer table. Static so the GUI trade flow ({@code PetTradeService}) can
     * reach the same accept/reject state the command uses.
     */
    static final HashMap<UUID, Offer> OFFERS = new HashMap<>();

    /**
     * Registers the {@code /pettrade} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("pettrade")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .then(Commands.literal("accept")
                                .executes(ctx -> {
                                    executeAccept((Player) ctx.getSource().getSender());
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("reject")
                                .executes(ctx -> {
                                    executeReject((Player) ctx.getSource().getSender());
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("cancel")
                                .executes(ctx -> {
                                    executeCancel((Player) ctx.getSource().getSender());
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    executeOffer((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "player"), 0);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            executeOffer((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "player"), DoubleArgumentType.getDouble(ctx, "price"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .build(),
                "Trade your pet with another player",
                List.of("pett", "pt")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Trade",
                "/pettrade",
                null,
                140,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && (Permissions.has(player, "MyPet.command.trade.offer")
                        || Permissions.has(player, "MyPet.command.trade.receive"))
        ));
    }

    /**
     * Handles {@code /pettrade accept}. Validates that a pending offer exists for the player,
     * checks permissions, proximity, and economy requirements, then transfers pet ownership
     * from the offer's owner to this player.
     *
     * @param player the player accepting the trade offer
     */
    private void executeAccept(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        if (OFFERS.containsKey(player.getUniqueId())) {
            Offer offer = OFFERS.get(player.getUniqueId());
            Player owner = Bukkit.getServer().getPlayer(offer.owner());
            if (owner == null || !owner.isOnline()) {
                player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                OFFERS.remove(player.getUniqueId());
                return;
            }

            if (!Permissions.has(player, "MyPet.command.trade.receive." + offer.pet().getPetType().name())) {
                player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.NoPermission", player));
                owner.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.Reject", owner, player.getName(), offer.pet().getDisplayName()));
                OFFERS.remove(player.getUniqueId());
                return;
            }

            if (MyPetApi.getPlayerManager().isMyPetPlayer(owner)) {
                final MyPetPlayer oldOwner = MyPetApi.getPlayerManager().getMyPetPlayer(owner);
                if (!oldOwner.hasPet() || oldOwner.getPet() != offer.pet()) {
                    player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                    OFFERS.remove(player.getUniqueId());
                    return;
                }
                if (MyPetApi.getPlayerManager().isMyPetPlayer(player) && MyPetApi.getPetManager().hasActivePet(player)) {
                    player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.HasPet", player));
                    return;
                }

                if (!player.getWorld().equals(owner.getWorld()) || player.getLocation().distanceSquared(owner.getLocation()) > 100) {
                    player.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Receiver.Distance", player, owner.getName()));
                    return;
                }

                if (offer.price() > 0) {
                    if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
                        player.sendMessage(Locale.getComponent("Message.No.Economy", player));
                        return;
                    }
                    if (!MyPetApi.getHookHelper().getEconomy().transfer(player, owner, offer.price())) {
                        player.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Receiver.NotEnoughMoney", player, MyPetApi.getHookHelper().getEconomy().format(offer.price())));
                        return;
                    }
                }

                OFFERS.remove(player.getUniqueId());

                final MyPetPlayer newOwner = MyPetApi.getPlayerManager().isMyPetPlayer(player) ? MyPetApi.getPlayerManager().getMyPetPlayer(player) : MyPetApi.getPlayerManager().registerMyPetPlayer(player);
                final String worldGroup = offer.pet().getWorldGroup();

                MyPetApi.getPetManager().deactivatePet(oldOwner, oldOwner.getPet(), false);
                final PersistedPet originalPet = MyPetApi.getPetManager().snapshot(offer.pet());

                final Repository repo = MyPetPlugin.getInstance().getRepository();
                repo.removePet(originalPet).thenAccept(value -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        PersistedPet persistedPet = originalPet.withOwner(newOwner);
                        PetSaveEvent event = new PetSaveEvent(persistedPet);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                        repo.addPet(persistedPet);
                        Optional<Pet> pet = MyPetApi.getPetManager().activatePet(persistedPet);

                        oldOwner.setPetForWorldGroup(worldGroup, null);
                        newOwner.setPetForWorldGroup(worldGroup, persistedPet.getUUID());
                        repo.updateMyPetPlayer(oldOwner);
                        repo.updateMyPetPlayer(newOwner);
                        // New owner is covered by the addPet cache hook; re-derive the
                        // losing side, who may still own other stored pets.
                        MyPetApi.getPetManager().refreshOwnership(oldOwner);

                        if (pet.isPresent()) {

                            newOwner.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Receiver.Success", newOwner, oldOwner.getName(), pet.get().getDisplayName()));
                            oldOwner.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.Success", oldOwner, newOwner.getName(), pet.get().getDisplayName()));

                            switch (pet.get().createEntity()) {
                                case Canceled:
                                    newOwner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", newOwner, pet.get().getDisplayName()));
                                    break;
                                case NoSpace:
                                    newOwner.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", newOwner, pet.get().getDisplayName()));
                                    break;
                                case NotAllowed:
                                    newOwner.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", newOwner, pet.get().getDisplayName()));
                                    break;
                                case Dead:
                                    if (!MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
                                        newOwner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", newOwner, pet.get().getDisplayName(), pet.get().getRespawnTime()));
                                    }
                                    break;
                                case Spectator:
                                    newOwner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Spectator", newOwner, pet.get().getDisplayName()));
                                    break;
                            }
                        } else {
                            newOwner.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.Error", newOwner));
                        }
                }, null));
            } else {
                player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                OFFERS.remove(player.getUniqueId());
            }
        } else {
            player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.NoOffer", player));
        }
    }

    /**
     * Handles {@code /pettrade reject}. Notifies the offering player that their trade was
     * rejected and removes the pending offer.
     *
     * @param player the player rejecting the trade offer
     */
    private void executeReject(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        if (OFFERS.containsKey(player.getUniqueId())) {
            Offer offer = OFFERS.get(player.getUniqueId());
            Player owner = Bukkit.getServer().getPlayer(offer.owner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.Reject", owner, player.getName(), offer.pet().getDisplayName()));
            }
            player.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Receiver.Reject", player, offer.ownerName()));
            OFFERS.remove(player.getUniqueId());
        } else {
            player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.NoOffer", player));
        }
    }

    /**
     * Handles {@code /pettrade cancel}. Cancels the outgoing trade offer made by this player
     * and notifies the intended receiver that the offer is no longer available.
     *
     * @param player the player cancelling their outgoing trade offer
     */
    private void executeCancel(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        UUID ownerUUID = player.getUniqueId();
        for (Offer offer : OFFERS.values()) {
            if (offer.owner().equals(ownerUUID)) {
                OFFERS.remove(offer.receiver());
                player.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.Cancel", player, offer.receiverName()));
                Player receiver = Bukkit.getPlayer(offer.receiver());
                if (receiver != null && receiver.isOnline()) {
                    receiver.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                }
                return;
            }
        }
        player.sendMessage(Locale.getComponent("Message.Command.Trade.Owner.NoOffer", player));
    }

    /**
     * Handles {@code /pettrade <player> [price]}. Resolves the active pet and the target player,
     * then delegates to {@link #beginTrade(Player, Player, Pet, double)}.
     */
    private void executeOffer(Player player, String targetName, double price) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        if (!MyPetApi.getPetManager().hasActivePet(player)) {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
            return;
        }

        Player receiver = Bukkit.getPlayer(targetName);
        if (receiver == null) {
            player.sendMessage(Locale.getComponent("Message.No.PlayerOnline", player));
            return;
        }

        beginTrade(player, receiver, MyPetApi.getPetManager().getPet(player), price);
    }

    /**
     * Canonical trade-offer entry point. Validates permissions, existing-offer/self-trade
     * conflicts, and economy availability, then records the offer and prompts both players.
     * Used by both {@code /pettrade} and the GUI trade flow.
     */
    public static void beginTrade(Player from, Player target, Pet pet, double price) {
        if (!Permissions.has(from, "MyPet.command.trade.offer." + pet.getPetType().name())) {
            from.sendMessage(Locale.getComponent("Message.No.Allowed", from));
            return;
        }

        if (OFFERS.containsKey(target.getUniqueId())) {
            from.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.OpenOffer", from, target.getName()));
            return;
        }

        if (target.equals(from)) {
            from.sendMessage(Locale.getComponent("Message.Command.Trade.Owner.Yourself", from));
            return;
        }

        if (price > 0) {
            if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
                from.sendMessage(Locale.getComponent("Message.No.Economy", from));
                return;
            }
        }

        Offer offer = new Offer(price, pet, from.getUniqueId(), target.getUniqueId(), target.getName(), from.getName());
        OFFERS.put(target.getUniqueId(), offer);
        if (price > 0) {
            from.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.Offer.Price", from, pet.getDisplayName(), target.getName(), MyPetApi.getHookHelper().getEconomy().format(price)));
            target.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Receiver.Offer.Price", target, from.getName(), MyPetApi.getHookHelper().getEconomy().format(price)));
        } else {
            from.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Owner.Offer", from, pet.getDisplayName(), target.getName()));
            target.sendMessage(Locale.getFormattedComponent("Message.Command.Trade.Receiver.Offer", target, from.getName()));
        }

        target.sendMessage(
                Component.text(" »» ").append(pet.getDisplayName())
                        .hoverEvent(PetInfoBuilder.petToItemHover(pet, Locale.getPlayerLanguage(target)))
                        .clickEvent(ClickEvent.runCommand("/pettrade accept"))
        );
    }

    record Offer(double price, Pet pet, UUID owner, UUID receiver, String receiverName, String ownerName) {
    }
}
