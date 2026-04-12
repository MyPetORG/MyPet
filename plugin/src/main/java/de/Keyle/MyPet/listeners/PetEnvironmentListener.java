package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPortalEvent;

/**
 * Vanilla-environment overrides for pet entities: spawn uncancel, portal
 * cancel, and block-interaction cancel (farmland trampling, turtle egg
 * crushing).
 *
 * <p>These handlers have no dependencies on pet state, hooks, or skills —
 * they only need to know whether the entity is a pet via
 * {@link PetEntityMarker#isMarked}.
 */
public class PetEnvironmentListener implements Listener {

    /**
     * Force-uncancels pet spawn events that other plugins may have cancelled.
     *
     * <p><b>Note:</b> mutating event state at {@link EventPriority#MONITOR}
     * violates the Bukkit convention that MONITOR handlers are observe-only.
     * This is intentional — without it, protection plugins that blanket-cancel
     * mob spawns would prevent pets from appearing. Preserved verbatim from
     * the original {@code MyPetEntityListener}.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        if (PetEntityMarker.isMarked(event.getEntity())) {
            if (event.getBlock().getType() == Material.FARMLAND) {
                event.setCancelled(true);
            } else if ("TURTLE_EGG".equals(event.getBlock().getType().name())) {
                event.setCancelled(true);
            }
        }
    }
}
