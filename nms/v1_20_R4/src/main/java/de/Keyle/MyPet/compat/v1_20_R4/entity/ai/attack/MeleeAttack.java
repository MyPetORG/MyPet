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

package de.Keyle.MyPet.compat.v1_20_R4.entity.ai.attack;

import org.bukkit.craftbukkit.v1_20_R4.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;

@Compat("v1_20_R4")
public class MeleeAttack implements AIGoal {

	MyPet myPet;
	EntityMyPet petEntity;
	net.minecraft.world.entity.LivingEntity targetEntity;
	double range;
	float walkSpeedModifier;
	private int ticksUntilNextHitLeft = 0;
	private final int ticksUntilNextHit;
	private int timeUntilNextNavigationUpdate;

	public MeleeAttack(EntityMyPet petEntity, float walkSpeedModifier, double range, int ticksUntilNextHit) {
		this.petEntity = petEntity;
		this.myPet = petEntity.getMyPet();
		this.walkSpeedModifier = walkSpeedModifier;
		this.range = range;
		this.ticksUntilNextHit = ticksUntilNextHit;
	}

	@Override
	public boolean shouldStart() {
		if (myPet.getDamage() <= 0) {
			return false;
		}
		if (!this.petEntity.hasTarget()) {
			return false;
		}
		net.minecraft.world.entity.LivingEntity targetEntity = ((CraftLivingEntity) this.petEntity.getMyPetTarget()).getHandle();

		if (targetEntity instanceof ArmorStand) {
			return false;
		}
		if (petEntity.getMyPet().getRangedDamage() > 0 && this.petEntity.distanceToSqr(targetEntity.getX(), targetEntity.getBoundingBox().minY, targetEntity.getZ()) >= 20) {
			return false;
		}

		Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
		if (behaviorSkill != null && behaviorSkill.isActive()) {
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
				return false;
			}
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
				if (targetEntity instanceof TamableAnimal && ((TamableAnimal) targetEntity).isTame()) {
					return false;
				}
				if (targetEntity instanceof EntityMyPet) {
					return false;
				}
				if (targetEntity instanceof ServerPlayer) {
					return false;
				}
			}
		}
		this.targetEntity = targetEntity;
		return true;
	}

	@Override
	public boolean shouldFinish() {
		if (!this.petEntity.hasTarget() || !this.petEntity.canMove()) {
			return true;
		} else if (this.targetEntity.getBukkitEntity() != this.petEntity.getMyPetTarget()) {
			return true;
		}
		if (petEntity.getMyPet().getRangedDamage() > 0 && this.petEntity.distanceToSqr(targetEntity.getX(), targetEntity.getBoundingBox().minY, targetEntity.getZ()) >= 20) {
			return true;
		}

		Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
		if (behaviorSkill != null && behaviorSkill.isActive()) {
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
				return true;
			}
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
				if (this.targetEntity instanceof TamableAnimal && ((TamableAnimal) this.targetEntity).isTame()) {
					return true;
				}
				if (this.targetEntity instanceof EntityMyPet) {
					return true;
				}
				return this.targetEntity instanceof ServerPlayer;
			}
		}
		return false;
	}

	@Override
	public void start() {
		this.petEntity.getPetNavigation().getParameters().addSpeedModifier("MeleeAttack", walkSpeedModifier);
		this.petEntity.getPetNavigation().navigateTo((LivingEntity) this.targetEntity.getBukkitEntity()); 
		this.timeUntilNextNavigationUpdate = 0;
	}

	@Override
	public void finish() {
		this.petEntity.getPetNavigation().getParameters().removeSpeedModifier("MeleeAttack");
		this.targetEntity = null;
		this.petEntity.getPetNavigation().stop();
	}

	@Override
	public void tick() {
		this.petEntity.getLookControl().setLookAt(targetEntity, 30.0F, 30.0F);
		if (--this.timeUntilNextNavigationUpdate <= 0) {
			this.timeUntilNextNavigationUpdate = (4 + this.petEntity.getRandom().nextInt(7));
			this.petEntity.getPetNavigation().navigateTo((LivingEntity) targetEntity.getBukkitEntity());
		}
		if (this.petEntity.distanceToSqr(targetEntity.getX(), targetEntity.getBoundingBox().minY, targetEntity.getZ()) - (targetEntity.getBbHeight() * (2. / 3.)) <= this.range && this.ticksUntilNextHitLeft-- <= 0) {
			if (this.petEntity.getSensing().hasLineOfSight(targetEntity)) {
				this.ticksUntilNextHitLeft = ticksUntilNextHit;
				if (this.petEntity instanceof MyPetEquipment) {
					if (((MyPetEquipment) this.petEntity).getEquipment(EquipmentSlot.MainHand) != null) {
						this.petEntity.swing(InteractionHand.MAIN_HAND); // -> swingItem()
					}
				}
				this.petEntity.attack(targetEntity);
			}
		}
	}
}
