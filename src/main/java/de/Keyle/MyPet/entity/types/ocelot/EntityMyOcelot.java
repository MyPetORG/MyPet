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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.Sit;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_7_R2.EntityHuman;
import net.minecraft.server.v1_7_R2.Item;
import net.minecraft.server.v1_7_R2.ItemStack;
import net.minecraft.server.v1_7_R2.World;
import org.bukkit.entity.Ocelot.Type;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet {
    private Sit sitPathfinder;

    public EntityMyOcelot(World world, MyPet myPet) {
        super(world, myPet);
    }

    public void applySitting(boolean flag) {
        int i = this.datawatcher.getByte(16);
        if (flag) {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        } else {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
    }

    public boolean canMove() {
        return !sitPathfinder.isSitting();
    }

    public void setCatType(int value) {
        this.datawatcher.watch(18, (byte) value);
    }

    protected String getDeathSound() {
        return "mob.cat.hitt";
    }

    protected String getHurtSound() {
        return "mob.cat.hitt";
    }

    protected String getLivingSound() {
        return this.random.nextInt(4) == 0 ? "mob.cat.purreow" : "mob.cat.meow";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman)) {
            if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
                if (Item.b(itemStack.getItem()) == 351) {
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
                } else if (MyOcelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // tamed/sitting
        this.datawatcher.a(17, "");                 // ownername
        this.datawatcher.a(18, new Byte((byte) 0)); // cat type

    }

    public void setBaby(boolean flag) {
        if (flag) {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        } else {
            this.datawatcher.watch(12, new Integer(0));
        }
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            this.sitPathfinder = new Sit(this);

            super.setMyPet(myPet);

            this.setBaby(getMyPet().isBaby());
            this.setCatType(getMyPet().getCatType().getId());
        }
    }

    public MyOcelot getMyPet() {
        return (MyOcelot) myPet;
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.addGoal("Sit", 2, sitPathfinder);
    }
}