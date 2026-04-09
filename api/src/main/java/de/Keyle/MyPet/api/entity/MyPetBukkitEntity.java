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

import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

public interface MyPetBukkitEntity extends Creature {
    MyPet getMyPet();

    MyPetMinecraftEntity getHandle();

    boolean canMove();

    boolean isSitting();

    void setSitting(boolean sitting);

    MyPetType getPetType();

    MyPetPlayer getOwner();

    void removeEntity();

    void setTarget(LivingEntity target, TargetPriority priority);

    void setTarget(LivingEntity target);

    void forgetTarget();

    boolean hasTarget();

    LivingEntity getMyPetTarget();

    /**
     * Attacks the target using the pet's damage system (MyPet skills, not vanilla attributes).
     * This delegates to the NMS entity's attack method which handles skill damage, kill credit,
     * and swing animation — unlike Bukkit's {@code LivingEntity.attack()} which calls the
     * wrong NMS method signature for MyPet entities.
     */
    boolean attackEntity(LivingEntity target);
}