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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetRemoveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandOptionRemove implements CommandOptionTabCompleter {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);

        if (args.length >= 1) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null || !player.isOnline()) {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", lang));
                return true;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                MyPetPlayer petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
                if (petOwner.hasMyPet()) {
                    MyPet myPet = petOwner.getMyPet();

                    MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.AdminCommand);
                    Bukkit.getServer().getPluginManager().callEvent(removeEvent);

                    myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()), null);
                    MyPetApi.getMyPetManager().deactivateMyPet(myPet.getOwner(), false);
                    MyPetApi.getRepository().removeMyPet(myPet.getUUID(), null);

                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] You removed the MyPet of: " + ChatColor.YELLOW + petOwner.getName());
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        return Collections.emptyList();
    }
}