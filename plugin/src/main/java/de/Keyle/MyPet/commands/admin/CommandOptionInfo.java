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
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandOptionInfo implements CommandOptionTabCompleter {

    public static final List<String> COMMAND_OPTIONS = new ArrayList<>();

    static {
        COMMAND_OPTIONS.add("item");
        COMMAND_OPTIONS.add("leashitem");
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin info " + ChatColor.RED + "<what info you want to see>");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "item":
                if (sender instanceof Player) {
                    ItemStack itemStack;
                    if (MyPetApi.getCompatUtil().isCompatible("1.9")) {
                        itemStack = ((Player) sender).getInventory().getItemInMainHand();
                    } else {
                        itemStack = ((Player) sender).getInventory().getItemInHand();
                    }
                    sender.sendMessage("See server logs for the result.");
                    if (itemStack != null && itemStack.getType() != Material.AIR) {
                        String itemString = MyPetApi.getPlatformHelper().itemstackToString(itemStack);
                        System.out.println("MyPet Info Item: " + itemString);
                    } else {
                        System.out.println("MyPet Info Item: air");
                    }
                } else {
                    sender.sendMessage("You can't use this command from server console!");
                }
                break;
            case "leashitem": {
                if (args.length >= 2) {
                    try {
                        MyPetType type = MyPetType.byName(args[1], true);
                        ConfigItem configItem = MyPetApi.getMyPetInfo().getLeashItem(type);
                        ItemStack configItemStack = configItem.getItem();
                        String itemString = "air";
                        if (configItemStack != null) {
                            itemString = MyPetApi.getPlatformHelper().itemstackToString(configItemStack);
                        }
                        System.out.println("MyPet Leash Item (" + type + "): " + itemString);
                        if (sender instanceof Player) {
                            ((Player) sender).getInventory().addItem(configItemStack);
                        }
                    } catch (MyPetTypeNotFoundException e) {
                        sender.sendMessage(Translation.getString("Message.Command.PetType.Unknown", sender));
                    }
                } else {
                    sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
                    sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin info leashitem " + ChatColor.RED + "<pet type>");
                }
                break;
            }
            default:
                sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin info " + ChatColor.RED + "<what info you want to see>");
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length > 2) {
            return Collections.emptyList();
        } else {
            return filterTabCompletionResults(COMMAND_OPTIONS, strings[1]);
        }
    }
}