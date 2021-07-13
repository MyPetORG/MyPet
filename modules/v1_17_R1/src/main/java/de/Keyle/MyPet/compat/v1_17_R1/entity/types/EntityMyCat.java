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
import de.Keyle.MyPet.api.entity.types.MyCat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import org.bukkit.DyeColor;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyCat extends EntityMyPet {

	protected static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.i);
	protected static final DataWatcherObject<Byte> SIT_WATCHER = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.a);
	protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.o);
	protected static final DataWatcherObject<Integer> TYPE_WATCHER = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.b);
	protected static final DataWatcherObject<Boolean> UNUSED_WATCHER_1 = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.i);
	protected static final DataWatcherObject<Boolean> UNUSED_WATCHER_2 = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.i);
	protected static final DataWatcherObject<Integer> COLLAR_COLOR_WATCHER = DataWatcher.a(EntityMyCat.class, DataWatcherRegistry.b);

	public EntityMyCat(World world, MyPet myPet) {
		super(world, myPet);
	}

	public void applySitting(boolean sitting) {
		byte i = getDataWatcher().get(SIT_WATCHER);
		if (sitting) {
			getDataWatcher().set(SIT_WATCHER, (byte) (i | 1));
		} else {
			getDataWatcher().set(SIT_WATCHER, (byte) (i & -2));
		}
	}

	@Override
	protected String getDeathSound() {
		return "entity.cat.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.cat.hurt";
	}

	@Override
	protected String getLivingSound() {
		return this.Q.nextInt(4) == 0 ? "entity.cat.purr" : "entity.cat.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.b;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
				if (itemStack.getItem() instanceof ItemDye) {
					if (((ItemDye) itemStack.getItem()).d().getColorIndex() != getMyPet().getCollarColor().ordinal()) {
						getMyPet().setCollarColor(DyeColor.values()[((ItemDye) itemStack.getItem()).d().getColorIndex()]);
						if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
							itemStack.subtract(1);
							if (itemStack.getCount() <= 0) {
								entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
							}
						}
						return EnumInteractionResult.a;
					}
				} else if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
					if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
						itemStack.subtract(1);
						if (itemStack.getCount() <= 0) {
							entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
						}
					}
					getMyPet().setBaby(false);
					return EnumInteractionResult.b;
				}
			}
		}
		return EnumInteractionResult.d;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(SIT_WATCHER, (byte) 0);
		getDataWatcher().register(OWNER_WATCHER, Optional.empty());
		getDataWatcher().register(TYPE_WATCHER, 1);
		getDataWatcher().register(UNUSED_WATCHER_1, false);
		getDataWatcher().register(UNUSED_WATCHER_2, false);
		getDataWatcher().register(COLLAR_COLOR_WATCHER, 14);
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		getDataWatcher().set(TYPE_WATCHER, getMyPet().getCatType().ordinal());
		getDataWatcher().set(COLLAR_COLOR_WATCHER, getMyPet().getCollarColor().ordinal());

		byte b0 = getDataWatcher().get(SIT_WATCHER);
		if (getMyPet().isTamed()) {
			getDataWatcher().set(SIT_WATCHER, (byte) (b0 | 0x4));
		} else {
			getDataWatcher().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFB));
		}
	}

	@Override
	public MyCat getMyPet() {
		return (MyCat) myPet;
	}
}
