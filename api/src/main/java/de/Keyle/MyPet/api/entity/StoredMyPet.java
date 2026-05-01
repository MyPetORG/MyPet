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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Read-only view of a pet at rest. Two implementations exist:
 *
 * <ul>
 *   <li>{@code PersistedMyPet} — the immutable record that the repository
 *       loads from disk and that {@code MyPetManager} round-trips between
 *       active and inactive states. "Mutations" return new instances via
 *       {@code withX} or {@code Builder}.
 *   <li>{@code ShopMyPet} — a mutable shop-template class used only by the
 *       in-game pet shop GUI. Many of its accessors are computed on demand
 *       (e.g. {@link #getHealth} returns the type's start HP) rather than
 *       stored, which is why it remains a class rather than a record.
 * </ul>
 *
 * <p>Sealing this interface would be a natural fit but is not done because
 * {@code ShopMyPet} cannot move into the {@code api} module without dragging
 * shop-only dependencies with it.
 */
public interface StoredMyPet {
    UUID getUUID();

    MyPetPlayer getOwner();

    MyPetType getPetType();

    String getPetName();

    /**
     * MiniMessage-rendered pet name. Default implementation deserializes
     * {@link #getPetName} via {@link Util#SANITIZED_MINIMESSAGE}; both
     * implementations were previously identical.
     */
    default Component getDisplayName() {
        return Util.SANITIZED_MINIMESSAGE.deserialize(getPetName());
    }

    String getWorldGroup();

    double getExp();

    /**
     * Pet's experience level — derived from {@link #getExp()} and the
     * skilltree's XP curve via {@link ExperienceCache}. Returns {@code 0}
     * if the cache has no curve loaded for this pet's world group / type
     * (e.g. before skilltrees have finished loading, or for a pet whose
     * world group has no configured curve).
     */
    default int getLevel() {
        return MyPetApi.getServiceManager().getService(ExperienceCache.class)
                .map(cache -> cache.getLevel(getWorldGroup(), getPetType(), getExp()))
                .orElse(0);
    }

    double getHealth();

    double getSaturation();

    int getRespawnTime();

    boolean wantsToRespawn();

    long getLastUsed();

    Skilltree getSkilltree();

    CompoundBinaryTag getSkillInfo();

    /**
     * Vanilla entity NBT for this pet, as parsed from Paper's
     * {@code Bukkit.getUnsafe().serializeEntity} bytes.
     * Empty for implementations that don't carry a snapshot
     * (e.g. shop templates).
     */
    CompoundBinaryTag getInfo();
}
