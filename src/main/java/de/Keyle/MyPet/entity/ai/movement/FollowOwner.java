/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.util.BukkitUtil;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;

public class FollowOwner extends AIGoal {
    private EntityMyPet petEntity;
    private float walkSpeedModifier;
    private AbstractNavigation nav;
    private int setPathTimer = 0;
    private float stopDistance;
    private double startDistance;
    private float teleportDistance;
    private Control controlPathfinderGoal;
    private EntityPlayer owner;

    public FollowOwner(EntityMyPet entityMyPet, float walkSpeedModifier, double startDistance, float stopDistance, float teleportDistance) {
        this.petEntity = entityMyPet;
        this.walkSpeedModifier = walkSpeedModifier;
        this.nav = entityMyPet.petNavigation;
        this.startDistance = startDistance * startDistance;
        this.stopDistance = stopDistance * stopDistance;
        this.teleportDistance = teleportDistance * teleportDistance;
        this.owner = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
    }

    @Override
    public boolean shouldStart() {
        if (controlPathfinderGoal == null) {
            if (petEntity.petPathfinderSelector.hasGoal("Control")) {
                controlPathfinderGoal = (Control) petEntity.petPathfinderSelector.getGoal("Control");
            }
        }
        if (!this.petEntity.canMove()) {
            return false;
        } else if (this.petEntity.getGoalTarget() != null && this.petEntity.getGoalTarget().isAlive()) {
            return false;
        } else if (this.petEntity.getOwner() == null) {
            return false;
        } else if (this.petEntity.e(owner) < this.startDistance) {
            return false;
        } else if (controlPathfinderGoal != null && controlPathfinderGoal.moveTo != null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (controlPathfinderGoal.moveTo != null) {
            return true;
        } else if (this.petEntity.getOwner() == null) {
            return true;
        } else if (this.petEntity.e(owner) < this.stopDistance) {
            return true;
        } else if (!this.petEntity.canMove()) {
            return true;
        } else if (this.petEntity.getGoalTarget() != null && this.petEntity.getGoalTarget().isAlive()) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        nav.getParameters().addSpeedModifier("FollowOwner", walkSpeedModifier);
        this.setPathTimer = 0;
    }

    @Override
    public void finish() {
        nav.getParameters().removeSpeedModifier("FollowOwner");
        this.nav.stop();
    }

    @Override
    public void tick() {
        Location ownerLocation = this.petEntity.getMyPet().getOwner().getPlayer().getLocation();
        Location petLocation = this.petEntity.getMyPet().getLocation();
        if (ownerLocation.getWorld() != petLocation.getWorld()) {
            return;
        }

        this.petEntity.getControllerLook().a(owner, 10.0F, (float) this.petEntity.bv());

        if (this.petEntity.canMove()) {
            if (--this.setPathTimer <= 0) {
                this.setPathTimer = 10;

                if (!this.nav.navigateTo(owner)) {
                    if (owner.onGround && this.petEntity.e(owner) > this.teleportDistance && controlPathfinderGoal.moveTo == null && petEntity.goalTarget == null && BukkitUtil.canSpawn(ownerLocation, this.petEntity)) {
                        this.petEntity.setPositionRotation(ownerLocation.getX(), ownerLocation.getY(), ownerLocation.getZ(), this.petEntity.yaw, this.petEntity.pitch);
                        this.nav.navigateTo(owner);
                    }
                }
            }
        }
    }
}