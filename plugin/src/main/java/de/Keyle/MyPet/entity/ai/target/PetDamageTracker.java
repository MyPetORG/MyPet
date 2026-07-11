/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.ai.target;

import de.Keyle.MyPet.api.entity.Pet;
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

import java.lang.ref.WeakReference;
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
 * {@link Pet}, resolving projectiles back to their shooter
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

    private static final long EXPIRY_MS = 5000L; // 5 seconds, matches the old 100-tick window
    private static final Map<UUID, DamageRecord> lastAttackers = new ConcurrentHashMap<>();
    private static int writeCounter = 0; // racy increment is fine — only affects sweep cadence

    /**
     * A single "last-hit" entry in the tracker map.
     *
     * @param attackerId    UUID of the attacker (projectile shooter, if applicable)
     * @param attackerRef   weak reference to the attacker so despawned entity graphs aren't pinned
     * @param timestampMs   wall-clock time (ms) at which the hit occurred; used to age-out stale entries.
     *                      Wall-clock is chosen over {@code Server#getCurrentTick()} because Folia's tick
     *                      counters are region-local — a record written on the victim's region and read
     *                      from another region would produce garbage diffs.
     */
    public record DamageRecord(UUID attackerId, WeakReference<LivingEntity> attackerRef, long timestampMs) {
    }

    /**
     * Drops the tracker entry when an entity dies, so dead victims don't
     * linger in the static map. Combined with the explicit cleanup in
     * {@code Pet.removePet()}, this bounds the map size: death covers
     * combat kills, removePet() covers owner-logout/respawn/despawn.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        lastAttackers.remove(event.getEntity().getUniqueId());
    }

    /**
     * Records the damager-victim pair for every live hit against a
     * {@link Player} or {@link Pet}. Non-tracked victims
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
        lastAttackers.put(victim.getUniqueId(),
                new DamageRecord(attacker.getUniqueId(), new WeakReference<>(attacker), System.currentTimeMillis()));
        if ((++writeCounter & 255) == 0) {
            sweepStale();
        }
    }

    /** Opportunistic sweep so entries for victims that are never queried don't linger. */
    private static void sweepStale() {
        long now = System.currentTimeMillis();
        lastAttackers.values().removeIf(record ->
                now - record.timestampMs() > EXPIRY_MS || record.attackerRef().get() == null);
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
        if (System.currentTimeMillis() - record.timestampMs() > EXPIRY_MS) {
            lastAttackers.remove(victim.getUniqueId());
            return null;
        }
        LivingEntity attacker = record.attackerRef().get();
        if (attacker == null || attacker.isDead() || !attacker.isValid()) {
            lastAttackers.remove(victim.getUniqueId());
            return null;
        }
        return attacker;
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
     * Call from {@code Pet.removePet()} (or any other despawn path)
     * so tracker entries for despawned pets don't linger beyond their
     * natural expiry window.
     *
     * @param entityId the UUID whose record should be forgotten
     */
    public static void cleanup(UUID entityId) {
        lastAttackers.remove(entityId);
    }
}
