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
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface StoredMyPet {
    double getExp();

    void setExp(double exp);

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

    void setHealth(double health);

    double getSaturation();

    void setSaturation(double value);

    /**
     * Vanilla entity NBT for this pet, as parsed from Paper's
     * {@code Bukkit.getUnsafe().serializeEntity} bytes.
     * Empty for implementations that don't carry a snapshot
     * (e.g. shop templates).
     */
    CompoundBinaryTag getInfo();

    void setInfo(CompoundBinaryTag info);

    MyPetPlayer getOwner();

    void setOwner(MyPetPlayer owner);

    String getPetName();

    void setPetName(String petName);

    Component getDisplayName();

    MyPetType getPetType();

    void setPetType(MyPetType petType);

    boolean wantsToRespawn();

    void setWantsToRespawn(boolean wantsToRespawn);

    int getRespawnTime();

    void setRespawnTime(int respawnTime);

    Skilltree getSkilltree();

    boolean setSkilltree(Skilltree skilltree);

    CompoundBinaryTag getSkillInfo();

    void setSkills(CompoundBinaryTag skills);

    UUID getUUID();

    void setUUID(UUID uuid);

    String getWorldGroup();

    void setWorldGroup(String worldGroup);

    long getLastUsed();

    void setLastUsed(long lastUsed);
}