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

package de.Keyle.MyPet.compat.v1_16_R3.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MySnowman;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;

import java.lang.reflect.InvocationTargetException;

import static de.Keyle.MyPet.compat.v1_16_R3.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.7F, height = 1.7F)
public class EntityMySnowman extends EntityMyPet {

	private static final DataWatcherObject<Byte> SHEARED_WATCHER = DataWatcher.a(EntityMySnowman.class, DataWatcherRegistry.a);

	public EntityMySnowman(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Item.getItemOf(Blocks.PUMPKIN) && getMyPet().isSheared() && entityhuman.isSneaking()) {
				getMyPet().setSheared(false);
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				return EnumInteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && !getMyPet().isSheared() && entityhuman.isSneaking()) {
				getMyPet().setSheared(true);
				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					try {
						itemStack.damage(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastItemBreak(enumhand));
					} catch (Error e) {
						// TODO REMOVE
						itemStack.damage(1, entityhuman, (entityhuman1) -> {
							try {
								ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
							} catch (IllegalAccessException | InvocationTargetException ex) {
								ex.printStackTrace();
							}
						});
					}
				}
				return EnumInteractionResult.CONSUME;
			}
		}
		return EnumInteractionResult.PASS;
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(SHEARED_WATCHER, (byte) (getMyPet().isSheared() ? 0 : 16));
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();

		getDataWatcher().register(SHEARED_WATCHER, (byte) 16);
	}

	@Override
	protected String getDeathSound() {
		return "entity.snow_golem.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.snow_golem.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.snow_golem.ambient";
	}

	@Override
	public void playPetStepSound() {
		makeSound("block.snow.step", 0.15F, 1.0F);
	}

	@Override
	public MySnowman getMyPet() {
		return (MySnowman) myPet;
	}
}