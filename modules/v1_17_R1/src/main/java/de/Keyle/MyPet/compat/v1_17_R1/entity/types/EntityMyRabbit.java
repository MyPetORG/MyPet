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

package de.Keyle.MyPet.compat.v1_17_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyRabbit;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 0.7F)
public class EntityMyRabbit extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyRabbit.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> VARIANT_WATCHER = SynchedEntityData.defineId(EntityMyRabbit.class, EntityDataSerializers.INT);

	int jumpDelay;

	public EntityMyRabbit(Level world, MyPet myPet) {
		super(world, myPet);
		this.jumpDelay = (this.random.nextInt(20) + 10);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.rabbit.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.rabbit.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.rabbit.ambient";
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.rabbit.jump", 1.0F, 1.0F);
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Rabbit.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				this.getMyPet().setBaby(false);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false); // is baby
		getEntityData().define(VARIANT_WATCHER, 0); // variant
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		this.getEntityData().set(VARIANT_WATCHER, (int) getMyPet().getVariant().getId());
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		/*for(Method m : getNavigation().getClass().getDeclaredMethods()) {
			Bukkit.getConsoleSender().sendMessage("Declared: "+m.getName()+" "+m.getReturnType());
		}
		for(Method m : getNavigation().getClass().getMethods()) {
			Bukkit.getConsoleSender().sendMessage("Normal: "+m.getName()+" "+m.getReturnType());
		}*/

		//if (this.onGround && getNavigation().k() != null && jumpDelay-- <= 0) {	//TODO Figure out k() and getPath() (Spigot and Paper)
		if (this.onGround && jumpDelay-- <= 0) {
			getJumpControl().jump();
			jumpDelay = (this.random.nextInt(10) + 10);
			if (getTarget() != null) {
				jumpDelay /= 3;
			}
			this.level.broadcastEntityEvent(this, (byte) 1);
		}
	}

	@Override
	public MyRabbit getMyPet() {
		return (MyRabbit) myPet;
	}
}
