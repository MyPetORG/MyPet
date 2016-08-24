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

package de.Keyle.MyPet.commands;


import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.LeashFlag;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.apache.commons.lang.WordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandPetType implements CommandExecutor, TabCompleter {
    private static List<String> petTypeList = new ArrayList<>();

    static {
        for (MyPetType petType : MyPetType.values()) {
            petTypeList.add(petType.name());
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String lang = "en";
        if (commandSender instanceof Player) {
            lang = MyPetApi.getPlatformHelper().getPlayerLanguage((Player) commandSender);
        }

        try {
            MyPetType myPetType = MyPetType.byName(args[0]);

            String leashFlagString = "";
            for (LeashFlag leashFlag : MyPetApi.getMyPetInfo().getLeashFlags(myPetType)) {
                leashFlagString += leashFlag.name() + ", ";
            }
            leashFlagString = leashFlagString.substring(0, leashFlagString.lastIndexOf(","));
            commandSender.sendMessage(Translation.getString("Name.LeashFlag", lang) + ": " + leashFlagString);

            String foodString = "";
            for (ConfigItem material : MyPetApi.getMyPetInfo().getFood(myPetType)) {
                foodString += WordUtils.capitalizeFully(MyPetApi.getPlatformHelper().getMaterialName(material.getItem().getTypeId()).replace("_", " ")) + ", ";
            }
            foodString = foodString.substring(0, foodString.lastIndexOf(","));
            commandSender.sendMessage(Translation.getString("Name.Food", lang) + ": " + foodString);

            commandSender.sendMessage(Translation.getString("Name.HP", lang) + ": " + MyPetApi.getMyPetInfo().getStartHP(myPetType));
        } catch (MyPetTypeNotFoundException e) {
            commandSender.sendMessage(Translation.getString("Message.Command.PetType.Unknown", lang));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return petTypeList;
    }
}