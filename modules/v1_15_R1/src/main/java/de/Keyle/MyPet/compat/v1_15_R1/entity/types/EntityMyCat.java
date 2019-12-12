/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_15_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCat;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;
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

    protected String getDeathSound() {
        return "entity.cat.death";
    }

    protected String getHurtSound() {
        return "entity.cat.hurt";
    }

    protected String getLivingSound() {
        return this.random.nextInt(4) == 0 ? "entity.cat.purr" : "entity.cat.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman)) {
            if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
                if (itemStack.getItem() instanceof ItemDye) {
                    if (((ItemDye) itemStack.getItem()).d().getColorIndex() != getMyPet().getCollarColor().ordinal()) {
                        getMyPet().setCollarColor(DyeColor.values()[((ItemDye) itemStack.getItem()).d().getColorIndex()]);
                        if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                            itemStack.subtract(1);
                            if (itemStack.getCount() <= 0) {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                            }
                        }
                        return true;
                    }
                } else if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                    if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                        itemStack.subtract(1);
                        if (itemStack.getCount() <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                        }
                    }
                    getMyPet().setBaby(false);
                    return true;
                }
            }
        }
        return false;
    }

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

    public MyCat getMyPet() {
        return (MyCat) myPet;
    }
}