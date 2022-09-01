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

package de.Keyle.MyPet.compat.v1_19_R1_2.entity.types;

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyAllay;
import de.Keyle.MyPet.compat.v1_19_R1_2.CompatManager;
import de.Keyle.MyPet.compat.v1_19_R1_2.entity.EntityMyPet;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceLocation;
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
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@EntitySize(width = 0.4F, height = 0.8F)
public class EntityMyAllay extends EntityMyPet {

	public EntityMyAllay(Level world, MyPet myPet) {
		super(world, myPet);
		//this.moveControl = new FlyingMoveControl(this, 20, true);
		//this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(1D);
		//this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(1D);
	}

	//@Override
	//protected PathNavigation setSpecialNav() {
	//	return new MyFlyingPetPathNavigation(this, this.level);
	//}

	/**
	 * Returns the sound that is played when the MyPet dies
	 */
	@Override
	protected String getMyPetDeathSound() {
		return "entity.allay.death";
	}

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 */
	@Override
	protected String getHurtSound() {
		return "entity.allay.hurt";
	}

	/**
	 * Returns the default sound of the MyPet
	 */
	@Override
	protected String getLivingSound() {
		if(hasItemInSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) {
			return "entity.allay.ambient_with_item";
		}
		return "entity.allay.ambient_without_item";
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

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && itemStack.getItem() != Items.AIR && canUseItem() && getOwner().getPlayer().isSneaking()) {
				if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
					return InteractionResult.CONSUME;
				}
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
						this.playSound(Registry.SOUND_EVENT.get(new ResourceLocation("entity.allay.item_thrown")));
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
		return InteractionResult.FAIL;
	}

	@Override
	public void updateVisuals() {
		Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
			if (getMyPet().getStatus() == MyPet.PetState.Here) {
				setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand)));
			}
		}, 5L);
	}

	@Override
	public MyAllay getMyPet() {
		return (MyAllay) myPet;
	}

	public void setPetEquipment(ItemStack itemStack) {
		((ServerLevel) this.level).getChunkSource().broadcastAndSend(this, new ClientboundSetEquipmentPacket(getId(), Arrays.asList(new Pair<>(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack))));
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
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Allay.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}

		this.updateVisuals();
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int calculateFallDamage(float f, float f1) {
		if (!Configuration.MyPet.Allay.CAN_GLIDE) {
			super.calculateFallDamage(f, f1);
		}
		return 0;
	}

	@Override
	protected boolean checkInteractCooldown() {
		boolean val = super.checkInteractCooldown();
		this.interactCooldown = 5;
		return val;
	}
}
