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

package de.Keyle.MyPet.compat.v1_21_R4.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R4.entity.EntityMyFlyingPet;
import de.Keyle.MyPet.compat.v1_21_R4.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R4.entity.ai.navigation.MyAquaticPetPathNavigation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;

@Compat("v1_21_R4")
public class FollowOwner implements AIGoal {

	private final EntityMyPet petEntity;
	private AbstractNavigation nav;
	protected int setPathTimer = 0;
	private final float stopDistance;
	private final double startDistance;
	private final float teleportDistance;
	private Control controlPathfinderGoal;
	private final Player owner;
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
		} else if (this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead()) {
			return false;
		} else if (this.petEntity.getOwner() == null) {
			return false;
		} else if (this.petEntity.distanceToSqr(owner) < this.startDistance) {
			return false;
		} else return controlPathfinderGoal == null || controlPathfinderGoal.moveTo == null;
	}

	@Override
	public boolean shouldFinish() {
		if (controlPathfinderGoal.moveTo != null) {
			return true;
		} else if (this.petEntity.getOwner() == null) {
			return true;
		} else if (this.petEntity.distanceToSqr(owner) < this.stopDistance) {
			return true;
		} else if (!this.petEntity.canMove()) {
			return true;
		} else return this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead();
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
		this.setPathTimer = 0;
	}

	@Override
	public void tick() {
		Location ownerLocation = this.petEntity.getOwner().getPlayer().getLocation();
		Location petLocation = this.petEntity.getBukkitEntity().getLocation();
		if (ownerLocation.getWorld() != petLocation.getWorld()) {
			return;
		}

		//Look at Owner
		this.petEntity.getLookControl().setLookAt(owner, this.petEntity.getMaxHeadXRot(), this.petEntity.getMaxHeadXRot()); //TODO MIGHT be wrong (also in different places) ->getMaxHeadXRot

		boolean flyingPet = petEntity instanceof EntityMyFlyingPet;

		//Teleportation
		if (this.petEntity.canMove()) {
			if ((!owner.getAbilities().flying && !owner.getBukkitEntity().isGliding()) || flyingPet) {
				if (!waitForGround) {
					if (owner.fallDistance <= 4 || flyingPet) {
						if (this.petEntity.distanceToSqr(owner) >= this.teleportDistance) {
							if (controlPathfinderGoal.moveTo == null) {
								if (!petEntity.hasTarget()) {
									if (MyPetApi.getPlatformHelper().canSpawn(ownerLocation, this.petEntity)) {
										this.petEntity.fallDistance = 0;
										this.petEntity.move(MoverType.SELF, new Vec3(ownerLocation.getX(), ownerLocation.getY(), ownerLocation.getZ()));
										this.setPathTimer = 0;
										return;
									}
								}
							}
						}
					}
				} else if (owner.onGround()) {
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
		float walkSpeed = owner.getAbilities().walkingSpeed;
		if (owner.getAbilities().flying) {
			// make the pet faster when the player is flying
			walkSpeed += owner.getAbilities().flyingSpeed;
		} else if (owner.isSprinting() || owner.getBukkitEntity().isGliding()) {
			// make the pet faster when the player is sprinting
			if (owner.getAttributes().getInstance(Attributes.MOVEMENT_SPEED) != null) {
				walkSpeed += owner.getAttributes().getInstance(Attributes.MOVEMENT_SPEED).getValue() - 0.037f;
			}
		} else if (owner.isPassenger() && owner.getVehicle() instanceof LivingEntity) {
			// adjust the speed to the pet can catch up with the vehicle the player is in
			AttributeInstance vehicleSpeedAttribute = ((LivingEntity) owner.getVehicle()).getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
			if (vehicleSpeedAttribute != null) {
				walkSpeed = (float) vehicleSpeedAttribute.getValue();
			}
		} else if (owner.hasEffect(MobEffects.SPEED)) {
			// make the pet faster when the player is has the SPEED effect
			// TODO check if this canbe removed in later versions (again)
			for(MobEffectInstance eff : owner.getActiveEffects()) {
				if (eff.getEffect() instanceof MobEffect && eff.getEffect() == MobEffects.SPEED) {
					walkSpeed += eff.getAmplifier() * 0.2 * walkSpeed;
					break;
				}
			}
		}

		// make aquatic/flying pets faster
		// This is actually due to there being a difference between MovementSpeed and FlyingSpeed, FlyingSpeed being higher
		// (I don't completely know why this is the case for swimming but I imagine it has a similar reason)
		if((this.petEntity.isInWater() || this.petEntity.getInBlockState().is(Blocks.BUBBLE_COLUMN)) && this.petEntity.getNavigation() instanceof MyAquaticPetPathNavigation) {
			walkSpeed += 0.6f;
			if(owner.hasEffect(MobEffects.DOLPHINS_GRACE)) {
				walkSpeed += 0.08f;
			}
		}

		if(this.petEntity.getMoveControl() instanceof MyPetFlyingMoveControl) {
			walkSpeed += 0.575f;
			if(owner.isSprinting()) {
				walkSpeed -= 0.05f;
			}
		}

		// make the pet a little bit faster than the player so it can catch up
		walkSpeed += 0.07f;
		nav.getParameters().addSpeedModifier("FollowOwner", walkSpeed);
	}
}
