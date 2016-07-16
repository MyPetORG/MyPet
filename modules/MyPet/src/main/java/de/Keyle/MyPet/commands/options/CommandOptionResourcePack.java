/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.commands.options;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.hooks.ResourcePackApiHook;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandOptionResourcePack implements CommandOption {
    @Override
    public boolean onCommandOption(final CommandSender sender, String[] args) {
        if (sender instanceof Player && MyPetApi.getPlayerManager().isMyPetPlayer((Player) sender)) {
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer((Player) sender);
            if (MyPetApi.getPluginHookManager().isHookActive(ResourcePackApiHook.class)) {
                myPetPlayer.setUsesResourcePack(!myPetPlayer.isUsingResourcePack());
                if (myPetPlayer.isUsingResourcePack()) {
                    sender.sendMessage(Translation.getString("Message.Command.Options.ResourcePack.Prompt", sender));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MyPetApi.getPluginHookManager().getHook(ResourcePackApiHook.class).installResourcePack((Player) sender);
                        }
                    }.runTaskLater(MyPetApi.getPlugin(), 30L);
                } else {
                    sender.sendMessage(Translation.getString("Message.Command.Options.ResourcePack.Disable", sender));
                }
            } else {
                myPetPlayer.setUsesResourcePack(false);
                sender.sendMessage(Translation.getString("Message.Command.Options.ResourcePack.NotActive", sender));
            }
            return true;
        }
        sender.sendMessage(Translation.getString("You can't use this command from server console!", sender));
        return true;
    }
}