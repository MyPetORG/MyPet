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

import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.util.Scheduler;
import org.bukkit.Location;

import java.util.Optional;

public interface MyPet extends StoredMyPet, Scheduler {
    MyPetExperience getExperience();

    enum PetState {
        Dead, Despawned, PetState, Here
    }

    enum SpawnFlags {
        Success, NoSpace, AlreadyHere, Dead, Canceled, OwnerDead, Flying, Spectator, WrongWorldGroup, NotAllowed, InvalidPosition
    }

    void removePet();

    void removePet(boolean wantsToRespawn);

    SpawnFlags createEntity();

    PetState getStatus();

    void setStatus(PetState status);

    Optional<Location> getLocation();

    double getMaxHealth();

    Skills getSkills();

    boolean autoAssignSkilltree();

    Optional<MyPetBukkitEntity> getEntity();

    double getDamage();

    double getRangedDamage();

    boolean isPassiv();

    boolean hasTarget();

    void decreaseSaturation(double value);
}