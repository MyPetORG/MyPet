package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetPlayer;
import static de.Keyle.MyPet.util.MyPetCaptureHelper.CaptureHelperMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandCaptureHelper implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {

        if(commandSender instanceof Player) {

            String mode = args[0];
            Player player = (Player)commandSender;
            MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(player);

            if(MyPetPermissions.has(player, "MyPet.capturehelper"))
            {

                if(args.length < 1)
                {
                    return false;
                }

                if(mode.equalsIgnoreCase("normal"))
                {
                    myPetPlayer.setCaptureHelperMode(CaptureHelperMode.Normal);
                }
                else if(mode.equalsIgnoreCase("half"))
                {
                    myPetPlayer.setCaptureHelperMode(CaptureHelperMode.Half);
                }
                else if(mode.equalsIgnoreCase("tameableonly"))
                {
                    myPetPlayer.setCaptureHelperMode(CaptureHelperMode.TameableOnly);
                }
                else if(mode.equalsIgnoreCase("deactivate"))
                {
                    myPetPlayer.setCaptureHelperMode(CaptureHelperMode.Deactivated);
                }
                else
                {
                    return false;
                }
                player.sendMessage("Set Capturehelper to " +  mode);
            }
            player.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotAllowed")));
        }
        return true;
    }
}
