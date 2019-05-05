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

package de.Keyle.MyPet.compat.v1_14_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyFox;
import de.Keyle.MyPet.compat.v1_14_R1.entity.EntityMyPet;
import net.minecraft.server.v1_14_R1.*;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyFox extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> FOX_TYPE_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Byte> ACTIONS_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Optional<UUID>> FRIEND_A_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Optional<UUID>> FRIEND_B_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.o);

    public EntityMyFox(World world, MyPet myPet) {
        super(world, myPet);
        this.getControllerLook().a(this, 60.0F, 30.0F);
    }

    protected String getDeathSound() {
        return "entity.fox.death";
    }

    protected String getHurtSound() {
        return "entity.fox.hurt";
    }

    protected String getLivingSound() {
        return "entity.fox.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman)) {
            if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
                if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
        getDataWatcher().register(FRIEND_A_WATCHER, Optional.empty());
        getDataWatcher().register(FRIEND_B_WATCHER, Optional.empty());
        getDataWatcher().register(FOX_TYPE_WATCHER, 0);
        getDataWatcher().register(ACTIONS_WATCHER, (byte) 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(FOX_TYPE_WATCHER, getMyPet().getType().ordinal());
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
            this.datawatcher.set(ACTIONS_WATCHER, (byte) (this.datawatcher.get(ACTIONS_WATCHER) | i));
        } else {
            this.datawatcher.set(ACTIONS_WATCHER, (byte) (this.datawatcher.get(ACTIONS_WATCHER) & ~i));
        }
    }

    public void movementTick() {
        super.movementTick();
        // foxes can't look up
        this.pitch = 0F;
    }

    public MyFox getMyPet() {
        return (MyFox) myPet;
    }
}