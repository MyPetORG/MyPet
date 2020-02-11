/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_11_R1.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_11_R1.entity.EntityMyPet;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;

@Compat("v1_11_R1")
public class FollowOwner implements AIGoal {
    private EntityMyPet petEntity;
    private AbstractNavigation nav;
    private int setPathTimer = 0;
    private float stopDistance;
    private double startDistance;
    private float teleportDistance;
    private Control controlPathfinderGoal;
    private EntityPlayer owner;
    private boolean waitForGround = false;

    public FollowOwner(EntityMyPet entityMyPet, double startDistance, float stopDistance, float teleportDistance) {
        this.petEntity = entityMyPet;
        this.nav = entityMyPet.getPetNavigation();
        this.startDistance = startDistance * startDistance;
        this.stopDistance = stopDistance * stopDistance;
        this.teleportDistance = teleportDistance * teleportDistance;
        this.owner = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
    }

    @Override
    public boolean shouldStart() {
        if (controlPathfinderGoal == null) {
            if (petEntity.getPathfinder().hasGoal("Control")) {
                controlPathfinderGoal = (Control) petEntity.getPathfinder().getGoal("Control");
            }
        }
        if (!this.petEntity.canMove()) {
            return false;
        } else if (this.petEntity.getTarget() != null && !this.petEntity.getTarget().isDead()) {
            return false;
        } else if (this.petEntity.getOwner() == null) {
            return false;
        } else if (this.petEntity.h(owner) < this.startDistance) {
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
        } else if (this.petEntity.h(owner) < this.stopDistance) {
            return true;
        } else if (!this.petEntity.canMove()) {
            return true;
        } else if (this.petEntity.getTarget() != null && !this.petEntity.getTarget().isDead()) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        applyWalkSpeed();
        this.setPathTimer = 0;
    }

    @Override
    public void finish() {
        nav.getParameters().removeSpeedModifier("FollowOwner");
        this.nav.stop();
    }

    @Override
    public void tick() {
        Location ownerLocation = this.petEntity.getOwner().getPlayer().getLocation();
        Location petLocation = this.petEntity.getBukkitEntity().getLocation();
        if (ownerLocation.getWorld() != petLocation.getWorld()) {
            return;
        }

        this.petEntity.getControllerLook().a(owner, this.petEntity.cL(), (float) this.petEntity.N());

        if (this.petEntity.canMove()) {
            if (!owner.abilities.isFlying) {
                if (!waitForGround) {
                    if (owner.fallDistance <= 4) {
                        if (this.petEntity.h(owner) >= this.teleportDistance) {
                            if (controlPathfinderGoal.moveTo == null) {
                                if (!petEntity.hasTarget()) {
                                    if (MyPetApi.getPlatformHelper().canSpawn(ownerLocation, this.petEntity)) {
                                        this.petEntity.fallDistance = 0;
                                        this.petEntity.setPositionRotation(ownerLocation.getX(), ownerLocation.getY(), ownerLocation.getZ(), this.petEntity.yaw, this.petEntity.pitch);
                                        this.setPathTimer = 0;
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (owner.onGround) {
                    waitForGround = false;
                }
            } else {
                waitForGround = true;
            }

            if (--this.setPathTimer <= 0) {
                this.setPathTimer = 10;
                if (this.nav.navigateTo(owner.getBukkitEntity())) {
                    applyWalkSpeed();
                }
            }
        }
    }

    private void applyWalkSpeed() {
        float walkSpeed = owner.abilities.walkSpeed;
        if (owner.abilities.isFlying) {
            // make the pet faster when the player is flying
            walkSpeed += owner.abilities.flySpeed;
        } else if (owner.isSprinting()) {
            // make the pet faster when the player is sprinting
            if (owner.getAttributeMap().a(GenericAttributes.MOVEMENT_SPEED) != null) {
                walkSpeed += owner.getAttributeMap().a(GenericAttributes.MOVEMENT_SPEED).getValue();
            }
        } else if (owner.isPassenger() && owner.getVehicle() instanceof EntityLiving) {
            // adjust the speed to the pet can catch up with the vehicle the player is in
            AttributeInstance vehicleSpeedAttribute = ((EntityLiving) owner.getVehicle()).getAttributeMap().a(GenericAttributes.MOVEMENT_SPEED);
            if (vehicleSpeedAttribute != null) {
                walkSpeed = (float) vehicleSpeedAttribute.getValue();
            }
        } else if (owner.hasEffect(MobEffects.FASTER_MOVEMENT)) {
            // make the pet faster when the player is has the SPEED effect
            walkSpeed += owner.getEffect(MobEffects.FASTER_MOVEMENT).getAmplifier() * 0.2 * walkSpeed;
        }
        // make the pet a little bit faster than the player so it can catch up
        walkSpeed += 0.07f;

        nav.getParameters().addSpeedModifier("FollowOwner", walkSpeed);
    }
}