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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyVillager extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyVillager.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Integer> UNUSED_WATCHER = DataWatcher.a(EntityMyVillager.class, DataWatcherRegistry.b);
	private static final DataWatcherObject<VillagerData> PROFESSION_WATCHER = DataWatcher.a(EntityMyVillager.class, DataWatcherRegistry.q);

	public EntityMyVillager(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.villager.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.villager.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.villager.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Villager.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		if (MyPetApi.getCompatUtil().isCompatible("1.14.1")) {
			getDataWatcher().register(UNUSED_WATCHER, 0);
		}
		getDataWatcher().register(PROFESSION_WATCHER, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		String professionKey = MyVillager.Profession.values()[getMyPet().getProfession()].getKey();
		VillagerProfession profession = IRegistry.VILLAGER_PROFESSION.get(new MinecraftKey(professionKey));
		VillagerType type = IRegistry.VILLAGER_TYPE.get(new MinecraftKey(getMyPet().getType().getKey()));
		getDataWatcher().set(PROFESSION_WATCHER, new VillagerData(type, profession, getMyPet().getVillagerLevel()));
	}

	@Override
	public MyVillager getMyPet() {
		return (MyVillager) myPet;
	}
}