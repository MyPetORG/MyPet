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

package de.Keyle.MyPet.compat.v1_11_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCow;
import de.Keyle.MyPet.compat.v1_11_R1.entity.EntityMyPet;
import net.minecraft.server.v1_11_R1.*;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyCow extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyCow.class, DataWatcherRegistry.h);

    public EntityMyCow(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.cow.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.cow.hurt";
    }

    protected String getLivingSound() {
        return "entity.cow.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.BUCKET && Configuration.MyPet.Cow.CAN_GIVE_MILK) {
                ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET, 1, 0);

                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, milkBucket);
                return true;
            } else if (Configuration.MyPet.Cow.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                getMyPet().setBaby(false);
                return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(AGE_WATCHER, false);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(AGE_WATCHER, getMyPet().isBaby());
    }

    public void playPetStepSound() {
        makeSound("entity.cow.step", 0.15F, 1.0F);
    }

    public MyCow getMyPet() {
        return (MyCow) myPet;
    }
}