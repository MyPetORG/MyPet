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

package de.Keyle.MyPet.entity.types.irongolem;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.logger.DebugLogger;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

@EntitySize(width = 1.4F, length = 1.4F, height = 2.9F)
public class EntityMyIronGolem extends EntityMyPet {
    int flowerCounter = 0;
    boolean flower = false;

    public EntityMyIronGolem(World world, MyPet myPet) {
        super(world, myPet);
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            this.world.broadcastEntityEffect(this, (byte) 4);
            flag = super.attack(entity);
            if (MyIronGolem.CAN_THROW_UP && flag) {
                entity.motY += 0.4000000059604645D;
                this.world.makeSound(this, "mob.irongolem.throw", 1.0F, 1.0F);
            }
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        return "mob.irongolem.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.irongolem.hit";
    }

    protected String getLivingSound() {
        return null;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(16, new Byte((byte) 0)); // flower???
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Item.getItemOf(Blocks.RED_FLOWER) && !getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
                getMyPet().setFlower(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
                EntityItem entityitem = this.a(CraftItemStack.asNMSCopy(getMyPet().getFlower()), 1.0F);
                entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);

                makeSound("mob.sheep.shear", 1.0F, 1.0F);
                getMyPet().setFlower(null);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            }
        }
        return false;
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setFlower(getMyPet().hasFlower());
            if (flower) {
                flowerCounter = 40;
            }
        }
    }

    public MyIronGolem getMyPet() {
        return (MyIronGolem) myPet;
    }

    @Override
    public void playStepSound() {
        makeSound("mob.irongolem.walk", 1.0F, 1.0F);
    }

    public void setFlower(boolean flag) {
        this.flower = flag;
        flowerCounter = 0;
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.flower && this.flowerCounter-- <= 0) {
            this.world.broadcastEntityEffect(this, (byte) 11);
            flowerCounter = 300;
        }
    }
}