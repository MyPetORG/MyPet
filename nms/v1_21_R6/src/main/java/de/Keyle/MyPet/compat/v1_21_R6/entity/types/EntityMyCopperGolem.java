/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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
import de.Keyle.MyPet.api.entity.types.MyCopperGolem;
import de.Keyle.MyPet.compat.v1_21_R6.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.coppergolem.CopperGolemState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import org.bukkit.Location;
import org.bukkit.Particle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.v1_21_R6.inventory.CraftItemStack;

import static de.Keyle.MyPet.compat.v1_21_R6.util.HandSlot.getSlotForHand;

@EntitySize(width = 0.7F, height = 0.7F)
public class EntityMyCopperGolem extends EntityMyPet {

	protected static final EntityDataAccessor<WeatheringCopper.WeatherState> DATA_WEATHER_STATE = SynchedEntityData.defineId(EntityMyCopperGolem.class, EntityDataSerializers.WEATHERING_COPPER_STATE);
	protected static final EntityDataAccessor<CopperGolemState> COPPER_GOLEM_STATE = SynchedEntityData.defineId(EntityMyCopperGolem.class, EntityDataSerializers.COPPER_GOLEM_STATE);

	public EntityMyCopperGolem(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			this.level().broadcastEntityEvent(this, (byte) 4);
			flag = super.attack(entity);
			if (flag) {
				this.makeSound("entity.copper_golem.hurt", 1.0F, 1.0F);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	protected String getMyPetDeathSound() {
		return getDeathSoundForOxidationState();
	}

	@Override
	protected String getHurtSound() {
		return getHurtSoundForOxidationState();
	}

	@Override
	protected String getLivingSound() {
		return null;
	}

	private String getDeathSoundForOxidationState() {
		MyCopperGolem.OxidationState state = getMyPet().getOxidationState();
		return switch (state) {
			case WEATHERED -> "entity.copper_golem_weathered.death";
			case OXIDIZED -> "entity.copper_golem_oxidized.death";
			default -> "entity.copper_golem.death";
		};
	}

	private String getHurtSoundForOxidationState() {
        MyCopperGolem.OxidationState state = getMyPet().getOxidationState();
		return switch (state) {
			case WEATHERED -> "entity.copper_golem_weathered.hurt";
			case OXIDIZED -> "entity.copper_golem_oxidized.hurt";
			default -> "entity.copper_golem.hurt";
		};
	}

	private WeatheringCopper.WeatherState convertToNMSState(MyCopperGolem.OxidationState state) {
		return switch (state) {
			case EXPOSED -> WeatheringCopper.WeatherState.EXPOSED;
			case WEATHERED -> WeatheringCopper.WeatherState.WEATHERED;
			case OXIDIZED -> WeatheringCopper.WeatherState.OXIDIZED;
			default -> WeatheringCopper.WeatherState.UNAFFECTED;
		};
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_WEATHER_STATE, WeatheringCopper.WeatherState.UNAFFECTED);
		builder.define(COPPER_GOLEM_STATE, CopperGolemState.IDLE);
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			boolean isAxe = itemStack.is(ItemTags.AXES);

			// Waxing with honeycomb
			if (itemStack.is(Items.HONEYCOMB) && !getMyPet().isWaxed()) {
				getMyPet().setWaxed(true);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().getSelectedSlot(), ItemStack.EMPTY);
					}
				}
				makeSound("item.honeycomb.wax_on", 1.0F, 1.0F);
				spawnParticlesAroundEntity(Particle.WAX_ON);
				return InteractionResult.CONSUME;
			}
			// Remove wax with axe
			else if (isAxe && getMyPet().isWaxed()) {
				getMyPet().setWaxed(false);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					try {
						itemStack.hurtAndBreak(1, entityhuman, getSlotForHand(enumhand));
					} catch (Error e) {
						// ignore
					}
				}
				makeSound("item.axe.wax_off", 1.0F, 1.0F);
				spawnParticlesAroundEntity(Particle.WAX_OFF);
				return InteractionResult.CONSUME;
			}
			// Scrape oxidation with axe
			else if (isAxe && !getMyPet().isWaxed()) {
				MyCopperGolem.OxidationState currentState = getMyPet().getOxidationState();
                MyCopperGolem.OxidationState newState = switch (currentState) {
					case OXIDIZED -> MyCopperGolem.OxidationState.WEATHERED;
					case WEATHERED -> MyCopperGolem.OxidationState.EXPOSED;
					case EXPOSED -> MyCopperGolem.OxidationState.UNAFFECTED;
					default -> currentState;
				};

				if (newState != currentState) {
					getMyPet().setOxidationState(newState);
					if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
						try {
							itemStack.hurtAndBreak(1, entityhuman, getSlotForHand(enumhand));
						} catch (Error e) {
							// ignore
						}
					}
					makeSound("item.axe.scrape", 1.0F, 1.0F);
					spawnParticlesAroundEntity(Particle.SCRAPE);
					return InteractionResult.CONSUME;
				}
			}
			// Add poppy interaction
			if (itemStack.getItem() == Blocks.POPPY.asItem() && !getMyPet().hasPoppy()) {
				getMyPet().setPoppy(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().getSelectedSlot(), ItemStack.EMPTY);
					}
				}
				makeSound("entity.iron_golem.repair", 1.0F, 1.0F);
				return InteractionResult.CONSUME;
			}
			// Remove poppy with shears
			if (itemStack.getItem() == Items.SHEARS && getMyPet().hasPoppy()) {
				ItemEntity entityitem = new ItemEntity(this.level(), this.getX(), this.getY() + 1, this.getZ(),
					CraftItemStack.asNMSCopy(getMyPet().getPoppy()));
				entityitem.pickupDelay = 10;
				entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
				this.level().addFreshEntity(entityitem);

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setPoppy(null);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					try {
						itemStack.hurtAndBreak(1, entityhuman, getSlotForHand(enumhand));
					} catch (Error e) {
						// ignore
					}
				}
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public void updateVisuals() {
		this.entityData.set(DATA_WEATHER_STATE, convertToNMSState(getMyPet().getOxidationState()));

		// Update poppy visual in antenna slot
		if (getMyPet().hasPoppy()) {
			org.bukkit.inventory.ItemStack bukkitPoppy = getMyPet().getPoppy();
			net.minecraft.world.item.ItemStack nmsPoppy = org.bukkit.craftbukkit.v1_21_R6.inventory.CraftItemStack.asNMSCopy(bukkitPoppy);
			this.setItemSlot(net.minecraft.world.entity.animal.coppergolem.CopperGolem.EQUIPMENT_SLOT_ANTENNA, nmsPoppy);
		} else {
			this.setItemSlot(net.minecraft.world.entity.animal.coppergolem.CopperGolem.EQUIPMENT_SLOT_ANTENNA, net.minecraft.world.item.ItemStack.EMPTY);
		}
	}

	@Override
	public MyCopperGolem getMyPet() {
		return (MyCopperGolem) myPet;
	}

	@Override
	public void playPetStepSound() {
        MyCopperGolem.OxidationState state = getMyPet().getOxidationState();
		String stepSound = switch (state) {
			case WEATHERED -> "entity.copper_golem_weathered.step";
			case OXIDIZED -> "entity.copper_golem_oxidized.step";
			default -> "entity.copper_golem.step";
		};
		makeSound(stepSound, 0.15F, 1.0F);
	}

	private void spawnParticlesAroundEntity(Particle particle) {
		org.bukkit.World world = this.getBukkitEntity().getWorld();
		Location location = this.getBukkitEntity().getLocation();
		double height = this.getBbHeight();
		double width = this.getBbWidth();

		// Spawn particles around the entity using Bukkit API
		for (int i = 0; i < 15; i++) {
			double offsetX = (this.random.nextDouble() - 0.5) * width * 1.5;
			double offsetY = (this.random.nextDouble() - 0.5) * height;
			double offsetZ = (this.random.nextDouble() - 0.5) * width * 1.5;

			world.spawnParticle(particle,
				location.getX() + offsetX,
				location.getY() + height / 2.0 + offsetY,
				location.getZ() + offsetZ,
				1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		// Handle oxidation over time
		if (!Configuration.MyPet.CopperGolem.CAN_OXIDIZE || getMyPet().isWaxed()) {
			return;
		}

		MyCopperGolem.OxidationState currentState = getMyPet().getOxidationState();
		if (currentState == MyCopperGolem.OxidationState.OXIDIZED) {
			return;
		}

		int tickCounter = getMyPet().getOxidationTickCounter() + 1;

		// Check if it's time to oxidize
		if (tickCounter >= Configuration.MyPet.CopperGolem.OXIDATION_TIME) {
			tickCounter = 0;

			// Progress to next oxidation state
			MyCopperGolem.OxidationState newState = switch (currentState) {
				case UNAFFECTED -> MyCopperGolem.OxidationState.EXPOSED;
				case EXPOSED -> MyCopperGolem.OxidationState.WEATHERED;
				case WEATHERED -> MyCopperGolem.OxidationState.OXIDIZED;
				default -> currentState;
			};

			if (newState != currentState) {
				getMyPet().setOxidationState(newState);
			}
		}
		getMyPet().setOxidationTickCounter(tickCounter);
	}
}
