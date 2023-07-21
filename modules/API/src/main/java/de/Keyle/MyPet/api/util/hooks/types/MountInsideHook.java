package de.Keyle.MyPet.api.util.hooks.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Entity;

/**
 * This interface defines that the hook checks if a player tries to enter a region by mounting their pet
 */
public interface MountInsideHook {
    /**
     * Return if a MyPet player is in any a place where he is allowed to have a pet
     *
     * @param player the MyPet player
     * @return if the player can mount the pet there
     */
    boolean playerCanMount(MyPetPlayer player, Entity pet);
}
