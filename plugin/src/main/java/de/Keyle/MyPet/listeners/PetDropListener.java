package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetNaturalDrop;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;

/**
 * Suppresses periodic vanilla item drops from MyPet pets when the pet's own
 * per-type config flag is disabled.
 *
 * <p>In v4 pets are real vanilla mobs, so behaviors that previously had to be
 * driven from NMS overrides — chickens laying eggs, armadillos shedding
 * scutes, etc. — now run for free from vanilla AI ticks. The flags that used
 * to *enable* those behaviors therefore flip role: they now *suppress* the
 * vanilla path when set to {@code false}.
 *
 * <p>The listener is pet-agnostic: it dispatches via the
 * {@link MyPetNaturalDrop} marker interface. Adding a new periodic drop to
 * suppress is one {@code extends MyPetNaturalDrop} clause plus two default
 * methods on the relevant {@code My<Type>} api interface — no listener
 * changes required.
 *
 * <p>Wild mobs are unaffected — the {@link PetEntityMarker} check ensures
 * suppression applies only to MyPet pets.
 */
public class PetDropListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) {
            return;
        }
        MyPet pet = MyPetApi.getPetManager().getMyPetFromEntity(event.getEntity());
        if (!(pet instanceof MyPetNaturalDrop dropper)) {
            return;
        }
        if (!dropper.naturalDropMaterials().contains(event.getItemDrop().getItemStack().getType())) {
            return;
        }
        if (dropper.isNaturalDropSuppressed()) {
            event.setCancelled(true);
        }
    }
}
