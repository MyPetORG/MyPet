package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Suppresses contact damage dealt by a pet EnderDragon.
 *
 * <p>Vanilla {@code EnderDragon#aiStep} damages entities its body brushes
 * against (the dragon's {@code attackTargets}-style collision routine).
 * Modern Paper surfaces this through {@link EntityDamageByEntityEvent} once
 * per victim, so a single handler can split player vs. non-player victims
 * via {@code event.getEntity() instanceof Player} and consult the matching
 * config flag.
 *
 * <p>{@link PetEntityMarker#isMarked} resolves {@code ComplexEntityPart} to
 * the parent dragon, so this listener works whether the damager surfaced as
 * the parent or a sub-part (head/neck/body/tail/wings).
 */
public class PetEnderDragonContactDamageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetDragonContactDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon)) return;
        if (!PetEntityMarker.isMarked(event.getDamager())) return;

        boolean victimIsPlayer = event.getEntity() instanceof Player;
        if (victimIsPlayer
                ? Configuration.MyPet.EnderDragon.ALLOW_PLAYER_CONTACT_DAMAGE
                : Configuration.MyPet.EnderDragon.ALLOW_ENTITY_CONTACT_DAMAGE) {
            return;
        }
        event.setCancelled(true);
    }
}
