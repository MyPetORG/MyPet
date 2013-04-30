package de.Keyle.MyPet.chatcommands;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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

        if (myPetType != null)
        {
            String leashFlagString = "";
            for (LeashFlag leashFlag : MyPet.getLeashFlags(myPetType.getMyPetClass()))
            {
                leashFlagString += leashFlag.name() + ", ";
            }
            leashFlagString = leashFlagString.substring(0, leashFlagString.lastIndexOf(","));
            commandSender.sendMessage("Leash Flags: " + leashFlagString);


            String foodString = "";
            for (Material material : MyPet.getFood(myPetType.getMyPetClass()))
            {
                foodString += material.name() + ", ";
            }
            foodString = foodString.substring(0, foodString.lastIndexOf(","));
            commandSender.sendMessage("Food: " + foodString);

            commandSender.sendMessage("Start HP: " + MyPet.getStartHP(myPetType.getMyPetClass()));
            return true;
        }
        commandSender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_UnknownPetType")));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        return petTypeList;
    }
}
