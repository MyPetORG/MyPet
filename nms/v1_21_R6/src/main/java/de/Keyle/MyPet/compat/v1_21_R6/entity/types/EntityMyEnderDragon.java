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

package de.Keyle.MyPet.compat.v1_21_R6.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_21_R6.entity.EntityMyFlyingPet;
import de.Keyle.MyPet.compat.v1_21_R6.entity.EntityMyPetPart;
import de.Keyle.MyPet.compat.v1_21_R6.entity.ai.attack.MeleeAttack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.Arrays;

@EntitySize(width = 1.F, height = 1.F)
public class EntityMyEnderDragon extends EntityMyFlyingPet {

	protected boolean registered = false;
	protected int d = -1;
	public final double[][] c = new double[64][3];

	public EntityMyPetPart[] children;

	public EntityMyEnderDragon(Level world, MyPet myPet) {
		super(world, myPet);
		indirectRiding = true;

		children = new EntityMyPetPart[]{
				new EntityMyPetPart(this, "head", 1.0F, 1.0F),
				new EntityMyPetPart(this, "head", 1.0F, 1.0F),
				new EntityMyPetPart(this, "neck", 3.0F, 3.0F),
				new EntityMyPetPart(this, "body", 5.0F, 3.0F),
				new EntityMyPetPart(this, "tail", 2.0F, 2.0F),
				new EntityMyPetPart(this, "tail", 2.0F, 2.0F),
				new EntityMyPetPart(this, "tail", 2.0F, 2.0F),
				new EntityMyPetPart(this, "wing", 4.0F, 2.0F),
				new EntityMyPetPart(this, "wing", 4.0F, 2.0F),
		};
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.ender_dragon.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.ender_dragon.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.ender_dragon.ambient";
	}

	@Override
	public void setPathfinder() {
		super.setPathfinder();
		petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 8.5, 20));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.EnderDragon.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}
		// TODO Since dragons are disabled/bugged anyway atm, it's commented out for 1.21.8+ support
		/*if (!registered && this.valid) {
			if (this.getCommandSenderWorld() instanceof ServerLevel) {
				ServerLevel world = (ServerLevel) this.getCommandSenderWorld();
				
				//The next part used to be prettier but... whilst it is listed everywhere I looked, ServerLevel dragonParts isn't public so...
				Int2ObjectMap dragonParts = (Int2ObjectMap) ReflectionUtil.getFieldValue(dragonPartsField, world);
				Arrays.stream(this.children)
						.forEach(entityMyPetPart -> dragonParts.put(entityMyPetPart.getId(), entityMyPetPart));
				ReflectionUtil.setFieldValue(dragonPartsField, world, dragonParts);
			}
			this.registered = true;
		}*/
		//registerPartsIfNeeded(); // TODO Generated solution for dragon parts, requires testing

	}

	/* fmm...
	@Override
	public void discard() {
		super.discard();
		Arrays.stream(this.children).forEach((en) -> (en.getBukkitEntity().getHandle()).discard());
	}
 	*/

	public void registerPartsIfNeeded() {
		if (!this.registered && this.valid) {
			Level lvl = this.level(); // 1.21.8 replacement for getCommandSenderWorld()
			if (lvl instanceof ServerLevel server) {

				Int2ObjectMap<Entity> dragonParts = getDragonParts(server);

				// Put your multipart children into the existing map
				for (EntityMyPetPart part : this.children) {
					dragonParts.put(part.getId(), part);
				}

				// NOTE: no need to set the field back; we're mutating the same map instance
			}
			this.registered = true;
		}
	}

	private static Int2ObjectMap<Entity> getDragonParts(ServerLevel serverLevel) {
		try {
			// Mojang-mapped name is still "dragonParts" in 1.21.8
			Field f = ServerLevel.class.getDeclaredField("dragonParts");
			f.setAccessible(true);
			return (Int2ObjectMap<Entity>) f.get(serverLevel);
		} catch (NoSuchFieldException e) {
			// Fallback by type in case the name ever changes
			for (Field f : ServerLevel.class.getDeclaredFields()) {
				if (Int2ObjectMap.class.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					try { return (Int2ObjectMap<Entity>) f.get(serverLevel); } catch (IllegalAccessException ignored) {}
				}
			}
			throw new RuntimeException("Could not locate ServerLevel.dragonParts", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Accessing ServerLevel.dragonParts failed", e);
		}
	}

	@Override
	public void die(DamageSource damagesource) {
		super.die(damagesource);
		Arrays.stream(this.children).forEach(Entity::discard);
	}

	@Override
	public void aiStep() {
		super.aiStep();

		//        if (++this.d == this.c.length) {
		//            this.d = 0;
		//        }
		//
		//        float f7 = (float) (this.a(5, 1.0F)[1] - this.a(10, 1.0F)[1]) * 10.0F * 0.017453292F;
		//        float f8 = MathHelper.cos(f7);
		//        float f9 = MathHelper.sin(f7);
		//        float f10 = this.yaw * 0.017453292F;
		//        float f11 = MathHelper.sin(f10);
		//        float f12 = MathHelper.cos(f10);
		//        this.children[2].tick();
		//        this.children[2].setPositionRotation(this.getX() + (double) (f11 * 0.5F), this.getY(), this.getZ() - (double) (f12 * 0.5F), 0.0F, 0.0F);
		//        this.children[6].tick();
		//        this.children[6].setPositionRotation(this.getX() + (double) (f12 * 4.5F), this.getY() + 2.0D, this.getZ() + (double) (f11 * 4.5F), 0.0F, 0.0F);
		//        this.children[7].tick();
		//        this.children[7].setPositionRotation(this.getX() - (double) (f12 * 4.5F), this.getY() + 2.0D, this.getZ() - (double) (f11 * 4.5F), 0.0F, 0.0F);
		//
		//        float f13 = MathHelper.sin(this.yaw * 0.017453292F - this.be * 0.01F);
		//        float f14 = MathHelper.cos(this.yaw * 0.017453292F - this.be * 0.01F);
		//        this.children[0].tick();
		//        this.children[1].tick();
		//        double f3 = this.v(1.0F);
		//        this.children[0].setPositionRotation(this.getX() + (double) (f13 * 6.5F * f8), this.getY() + (double) f3 + (double) (f9 * 6.5F), this.getZ() - (double) (f14 * 6.5F * f8), 0.0F, 0.0F);
		//        this.children[1].setPositionRotation(this.getX() + (double) (f13 * 5.5F * f8), this.getY() + (double) f3 + (double) (f9 * 5.5F), this.getZ() - (double) (f14 * 5.5F * f8), 0.0F, 0.0F);
	}
}
