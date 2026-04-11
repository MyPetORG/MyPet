package de.Keyle.MyPet.entity.ai.target;

import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event-driven "who hit me last?" tracker used by the retaliation goals
 * ({@link PetHurtByTargetGoal}, {@link PetOwnerHurtByTargetGoal}) to
 * resolve the most recent attacker of a player or pet without touching
 * any server-internals.
 *
 * <p>The tracker listens to {@link EntityDamageByEntityEvent} and records
 * a {@link DamageRecord} for every hit against a {@link Player} or
 * {@link MyPetBukkitEntity}, resolving projectiles back to their shooter
 * via {@link ProjectileSource}. Entries expire after
 * {@link #EXPIRY_TICKS} ticks (5 seconds) and are also dropped when the
 * victim dies ({@link EntityDeathEvent}) or when a pet is explicitly
 * cleaned up via {@link #cleanup(UUID)} during unlink/despawn — together
 * these paths bound the size of the static attacker map so it doesn't
 * grow unbounded on long-lived servers.
 *
 * <p>All state is held in a thread-safe static
 * {@link ConcurrentHashMap}, so the tracker behaves as a singleton
 * registered once at plugin load and queried from any goal's tick.
 */
public class PetDamageTracker implements Listener {

    private static final int EXPIRY_TICKS = 100;
    private static final Map<UUID, DamageRecord> lastAttackers = new ConcurrentHashMap<>();

    /**
     * A single "last-hit" entry in the tracker map.
     *
     * @param attacker  the living entity that dealt the damage (projectile shooter, if applicable)
     * @param tickStamp the server tick at which the hit occurred; used to age-out stale entries
     */
    public record DamageRecord(LivingEntity attacker, int tickStamp) {
    }

    /**
     * Drops the tracker entry when an entity dies, so dead victims don't
     * linger in the static map. Combined with the explicit cleanup in
     * {@code MyPet.removePet()}, this bounds the map size: death covers
     * combat kills, removePet() covers owner-logout/respawn/despawn.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        lastAttackers.remove(event.getEntity().getUniqueId());
    }

    /**
     * Records the damager-victim pair for every live hit against a
     * {@link Player} or {@link MyPetBukkitEntity}. Non-tracked victims
     * (monsters, animals, etc.) are ignored so the map only holds
     * entries the retaliation goals will ever need.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        // Only track damage to players and pet entities
        if (!(victim instanceof Player) && !(PetEntityMarker.isMarked(victim))) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        int currentTick = victim.getServer().getCurrentTick();
        lastAttackers.put(victim.getUniqueId(), new DamageRecord(attacker, currentTick));
    }

    /**
     * Looks up the most recent attacker for {@code victim}.
     *
     * <p>Entries older than {@link #EXPIRY_TICKS} ticks are evicted
     * lazily on read, and dead/invalid attackers are dropped the first
     * time they're requested — so callers never see a stale record.
     *
     * @param victim the entity whose recent attacker is being queried
     * @return the living attacker, or {@code null} if none is recorded, the
     *         record has expired, or the attacker is no longer alive/valid
     */
    public static LivingEntity getLastAttacker(LivingEntity victim) {
        DamageRecord record = lastAttackers.get(victim.getUniqueId());
        if (record == null) {
            return null;
        }
        int currentTick = victim.getServer().getCurrentTick();
        if (currentTick - record.tickStamp() > EXPIRY_TICKS) {
            lastAttackers.remove(victim.getUniqueId());
            return null;
        }
        if (record.attacker().isDead() || !record.attacker().isValid()) {
            lastAttackers.remove(victim.getUniqueId());
            return null;
        }
        return record.attacker();
    }

    /**
     * Resolves the true attacker from a damage event's damager entity.
     * For projectiles, traces back to the shooter.
     */
    private static LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof LivingEntity shooter) {
                return shooter;
            }
            return null;
        }
        if (damager instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /**
     * Explicitly removes the tracked entry for the given entity UUID.
     * Call from {@code MyPet.removePet()} (or any other despawn path)
     * so tracker entries for despawned pets don't linger beyond their
     * natural expiry window.
     *
     * @param entityId the UUID whose record should be forgotten
     */
    public static void cleanup(UUID entityId) {
        lastAttackers.remove(entityId);
    }
}
