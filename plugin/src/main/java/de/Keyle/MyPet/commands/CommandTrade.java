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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandTrade implements CommandTabCompleter {

    protected HashMap<UUID, Offer> offers = new HashMap<>();
    private List<String> tradeList = new ArrayList<>();

    public CommandTrade() {
        tradeList.add("accept");
        tradeList.add("reject");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {

            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                player.sendMessage(Translation.getComponent("Message.No.AllowedHere", player));
                return true;
            }

            if (args.length == 0) {
                return false;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                if (offers.containsKey(player.getUniqueId())) {
                    Offer offer = offers.get(player.getUniqueId());
                    Player owner = Bukkit.getServer().getPlayer(offer.owner());
                    if (owner == null || !owner.isOnline()) {
                        sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                        offers.remove(player.getUniqueId());
                        return true;
                    }

                    if (!Permissions.has(player, "MyPet.command.trade.receive." + offer.pet().getPetType().name())) {
                        sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.NoPermission", player));
                        owner.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.Reject", owner, player.getName(), offer.pet().getDisplayName()));
                        offers.remove(player.getUniqueId());
                        return true;
                    }

                    if (MyPetApi.getPlayerManager().isMyPetPlayer(owner)) {
                        final MyPetPlayer oldOwner = MyPetApi.getPlayerManager().getMyPetPlayer(owner);
                        if (!oldOwner.hasMyPet() || oldOwner.getMyPet() != offer.pet()) {
                            sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                            offers.remove(player.getUniqueId());
                            return true;
                        }
                        if (MyPetApi.getPlayerManager().isMyPetPlayer(player) && MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                            sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.HasPet", player));
                            return true;
                        }

                        if (!player.getWorld().equals(owner.getWorld()) || MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), owner.getLocation()) > 100) {
                            sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Receiver.Distance", player, owner.getName()));
                            return true;
                        }

                        if (offer.price() > 0) {
                            if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
                                player.sendMessage(Translation.getComponent("Message.No.Economy", player));
                                return true;
                            }
                            if (!MyPetApi.getHookHelper().getEconomy().transfer(player, owner, offer.price())) {
                                sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Receiver.NotEnoughMoney", player, MyPetApi.getHookHelper().getEconomy().format(offer.price())));
                                return true;
                            }
                        }

                        offers.remove(player.getUniqueId());

                        final MyPetPlayer newOwner = MyPetApi.getPlayerManager().isMyPetPlayer(player) ? MyPetApi.getPlayerManager().getMyPetPlayer(player) : MyPetApi.getPlayerManager().registerMyPetPlayer(player);
                        final String worldGroup = offer.pet().getWorldGroup();

                        MyPetApi.getMyPetManager().deactivateMyPet(oldOwner, false);
                        final StoredMyPet pet = MyPetApi.getMyPetManager().getInactiveMyPetFromMyPet(offer.pet());

                        final Repository repo = MyPetApi.getRepository();
                        repo.removeMyPet(pet, new RepositoryCallback<>() {
                            @Override
                            public void callback(Boolean value) {
                                pet.setOwner(newOwner);
                                MyPetSaveEvent event = new MyPetSaveEvent(pet);
                                Bukkit.getServer().getPluginManager().callEvent(event);
                                repo.addMyPet(pet, null);
                                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(pet);

                                oldOwner.setMyPetForWorldGroup(worldGroup, null);
                                newOwner.setMyPetForWorldGroup(worldGroup, pet.getUUID());
                                repo.updateMyPetPlayer(oldOwner, null);
                                repo.updateMyPetPlayer(newOwner, null);

                                if (myPet.isPresent()) {

                                    newOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Receiver.Success", newOwner, oldOwner.getName(), myPet.get().getDisplayName()));
                                    oldOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.Success", oldOwner, newOwner.getName(), myPet.get().getDisplayName()));

                                    switch (myPet.get().createEntity()) {
                                        case Canceled:
                                            newOwner.sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", newOwner, myPet.get().getDisplayName()));
                                            break;
                                        case NoSpace:
                                            newOwner.sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", newOwner, myPet.get().getDisplayName()));
                                            break;
                                        case NotAllowed:
                                            newOwner.sendMessage(Translation.getFormattedComponent("Message.No.AllowedHere", newOwner, myPet.get().getDisplayName()));
                                            break;
                                        case Dead:
                                            if (!Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                newOwner.sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn.In", newOwner, myPet.get().getDisplayName(), myPet.get().getRespawnTime()));
                                            }
                                            break;
                                        case Spectator:
                                            newOwner.sendMessage(Translation.getFormattedComponent("Message.Spawn.Spectator", newOwner, myPet.get().getDisplayName()));
                                            break;
                                    }
                                } else {
                                    newOwner.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.Error", newOwner));
                                }
                            }
                        });
                    } else {
                        sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                        offers.remove(player.getUniqueId());
                        return true;
                    }
                } else {
                    sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.NoOffer", player));
                }
                return true;

            } else if (args[0].equalsIgnoreCase("reject")) {
                if (offers.containsKey(player.getUniqueId())) {
                    Offer offer = offers.get(player.getUniqueId());
                    Player owner = Bukkit.getServer().getPlayer(offer.owner());
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.Reject", owner, player.getName(), offer.pet().getDisplayName()));
                    }
                    sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Receiver.Reject", player, offer.ownerName()));
                    offers.remove(player.getUniqueId());
                } else {
                    sender.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.NoOffer", player));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("cancel")) {
                UUID ownerUUID = player.getUniqueId();
                for (Offer offer : offers.values()) {
                    if (offer.owner().equals(ownerUUID)) {
                        offers.remove(offer.receiver());
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.Cancel", player, offer.receiverName()));
                        Player receiver = Bukkit.getPlayer(offer.receiver());
                        if (receiver != null && receiver.isOnline()) {
                            receiver.sendMessage(Translation.getComponent("Message.Command.Trade.Receiver.PetUnavailable", player));
                        }
                        return true;
                    }
                }
                sender.sendMessage(Translation.getComponent("Message.Command.Trade.Owner.NoOffer", player));
                return true;
            } else {
                if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                    MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);

                    if (!Permissions.has((Player) sender, "MyPet.command.trade.offer." + myPet.getPetType().name())) {
                        player.sendMessage(Translation.getComponent("Message.No.Allowed", player));
                        return true;
                    }

                    Player receiver = Bukkit.getPlayer(args[0]);
                    if (receiver == null) {
                        sender.sendMessage(Translation.getComponent("Message.No.PlayerOnline", player));
                        return true;
                    }

                    if (offers.containsKey(receiver.getUniqueId())) {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.OpenOffer", player, receiver.getName()));
                        return true;
                    }

                    if (receiver.equals(player)) {
                        sender.sendMessage(Translation.getComponent("Message.Command.Trade.Owner.Yourself", player));
                        return true;
                    }

                    double price = 0;

                    if (args.length >= 2) {
                        if (MyPetApi.getHookHelper().isEconomyEnabled()) {
                            if (Util.isDouble(args[1])) {
                                price = Double.parseDouble(args[1]);
                            } else {
                                receiver.sendMessage(Translation.getComponent("Message.Command.Trade.Owner.InvalidPrice", player));
                                return true;
                            }
                        } else {
                            sender.sendMessage(Translation.getComponent("Message.No.Economy", player));
                            return true;
                        }
                    }

                    Offer offer = new Offer(price, myPet, player.getUniqueId(), receiver.getUniqueId(), receiver.getName(), player.getName());
                    offers.put(receiver.getUniqueId(), offer);
                    if (price > 0) {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.Offer.Price", player, myPet.getDisplayName(), receiver.getName(), MyPetApi.getHookHelper().getEconomy().format(price)));
                        receiver.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Receiver.Offer.Price", receiver, player.getName(), MyPetApi.getHookHelper().getEconomy().format(price)));
                    } else {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Owner.Offer", player, myPet.getDisplayName(), receiver.getName()));
                        receiver.sendMessage(Translation.getFormattedComponent("Message.Command.Trade.Receiver.Offer", receiver, player.getName()));
                    }

                    receiver.sendMessage(
                            Component.text(" »» ").append(myPet.getDisplayName())
                                    .hoverEvent(Util.myPetToItemHover(myPet, MyPetApi.getPlatformHelper().getPlayerLanguage(receiver)))
                                    .clickEvent(ClickEvent.runCommand("/pettrade accept"))
                    );

                    return true;
                } else {
                    sender.sendMessage(Translation.getComponent("Message.No.HasPet", player));
                    return true;
                }
            }
        }

        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player && strings.length == 1) {
            if (offers.containsKey(((Player) sender).getUniqueId())) {
                return filterTabCompletionResults(tradeList, strings[0]);
            }
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return filterTabCompletionResults(playerNames, strings[0]);
        }
        return Collections.emptyList();
    }

    private record Offer(double price, MyPet pet, UUID owner, UUID receiver, String receiverName, String ownerName) {
    }

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.Trade";
    }

    @Override
    public String getHelpCommand() {
        return "/pettrade";
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return MyPetApi.getMyPetManager().hasActiveMyPet(player)
                && (Permissions.has(player, "MyPet.command.trade.offer")
                || Permissions.has(player, "MyPet.command.trade.receive"));
    }

    @Override
    public int getHelpOrder() {
        return 140;
    }
}
