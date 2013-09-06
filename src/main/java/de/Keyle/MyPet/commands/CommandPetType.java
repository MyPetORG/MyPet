package de.Keyle.MyPet.commands;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.itemstringinterpreter.ConfigItem;
import de.Keyle.MyPet.util.locale.Locales;
import org.apache.commons.lang.WordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandPetType implements CommandExecutor, TabCompleter
{
    private static List<String> petTypeList = new ArrayList<String>();

    static
    {
        for (MyPetType petType : MyPetType.values())
        {
            petTypeList.add(petType.getTypeName());
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if (args.length < 1)
        {
            return false;
        }

        MyPetType myPetType = MyPetType.getMyPetTypeByName(args[0]);

        String lang = "en";
        if (commandSender instanceof Player)
        {
            lang = BukkitUtil.getPlayerLanguage((Player) commandSender);
        }

        if (myPetType != null)
        {
            String leashFlagString = "";
            for (LeashFlag leashFlag : MyPet.getLeashFlags(myPetType.getMyPetClass()))
            {
                leashFlagString += leashFlag.name() + ", ";
            }
            leashFlagString = leashFlagString.substring(0, leashFlagString.lastIndexOf(","));
            commandSender.sendMessage(Locales.getString("Name.LeashFlag", lang) + ": " + leashFlagString);

            String foodString = "";
            for (ConfigItem material : MyPet.getFood(myPetType.getMyPetClass()))
            {
                foodString += WordUtils.capitalizeFully(BukkitUtil.getMaterialName(material.getItem().getTypeId()).replace("_", " ")) + ", ";
            }
            foodString = foodString.substring(0, foodString.lastIndexOf(","));
            commandSender.sendMessage(Locales.getString("Name.Food", lang) + ": " + foodString);

            commandSender.sendMessage(Locales.getString("Name.HP", lang) + ": " + MyPet.getStartHP(myPetType.getMyPetClass()));
            return true;
        }
        commandSender.sendMessage(Locales.getString("Message.Command.PetType.Unknown", lang));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        return petTypeList;
    }
}