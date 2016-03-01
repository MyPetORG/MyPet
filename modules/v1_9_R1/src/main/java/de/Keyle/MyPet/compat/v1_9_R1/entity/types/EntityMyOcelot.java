/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_9_R1.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.types.MyOcelot;
import de.Keyle.MyPet.compat.v1_9_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_9_R1.entity.ai.movement.Sit;
import net.minecraft.server.v1_9_R1.*;
import org.bukkit.entity.Ocelot.Type;

import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet {
    private static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityAgeable.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Integer> typeWatcher = DataWatcher.a(EntityOcelot.class, DataWatcherRegistry.b);
    protected static final DataWatcherObject<Byte> sitWatcher = DataWatcher.a(EntityTameableAnimal.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> ownerWatcher = DataWatcher.a(EntityTameableAnimal.class, DataWatcherRegistry.m);

    private Sit sitPathfinder;

    public EntityMyOcelot(World world, ActiveMyPet myPet) {
        super(world, myPet);
    }

    public void applySitting(boolean sitting) {
        int i = this.datawatcher.get(sitWatcher).byteValue();
        if (sitting) {
            this.datawatcher.set(sitWatcher, (byte) (i | 0x1));
        } else {
            this.datawatcher.set(sitWatcher, (byte) (i & 0xFFFFFFFE));
        }
    }

    public boolean canMove() {
        return !sitPathfinder.isSitting();
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
            this.sitPathfinder.toogleSitting();
            return true;
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(ageWatcher, false);               // age
        this.datawatcher.register(sitWatcher, (byte) 0);            // tamed/sitting
        this.datawatcher.register(ownerWatcher, Optional.absent()); // owner
        this.datawatcher.register(typeWatcher, 0);                  // cat type
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());
        this.datawatcher.set(typeWatcher, getMyPet().getCatType().ordinal());
    }

    public MyOcelot getMyPet() {
        return (MyOcelot) myPet;
    }

    public void setPathfinder() {
        super.setPathfinder();
        sitPathfinder = new Sit(this);
        petPathfinderSelector.addGoal("Sit", 2, sitPathfinder);
    }
}