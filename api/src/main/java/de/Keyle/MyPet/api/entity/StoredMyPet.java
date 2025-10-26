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

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.keyle.knbt.TagCompound;

import java.util.UUID;

public interface StoredMyPet {
    double getExp();

    void setExp(double exp);

    double getHealth();

    void setHealth(double health);

    double getSaturation();

    void setSaturation(double value);

    TagCompound getInfo();

    void setInfo(TagCompound info);

    void setOwner(MyPetPlayer owner);

    MyPetPlayer getOwner();

    String getPetName();

    void setPetName(String petName);

    MyPetType getPetType();

    void setPetType(MyPetType petType);

    boolean wantsToRespawn();

    void setWantsToRespawn(boolean wantsToRespawn);

    int getRespawnTime();

    void setRespawnTime(int respawnTime);

    Skilltree getSkilltree();

    boolean setSkilltree(Skilltree skilltree);

    TagCompound getSkillInfo();

    void setSkills(TagCompound skills);

    UUID getUUID();

    void setUUID(UUID uuid);

    String getWorldGroup();

    void setWorldGroup(String worldGroup);

    long getLastUsed();

    void setLastUsed(long lastUsed);
}