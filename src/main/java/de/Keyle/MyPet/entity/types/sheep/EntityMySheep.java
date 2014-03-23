/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.EatGrass;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_7_R2.*;
import org.bukkit.DyeColor;

@EntitySize(width = 0.9F, height = 1.3F)
public class EntityMySheep extends EntityMyPet {
    public EntityMySheep(World world, MyPet myPet) {
        super(world, myPet);
    }

    public void setColor(byte color) {
        this.datawatcher.watch(16, color);
    }

    @Override
    protected String getDeathSound() {
        return "mob.sheep.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.sheep.say";
    }

    protected String getLivingSound() {
        return "mob.sheep.say";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.INK_SACK && itemStack.getData() <= 15 && itemStack.getData() != getMyPet().getColor().getDyeData() && !getMyPet().isSheared()) {
                getMyPet().setColor(DyeColor.getByDyeData((byte) itemStack.getData()));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && MySheep.CAN_BE_SHEARED && !getMyPet().isSheared()) {
                getMyPet().setSheared(true);
                int woolDropCount = 1 + this.random.nextInt(3);

                for (int j = 0; j < woolDropCount; ++j) {
                    EntityItem entityitem = this.a(new ItemStack(Blocks.WOOL, 1, getMyPet().getColor().getDyeData()), 1.0F);

                    entityitem.motY += (double) this.random.nextFloat() * 0.05F;
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                makeSound("mob.sheep.shear", 1.0F, 1.0F);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }
                return true;
            } else if (MySheep.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
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
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // color/sheared
    }

    public void setBaby(boolean flag) {
        if (flag) {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        } else {
            this.datawatcher.watch(12, new Integer(0));
        }
    }

    public void setSheared(boolean flag) {

        byte b0 = this.datawatcher.getByte(16);
        if (flag) {
            this.datawatcher.watch(16, (byte) (b0 | 16));
        } else {
            this.datawatcher.watch(16, (byte) (b0 & -17));
        }
    }

    public void playStepSound() {
        makeSound("mob.sheep.step", 0.15F, 1.0F);
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setColor(getMyPet().getColor().getWoolData());
            this.setSheared(getMyPet().isSheared());
            this.setBaby(getMyPet().isBaby());
        }
    }

    public MySheep getMyPet() {
        return (MySheep) myPet;
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.addGoal("EatGrass", new EatGrass(this, 0.02));
    }
}