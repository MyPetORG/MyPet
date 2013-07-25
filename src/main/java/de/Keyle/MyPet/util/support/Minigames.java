package de.Keyle.MyPet.util.support;

import com.pauldavdesign.mineauz.minigames.events.JoinMinigameEvent;
import com.pauldavdesign.mineauz.minigames.events.SpectateMinigameEvent;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Minigames implements Listener
{
    public static boolean DISABLE_PETS_IN_MINIGAMES = true;

    private static com.pauldavdesign.mineauz.minigames.Minigames plugin;

    public static void findPlugin()
    {
        boolean active = false;
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Minigames"))
        {
            plugin = (com.pauldavdesign.mineauz.minigames.Minigames) Bukkit.getServer().getPluginManager().getPlugin("Minigames");
            Bukkit.getPluginManager().registerEvents(new Minigames(), MyPetPlugin.getPlugin());
            active = true;
        }
        DebugLogger.info("Minigames support " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInMinigame(MyPetPlayer owner)
    {
        if (plugin != null)
        {
            Player p = owner.getPlayer();
            return plugin.pdata.playersInMinigame().contains(p);
        }
        return false;
    }

    @EventHandler
    public void onJoinMinigame(JoinMinigameEvent event)
    {
        if (DISABLE_PETS_IN_MINIGAMES && MyPetPlayer.isMyPetPlayer(event.getPlayer()))
        {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here)
            {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.NotAllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onSpectateMinigame(SpectateMinigameEvent event)
    {
        if (DISABLE_PETS_IN_MINIGAMES && MyPetPlayer.isMyPetPlayer(event.getPlayer()))
        {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here)
            {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.NotAllowedHere", player.getPlayer()));
            }
        }
    }
}
