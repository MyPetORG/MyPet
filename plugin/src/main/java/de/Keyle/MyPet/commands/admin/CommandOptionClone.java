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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.InactiveMyPet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandOptionClone implements CommandOptionTabCompleter {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);
        Player oldOwner = Bukkit.getPlayer(args[0]);
        if (oldOwner == null || !oldOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", lang));
            return true;
        }
        final Player newOwner = Bukkit.getPlayer(args[1]);
        if (newOwner == null || !newOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", lang));
            return true;
        }

        if (!MyPetApi.getPlayerManager().isMyPetPlayer(oldOwner)) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Translation.getString("Message.No.UserHavePet", lang), oldOwner.getName()));
            return true;
        }

        MyPetPlayer oldPetOwner = MyPetApi.getPlayerManager().getMyPetPlayer(oldOwner);

        if (!oldPetOwner.hasMyPet()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Translation.getString("Message.No.UserHavePet", lang), oldOwner.getName()));
            return true;
        }

        final MyPetPlayer newPetOwner;
        if (MyPetApi.getPlayerManager().isMyPetPlayer(newOwner)) {
            newPetOwner = MyPetApi.getPlayerManager().getMyPetPlayer(newOwner);
        } else {
            newPetOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(newOwner);
        }

        if (newPetOwner.hasMyPet()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + newOwner.getName() + " has already an active MyPet!");
            return true;
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

        MyPetApi.getRepository().addMyPet(newPet, new RepositoryCallback<Boolean>() {
            @Override
            public void callback(Boolean value) {
                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(newPet);
                if (myPet.isPresent()) {
                    WorldGroup worldGroup = WorldGroup.getGroupByWorld(newPet.getOwner().getPlayer().getWorld().getName());
                    newPet.setWorldGroup(worldGroup.getName());
                    newPet.getOwner().setMyPetForWorldGroup(worldGroup, newPet.getUUID());
                    MyPetApi.getRepository().updateMyPetPlayer(newPetOwner, null);

                    newOwner.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] MyPet owned by " + newPetOwner.getName() + " successfully cloned!");
                }
            }
        });


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        if (strings.length == 3) {
            return null;
        }
        return Collections.emptyList();
    }
}