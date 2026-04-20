package de.Keyle.MyPet.entity.ai.attack;

import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Applies damage for MyPet-fired projectiles whose vanilla {@code onHit()}
 * would otherwise deal no configurable damage.
 *
 * <p>Arrows and tridents do <em>not</em> pass through this listener: their
 * damage is set natively on the projectile in
 * {@link PetRangedAttackGoal} via {@code AbstractArrow.setDamage(double)}
 * and handled by the vanilla damage pipeline. Everything else launched by
 * {@link PetRangedAttackGoal} — throwables (snowball, egg, ender pearl,
 * llama spit) and fireballs (small/large, wither skull, dragon fireball) —
 * carries its damage as a float in the projectile's {@link PersistentDataContainer}
 * under {@link PetRangedAttackGoal#PROJECTILE_DAMAGE_KEY}, and this listener
 * reads that tag on {@link ProjectileHitEvent} to apply the damage at hit
 * time.
 *
 * <p>The listener also resolves the MyPet owner for kill-credit attribution
 * via {@link PetRangedAttackGoal#PROJECTILE_OWNER_KEY} — so a pet-fired
 * fireball that finishes a mob still credits the owning player for the
 * kill.
 */
public class PetProjectileHitListener implements Listener {

    /**
     * Applies PDC-tagged damage when a MyPet-fired throwable or fireball
     * strikes a living entity.
     *
     * <p><b>Important:</b> this handler deliberately does NOT call
     * {@code event.getEntity().remove()}. {@link ProjectileHitEvent} fires
     * <em>before</em> vanilla {@code onHit()} runs, so a premature
     * {@code remove()} would suppress the vanilla hit-side effects
     * (particles, sounds, dragon fireball's area-effect cloud). Throwables
     * and fireballs already self-clean-up via vanilla {@code onHit()};
     * fireballs additionally have {@code setYield(0)} and
     * {@code setIsIncendiary(false)} applied at launch so their explosion is
     * already neutralized.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity hitEntity = event.getHitEntity();
        if (!(hitEntity instanceof LivingEntity target)) {
            return;
        }

        PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        if (!pdc.has(PetRangedAttackGoal.PROJECTILE_DAMAGE_KEY)) {
            return;
        }

        float damage = pdc.getOrDefault(PetRangedAttackGoal.PROJECTILE_DAMAGE_KEY, PersistentDataType.FLOAT, 0F);
        if (damage <= 0) {
            return;
        }

        // Resolve the owner and pet mob for Paper DamageSource kill credit.
        Entity shooter = event.getEntity().getShooter() instanceof Entity e ? e : event.getEntity();
        Player owner = null;
        String ownerUuid = pdc.get(PetRangedAttackGoal.PROJECTILE_OWNER_KEY, PersistentDataType.STRING);
        if (ownerUuid != null) {
            owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
        }

        // Build a Paper DamageSource that credits the owner as the causing
        // entity (routing XP + drops to them) while naming the pet or the
        // projectile as the direct source. Owner attribution is skipped when
        // the target is a Player — the drops/XP routing rationale only
        // applies to mob kills, and attributing PvP kills to the owner
        // makes pet-fired projectiles read as the owner killing the victim
        // in death messages and Player#getKiller().
        DamageSource.Builder builder = DamageSource
                .builder(DamageType.MOB_PROJECTILE)
                .withDirectEntity(event.getEntity());
        boolean creditOwner = !(target instanceof Player);
        if (creditOwner && owner != null && owner.isOnline()) {
            builder = builder.withCausingEntity(owner);
        } else if (creditOwner && shooter != null) {
            builder = builder.withCausingEntity(shooter);
        }

        try {
            target.damage(damage, builder.build());
        } catch (IllegalStateException ignored) {
            Entity fallback = creditOwner && owner != null ? owner : shooter;
            target.damage(damage, fallback);
        }

        // Do not call event.getEntity().remove() here. ProjectileHitEvent
        // fires before vanilla onHit() runs. Calling remove() pre-empts
        // vanilla and suppresses hit-side effects (particles, sounds, and
        // for DragonFireball the area-effect cloud). Throwables and
        // fireballs already self-cleanup via vanilla onHit; fireballs have
        // setYield(0)/setIsIncendiary(false) applied at launch so their
        // explosion is already neutralized. Arrows/Tridents are handled by
        // vanilla damage and never reach this path (they don't carry
        // PROJECTILE_DAMAGE_KEY).
    }
}
