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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CommandOptionSwitch implements CommandOptionTabCompleter {

    @Override
    public boolean onCommandOption(final CommandSender sender, String[] parameter) {
        boolean show = true;
        MyPetPlayer o = null;
        UUID petUUID = null;
        final String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);

        if (parameter.length == 0) {
            if (sender instanceof Player petOwner) {
                if (MyPetApi.getPlayerManager().isMyPetPlayer(petOwner)) {
                    o = MyPetApi.getPlayerManager().getMyPetPlayer(petOwner);
                }
            } else {
                sender.sendMessage("You can't use this command from server console!");
                return true;
            }
        } else if (parameter.length == 1) {
            Player player = Bukkit.getPlayer(parameter[0]);
            if (player == null || !player.isOnline()) {
                sender.sendMessage(de.Keyle.MyPet.util.MessageUtil.prefixed(Translation.getComponent("Message.No.PlayerOnline", lang)));
                return true;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                o = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            }
            if (o == null) {
                sender.sendMessage(de.Keyle.MyPet.util.MessageUtil.prefixed(Translation.getFormattedComponent("Message.No.UserHavePet", lang, player.getName())));
            }
        } else if (parameter.length == 2) {
            show = false;
            try {
                o = MyPetApi.getPlayerManager().getMyPetPlayer(UUID.fromString(parameter[0]));
                petUUID = UUID.fromString(parameter[1]);
            } catch (IllegalArgumentException ignored) {
            }
        }

        final MyPetPlayer owner = o;

        if (show && owner != null) {
            MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<>() {
                @Override
                public void callback(List<StoredMyPet> value) {
                    sender.sendMessage("Select the MyPet you want the player to switch to:");
                    if (sender instanceof Player) {
                        boolean doComma = false;

                        TextComponent.Builder messageBuilder = Component.text();
                        for (StoredMyPet mypet : value) {

                            if (doComma) {
                                messageBuilder.append(Component.text(", "));
                            }
                            messageBuilder.append(
                                    mypet.getDisplayName()
                                            .clickEvent(ClickEvent.runCommand("/petadmin switch " + owner.getUniqueId() + " " + mypet.getUUID()))
                                            .hoverEvent(Util.myPetToItemHover(mypet, lang))
                            );
                            if (!doComma) {
                                doComma = true;
                            }
                        }
                        if (sender instanceof Player player) {
                            player.sendMessage(messageBuilder.build());
                        }
                    } else {
                        for (StoredMyPet mypet : value) {
                            sender.sendMessage(Util.SANITIZED_MINIMESSAGE.stripTags(mypet.getPetName()) + "(" + mypet.getPetType().name() + ") -> /petadmin switch " + owner.getUniqueId() + " " + mypet.getUUID());
                        }
                    }
                }
            });

        } else if (!show && owner != null && petUUID != null) {
            MyPetApi.getRepository().getMyPet(petUUID, new RepositoryCallback<>() {
                @Override
                public void callback(StoredMyPet newPet) {
                    if (newPet != null) {
                        if (owner.hasMyPet()) {
                            MyPetApi.getMyPetManager().deactivateMyPet(owner, true);
                        }

                        Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(newPet);
                        sender.sendMessage(Translation.getComponent("Message.Command.Success", sender));
                        if (myPet.isPresent()) {

                            WorldGroup worldGroup = WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName());
                            newPet.setWorldGroup(worldGroup.getName());
                            newPet.getOwner().setMyPetForWorldGroup(worldGroup, newPet.getUUID());

                            owner.sendMessage(Translation.getFormattedComponent("Message.MultiWorld.NowActivePet", owner, myPet.get().getDisplayName()));
                            switch (myPet.get().createEntity()) {
                                case Success:
                                    sender.sendMessage(Translation.getFormattedComponent("Message.Command.Call.Success", owner, myPet.get().getDisplayName()));
                                    break;
                                case Canceled:
                                    sender.sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", owner, myPet.get().getDisplayName()));
                                    break;
                                case NoSpace:
                                    sender.sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", owner, myPet.get().getDisplayName()));
                                    break;
                                case NotAllowed:
                                    sender.sendMessage(Translation.getFormattedComponent("Message.No.AllowedHere", owner, myPet.get().getDisplayName()));
                                    break;
                                case Dead:
                                    if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                        sender.sendMessage(Translation.getFormattedComponent("Message.Call.Dead", owner, myPet.get().getDisplayName()));
                                    } else {
                                        sender.sendMessage(Translation.getFormattedComponent("Message.Call.Dead.Respawn", owner, myPet.get().getDisplayName(), myPet.get().getRespawnTime()));
                                    }
                                    break;
                                case Flying:
                                    sender.sendMessage(Translation.getFormattedComponent("Message.Spawn.Flying", owner, myPet.get().getDisplayName()));
                                    break;
                            }
                        }
                    }
                }
            });

        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getHelpCommand() {
        return "/petadmin switch";
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }

    @Override
    public String getHelpDescription() {
        return "Switches a player's active pet";
    }

    @Override
    public int getHelpOrder() {
        return 36;
    }
}