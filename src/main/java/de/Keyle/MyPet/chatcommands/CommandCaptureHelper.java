package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandCaptureHelper implements CommandExecutor, TabCompleter
{
    private static List<String> captureModeList = new ArrayList<String>();
    private static List<String> emptyList = new ArrayList<String>();

    static
    {
        captureModeList.add("normal");
        captureModeList.add("half");
        captureModeList.add("tameableonly");
        captureModeList.add("deactivate");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if (commandSender instanceof Player)
        {
            Player player = (Player) commandSender;
            MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(player);

            if (MyPetPermissions.has(player, "MyPet.user.capturehelper"))
            {
                myPetPlayer.setCaptureHelperActive(!myPetPlayer.isCaptureHelperActive());
                String mode = myPetPlayer.isCaptureHelperActive() ? MyPetLanguage.getString("Name_Enabled") : MyPetLanguage.getString("Name_Disabled");
                player.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CaptureHelperMode")).replace("%mode%", "" + mode));
                return true;
            }
            player.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotAllowed")));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings)
    {
        if (strings.length == 1 && MyPetPermissions.has((Player) sender, "MyPet.user.capturehelper"))
        {
            return captureModeList;
        }
        return emptyList;
    }
}