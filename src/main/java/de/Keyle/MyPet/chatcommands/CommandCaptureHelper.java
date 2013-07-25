package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.Permissions;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandCaptureHelper implements CommandExecutor, TabCompleter
{
    private static List<String> emptyList = new ArrayList<String>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if (commandSender instanceof Player)
        {
            Player player = (Player) commandSender;
            MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(player);

            if (Permissions.has(player, "MyPet.user.command.capturehelper"))
            {
                myPetPlayer.setCaptureHelperActive(!myPetPlayer.isCaptureHelperActive());
                String mode = myPetPlayer.isCaptureHelperActive() ? Locales.getString("Name.Enabled", player) : Locales.getString("Name.Disabled", player);
                player.sendMessage(Util.formatText(Locales.getString("Message.CaptureHelperMode", player), mode));
                return true;
            }
            player.sendMessage(Locales.getString("Message.NotAllowed", player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings)
    {
        return emptyList;
    }
}