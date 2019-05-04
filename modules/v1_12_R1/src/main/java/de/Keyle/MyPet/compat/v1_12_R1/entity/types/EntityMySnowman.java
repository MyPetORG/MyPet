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

package de.Keyle.MyPet.compat.v1_12_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MySnowman;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import net.minecraft.server.v1_12_R1.*;

@EntitySize(width = 0.7F, height = 1.7F)
public class EntityMySnowman extends EntityMyPet {

    private static final DataWatcherObject<Byte> SHEARED_WATCHER = DataWatcher.a(EntityMySnowman.class, DataWatcherRegistry.a);

    public EntityMySnowman(World world, MyPet myPet) {
        super(world, myPet);
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Item.getItemOf(Blocks.PUMPKIN) && getMyPet().isSheared() && entityhuman.isSneaking()) {
                getMyPet().setSheared(false);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && !getMyPet().isSheared() && entityhuman.isSneaking()) {
                getMyPet().setSheared(true);
                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateVisuals() {
        byte oldValue = this.datawatcher.get(SHEARED_WATCHER);
        if (getMyPet().isSheared()) {
            this.datawatcher.set(SHEARED_WATCHER, (byte) (oldValue & 0xFFFFFFEF));
        } else {
            this.datawatcher.set(SHEARED_WATCHER, (byte) (oldValue | 0x10));
        }
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();

        this.datawatcher.register(SHEARED_WATCHER, (byte) 0);
    }

    @Override
    protected String getDeathSound() {
        return "entity.snowman.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.snowman.hurt";
    }

    protected String getLivingSound() {
        return "entity.snowman.ambient";
    }

    @Override
    public void playPetStepSound() {
        makeSound("block.snow.step", 0.15F, 1.0F);
    }

    public MySnowman getMyPet() {
        return (MySnowman) myPet;
    }
}