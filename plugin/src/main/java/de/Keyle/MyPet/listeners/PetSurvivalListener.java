package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetAquaticEntity;
import de.Keyle.MyPet.api.entity.MyPetSunSensitive;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Locale;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

/**
 * Keeps pets alive through vanilla environmental hazards:
 * <ul>
 *   <li>Fall damage immunity when the pet has a JUMP_BOOST effect</li>
 *   <li>Suffocation respawn — despawns the pet and re-spawns it on the
 *       next tick to escape solid blocks</li>
 *   <li>Daylight burn suppression for {@link MyPetSunSensitive} pets when
 *       the per-type {@code PreventDaylightBurn} flag is on</li>
 *   <li>Out-of-water suffocation suppression for {@link MyPetAquaticEntity}
 *       pets when the per-type {@code PreventSuffocation} flag is on</li>
 * </ul>
 */
public class PetSurvivalListener implements Listener {

    // JUMP was renamed to JUMP_BOOST in newer API versions
    private static final PotionEffectType JUMP_EFFECT;
    static {
        PotionEffectType jump = PotionEffectType.getByName("JUMP_BOOST");
        if (jump == null) {
            jump = PotionEffectType.getByName("JUMP");
        }
        JUMP_EFFECT = jump;
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        MyPet myPet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (myPet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        LivingEntity bukkitEntity = (LivingEntity) event.getEntity();

        if (event.getCause() == DamageCause.FALL && JUMP_EFFECT != null && bukkitEntity.hasPotionEffect(JUMP_EFFECT)) {
            event.setCancelled(true);
            return;
        }

        // Bukkit reuses DROWNING for both directions (land-breather under
        // water, water-breather in air); for aquatic pets the latter case is
        // the only one that fires in vanilla — strict water-breathers cannot
        // enter the land-breather state.
        if (event.getCause() == DamageCause.DROWNING
                && myPet instanceof MyPetAquaticEntity aquatic
                && aquatic.preventSuffocation()) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == DamageCause.SUFFOCATION) {
            if (myPet.hasMyPetRider()) {
                event.setCancelled(true);
                return;
            }
            final MyPetPlayer myPetPlayer = myPet.getOwner();

            myPet.removePet();
            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Despawn", myPetPlayer.getLanguage(), myPet.getDisplayName()));

            Player ownerPlayer = myPetPlayer.getPlayer();
            if (ownerPlayer == null) return;
            ownerPlayer.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> {
                if (myPetPlayer.hasMyPet()) {
                    MyPet runMyPet = myPetPlayer.getMyPet();
                    switch (runMyPet.createEntity()) {
                        case Canceled:
                            runMyPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", myPet.getOwner(), runMyPet.getDisplayName()));
                            break;
                        case NoSpace:
                            runMyPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", myPet.getOwner(), runMyPet.getDisplayName()));
                            break;
                        case NotAllowed:
                            runMyPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", myPet.getOwner(), myPet.getDisplayName()));
                            break;
                        case Flying:
                            runMyPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", myPet.getOwner(), myPet.getDisplayName()));
                            break;
                        case Success:
                            if (runMyPet != myPet) {
                                runMyPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", myPet.getOwner(), runMyPet.getDisplayName()));
                            }
                            break;
                    }
                }
            }, null, 10L);
        }
    }

    // Hooked at default priority with ignoreCancelled so other plugins can
    // pre-empt. Block-caused (lava, magma) and entity-caused (flame arrow)
    // combust fire as the typed subclasses; the bare EntityCombustEvent is
    // what vanilla raises for sunlight burning, so the instanceof exclusions
    // are how we narrow to "natural" causes.
    @EventHandler(ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        if (event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent) {
            return;
        }
        MyPet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;
        if (pet instanceof MyPetSunSensitive sunSensitive && sunSensitive.preventDaylightBurn()) {
            event.setCancelled(true);
        }
    }
}
