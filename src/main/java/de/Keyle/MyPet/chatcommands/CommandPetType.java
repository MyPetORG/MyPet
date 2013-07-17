package de.Keyle.MyPet.chatcommands;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.Colorizer;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
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
            lang = MyPetBukkitUtil.getPlayerLanguage((Player) commandSender);
        }

        if (myPetType != null)
        {
            String leashFlagString = "";
            for (LeashFlag leashFlag : MyPet.getLeashFlags(myPetType.getMyPetClass()))
            {
                leashFlagString += leashFlag.name() + ", ";
            }
            leashFlagString = leashFlagString.substring(0, leashFlagString.lastIndexOf(","));
            commandSender.sendMessage(MyPetLocales.getString("Name.LeashFlag", lang) + ": " + leashFlagString);

            String foodString = "";
            for (int material : MyPet.getFood(myPetType.getMyPetClass()))
            {
                foodString += WordUtils.capitalizeFully(MyPetBukkitUtil.getMaterialName(material).replace("_", " ")) + ", ";
            }
            foodString = foodString.substring(0, foodString.lastIndexOf(","));
            commandSender.sendMessage(MyPetLocales.getString("Name.Food", lang) + ": " + foodString);

            commandSender.sendMessage(MyPetLocales.getString("Name.HP", lang) + ": " + MyPet.getStartHP(myPetType.getMyPetClass()));
            return true;
        }
        commandSender.sendMessage(Colorizer.setColors(MyPetLocales.getString("Message.UnknownPetType", lang)));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        return petTypeList;
    }
}