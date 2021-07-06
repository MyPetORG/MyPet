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

package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.target;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_17_R1.entity.ai.movement.Control;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Compat("v1_17_R1")
public class ControlTarget implements AIGoal {

	private final MyPet myPet;
	private final EntityMyPet petEntity;
	private EntityLiving target;
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
			for (EntityLiving entityLiving : this.petEntity.t.a(EntityLiving.class, this.petEntity.getBoundingBox().grow(this.range, 4.0D, this.range))) {
				if (entityLiving != petEntity && !(entityLiving instanceof EntityArmorStand)) {
					if (entityLiving instanceof EntityPlayer) {
						Player targetPlayer = (Player) entityLiving.getBukkitEntity();
						if (myPet.getOwner().equals(targetPlayer)) {
							continue;
						} else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
							continue;
						}
					} else if (entityLiving instanceof EntityTameableAnimal) {
						EntityTameableAnimal tameable = (EntityTameableAnimal) entityLiving;
						if (tameable.isTamed() && tameable.getOwner() != null) {
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
							if (entityLiving instanceof EntityTameableAnimal) {
								continue;
							} else if (entityLiving instanceof EntityMyPet) {
								continue;
							} else if (entityLiving instanceof EntityPlayer) {
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

		EntityLiving target = ((CraftLivingEntity) this.petEntity.getTarget()).getHandle();

		if (target.t != petEntity.t) {
			return true;
		} else if (petEntity.f(target) > 400) {
			return true;
		} else return petEntity.f(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600;
	}

	@Override
	public void start() {
		petEntity.setTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.Control);
	}

	@Override
	public void finish() {
		petEntity.forgetTarget();
	}
}
