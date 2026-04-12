package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

/**
 * Keeps pets alive through vanilla environmental hazards:
 * <ul>
 *   <li>Fall damage immunity when the pet has a JUMP_BOOST effect</li>
 *   <li>Suffocation respawn — despawns the pet and re-spawns it on the
 *       next tick to escape solid blocks</li>
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

        if (event.getCause() == DamageCause.SUFFOCATION) {
            if (myPet.hasMyPetRider()) {
                event.setCancelled(true);
                return;
            }
            final MyPetPlayer myPetPlayer = myPet.getOwner();

            myPet.removePet();
            myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Spawn.Despawn", myPetPlayer.getLanguage(), myPet.getDisplayName()));

            MyPetApi.getPlugin().getServer().getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
                if (myPetPlayer.hasMyPet()) {
                    MyPet runMyPet = myPetPlayer.getMyPet();
                    switch (runMyPet.createEntity()) {
                        case Canceled:
                            runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", myPet.getOwner(), runMyPet.getDisplayName()));
                            break;
                        case NoSpace:
                            runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", myPet.getOwner(), runMyPet.getDisplayName()));
                            break;
                        case NotAllowed:
                            runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.No.AllowedHere", myPet.getOwner(), myPet.getDisplayName()));
                            break;
                        case Flying:
                            runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Flying", myPet.getOwner(), myPet.getDisplayName()));
                            break;
                        case Success:
                            if (runMyPet != myPet) {
                                runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Command.Call.Success", myPet.getOwner(), runMyPet.getDisplayName()));
                            }
                            break;
                    }
                }
            }, 10L);
        }
    }
}
