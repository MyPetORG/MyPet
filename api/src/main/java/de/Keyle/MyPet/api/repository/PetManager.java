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

package de.Keyle.MyPet.api.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of active (in-world) pets and provides access to
 * stored (inactive) pet records. Maps each player to their active pets, in
 * activation order; how many a player may have at once is governed by
 * {@link #getMaxActivePets(MyPetPlayer)}, not by this container.
 * <p>
 * The concrete implementation in the plugin module handles spawning,
 * despawning, repository I/O, and the active/inactive state transitions.
 */
public abstract class PetManager {
    protected final ListMultimap<MyPetPlayer, Pet> mActivePlayerPets = ArrayListMultimap.create();
    protected final Map<UUID, Pet> mActivePetsByEntityUuid = new ConcurrentHashMap<>();

    /**
     * Owner UUIDs known to have at least one pet — active or stored. Populated
     * asynchronously (repository I/O) at the few points ownership can change, so
     * that {@link #ownsAnyPet(UUID)} can answer synchronously for callers on the
     * main thread (e.g. the PlaceholderAPI hook). May lag reality by the duration
     * of an in-flight refresh.
     */
    protected final Set<UUID> petOwners = ConcurrentHashMap.newKeySet();

    // ─── Active Pets ────────────────────────────────────────────────────────────

    /**
     * Returns the player's primary active pet — the first one activated that is
     * still active — or {@code null} if none.
     * <p>
     * Prefer {@link #getPets(MyPetPlayer)} for anything that should affect every
     * pet the player has out.
     */
    public Pet getPet(MyPetPlayer owner) {
        List<Pet> pets = mActivePlayerPets.get(owner);
        return pets.isEmpty() ? null : pets.get(0);
    }

    /** Returns the primary active pet for the given Bukkit player, or {@code null}. */
    public Pet getPet(Player owner) {
        return getPet(MyPetApi.getPlayerManager().getMyPetPlayer(owner));
    }

    /**
     * Returns all of the player's active pets, in activation order. Never {@code null}.
     * <p>
     * This is an immutable <em>snapshot</em>, not a view: callers routinely iterate
     * it while deactivating pets inside the loop, which would otherwise mutate the
     * backing multimap mid-iteration.
     */
    public List<Pet> getPets(MyPetPlayer owner) {
        return List.copyOf(mActivePlayerPets.get(owner));
    }

    /** Returns all active pets for the given Bukkit player, in activation order. */
    public List<Pet> getPets(Player owner) {
        return getPets(MyPetApi.getPlayerManager().getMyPetPlayer(owner));
    }

    /**
     * Maximum simultaneously-active pets for a player. Phase 1 pins this at 1 so
     * behavior is unchanged; the {@code mypet.maxActivePets} system property exists
     * only so the e2e suite can prove the containers really hold more than one.
     * Phase 2 replaces this with MyPetGlobal.Misc.MAX_ACTIVE_PET_COUNT plus the
     * MyPet.petlimit.active.&lt;n&gt; permission ladder — see MyPetORG/MyPet#1435.
     */
    protected int getMaxActivePets(MyPetPlayer owner) {
        return Integer.getInteger("mypet.maxActivePets", 1);
    }

    /** Returns a snapshot array of all currently active pets across all players. */
    public Pet[] getAllActivePets() {
        return mActivePlayerPets.values().toArray(new Pet[0]);
    }

    /**
     * Resolves a Bukkit entity to its owning {@link Pet} via the entity-UUID
     * index. Returns {@code null} if the entity is not a live pet entity.
     */
    public Pet getPetFromEntity(Entity entity) {
        if (entity == null) return null;
        // Subparts of a ComplexLivingEntity (EnderDragon head/neck/body/tail/
        // wings) carry their own entity ID and UUID server-side.
        if (entity instanceof ComplexEntityPart part) {
            entity = part.getParent();
        }
        return mActivePetsByEntityUuid.get(entity.getUniqueId());
    }

    /** Registers the live entity→pet binding. Called from {@code Pet#setBukkitEntity}. */
    public void registerPetEntity(UUID entityUuid, Pet pet) {
        mActivePetsByEntityUuid.put(entityUuid, pet);
    }

    /** Removes a live entity→pet binding. Called from {@code Pet#setBukkitEntity}. */
    public void unregisterPetEntity(UUID entityUuid) {
        mActivePetsByEntityUuid.remove(entityUuid);
    }

    /** Returns {@code true} if the given player has an active pet. */
    public boolean hasActivePet(MyPetPlayer player) {
        return mActivePlayerPets.containsKey(player);
    }

    /** Returns {@code true} if the given Bukkit player has an active pet. */
    public boolean hasActivePet(Player player) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            return hasActivePet(petPlayer);
        }
        return false;
    }

    /** Returns {@code true} if the player with this name has an active pet. */
    public boolean hasActivePet(String name) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(name)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(name);
            return hasActivePet(petPlayer);
        }
        return false;
    }

    /**
     * Returns {@code true} if the player owns at least one pet — active or stored.
     * Reads the cached {@link #petOwners} set, so it is safe to call on the main
     * thread. The value is refreshed asynchronously on login and whenever a pet is
     * created or removed; see the concrete implementation's {@code refreshOwnership}.
     */
    public boolean ownsAnyPet(UUID playerUuid) {
        return petOwners.contains(playerUuid);
    }

    /** Updates the cached ownership flag for a player. Called by the async refresh. */
    public void setOwnsPet(UUID playerUuid, boolean owns) {
        if (owns) {
            petOwners.add(playerUuid);
        } else {
            petOwners.remove(playerUuid);
        }
    }

    // ─── Inactive / Stored ──────────────────────────────────────────────────────

    /**
     * Returns an immutable point-in-time snapshot of an active pet, suitable for
     * hand-off across an active/inactive boundary (deactivation, trade, admin
     * clone). Callers needing to tweak fields after the fact should use the
     * record's {@code withX} methods or {@code toBuilder}.
     *
     * @param activePet the live pet to snapshot
     * @return an immutable record carrying the pet's persistent fields
     */
    public abstract PersistedPet snapshot(Pet activePet);

    // ─── Activation / Deactivation ──────────────────────────────────────────────

    /**
     * Activates a stored pet — creates the live {@link Pet} instance,
     * registers it in the active map, and spawns the entity if possible.
     *
     * @return the activated pet, or empty if activation failed (e.g.,
     *         player already has an active pet, or spawn conditions not met)
     */
    public abstract Optional<Pet> activatePet(StoredPet storedPet);

    /**
     * Deactivates one of the owner's active pets — despawns the entity and
     * moves the pet back to a stored / inactive state.
     *
     * @param pet    the pet to deactivate; must currently be active for {@code owner}
     * @param update if {@code true}, persists the pet's current state to
     *               the repository before deactivation
     * @return {@code true} if the pet was deactivated
     */
    public abstract boolean deactivatePet(MyPetPlayer owner, Pet pet, boolean update);

    /**
     * Deactivates every pet the owner currently has active, in activation order.
     *
     * @param update if {@code true}, persists each pet's state before deactivating it
     * @return {@code true} if at least one pet was deactivated
     */
    public boolean deactivatePets(MyPetPlayer owner, boolean update) {
        boolean deactivatedAny = false;
        for (Pet pet : getPets(owner)) {
            deactivatedAny |= deactivatePet(owner, pet, update);
        }
        return deactivatedAny;
    }

    /**
     * Deactivates the owner's pet.
     *
     * @deprecated a player may have more than one active pet. Use
     *             {@link #deactivatePets(MyPetPlayer, boolean)} to deactivate all of
     *             them, or {@link #deactivatePet(MyPetPlayer, Pet, boolean)} to name
     *             the one you mean. This overload deactivates all of them.
     */
    @Deprecated
    public boolean deactivatePet(MyPetPlayer owner, boolean update) {
        return deactivatePets(owner, update);
    }

    /**
     * Lists all pets — active or stored — owned by the given player.
     *
     * <p>The returned {@link CompletableFuture} completes on a background thread.
     * Callers that intend to touch Bukkit API in a continuation must hop to the
     * appropriate scheduler (e.g.
     * {@code Bukkit.getServer().getGlobalRegionScheduler().run(plugin, t -> ...)}
     * or {@code player.getScheduler().run(plugin, t -> ..., null)}) inside the
     * continuation; touching Bukkit API directly from the continuation is
     * undefined behavior on Paper and an outright crash on Folia.
     */
    public abstract CompletableFuture<List<StoredPet>> getStoredPets(MyPetPlayer owner);

    /**
     * Asynchronously re-derives the cached {@link #ownsAnyPet(UUID)} flag for the
     * given owner from the repository. Safe to ignore the returned future — the
     * cache is updated as a side effect when it completes.
     */
    public abstract CompletableFuture<Void> refreshOwnership(MyPetPlayer owner);

    /** Returns the total number of currently active pets across all players. */
    public int countActivePets() {
        return mActivePlayerPets.size();
    }
}