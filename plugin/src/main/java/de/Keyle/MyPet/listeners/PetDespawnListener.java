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

package de.Keyle.MyPet.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Ensures Pet entities are cleanly removed from the world at the right times.
 *
 * <p>Pet entities are spawned with {@code setPersistent(false)} + {@code setRemoveWhenFarAway(false)},
 * which means they stay alive in loaded chunks but are not saved to disk on
 * chunk unload. This listener handles edge cases that the flags alone don't
 * cover:
 * <ol>
 *   <li><b>Owner disconnects.</b> The pet's Bukkit entity is removed and the
 *       {@link Pet} domain object's state is preserved for respawn on relogin
 *       via {@link Pet#removePet}.</li>
 *   <li><b>Chunk unload safety sweep.</b> Redundant with {@code persistent=false}
 *       but guarantees in-memory cleanup if any pet entities remain in the
 *       chunk at unload time.</li>
 *   <li><b>External removal safety net.</b> If a bound pet entity leaves the
 *       world without MyPet initiating it — an END-portal dimension transfer
 *       that slipped past the portal guards, another plugin removing the
 *       entity, the chunk-unload sweep above — the Pet object is demoted
 *       cleanly instead of keeping a stale reference (see
 *       {@link #onEntityRemoveFromWorld}).</li>
 *   <li><b>Stray watch.</b> A dimension transfer re-adds the entity's live copy
 *       to the destination world a moment later; during a short window after an
 *       external removal, arriving marked-but-unregistered entities are removed
 *       (see {@link #onEntityAddToWorld}).</li>
 * </ol>
 *
 */
public class PetDespawnListener implements Listener {

    /**
     * How long after an external pet-entity removal the add-to-world stray watch
     * stays armed. Generous enough to cover a dimension transfer's re-add; the
     * handler is a single volatile read while disarmed, so the cost of a long
     * window is negligible.
     */
    private static final long STRAY_WATCH_MILLIS = 10_000L;

    /** Disarmed (=0) until {@link #onEntityRemoveFromWorld} detects an external removal. */
    private final AtomicLong strayWatchUntil = new AtomicLong(0L);

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Every Pet, not just the primary one: a Pet left behind here is an entity
        // that stays in the world after its owner logs out.
        for (Pet pet : MyPetApi.getPetManager().getPets(event.getPlayer())) {
            pet.removePet(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Sweep unconditionally: an orphaned marked entity (a despawn that failed to
        // remove its Bukkit entity, or one another plugin flipped to persistent) can
        // exist precisely when no pet is active, so gating on countActivePets()==0
        // would skip cleanup exactly when it's needed. isMarked is a cheap PDC check.
        for (Entity entity : event.getChunk().getEntities()) {
            if (PetEntityMarker.isMarked(entity)) {
                entity.remove();
            }
        }
    }

    /**
     * Safety net for pet entities that leave the world without MyPet initiating
     * it. Left alone, the Pet object keeps a stale entity reference and a cached
     * Here status: summon commands break against the detached reference, and a
     * later snapshot-based respawn can materialize a second copy of the pet's
     * equipment while the transferred live copy still walks around (item
     * duplication).
     *
     * <p>Demotion is deferred rather than done inline: MyPet's own
     * {@link Pet#removePet()} also removes the entity, and this event fires
     * synchronously inside {@code remove()} while the Pet is still index-bound.
     * By the time the delayed task runs, plugin-initiated removals have
     * unregistered the entity, so only genuinely external removals are still
     * bound — no reentrancy guard needed. Re-resolving through the entity-UUID
     * index (rather than holding the Pet) is what makes that distinction
     * possible; reading the UUID works on a detached entity reference.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!PetEntityMarker.isMarked(mob)) return;
        Pet bound = MyPetApi.getPetManager().getPetFromEntity(mob);
        // Unbound = an already-handled removal or an orphan nobody owns; anything but
        // a cached Here status is a flow that manages its own lifecycle (death,
        // demotion already in progress).
        if (bound == null || bound.getCachedStatus() != PetState.Here) return;

        // Arm the stray watch: a dimension transfer re-adds this entity's live copy
        // to the destination world a moment later (see onEntityAddToWorld).
        strayWatchUntil.set(System.currentTimeMillis() + STRAY_WATCH_MILLIS);

        // Capture the entity reference now; it is detached from the world but its
        // UUID stays readable, which is all the deferred index lookup needs.
        final Mob entityRef = mob;
        Bukkit.getGlobalRegionScheduler().runDelayed(MyPetApi.getPlugin(), task -> {
            Pet pet = MyPetApi.getPetManager().getPetFromEntity(entityRef);
            // Still index-bound with cached Here status = no plugin-initiated removal
            // completed for it: the entity left on its own. Demote cleanly — the
            // snapshot is captured from the detached entity where possible, and
            // wantsToRespawn=true has the pet rejoin its owner.
            if (pet == null || pet.getCachedStatus() != PetState.Here) return;
            pet.removePet(true);
        }, 2L);
    }

    /**
     * Removes marked entities that arrive in a world without being registered —
     * the live copy a dimension transfer created (its Pet twin was demoted from
     * the removal side), or any other marked orphan that materializes during the
     * watch window. Gated by {@link #strayWatchUntil}, so outside that window
     * this handler is one volatile read per entity add and otherwise dormant.
     *
     * <p>The removal decision is deferred by two ticks:
     * {@code VanillaMobSpawner} registers a freshly spawned pet entity in the
     * entity→pet index right after it enters the world, so an immediate check
     * would mistake a just-spawned pet for an orphan. The entity scheduler
     * cancels the task if the entity is removed before it runs.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        if (System.currentTimeMillis() > strayWatchUntil.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!PetEntityMarker.isMarked(mob)) return;
        mob.getScheduler().runDelayed(MyPetApi.getPlugin(), task -> {
            if (!mob.isValid()) return;
            if (!PetEntityMarker.isMarked(mob)) return;
            // Registered = an active pet owns this entity (covers both VanillaMobSpawner's
            // just-spawned pets and any pet rebound during the window).
            if (MyPetApi.getPetManager().getPetFromEntity(mob) != null) return;
            mob.remove();
        }, null, 2L);
    }
}
