package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.entity.visual.PetStateSnapshot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Dispatches player right-click on a MyPet to the pet's {@link MyPet#onInteract}
 * method. Cancels the underlying {@link PlayerInteractEntityEvent} if the pet
 * consumed the interaction (feed, sit toggle, per-type action).
 */
public class PetInteractionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // PlayerInteractEntityEvent fires once per hand (MAIN_HAND then OFF_HAND).
        // We dispatch only on the main-hand pass so feed/sit/equip handlers
        // run exactly once per right-click. Matches the legacy
        // EntityMyPet#handlePlayerInteraction guard that returned SUCCESS for
        // OFF_HAND without taking any action.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!PetEntityMarker.isMarked(event.getRightClicked())) {
            return;
        }
        MyPet pet = MyPetApi.getMyPetManager().getMyPetFromEntity(event.getRightClicked());
        if (pet == null) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        ItemStack item = event.getPlayer().getInventory().getItem(hand);

        if (pet.onInteract(event.getPlayer(), item, hand)) {
            event.setCancelled(true);
        }
    }

    /**
     * Post-interaction sync: after vanilla has handled a right-click on a pet
     * (applying a saddle, dyeing wool, equipping armor, opening a chest, etc.),
     * read the current mob state back into the {@link MyPet} domain fields so
     * the change is persisted across despawn/respawn cycles.
     *
     * <p>Runs at {@code MONITOR} priority on a next-tick scheduler so vanilla's
     * interaction logic has fully settled before we snapshot.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteractEntityPostSync(PlayerInteractEntityEvent event) {
        // Match the off-hand guard on onInteractEntity so the post-sync
        // snapshot does not get double-scheduled (MAIN_HAND + OFF_HAND).
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        resyncFromMob(event.getRightClicked());
    }

    /**
     * Covers shear-based state changes: removing a saddle from a pig, shearing
     * wool from a sheep, removing pumpkin from a snow golem, removing the red
     * mushroom from a mooshroom, etc. These fire {@link PlayerShearEntityEvent}
     * instead of {@link PlayerInteractEntityEvent}.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onShearEntityPostSync(PlayerShearEntityEvent event) {
        resyncFromMob(event.getEntity());
    }

    /**
     * Snapshots the current Bukkit mob state back into the {@link MyPet} domain
     * fields via {@code readExtendedInfo(PetStateSnapshot.toTag(mob, false))}.
     * Runs on a next-tick scheduler so vanilla's interaction/shear logic has
     * fully settled before we read.
     */
    private void resyncFromMob(Entity entity) {
        if (!PetEntityMarker.isMarked(entity)) {
            return;
        }
        MyPet pet = MyPetApi.getMyPetManager().getMyPetFromEntity(entity);
        if (!(pet instanceof de.Keyle.MyPet.entity.MyPet concretePet)) {
            return;
        }
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        entity.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
            if (concretePet.getBukkitEntity() == null) return;
            try {
                concretePet.readExtendedInfo(PetStateSnapshot.toTag(living, false));
            } catch (Throwable ignored) {
            }
        }, null);
    }
}
