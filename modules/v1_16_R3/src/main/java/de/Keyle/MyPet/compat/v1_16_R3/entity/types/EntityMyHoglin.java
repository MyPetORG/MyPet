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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyHoglin;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;

@EntitySize(width = 1.3965F, height = 1.4F)
public class EntityMyHoglin extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyHoglin.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Boolean> NO_SHAKE_WATCHER = DataWatcher.a(EntityMyHoglin.class, DataWatcherRegistry.i);

	public EntityMyHoglin(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.hoglin.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.hoglin.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.hoglin.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Hoglin.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				getMyPet().setBaby(false);
				return EnumInteractionResult.CONSUME;
			}
		}
		return EnumInteractionResult.PASS;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(NO_SHAKE_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		getDataWatcher().set(NO_SHAKE_WATCHER, getMyPet().isShakeImmune());
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.hoglin.step", 0.15F, 1.0F);
	}

	@Override
	public MyHoglin getMyPet() {
		return (MyHoglin) myPet;
	}
}