package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Per-type Creeper handling: ignition gating, explosion-damage suppression,
 * and death-pipeline routing for self-detonation.
 *
 * <p><b>Why the explicit death routing?</b> Vanilla
 * {@code Creeper#explodeCreeper} calls {@code discard()} directly, skipping
 * {@code die()} and therefore {@link org.bukkit.event.entity.EntityDeathEvent}.
 * Without this listener the pet is removed without ever being marked
 * {@link PetState#Dead}, so the spawn pipeline immediately respawns it — an
 * instant-respawn loop. The routing here mirrors the minimum subset of
 * {@link PetDeathListener#onPetDeath} required to fix that regression. XP
 * loss and backpack drops are intentionally omitted because vanilla creepers
 * that self-detonate likewise drop nothing.
 */
public class PetCreeperListener implements Listener {

    /**
     * Gates flint-and-steel ignition on Creeper pets. Runs at
     * {@link EventPriority#LOW} (matching {@link PetInteractionGateListener}) so
     * the cancellation lands before vanilla's {@code Creeper#mobInteract}.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlintAndSteelIgnite(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Creeper)) return;
        if (!PetEntityMarker.isMarked(event.getRightClicked())) return;
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (item == null || item.getType() != Material.FLINT_AND_STEEL) return;

        if (!Configuration.MyPet.Creeper.ALLOW_FLINT_AND_STEEL_EXPLODE) {
            event.setCancelled(true);
            return;
        }
        MyPet pet = MyPetApi.getMyPetManager().getMyPetFromEntity(event.getRightClicked());
        if (pet != null
                && !Configuration.MyPet.Creeper.ALLOW_NON_OWNER_FLINT_AND_STEEL
                && !isOwner(event.getPlayer(), pet)) {
            event.setCancelled(true);
        }
    }

    /**
     * Strips terrain/yield damage when the explosion-damage flag is off, then
     * routes the pet through the death pipeline regardless of upstream
     * cancellation — vanilla {@code discard()} runs unconditionally after
     * {@code explodeCreeper}, so the pet is dying either way.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPetExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        Optional<MyPet> petOpt = PetListenerGuards.markedPet(creeper);
        if (petOpt.isEmpty()) return;
        MyPet pet = petOpt.get();

        if (!Configuration.MyPet.Creeper.ALLOW_EXPLOSION_BLOCK_DAMAGE) {
            event.blockList().clear();
            event.setYield(0f);
        }

        // Scrub the live ignite state before snapshot. Vanilla writes
        // `ignited` and a near-zero `Fuse` into NBT during explodeCreeper,
        // and Creeper#tick re-triggers the fuse on respawn if those persist —
        // the pet would explode again the moment it comes back. Mutating the
        // doomed entity is safe because it's about to be discarded.
        creeper.setIgnited(false);
        creeper.setFuseTicks(creeper.getMaxFuseTicks());
        try {
            pet.setInfo(PetEntitySnapshot.capture(creeper));
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("Failed to capture EntitySnapshot for Creeper pet "
                    + pet.getUUID() + " on explode — pet will respawn with default "
                    + "live-entity state. " + t.getMessage());
        }
        pet.setRespawnTime(
                (Configuration.Respawn.TIME_FIXED
                        + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(pet.getPetType()))
                        + (pet.getExperience().getLevel()
                            * (Configuration.Respawn.TIME_FACTOR
                                + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(pet.getPetType())))
        );
        pet.setStatus(PetState.Dead);
        if (pet.getOwner() != null && pet.getOwner().getPlayer() != null) {
            pet.getOwner().sendMessage(Locale.getFormattedComponent(
                    "Message.Spawn.Respawn.In",
                    pet.getOwner().getPlayer(),
                    pet.getDisplayName(),
                    pet.getRespawnTime()));
        }
    }

    /**
     * Cancels the per-victim splash damage that vanilla applies to nearby
     * entities during the explosion. The block-damage and entity-damage
     * paths are independent in modern Paper — {@link EntityExplodeEvent}
     * cancellation only suppresses block damage, so the entity-damage flag
     * has its own dedicated handler here.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetExplosionSplashDamage(EntityDamageByEntityEvent event) {
        if (Configuration.MyPet.Creeper.ALLOW_EXPLOSION_ENTITY_DAMAGE) return;
        if (event.getCause() != DamageCause.ENTITY_EXPLOSION) return;
        if (!(event.getDamager() instanceof Creeper)) return;
        if (!PetEntityMarker.isMarked(event.getDamager())) return;
        event.setCancelled(true);
    }

    private static boolean isOwner(Player player, MyPet pet) {
        return pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }
}
