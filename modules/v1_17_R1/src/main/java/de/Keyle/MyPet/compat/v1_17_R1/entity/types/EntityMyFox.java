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

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyFox;
import de.Keyle.MyPet.compat.v1_17_R1.CompatManager;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyFox extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyFox.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> FOX_TYPE_WATCHER = SynchedEntityData.defineId(EntityMyFox.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> ACTIONS_WATCHER = SynchedEntityData.defineId(EntityMyFox.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Optional<UUID>> FRIEND_A_WATCHER = SynchedEntityData.defineId(EntityMyFox.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Optional<UUID>> FRIEND_B_WATCHER = SynchedEntityData.defineId(EntityMyFox.class, EntityDataSerializers.OPTIONAL_UUID);

	public EntityMyFox(Level world, MyPet myPet) {
		super(world, myPet);
		this.getLookControl().setLookAt(this, 60.0F, 30.0F);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.fox.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.fox.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.fox.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && itemStack.getItem() != Items.AIR && canUseItem() && getOwner().getPlayer().isSneaking()) {
				if (itemStack.getItem() != Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
					ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand));
					if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && itemInSlot != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
						ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), itemInSlot);
						entityitem.pickupDelay = 10;
						entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
						this.level.addFreshEntity(entityitem);
					}
					getMyPet().setEquipment(EquipmentSlot.MainHand, CraftItemStack.asBukkitCopy(itemStack));
					if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
						itemStack.shrink(1);
						if (itemStack.getCount() <= 0) {
							entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
						}
					}
					return InteractionResult.CONSUME;
				} else if (Configuration.MyPet.Fox.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
					if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
						itemStack.shrink(1);
						if (itemStack.getCount() <= 0) {
							entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
						}
					}
					getMyPet().setBaby(false);
					return InteractionResult.CONSUME;
				} else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
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
				}
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
		getEntityData().define(FRIEND_A_WATCHER, Optional.empty());
		getEntityData().define(FRIEND_B_WATCHER, Optional.empty());
		getEntityData().define(FOX_TYPE_WATCHER, 0);
		getEntityData().define(ACTIONS_WATCHER, (byte) 0);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		this.getEntityData().set(FOX_TYPE_WATCHER, getMyPet().getFoxType().ordinal());

		Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
			if (getMyPet().getStatus() == MyPet.PetState.Here) {
				setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand)));
			}
		}, 5L);
	}

	/*
	 *  1   = sitting
	 *  2   =
	 *  4   = ready for jumping
	 *  8   = curious
	 *  16  =
	 *  32  = sleeping
	 *  64  = feet spasm
	 *  128 =
	 */
	public void updateActionsWatcher(int i, boolean flag) {
		if (flag) {
			this.entityData.set(ACTIONS_WATCHER, (byte) (this.entityData.get(ACTIONS_WATCHER) | i));
		} else {
			this.entityData.set(ACTIONS_WATCHER, (byte) (this.entityData.get(ACTIONS_WATCHER) & ~i));
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// foxes can't look up
		this.setXRot(0F);
	}

	public void setPetEquipment(ItemStack itemStack) {
		((ServerLevel) this.level).getChunkProvider().broadcastAndSend(this, new ClientboundSetEquipmentPacket(getId(), Arrays.asList(new Pair<>(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack))));
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

	@Override
	public MyFox getMyPet() {
		return (MyFox) myPet;
	}
}
