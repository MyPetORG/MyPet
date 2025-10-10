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

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.compat.v1_21_R6.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R6.services.EntityConverterService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R6.inventory.CraftItemStack;

import java.util.Arrays;

import static de.Keyle.MyPet.compat.v1_21_R6.util.HandSlot.getSlotForHand;

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

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
			if (Configuration.MyPet.Villager.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby()) {
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().getSelectedSlot(), ItemStack.EMPTY);
					}
				}
				getMyPet().setBaby(false);
				return InteractionResult.CONSUME;
			}
			if (itemStack.getItem() != Items.SHEARS && canEquip()) {
				ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand));
				if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && itemInSlot != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					ItemEntity entityitem = new ItemEntity(this.level(), this.getX(), this.getY() + 1, this.getZ(), itemInSlot);
					entityitem.pickupDelay = 10;
					entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
					this.level().addFreshEntity(entityitem);
				}
				getMyPet().setEquipment(EquipmentSlot.MainHand, CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().getSelectedSlot(), ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && canEquip()) {
				boolean hadEquipment = false;
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
					if (itemInSlot != null && itemInSlot.getItem() != Items.AIR) {
						ItemEntity entityitem = new ItemEntity(this.level(), this.getX(), this.getY() + 1, this.getZ(), itemInSlot);
						entityitem.pickupDelay = 10;
						entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
						this.level().addFreshEntity(entityitem);
						getMyPet().setEquipment(slot, null);
						hadEquipment = true;
					}
				}
				if (hadEquipment) {
					if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
						try {
							itemStack.hurtAndBreak(1, entityhuman, getSlotForHand(enumhand));
						} catch (Error e) {
							// TODO REMOVE
						}
					}
				}
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(AGE_WATCHER, false);
		if (MyPetApi.getCompatUtil().isCompatible("1.14.1")) {
			builder.define(UNUSED_WATCHER, 0);
		}
		builder.define(PROFESSION_WATCHER, new VillagerData(
			EntityConverterService.VILLAGER_TYPE_REGISTRY.wrapAsHolder(EntityConverterService.VILLAGER_TYPE_REGISTRY.getValue(VillagerType.PLAINS)),
			EntityConverterService.VILLAGER_PROFESSION_REGISTRY.wrapAsHolder(EntityConverterService.VILLAGER_PROFESSION_REGISTRY.getValue(VillagerProfession.NONE)),
			1
		));
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		String professionKey = MyVillager.Profession.values()[getMyPet().getProfession()].getKey();
		VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.get(ResourceLocation.tryParse(professionKey)).get().value();
		VillagerType type = BuiltInRegistries.VILLAGER_TYPE.get(ResourceLocation.tryParse(getMyPet().getType().getKey())).get().value(); //TODO
		this.getEntityData().set(PROFESSION_WATCHER, new VillagerData(
			EntityConverterService.VILLAGER_TYPE_REGISTRY.wrapAsHolder(type),
			EntityConverterService.VILLAGER_PROFESSION_REGISTRY.wrapAsHolder(profession),
			getMyPet().getVillagerLevel()
		));

		Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
			if (getMyPet().getStatus() == MyPet.PetState.Here) {
				setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand)));
			}
		}, 5L);
	}

	public void setPetEquipment(ItemStack itemStack) {
		((ServerLevel) this.level()).getChunkSource().sendToTrackingPlayersAndSelf(this, new ClientboundSetEquipmentPacket(getId(), Arrays.asList(new Pair<>(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack))));
	}

	@Override
	public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot vanillaSlot) {
		if (MyPetApi.getPlatformHelper().doStackWalking(ServerEntity.class, 2)) {
			EquipmentSlot slot = EquipmentSlot.getSlotById(vanillaSlot.getId());
			if (getMyPet().getEquipment(slot) != null) {
				return CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
			}
		}
		return super.getItemBySlot(vanillaSlot);
	}

	@Override
	public MyVillager getMyPet() {
		return (MyVillager) myPet;
	}
}
