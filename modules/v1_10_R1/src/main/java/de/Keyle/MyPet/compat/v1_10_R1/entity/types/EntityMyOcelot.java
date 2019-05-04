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

package de.Keyle.MyPet.compat.v1_10_R1.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyOcelot;
import de.Keyle.MyPet.compat.v1_10_R1.entity.EntityMyPet;
import net.minecraft.server.v1_10_R1.*;
import org.bukkit.entity.Ocelot.Type;

import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyOcelot.class, DataWatcherRegistry.h);
    protected static final DataWatcherObject<Byte> SIT_WATCHER = DataWatcher.a(EntityMyOcelot.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyOcelot.class, DataWatcherRegistry.m);
    private static final DataWatcherObject<Integer> TYPE_WATCHER = DataWatcher.a(EntityMyOcelot.class, DataWatcherRegistry.b);

    public EntityMyOcelot(World world, MyPet myPet) {
        super(world, myPet);
    }

    public void applySitting(boolean sitting) {
        int i = this.datawatcher.get(SIT_WATCHER);
        if (sitting) {
            this.datawatcher.set(SIT_WATCHER, (byte) (i | 0x1));
        } else {
            this.datawatcher.set(SIT_WATCHER, (byte) (i & 0xFFFFFFFE));
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
                if (Item.getId(itemStack.getItem()) == 351) {
                    boolean colorChanged = false;
                    if (itemStack.getData() == 11 && getMyPet().getCatType() != Type.WILD_OCELOT) {
                        getMyPet().setCatType(Type.WILD_OCELOT);
                        colorChanged = true;
                    } else if (itemStack.getData() == 0 && getMyPet().getCatType() != Type.BLACK_CAT) {
                        getMyPet().setCatType(Type.BLACK_CAT);
                        colorChanged = true;
                    } else if (itemStack.getData() == 14 && getMyPet().getCatType() != Type.RED_CAT) {
                        getMyPet().setCatType(Type.RED_CAT);
                        colorChanged = true;
                    } else if (itemStack.getData() == 7 && getMyPet().getCatType() != Type.SIAMESE_CAT) {
                        getMyPet().setCatType(Type.SIAMESE_CAT);
                        colorChanged = true;
                    }
                    if (colorChanged) {
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            if (--itemStack.count <= 0) {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        return true;
                    }
                } else if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        if (--itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
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
        this.datawatcher.register(AGE_WATCHER, false);
        this.datawatcher.register(SIT_WATCHER, (byte) 0);
        this.datawatcher.register(OWNER_WATCHER, Optional.absent());
        this.datawatcher.register(TYPE_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(AGE_WATCHER, getMyPet().isBaby());
        this.datawatcher.set(TYPE_WATCHER, getMyPet().getCatType().ordinal());
    }

    public MyOcelot getMyPet() {
        return (MyOcelot) myPet;
    }
}