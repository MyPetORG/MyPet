package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetInteractionGate;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Suppresses vanilla item-driven right-click interactions on MyPet pets when
 * the pet's own per-type config flag is disabled (cow milking, sheep shearing,
 * mushroom stew, etc.).
 *
 * <p>In v4 pets are real vanilla mobs, so behaviors that previously had to be
 * driven from NMS overrides — bucket-on-cow, shears-on-sheep, etc. — now run
 * for free from vanilla {@code Mob#mobInteract}. The flags that used to
 * *enable* those behaviors therefore flip role: they now *suppress* the
 * vanilla path when set to {@code false}.
 *
 * <p>The listener is pet-agnostic: it dispatches via the
 * {@link MyPetInteractionGate} marker interface. Adding a new gated
 * interaction is one {@code extends MyPetInteractionGate} clause plus the two
 * abstract methods on the relevant {@code My<Type>} class — no listener
 * changes required.
 *
 * <p>Cancelling {@link PlayerInteractEntityEvent} short-circuits vanilla's
 * {@code mobInteract} before it runs, which also prevents downstream events
 * like {@code PlayerShearEntityEvent} from firing — a single hook suffices
 * for both the bucket/bowl interactions and the shears interaction.
 *
 * <p>Both hand passes are honored: if the player has the gated item in
 * offhand only, vanilla still milks/shears via the offhand pass, so we
 * must gate both. The event reports which hand triggered the call via
 * {@link PlayerInteractEntityEvent#getHand()}.
 */
public class PetInteractionGateListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!PetEntityMarker.isMarked(event.getRightClicked())) {
            return;
        }
        MyPet pet = MyPetApi.getMyPetManager().getMyPetFromEntity(event.getRightClicked());
        if (!(pet instanceof MyPetInteractionGate gate)) {
            return;
        }
        ItemStack handItem = event.getPlayer().getInventory().getItem(event.getHand());
        if (handItem == null) {
            return;
        }
        if (!gate.gatedInteractionItems().contains(handItem.getType())) {
            return;
        }
        if (gate.isInteractionSuppressed()) {
            event.setCancelled(true);
        }
    }
}
