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

package de.Keyle.MyPet.compat.v1_15_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyBlaze;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;

import java.lang.reflect.InvocationTargetException;

import static de.Keyle.MyPet.compat.v1_15_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.6F, height = 1.7F)
public class EntityMyBlaze extends EntityMyPet {

    private static final DataWatcherObject<Byte> BURNING_WATCHER = DataWatcher.a(EntityMyBlaze.class, DataWatcherRegistry.a);

    public EntityMyBlaze(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.blaze.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.blaze.hurt";
    }

    protected String getLivingSound() {
        return "entity.blaze.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (getMyPet().isOnFire() && itemStack.getItem() == Items.WATER_BUCKET && getOwner().getPlayer().isSneaking()) {
                getMyPet().setOnFire(false);
                makeSound("block.fire.extinguish", 1.0F, 1.0F);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, new ItemStack(Items.BUCKET));
                    } else {
                        if (!entityhuman.inventory.pickup(new ItemStack(Items.BUCKET))) {
                            entityhuman.drop(new ItemStack(Items.BUCKET), true);
                        }
                    }
                }
                return true;
            } else if (!getMyPet().isOnFire() && itemStack.getItem() == Items.FLINT_AND_STEEL && getOwner().getPlayer().isSneaking()) {
                getMyPet().setOnFire(true);
                makeSound("item.flintandsteel.use", 1.0F, 1.0F);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    try {
                        itemStack.damage(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastItemBreak(enumhand));
                    } catch (Error e) {
                        // TODO REMOVE
                        itemStack.damage(1, entityhuman, (entityhuman1) -> {
                            try {
                                ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                ex.printStackTrace();
                            }
                        });
                    }
                }
                return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(BURNING_WATCHER, (byte) 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(BURNING_WATCHER, (byte) (getMyPet().isOnFire() ? 1 : 0));
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Blaze.CAN_GLIDE) {
            if (!this.onGround && this.getMot().y < 0.0D) {
                this.setMot(getMot().d(1, 0.6D, 1));
            }
        }
    }

    public MyBlaze getMyPet() {
        return (MyBlaze) myPet;
    }

    /**
     * -> disable falldamage
     */
    public int e(float f, float f1) {
        if (!Configuration.MyPet.Blaze.CAN_GLIDE) {
            super.e(f, f1);
        }
        return 0;
    }
}