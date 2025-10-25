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

package de.Keyle.MyPet.commands.settings;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSettingHealthbar implements CommandOption {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (sender instanceof Player && MyPetApi.getPlayerManager().isMyPetPlayer((Player) sender)) {
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer((Player) sender);
            myPetPlayer.setHealthBarActive(!myPetPlayer.isHealthBarActive());
            sender.sendMessage(Translation.getString("Message.Command.Success", sender));
            return true;
        }
        sender.sendMessage(Translation.getString("Message.Command.Fail", sender));
        return true;
    }
}