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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of active (in-world) pets and provides access to
 * stored (inactive) pet records. Maintains a bidirectional mapping
 * between players and their active pet — at most one active pet per
 * player at any time.
 * <p>
 * The concrete implementation in the plugin module handles spawning,
 * despawning, repository I/O, and the active/inactive state transitions.
 */
public abstract class PetManager {
    protected final BiMap<MyPetPlayer, Pet> mActivePlayerPets = HashBiMap.create();
    protected final BiMap<Pet, MyPetPlayer> mActivePetsPlayer = mActivePlayerPets.inverse();
    protected final Map<UUID, Pet> mActivePetsByEntityUuid = new ConcurrentHashMap<>();

    // ─── Active Pets ────────────────────────────────────────────────────────────

    /** Returns the active pet for the given player, or {@code null} if none. */
    public Pet getPet(MyPetPlayer owner) {
        return mActivePlayerPets.get(owner);
    }

    /** Returns the active pet for the given Bukkit player, or {@code null}. */
    public Pet getPet(Player owner) {
        return mActivePlayerPets.get(MyPetApi.getPlayerManager().getMyPetPlayer(owner));
    }

    /** Returns a snapshot array of all currently active pets across all players. */
    public Pet[] getAllActivePets() {
        return mActivePetsPlayer.keySet().toArray(new Pet[0]);
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
     * Deactivates the owner's active pet — despawns the entity and moves
     * the pet back to a stored / inactive state.
     *
     * @param update if {@code true}, persists the pet's current state to
     *               the repository before deactivation
     * @return {@code true} if a pet was deactivated
     */
    public abstract boolean deactivatePet(MyPetPlayer owner, boolean update);

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

    /** Returns the total number of currently active pets across all players. */
    public int countActivePets() {
        return mActivePetsPlayer.size();
    }
}