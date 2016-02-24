/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.hooks.EconomyHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CommandTrade implements CommandExecutor, TabCompleter {
    protected HashMap<UUID, Offer> offers = new HashMap<>();
    private List<String> tradeList = new ArrayList<>();

    public CommandTrade() {
        tradeList.add("accept");
        tradeList.add("reject");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                return false;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                if (offers.containsKey(player.getUniqueId())) {
                    Offer offer = offers.get(player.getUniqueId());
                    Player owner = Bukkit.getServer().getPlayer(offer.getOwner());
                    if (owner == null || !owner.isOnline()) {
                        sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.PetUnavailable", player));
                        offers.remove(player.getUniqueId());
                        return true;
                    }

                    if (!Permissions.has(player, "MyPet.user.command.trade.recieve", false)) {
                        sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.NoPermission", player));
                        owner.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.Reject", owner), player.getName(), offer.getPet().getPetName()));
                        offers.remove(player.getUniqueId());
                        return true;
                    }

                    if (MyPetApi.getPlayerList().isMyPetPlayer(owner)) {
                        final MyPetPlayer oldOwner = MyPetApi.getPlayerList().getMyPetPlayer(owner);
                        if (!oldOwner.hasMyPet() || oldOwner.getMyPet() != offer.getPet()) {
                            sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.PetUnavailable", player));
                            offers.remove(player.getUniqueId());
                            return true;
                        }
                        if (MyPetApi.getPlayerList().isMyPetPlayer(player) && MyPetApi.getMyPetList().hasActiveMyPet(player)) {
                            sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.HasPet", player));
                            return true;
                        }

                        if (!player.getWorld().equals(owner.getWorld()) || player.getLocation().distanceSquared(owner.getLocation()) > 100) {
                            sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Reciever.Distance", player), owner.getName()));
                            return true;
                        }

                        if (offer.getPrice() > 0) {
                            if (!EconomyHook.transfer(player, owner, offer.getPrice())) {
                                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Reciever.NotEnoughMoney", player), EconomyHook.getEconomy().format(offer.getPrice())));
                                return true;
                            }
                        }

                        offers.remove(player.getUniqueId());

                        final MyPetPlayer newOwner = MyPetApi.getPlayerList().isMyPetPlayer(player) ? MyPetApi.getPlayerList().getMyPetPlayer(player) : MyPetApi.getPlayerList().registerMyPetPlayer(player);
                        final String worldGroup = offer.getPet().getWorldGroup();

                        MyPetApi.getMyPetList().deactivateMyPet(oldOwner, false);
                        final MyPet pet = MyPetApi.getMyPetList().getInactiveMyPetFromMyPet(offer.getPet());

                        final Repository repo = MyPetApi.getRepository();
                        repo.removeMyPet(pet, new RepositoryCallback<Boolean>() {
                            @Override
                            public void callback(Boolean value) {
                                pet.setOwner(newOwner);
                                repo.addMyPet(pet, null);
                                ActiveMyPet myPet = MyPetApi.getMyPetList().activateMyPet(pet);

                                oldOwner.setMyPetForWorldGroup(worldGroup, null);
                                newOwner.setMyPetForWorldGroup(worldGroup, pet.getUUID());
                                repo.updateMyPetPlayer(oldOwner, null);
                                repo.updateMyPetPlayer(newOwner, null);

                                if (myPet != null) {

                                    newOwner.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Reciever.Success", newOwner), oldOwner.getName(), myPet.getPetName()));
                                    oldOwner.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.Success", oldOwner), newOwner.getName(), myPet.getPetName()));

                                    switch (myPet.createEntity()) {
                                        case Canceled:
                                            newOwner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", newOwner), myPet.getPetName()));
                                            break;
                                        case NoSpace:
                                            newOwner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", newOwner), myPet.getPetName()));
                                            break;
                                        case NotAllowed:
                                            newOwner.sendMessage(Translation.getString("Message.No.AllowedHere", newOwner).replace("%petname%", myPet.getPetName()));
                                            break;
                                        case Dead:
                                            newOwner.sendMessage(Translation.getString("Message.Spawn.Respawn.In", newOwner).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime()));
                                            break;
                                    }
                                } else {
                                    myPet.getOwner().sendMessage(Translation.getString("Message.Command.Trade.Reciever.Error", newOwner));
                                }
                            }
                        });
                    } else {
                        sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.PetUnavailable", player));
                        offers.remove(player.getUniqueId());
                        return true;
                    }
                } else {
                    sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.NoOffer", player));
                }
                return true;

            } else if (args[0].equalsIgnoreCase("reject")) {
                if (offers.containsKey(player.getUniqueId())) {
                    Offer offer = offers.get(player.getUniqueId());
                    Player owner = Bukkit.getServer().getPlayer(offer.getOwner());
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.Reject", owner), player.getName(), offer.getPet().getPetName()));
                    }
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Reciever.Reject", player), offer.getRecieverName()));
                    offers.remove(player.getUniqueId());
                } else {
                    sender.sendMessage(Translation.getString("Message.Command.Trade.Reciever.NoOffer", player));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("cancel")) {
                UUID ownerUUID = player.getUniqueId();
                for (Offer offer : offers.values()) {
                    if (offer.getOwner().equals(ownerUUID)) {
                        offers.remove(offer.getReciever());
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.Cancel", player), offer.getRecieverName()));
                        Player reciever = Bukkit.getPlayer(offer.getReciever());
                        if (reciever != null && reciever.isOnline()) {
                            reciever.sendMessage(Translation.getString("Message.Command.Trade.Reciever.PetUnavailable", player));
                        }
                        return true;
                    }
                }
                sender.sendMessage(Translation.getString("Message.Command.Trade.Owner.NoOffer", player));
                return true;
            } else {
                if (!Permissions.has((Player) sender, "MyPet.user.command.trade.offer", false)) {
                    player.sendMessage(Translation.getString("Message.No.Allowed", player));
                    return true;
                }
                if (MyPetApi.getMyPetList().hasActiveMyPet(player)) {
                    ActiveMyPet myPet = MyPetApi.getMyPetList().getMyPet(player);

                    Player reciever = Bukkit.getPlayer(args[0]);
                    if (reciever == null) {
                        sender.sendMessage(Translation.getString("Message.No.PlayerOnline", player));
                        return true;
                    }

                    if (offers.containsKey(reciever.getUniqueId())) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.OpenOffer", player), reciever.getName()));
                        return true;
                    }

                    if (reciever.equals(player)) {
                        sender.sendMessage(Translation.getString("Message.Command.Trade.Owner.Yourself", player));
                        return true;
                    }

                    double price = 0;

                    if (args.length >= 2) {
                        if (EconomyHook.canUseEconomy()) {
                            if (Util.isDouble(args[1])) {
                                price = Double.parseDouble(args[1]);
                            } else {
                                reciever.sendMessage(Translation.getString("Message.Command.Trade.Owner.InvalidPrice", player));
                                return true;
                            }
                        } else {
                            reciever.sendMessage(Translation.getString("Message.No.Economy", player));
                            return true;
                        }
                    }

                    Offer offer = new Offer(price, myPet, player.getUniqueId(), reciever.getUniqueId(), reciever.getName());
                    offers.put(reciever.getUniqueId(), offer);
                    if (price > 0) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.Offer.Price", player), myPet.getPetName(), reciever.getName(), EconomyHook.getEconomy().format(price)));
                        reciever.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Reciever.Offer.Price", reciever), player.getName(), EconomyHook.getEconomy().format(price)));
                    } else {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Owner.Offer", player), myPet.getPetName(), reciever.getName()));
                        reciever.sendMessage(Util.formatText(Translation.getString("Message.Command.Trade.Reciever.Offer", reciever), player.getName()));
                    }
                    FancyMessage petMessage = new FancyMessage(" »» ")
                            .then(myPet.getPetName())
                            .itemTooltip(Util.myPetToItemTooltip(myPet, MyPetApi.getBukkitHelper().getPlayerLanguage(reciever)))
                            .command("/pettrade accept");
                    MyPetApi.getBukkitHelper().sendMessageRaw(reciever, petMessage.toJSONString());
                    return true;
                } else {
                    sender.sendMessage(Translation.getString("Message.No.HasPet", player));
                    return true;
                }
            }
        }

        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player) {
            if (offers.containsKey(((Player) sender).getUniqueId())) {
                return tradeList;
            }
            return null;
        }
        return CommandAdmin.EMPTY_LIST;
    }

    private class Offer {
        double price = 0;
        ActiveMyPet pet;
        UUID owner;
        UUID reciever;
        String recieverName;

        public Offer(double price, ActiveMyPet pet, UUID owner, UUID reciever, String recieverName) {
            this.price = price;
            this.pet = pet;
            this.owner = owner;
            this.reciever = reciever;
            this.recieverName = recieverName;
        }

        public double getPrice() {
            return price;
        }

        public ActiveMyPet getPet() {
            return pet;
        }

        public UUID getOwner() {
            return owner;
        }

        public UUID getReciever() {
            return reciever;
        }

        public String getRecieverName() {
            return recieverName;
        }
    }
}