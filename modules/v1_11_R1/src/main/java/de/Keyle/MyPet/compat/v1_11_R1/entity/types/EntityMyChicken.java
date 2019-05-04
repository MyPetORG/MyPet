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
import de.Keyle.MyPet.api.entity.types.MyChicken;
import de.Keyle.MyPet.compat.v1_11_R1.entity.EntityMyPet;
import net.minecraft.server.v1_11_R1.*;

@EntitySize(width = 0.4F, height = 0.7F)
public class EntityMyChicken extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyChicken.class, DataWatcherRegistry.h);

    private int nextEggTimer;

    public EntityMyChicken(World world, MyPet myPet) {
        super(world, myPet);
        nextEggTimer = (random.nextInt(6000) + 6000);
    }

    @Override
    protected String getDeathSound() {
        return "entity.chicken.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.chicken.hurt";
    }

    protected String getLivingSound() {
        return "entity.chicken.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null) {
            if (Configuration.MyPet.Chicken.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (Configuration.MyPet.Chicken.CAN_GLIDE) {
            if (!this.onGround && this.motY < 0.0D) {
                this.motY *= 0.6D;
            }
        }

        if (Configuration.MyPet.Chicken.CAN_LAY_EGGS && canUseItem() && --nextEggTimer <= 0) {
            this.makeSound("entity.chicken.egg", 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            a(Items.EGG, 1);
            nextEggTimer = random.nextInt(6000) + 6000;
        }
    }

    public void playPetStepSound() {
        makeSound("entity.chicken.step", 0.15F, 1.0F);
    }

    public MyChicken getMyPet() {
        return (MyChicken) myPet;
    }

    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
        if (!Configuration.MyPet.Chicken.CAN_GLIDE) {
            super.e(f, f1);
        }
    }
}