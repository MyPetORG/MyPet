package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Manages damage routing for MyPet entities:
 * <ul>
 *   <li><b>Outgoing damage</b> — attack goals call {@link #applyPetDamage} which
 *       constructs a Paper {@link DamageSource} crediting the pet's owner as the
 *       "causing entity" so mob drops and XP route to the owner via vanilla's
 *       death path. Visually the damage is attributed to the pet via
 *       {@code withDirectEntity(mob)}.</li>
 *   <li><b>Incoming damage</b> — the event listener below cancels friendly fire
 *       (owner hitting their own pet). Hook-plugin interactions (WorldGuard,
 *       MobArena, etc.) are still routed through the existing
 *       {@code MyPetEntityListener} in {@code plugin/listeners/}.</li>
 * </ul>
 */
public class PetDamageListener implements Listener {

    /**
     * Applies damage from a MyPet to a target entity. Uses Paper's
     * {@link DamageSource} builder to construct a damage source that credits
     * the pet's owner as the "causing entity" — this routes mob drops and XP
     * through vanilla's death path so the owner's inventory receives drops
     * and the owner's XP bar receives experience orbs.
     *
     * <p>Fallbacks:
     * <ul>
     *   <li>If the owner is offline or in a different world, the damage source
     *       only names the pet, so drops/XP go to the world.</li>
     *   <li>If the target is already dead or the pet's Bukkit entity has been
     *       cleared, the call is a no-op.</li>
     * </ul>
     */
    public static void applyPetDamage(MyPet pet, LivingEntity target, double damage) {
        if (pet == null || target == null || target.isDead()) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;

        Player owner = pet.getOwner() != null ? pet.getOwner().getPlayer() : null;

        DamageSource.Builder builder = DamageSource.builder(DamageType.MOB_ATTACK)
                .withDirectEntity(mob);
        if (owner != null && owner.isOnline() && owner.getWorld().equals(mob.getWorld())) {
            // withCausingEntity(owner) routes kill credit to the owner via
            // vanilla's lastHurtByPlayer tracking, so drops and XP land on
            // the owner when the target dies.
            builder = builder.withCausingEntity(owner);
        }

        try {
            target.damage(damage, builder.build());
        } catch (IllegalStateException ignored) {
            // Paper may reject a damage source if the target is invulnerable
            // or the damage type was registered after world load. Swallow and
            // fall through to a plain damage call.
            target.damage(damage, mob);
        }
        mob.swingMainHand();
    }

    /**
     * Cancels owner-on-pet damage. All other incoming damage paths
     * (external mobs, plugins, environment) continue to the existing
     * {@link de.Keyle.MyPet.listeners.MyPetEntityListener} which handles
     * the hook-plugin nuance (WorldGuard claims, MobArena, CombatLogX, etc.).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) return;
        MyPet pet = MyPetApi.getMyPetManager().getMyPetFromEntity(event.getEntity());
        if (pet == null) return;

        Entity damager = event.getDamager();
        if (damager instanceof Player player && player.equals(pet.getOwner() != null ? pet.getOwner().getPlayer() : null)) {
            event.setCancelled(true);
        }
    }
}
