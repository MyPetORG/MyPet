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

package de.Keyle.MyPet.compat.v1_18_R2.entity.types;

import com.comphenix.protocol.wrappers.MinecraftKey;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.compat.v1_18_R2.entity.EntityMyPet;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyVillager extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyVillager.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> UNUSED_WATCHER = SynchedEntityData.defineId(EntityMyVillager.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<VillagerData> PROFESSION_WATCHER = SynchedEntityData.defineId(EntityMyVillager.class, EntityDataSerializers.VILLAGER_DATA);

	public EntityMyVillager(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
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
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Villager.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				getMyPet().setBaby(false);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
		if (MyPetApi.getCompatUtil().isCompatible("1.14.1")) {
			getEntityData().define(UNUSED_WATCHER, 0);
		}
		getEntityData().define(PROFESSION_WATCHER, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		String professionKey = MyVillager.Profession.values()[getMyPet().getProfession()].getKey();
		VillagerProfession profession = Registry.VILLAGER_PROFESSION.get(new ResourceLocation(professionKey));
		VillagerType type = Registry.VILLAGER_TYPE.get(new ResourceLocation(getMyPet().getType().getKey())); //TODO
		this.getEntityData().set(PROFESSION_WATCHER, new VillagerData(type, profession, getMyPet().getVillagerLevel()));
	}

	@Override
	public MyVillager getMyPet() {
		return (MyVillager) myPet;
	}
}
