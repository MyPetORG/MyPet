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
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandOptionName implements CommandOptionTabCompleter {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Translation.getComponent("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(Component.text(" -> ").append(Component.text("/petadmin name ").color(NamedTextColor.DARK_AQUA)).append(Component.text("<a player name>").color(NamedTextColor.RED)));
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(Translation.getComponent("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(Component.text(" -> ").append(Component.text("/petadmin name " + args[0] + " ").color(NamedTextColor.DARK_AQUA)).append(Component.text("<new name>").color(NamedTextColor.RED)));
            return false;
        }

        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);
        Player petOwner = Bukkit.getServer().getPlayer(args[0]);

        if (petOwner == null || !petOwner.isOnline()) {
            sender.sendMessage(MessageUtil.prefixed(Translation.getComponent("Message.No.PlayerOnline", lang)));
            return true;
        } else if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Translation.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return true;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (!name.isEmpty()) {
                name.append(" ");
            }
            name.append(args[i]);
        }
        Pattern regex = Pattern.compile("<[a-zA-Z_]+>");
        Matcher regexMatcher = regex.matcher(name.toString());
        if (regexMatcher.find()) {
            name.append("<reset>");
        }

        myPet.setPetName(name.toString());
        sender.sendMessage(MessageUtil.prefixed(Component.text("new name is now: ").append(Util.SANITIZED_MINIMESSAGE.deserialize(name.toString()))));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        return Collections.emptyList();
    }

    @Override
    public String getHelpCommand() {
        return "/petadmin name";
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }

    @Override
    public String getHelpDescription() {
        return "Renames a player's pet";
    }

    @Override
    public int getHelpOrder() {
        return 24;
    }
}