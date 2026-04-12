package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static de.Keyle.MyPet.MyPetApi.getMyPetManager;

/**
 * PvP policy engine for pet entities: determines who can damage whom.
 * <ul>
 *   <li>Combust-by-entity: cancels owner-on-pet burn and hook-plugin violations</li>
 *   <li>Owner friendly-fire gate ({@code OWNER_CAN_ATTACK_PET} config)</li>
 *   <li>Hook-plugin {@code canHurt} integration (WorldGuard, MobArena, etc.)</li>
 *   <li>Pet-on-pet projectile self-damage prevention and duel-mode bypass</li>
 * </ul>
 */
public class PetPvPListener implements Listener {

    @EventHandler
    public void onCombustByEntity(EntityCombustByEntityEvent event) {
        @SuppressWarnings("ConstantConditions")
        boolean nullEntity = event.getEntity() == null;
        if (nullEntity) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        if (!PetEntityMarker.isMarked(event.getEntity())) return;

        if (!(event.getCombuster() instanceof Player || (event.getCombuster() instanceof Projectile && ((Projectile) event.getCombuster()).getShooter() instanceof Player))) {
            return;
        }
        Player damager;
        if (event.getCombuster() instanceof Projectile) {
            damager = (Player) ((Projectile) event.getCombuster()).getShooter();
        } else {
            damager = (Player) event.getCombuster();
        }

        MyPet myPet = getMyPetManager().getMyPetFromEntity(event.getEntity());
        if (myPet == null) return;

        if (myPet.getOwner().equals(damager) && !Configuration.Misc.OWNER_CAN_ATTACK_PET) {
            event.setCancelled(true);
        } else if (!myPet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, myPet.getOwner().getPlayer(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(final EntityDamageByEntityEvent event) {
        MyPet myPet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (myPet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        // Player-on-pet PvP gate
        if (event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player)) {
            Player damager;
            if (event.getDamager() instanceof Projectile) {
                damager = (Player) ((Projectile) event.getDamager()).getShooter();
            } else {
                damager = (Player) event.getDamager();
            }
            if (myPet.getOwner().equals(damager) && (!Configuration.Misc.OWNER_CAN_ATTACK_PET)) {
                event.setCancelled(true);
            } else if (!myPet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, myPet.getOwner().getPlayer(), true)) {
                event.setCancelled(true);
            }
        }

        // Pet-on-pet projectile: self-damage prevention + duel bypass
        if (event.getDamager() instanceof Projectile projectile) {
            MyPet shooterPet = PetRangedAttackGoal.getSourceMyPet(projectile);
            if (shooterPet != null && shooterPet.getEntity().isPresent()) {
                if (myPet == shooterPet) {
                    event.setCancelled(true);
                }
                boolean inDuel = shooterPet.getTargetPriority() == TargetPriority.Duel
                        && myPet.getTargetPriority() == TargetPriority.Duel
                        && shooterPet.getMyPetTarget() == myPet.getBukkitEntity();
                if (!inDuel && !MyPetApi.getHookHelper().canHurt(shooterPet.getOwner().getPlayer(), myPet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
