/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class MyPetManager {
    protected final BiMap<MyPetPlayer, MyPet> mActivePlayerPets = HashBiMap.create();
    protected final BiMap<MyPet, MyPetPlayer> mActivePetsPlayer = mActivePlayerPets.inverse();

    // Active -------------------------------------------------------------------

    public MyPet getMyPet(MyPetPlayer owner) {
        return mActivePlayerPets.get(owner);
    }

    public MyPet getMyPet(Player owner) {
        return mActivePlayerPets.get(MyPetApi.getPlayerManager().getMyPetPlayer(owner));
    }

    public MyPet[] getAllActiveMyPets() {
        return mActivePetsPlayer.keySet().toArray(new MyPet[0]);
    }

    /**
     * Resolves a Bukkit entity to its owning {@link MyPet} by checking the
     * {@code mypet:pet} PDC marker and scanning active pets by UUID. Returns
     * {@code null} if the entity is not a MyPet or the pet is no longer tracked.
     */
    public MyPet getMyPetFromEntity(Entity entity) {
        if (entity == null) return null;
        NamespacedKey markerKey = new NamespacedKey("mypet", "pet");
        if (!entity.getPersistentDataContainer().has(markerKey,
                PersistentDataType.BYTE)) {
            return null;
        }
        java.util.UUID uuid = entity.getUniqueId();
        for (MyPet pet : getAllActiveMyPets()) {
            Mob mob = pet.getBukkitEntity();
            if (mob != null && uuid.equals(mob.getUniqueId())) {
                return pet;
            }
        }
        return null;
    }

    public boolean hasActiveMyPet(MyPetPlayer player) {
        return mActivePlayerPets.containsKey(player);
    }

    public boolean hasActiveMyPet(Player player) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            return hasActiveMyPet(petPlayer);
        }
        return false;
    }

    public boolean hasActiveMyPet(String name) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(name)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(name);
            return hasActiveMyPet(petPlayer);
        }
        return false;
    }

    // Inactive -----------------------------------------------------------------

    /**
     * Snapshot a pet's current state into a fresh {@link StoredMyPet}, suitable
     * for hand-off across an active/inactive boundary (deactivation, trade,
     * shop purchase, admin clone). Concrete return is the immutable
     * {@code PersistedMyPet} record; callers needing to tweak fields after
     * the fact should use its {@code withX} methods or {@code toBuilder}.
     */
    public abstract StoredMyPet getInactiveMyPetFromMyPet(StoredMyPet storedMyPet);

    // All ----------------------------------------------------------------------

    public abstract Optional<MyPet> activateMyPet(StoredMyPet storedMyPet);

    public abstract boolean deactivateMyPet(MyPetPlayer owner, boolean update);

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
    public abstract CompletableFuture<List<StoredMyPet>> getStoredPets(MyPetPlayer owner);

    public int countActiveMyPets() {
        return mActivePetsPlayer.size();
    }
}