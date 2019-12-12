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
import de.Keyle.MyPet.api.entity.types.MyPanda;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;

@EntitySize(width = 1.825F, height = 1.25F)
public class EntityMyPanda extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> ASK_FOR_BAMBOO_TICKS_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> SNEEZE_PROGRESS_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> EATING_TICKS_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Byte> MAIN_GENE_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Byte> HIDDEN_GENE_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Byte> ACTIONS_WATCHER = DataWatcher.a(EntityMyPanda.class, DataWatcherRegistry.a);

    public EntityMyPanda(World world, MyPet myPet) {
        super(world, myPet);
    }

    protected String getDeathSound() {
        return "entity.panda.death";
    }

    protected String getHurtSound() {
        return "entity.panda.hurt";
    }

    protected String getLivingSound() {
        return "entity.panda.ambient";
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
        getDataWatcher().register(ASK_FOR_BAMBOO_TICKS_WATCHER, 0);
        getDataWatcher().register(SNEEZE_PROGRESS_WATCHER, 0);
        getDataWatcher().register(MAIN_GENE_WATCHER, (byte) 0);
        getDataWatcher().register(HIDDEN_GENE_WATCHER, (byte) 0);
        getDataWatcher().register(ACTIONS_WATCHER, (byte) 0);
        getDataWatcher().register(EATING_TICKS_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(MAIN_GENE_WATCHER, (byte) getMyPet().getMainGene().ordinal());
        getDataWatcher().set(HIDDEN_GENE_WATCHER, (byte) getMyPet().getHiddenGene().ordinal());
    }

    /*
     *  1   =
     *  2   =
     *  4   =  roll foward
     *  8   =  sitting
     */
    public void updateActionsWatcher(int i, boolean flag) {
        if (flag) {
            this.datawatcher.set(ACTIONS_WATCHER, (byte) (this.datawatcher.get(ACTIONS_WATCHER) | i));
        } else {
            this.datawatcher.set(ACTIONS_WATCHER, (byte) (this.datawatcher.get(ACTIONS_WATCHER) & ~i));
        }
    }

    public MyPanda getMyPet() {
        return (MyPanda) myPet;
    }
}