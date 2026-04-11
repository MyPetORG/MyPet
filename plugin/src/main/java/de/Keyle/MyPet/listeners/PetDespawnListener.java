package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Ensures MyPet entities are cleanly removed from the world at the right times.
 *
 * <p>Pet entities are spawned with {@code setPersistent(false)} + {@code setRemoveWhenFarAway(false)},
 * which means they stay alive in loaded chunks but are not saved to disk on
 * chunk unload. This listener handles two edge cases that the flags alone
 * don't cover:
 * <ol>
 *   <li><b>Owner disconnects.</b> The pet's Bukkit entity is removed and the
 *       {@link MyPet} domain object's state is preserved for respawn on relogin
 *       via {@link MyPet#removePet}.</li>
 *   <li><b>Chunk unload safety sweep.</b> Redundant with {@code persistent=false}
 *       but guarantees in-memory cleanup if any pet entities remain in the
 *       chunk at unload time.</li>
 * </ol>
 *
 */
public class PetDespawnListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (MyPetApi.getMyPetManager().hasActiveMyPet(event.getPlayer())) {
            MyPet pet = MyPetApi.getMyPetManager().getMyPet(event.getPlayer());
            if (pet != null) {
                pet.removePet(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (PetEntityMarker.isMarked(entity)) {
                entity.remove();
            }
        }
    }
}
