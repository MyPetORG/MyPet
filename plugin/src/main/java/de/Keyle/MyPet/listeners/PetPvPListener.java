package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * PvP policy engine for pet entities: determines who can damage whom.
 * <ul>
 *   <li>Combust-by-entity: cancels owner-on-pet burn and hook-plugin violations</li>
 *   <li>Owner friendly-fire gate ({@code OWNER_CAN_ATTACK_PET} config)</li>
 *   <li>Hook-plugin {@code canHurt} integration (WorldGuard, MobArena, etc.)</li>
 *   <li>Pet-on-pet projectile self-damage prevention and duel-mode bypass</li>
 *   <li>Cube-mob (Slime, MagmaCube) passive contact damage: owner-protect always, per-type flag for non-owner players, target-bypass for deliberate attacks</li>
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

        MyPet myPet = getPetManager().getMyPetFromEntity(event.getEntity());
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

    /**
     * Suppresses passive contact damage from cube-mob pets (Slime, MagmaCube) when
     * configured to. Vanilla {@code Slime#playerTouch(Player)} fires automatically when a
     * slime is in contact with a Player — without this gate, slime/magma pets damage
     * their owner and other players just by hopping near them.
     *
     * <p>Owner is universally protected: the pet never damages its owner regardless of
     * config. Non-owner Player damage is gated by {@code CAN_HURT_PLAYERS_ON_CONTACT}.
     *
     * <p>Deliberate attacks via {@code PetMeleeAttackGoal} (Aggressive/Farm/Duel/Control
     * Behavior) on the pet's current target bypass this gate — both vanilla contact
     * damage and {@code applyPetDamage} fire {@link EntityDamageByEntityEvent} with the
     * same damager, cause, and damage type, so the only signal that distinguishes
     * "intentional skill use" from "incidental hop-by" is whether the victim equals the
     * pet's current target. This is a best-effort discriminator — if the target reference
     * is cleared between the attack and the event (rare but possible in unusual goal
     * transitions), a legitimate deliberate hit could fall through to the config gate and
     * get cancelled.
     */
    @EventHandler
    public void onPetCubeMobContactDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Slime damagerSlime)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!PetEntityMarker.isMarked(damagerSlime)) return;
        if (WorldGroup.getGroupByWorld(damagerSlime.getWorld()).isDisabled()) return;

        MyPet myPet = getPetManager().getMyPetFromEntity(damagerSlime);
        if (myPet == null) return;

        // Deliberate attacks via PetMeleeAttackGoal target the pet's current target.
        // Don't gate those — that's the user's intentional Aggressive/Farm/Duel use.
        if (myPet.getMyPetTarget() == victim) {
            return;
        }

        // Owner is universally protected.
        var owner = myPet.getOwner();
        Player ownerPlayer = owner != null ? owner.getPlayer() : null;
        if (ownerPlayer != null && ownerPlayer.equals(victim)) {
            event.setCancelled(true);
            return;
        }

        boolean allowed = damagerSlime instanceof MagmaCube
                ? Configuration.MyPet.MagmaCube.CAN_HURT_PLAYERS_ON_CONTACT
                : Configuration.MyPet.Slime.CAN_HURT_PLAYERS_ON_CONTACT;
        if (!allowed) event.setCancelled(true);
    }
}
