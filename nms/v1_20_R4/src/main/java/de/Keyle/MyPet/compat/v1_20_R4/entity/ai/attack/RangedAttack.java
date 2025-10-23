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

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.api.skill.skills.Ranged.Projectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetArrow;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetDragonFireball;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetEgg;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetEnderPearl;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetLargeFireball;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetLlamaSpit;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetSmallFireball;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetSnowball;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetTrident;
import de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.nms.MyPetWitherSkull;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

@Compat("v1_20_R4")
public class RangedAttack implements AIGoal {

	private final MyPet myPet;
	private final EntityMyPet entityMyPet;
	private LivingEntity target;
	private int shootTimer;
	private final float walkSpeedModifier;
	private int lastSeenTimer;
	private final float range;

	public RangedAttack(EntityMyPet entityMyPet, float walkSpeedModifier, float range) {
		this.entityMyPet = entityMyPet;
		this.myPet = entityMyPet.getMyPet();
		this.shootTimer = -1;
		this.lastSeenTimer = 0;
		this.walkSpeedModifier = walkSpeedModifier;
		this.range = range * range;
	}

	@Override
	public boolean shouldStart() {
		if (myPet.getRangedDamage() <= 0) {
			return false;
		}
		if (!entityMyPet.canMove()) {
			return false;
		}
		if (!entityMyPet.hasTarget()) {
			return false;
		}

		LivingEntity target = ((CraftLivingEntity) this.entityMyPet.getMyPetTarget()).getHandle();

		if (target instanceof ArmorStand) {
			return false;
		}
		double meleeDamage = myPet.getDamage();
		if (meleeDamage > 0 && this.entityMyPet.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) < 4) {
			Ranged rangedSkill = myPet.getSkills().get(Ranged.class);
			if (meleeDamage > rangedSkill.getDamage().getValue().doubleValue()) {
				return false;
			}
		}

		Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
		if (behaviorSkill != null && behaviorSkill.isActive()) {
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
				return false;
			}
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
				if (target instanceof TamableAnimal && ((TamableAnimal) target).isTame()) {
					return false;
				}
				if (target instanceof EntityMyPet) {
					return false;
				}
				if (target instanceof ServerPlayer) {
					return false;
				}
			}
		}
		this.target = target;
		return true;
	}

	@Override
	public boolean shouldFinish() {
		if (!entityMyPet.hasTarget() || myPet.getRangedDamage() <= 0 || !entityMyPet.canMove()) {
			return true;
		}
		if (this.target.getBukkitEntity() != entityMyPet.getMyPetTarget()) {
			return true;
		}
		double meleeDamage = myPet.getDamage();
		if (meleeDamage > 0 && this.entityMyPet.distanceToSqr(target.getX(), this.target.getBoundingBox().minY, target.getZ()) < 4) {
			Ranged rangedSkill = myPet.getSkills().get(Ranged.class);
			if (meleeDamage > rangedSkill.getDamage().getValue().doubleValue()) {
				return true;
			}
		}

		Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
		if (behaviorSkill != null && behaviorSkill.isActive()) {
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
				return true;
			}
			if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
				if (this.target instanceof TamableAnimal && ((TamableAnimal) this.target).isTame()) {
					return true;
				}
				if (this.target instanceof EntityMyPet) {
					return true;
				}
				return this.target instanceof ServerPlayer;
			}
		}
		return false;
	}

	@Override
	public void finish() {
		this.target = null;
		this.lastSeenTimer = 0;

		this.entityMyPet.getPetNavigation().getParameters().removeSpeedModifier("RangedAttack");
	}

	@Override
	public void tick() {
		double distanceToTarget = this.entityMyPet.distanceToSqr(this.target.getX(), this.target.getBoundingBox().minY, this.target.getZ());
		boolean canSee = this.entityMyPet.getSensing().hasLineOfSight(this.target);

		if (canSee) {
			this.lastSeenTimer++;
		} else {
			this.lastSeenTimer = 0;
		}

		if ((distanceToTarget <= this.range) && (this.lastSeenTimer >= 20)) {
			this.entityMyPet.getPetNavigation().getParameters().removeSpeedModifier("RangedAttack");
			this.entityMyPet.getPetNavigation().stop();
		} else {
			this.entityMyPet.getPetNavigation().getParameters().addSpeedModifier("RangedAttack", walkSpeedModifier);
			this.entityMyPet.getPetNavigation().navigateTo(this.target.getBukkitEntity().getLocation());
		}

		this.entityMyPet.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

		if (--this.shootTimer <= 0) {
			if (distanceToTarget < this.range && canSee) {
				shootProjectile(this.target, (float) myPet.getRangedDamage(), getProjectile());
				Ranged rangedSkill = myPet.getSkills().get(Ranged.class);
				this.shootTimer = rangedSkill.getRateOfFire().getValue();
			}
		}
	}

	private Projectile getProjectile() {
		Ranged rangedSkill = myPet.getSkills().get(Ranged.class);
		if (rangedSkill.isActive()) {
			return rangedSkill.getProjectile().getValue();
		}
		return Projectile.Arrow;
	}

	public void shootProjectile(LivingEntity target, float damage, Projectile projectile) {
		Level world = target.level();

		double minY = this.target.getBoundingBox().minY;
		switch (projectile) {
			case Snowball: {
				MyPetSnowball snowball = new MyPetSnowball(world, entityMyPet);
				double distanceX = target.getX() - entityMyPet.getX();
				double distanceY = target.getY() + target.getEyeHeight() - 1.100000023841858D - snowball.getY();
				double distanceZ = target.getZ() - entityMyPet.getZ();
				double distance20percent = Mth.sqrt((float)(distanceX * distanceX + distanceZ * distanceZ)) * 0.2F;
				snowball.setDamage(damage);
				snowball.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
				entityMyPet.makeSound("entity.arrow.shoot", 0.5F, 0.4F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
				world.addFreshEntity(snowball);
				break;
			}
			case Egg: {
				MyPetEgg egg = new MyPetEgg(world, entityMyPet);
				double distanceX = target.getX() - entityMyPet.getX();
				double distanceY = target.getY() + target.getEyeHeight() - 1.100000023841858D - egg.getY();
				double distanceZ = target.getZ() - entityMyPet.getZ();
				double distance20percent = Mth.sqrt((float)(distanceX * distanceX + distanceZ * distanceZ)) * 0.2F;
				egg.setDamage(damage);
				egg.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
				entityMyPet.makeSound("entity.arrow.shoot", 0.5F, 0.4F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
				world.addFreshEntity(egg);
				break;
			}
			case LargeFireball: {
				double distanceX = this.target.getX() - entityMyPet.getX();
				double distanceY = minY + (double) (this.target.getBbHeight() / 2.0F) - (0.5D + entityMyPet.getY() + (double) (entityMyPet.getBbHeight() / 2.0F));
				double distanceZ = this.target.getZ() - entityMyPet.getZ();
				MyPetLargeFireball largeFireball = new MyPetLargeFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
				largeFireball.setPosRaw(largeFireball.getX(), (entityMyPet.getY() + entityMyPet.getBbHeight() / 2.0F + 0.5D), largeFireball.getZ());
				largeFireball.setDamage(damage);
				world.addFreshEntity(largeFireball);
				entityMyPet.makeSound("entity.ghast.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
				break;
			}
			case SmallFireball: {
				double distanceX = this.target.getX() - entityMyPet.getX();
				double distanceY = minY + (this.target.getBbHeight() / 2.0F) - (0.5D + entityMyPet.getY() + (entityMyPet.getBbHeight() / 2.0F));
				double distanceZ = this.target.getZ() - entityMyPet.getZ();
				MyPetSmallFireball smallFireball = new MyPetSmallFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
				smallFireball.setPosRaw(smallFireball.getX(), (entityMyPet.getY() + entityMyPet.getBbHeight() / 2.0F + 0.5D), smallFireball.getZ());
				smallFireball.setDamage(damage);
				world.addFreshEntity(smallFireball);
				entityMyPet.makeSound("entity.ghast.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
				break;
			}
			case WitherSkull: {
				double distanceX = this.target.getX() - entityMyPet.getX();
				double distanceY = minY + (double) (this.target.getBbHeight() / 2.0F) - (0.5D + entityMyPet.getY() + (double) (entityMyPet.getBbHeight() / 2.0F));
				double distanceZ = this.target.getZ() - entityMyPet.getZ();
				MyPetWitherSkull witherSkull = new MyPetWitherSkull(world, entityMyPet, distanceX, distanceY, distanceZ);
				witherSkull.setPosRaw(witherSkull.getX(), (entityMyPet.getY() + entityMyPet.getBbHeight() / 2.0F + 0.5D), witherSkull.getZ());
				witherSkull.setDamage(damage);
				world.addFreshEntity(witherSkull);
				entityMyPet.makeSound("entity.wither.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
				break;
			}
			case DragonFireball: {
				double distanceX = this.target.getX() - entityMyPet.getX();
				double distanceY = minY + (double) (this.target.getBbHeight() / 2.0F) - (0.5D + entityMyPet.getY() + (double) (entityMyPet.getBbHeight() / 2.0F));
				double distanceZ = this.target.getZ() - entityMyPet.getZ();
				MyPetDragonFireball dragonFireball = new MyPetDragonFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
				dragonFireball.setPosRaw(dragonFireball.getX(), (entityMyPet.getY() + entityMyPet.getBbHeight() / 2.0F + 0.5D), dragonFireball.getZ());
				dragonFireball.setDamage(damage);
				world.addFreshEntity(dragonFireball);
				entityMyPet.makeSound("entity.ender_dragon.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
				break;
			}
			case Trident: {
				MyPetTrident trident = new MyPetTrident(world, entityMyPet);
				trident.setBaseDamage(damage);
				trident.setCritArrow(false);
				entityMyPet.makeSound("item.trident.throw", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
				double distanceX = target.getX() - entityMyPet.getX();
				double distanceY = target.getY() + target.getEyeHeight() - 1.100000023841858D - trident.getY();
				double distanceZ = target.getZ() - entityMyPet.getZ();
				double distance20percent = Mth.sqrt((float)(distanceX * distanceX + distanceZ * distanceZ)) * 0.2F;
				trident.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
				world.addFreshEntity(trident);
				break;
			}
			case EnderPearl: {
				MyPetEnderPearl enderPearl = new MyPetEnderPearl(world, entityMyPet);
				enderPearl.setDamage(damage);
				entityMyPet.makeSound("entity.ender_pearl.throw", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
				double distanceX = target.getX() - entityMyPet.getX();
				double distanceY = target.getY() + target.getEyeHeight() - 1.100000023841858D - enderPearl.getY();
				double distanceZ = target.getZ() - entityMyPet.getZ();
				double distance20percent = Mth.sqrt((float)(distanceX * distanceX + distanceZ * distanceZ)) * 0.2F;
				enderPearl.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
				world.addFreshEntity(enderPearl);
				break;
			}
			case LlamaSpit: {
				MyPetLlamaSpit llamaSpit = new MyPetLlamaSpit(world, entityMyPet);
				llamaSpit.setDamage(damage);
				entityMyPet.makeSound("entity.llama.spit", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
				double distanceX = target.getX() - entityMyPet.getX();
				double distanceY = target.getY() + (target.getEyeHeight() / 3.0F) - llamaSpit.getY();
				double distanceZ = target.getZ() - entityMyPet.getZ();
				double distance20percent = Mth.sqrt((float)(distanceX * distanceX + distanceZ * distanceZ)) * 0.2D;
				llamaSpit.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.5F, 10.0F);
				world.addFreshEntity(llamaSpit);
				break;
			}
			case Arrow:
			default: {
				Arrow arrow = new MyPetArrow(world, entityMyPet);
				arrow.setBaseDamage(damage);
				arrow.setCritArrow(false);
				entityMyPet.makeSound("entity.arrow.shoot", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
				double distanceX = target.getX() - entityMyPet.getX();
				double distanceY = target.getY() + target.getEyeHeight() - 1.100000023841858D - arrow.getY();
				double distanceZ = target.getZ() - entityMyPet.getZ();
				double distance20percent = Mth.sqrt((float)(distanceX * distanceX + distanceZ * distanceZ)) * 0.2F;
				arrow.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
				world.addFreshEntity(arrow);
				break;
			}
		}
	}
}
