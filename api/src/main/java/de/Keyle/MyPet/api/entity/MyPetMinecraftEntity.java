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

import de.Keyle.MyPet.api.entity.ai.AIGoalSelector;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public interface MyPetMinecraftEntity {
    boolean isMyPet();

    MyPet getMyPet();

    AIGoalSelector getPathfinder();

    AIGoalSelector getTargetSelector();

    void setPathfinder();

    double getWalkSpeed();

    void makeSound(String sound, float volume, float pitch);

    MyPetBukkitEntity getBukkitEntity();

    MyPetPlayer getOwner();

    void updateNameTag();

    void setLocation(Location loc);

    AbstractNavigation getPetNavigation();

    void updateVisuals();

    TargetPriority getTargetPriority();

    void forgetTarget();

    boolean hasTarget();

    boolean hasMyPetRider();

    void showPotionParticles(Color color);

    void hidePotionParticles();

    boolean isSitting();

    void setSitting(boolean sitting);

    UUID getUniqueID();

    LivingEntity getMyPetTarget();

    void setMyPetTarget(LivingEntity entity, TargetPriority priority);

    /**
     * Broadcasts an NMS entity event to all tracking players.
     * Used for client-side animations (e.g., byte 10 = sheep eat animation).
     */
    void broadcastEntityEvent(byte eventId);

    default boolean floatsInLava() {
        return false;
    }

    default boolean specialFloat() {
        return false;
    }

    /**
     * Whether this entity uses Paper Goal-based movement control instead of NMS MoveControl.
     * When true, the NMS {@code getMoveControl().tick()} call should be skipped because
     * a Paper Goal (e.g., MyPetFlyingMovementGoal or MyPetAquaticMovementGoal) handles
     * per-tick movement execution via Paper API.
     */
    default boolean usesPaperMovement() {
        return false;
    }
}