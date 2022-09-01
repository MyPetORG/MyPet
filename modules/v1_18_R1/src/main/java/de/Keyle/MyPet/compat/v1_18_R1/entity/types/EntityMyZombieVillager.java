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

package de.Keyle.MyPet.compat.v1_18_R1.entity.types;

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.api.entity.types.MyZombieVillager;
import de.Keyle.MyPet.compat.v1_18_R1.CompatManager;
import de.Keyle.MyPet.compat.v1_18_R1.entity.EntityMyPet;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyZombieVillager extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> BABY_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> TYPE_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DROWN_CONVERTING = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SHIVER_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<VillagerData> PROFESSION_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.VILLAGER_DATA);

	public EntityMyZombieVillager(Level world, MyPet myPet) {
		super(world, myPet);
	}

	/**
	 * Returns the sound that is played when the MyPet dies
	 */
	@Override
	protected String getMyPetDeathSound() {
		return "entity.zombie.death";
	}

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 */
	@Override
	protected String getHurtSound() {
		return "entity.zombie.hurt";
	}

	/**
	 * Returns the default sound of the MyPet
	 */
	@Override
	protected String getLivingSound() {
		return "entity.zombie.ambient";
	}

	/**
	 * Is called when player rightclicks this MyPet
	 * return:
	 * true: there was a reaction on rightclick
	 * false: no reaction on rightclick
	 */
	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null) {
			if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
				boolean hadEquipment = false;
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
					if (itemInSlot != null && itemInSlot.getItem() != Items.AIR) {
						ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), itemInSlot);
						entityitem.pickupDelay = 10;
						entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
						this.level.addFreshEntity(entityitem);
						getMyPet().setEquipment(slot, null);
						hadEquipment = true;
					}
				}
				if (hadEquipment) {
					if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
						try {
							itemStack.hurtAndBreak(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastBreakEvent(enumhand));
						} catch (Error e) {
							// TODO REMOVE
							itemStack.hurtAndBreak(1, entityhuman, (entityhuman1) -> {
								try {
									CompatManager.ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
								} catch (IllegalAccessException | InvocationTargetException ex) {
									ex.printStackTrace();
								}
							});
						}
					}
				}
				return InteractionResult.CONSUME;
			} else if (MyPetApi.getPlatformHelper().isEquipment(CraftItemStack.asBukkitCopy(itemStack)) && getOwner().getPlayer().isSneaking() && canEquip()) {
				EquipmentSlot slot = EquipmentSlot.getSlotById(Mob.getEquipmentSlotForItem(itemStack).getFilterFlag());
				ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
				if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && itemInSlot != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), itemInSlot);
					entityitem.pickupDelay = 10;
					entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
					this.level.addFreshEntity(entityitem);
				}
				getMyPet().setEquipment(slot, CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (Configuration.MyPet.Zombie.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		getEntityData().define(BABY_WATCHER, false);
		getEntityData().define(TYPE_WATCHER, 0);
		getEntityData().define(DROWN_CONVERTING, false);
		getEntityData().define(SHIVER_WATCHER, false);

		getEntityData().define(PROFESSION_WATCHER, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
	}

	@Override
	public void updateVisuals() {
		getEntityData().set(BABY_WATCHER, getMyPet().isBaby());
		String professionKey = MyVillager.Profession.values()[getMyPet().getProfession()].getKey();
		VillagerProfession profession = Registry.VILLAGER_PROFESSION.get(new ResourceLocation(professionKey));
		VillagerType type = Registry.VILLAGER_TYPE.get(new ResourceLocation(getMyPet().getType().getKey())); //TODO
		getEntityData().set(PROFESSION_WATCHER, new VillagerData(type, profession, getMyPet().getTradingLevel()));

		Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
			if (getMyPet().getStatus() == MyPet.PetState.Here) {
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					setPetEquipment(slot, CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot)));
				}
			}
		}, 5L);
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.zombie.step", 0.15F, 1.0F);
	}

	@Override
	public MyZombieVillager getMyPet() {
		return (MyZombieVillager) myPet;
	}

	public void setPetEquipment(EquipmentSlot slot, ItemStack itemStack) {
		((ServerLevel) this.level).getChunkSource().broadcastAndSend(this, new ClientboundSetEquipmentPacket(getId(), Arrays.asList(new Pair<>(net.minecraft.world.entity.EquipmentSlot.values()[slot.get19Slot()], itemStack))));
	}

	@Override
	public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot vanillaSlot) {
		if (MyPetApi.getPlatformHelper().doStackWalking(ServerEntity.class, 2)) {
			EquipmentSlot slot = EquipmentSlot.getSlotById(vanillaSlot.getFilterFlag());
			if (getMyPet().getEquipment(slot) != null) {
				return CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
			}
		}
		return super.getItemBySlot(vanillaSlot);
	}
}
