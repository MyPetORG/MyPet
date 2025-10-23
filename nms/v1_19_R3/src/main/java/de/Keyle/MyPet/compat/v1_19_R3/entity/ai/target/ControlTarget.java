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

package de.Keyle.MyPet.compat.v1_19_R3.entity.ai.target;

import org.bukkit.craftbukkit.v1_19_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R3.entity.ai.movement.Control;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;

@Compat("v1_19_R3")
public class ControlTarget implements AIGoal {

	private final MyPet myPet;
	private final EntityMyPet petEntity;
	private net.minecraft.world.entity.LivingEntity target;
	private final float range;
	private Control controlPathfinderGoal;

	public ControlTarget(EntityMyPet petEntity, float range) {
		this.petEntity = petEntity;
		this.myPet = petEntity.getMyPet();
		this.range = range;
	}

	@Override
	public boolean shouldStart() {
		if (controlPathfinderGoal == null) {
			if (petEntity.getPathfinder().hasGoal("Control")) {
				controlPathfinderGoal = (Control) petEntity.getPathfinder().getGoal("Control");
			}
		}
		if (controlPathfinderGoal == null) {
			return false;
		}
		if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
			return false;
		}
		if (controlPathfinderGoal.moveTo != null && petEntity.canMove()) {
			Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
			if (behaviorSkill.isActive()) {
				if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
					return false;
				}
			}
			for (net.minecraft.world.entity.LivingEntity entityLiving : this.petEntity.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, this.petEntity.getBoundingBox().inflate(this.range, 4.0D, this.range))) {
				if (entityLiving != petEntity && !(entityLiving instanceof ArmorStand)) {
					if (entityLiving instanceof ServerPlayer) {
						Player targetPlayer = (Player) entityLiving.getBukkitEntity();
						if (myPet.getOwner().equals(targetPlayer)) {
							continue;
						} else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
							continue;
						}
					} else if (entityLiving instanceof TamableAnimal) {
						TamableAnimal tameable = (TamableAnimal) entityLiving;
						if (tameable.isTame() && tameable.getOwner() != null) {
							Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
							if (myPet.getOwner().equals(tameableOwner)) {
								continue;
							} else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), tameableOwner, true)) {
								continue;
							}
						}
					} else if (entityLiving instanceof EntityMyPet) {
						MyPet targetMyPet = ((EntityMyPet) entityLiving).getMyPet();
						if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
							continue;
						}
					}
					if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), entityLiving.getBukkitEntity())) {
						continue;
					}
					if (behaviorSkill != null) {
						if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
							if (entityLiving instanceof TamableAnimal) {
								continue;
							} else if (entityLiving instanceof EntityMyPet) {
								continue;
							} else if (entityLiving instanceof ServerPlayer) {
								continue;
							}
						}
					}
					controlPathfinderGoal.stopControl();
					this.target = entityLiving;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean shouldFinish() {
		if (!petEntity.canMove()) {
			return true;
		}
		if (!petEntity.hasTarget()) {
			return true;
		}

		net.minecraft.world.entity.LivingEntity target = ((CraftLivingEntity) this.petEntity.getMyPetTarget()).getHandle();

		if (target.level != petEntity.level) {
			return true;
		} else if (petEntity.distanceToSqr(target) > 400) {
			return true;
		} else return petEntity.distanceToSqr(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600;
	}

	@Override
	public void start() {
		petEntity.setMyPetTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.Control);
	}

	@Override
	public void finish() {
		petEntity.forgetTarget();
	}
}
