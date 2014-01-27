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

package de.Keyle.MyPet.entity.types.skeleton;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.BukkitUtil;
import net.minecraft.server.v1_7_R1.*;
import org.bukkit.Bukkit;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMySkeleton extends EntityMyPet {
    public EntityMySkeleton(World world, MyPet myPet) {
        super(world, myPet);
    }

    protected String getDeathSound() {
        return "mob.skeleton.death";
    }

    protected String getHurtSound() {
        return "mob.skeleton.hurt";
    }

    protected String getLivingSound() {
        return "mob.skeleton.say";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                boolean hadEquipment = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack itemInSlot = getMyPet().getEquipment(slot);
                    if (itemInSlot != null) {
                        EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        getMyPet().setEquipment(slot, null);
                        hadEquipment = true;
                    }
                }
                if (hadEquipment) {
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        itemStack.damage(1, entityhuman);
                    }
                }
                return true;
            } else if (BukkitUtil.isEquipment(itemStack) && getOwner().getPlayer().isSneaking() && canEquip()) {
                EquipmentSlot slot = EquipmentSlot.getSlotById(b(itemStack));
                ItemStack itemInSlot = getMyPet().getEquipment(slot);
                if (itemInSlot != null && !entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                getMyPet().setEquipment(slot, itemStack);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    --itemStack.count;
                }
                if (itemStack.count <= 0) {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(13, new Byte((byte) 0)); // skeleton type
    }

    public void setWither(boolean flag) {
        this.datawatcher.watch(13, (byte) (flag ? 1 : 0));
    }

    public void playStepSound() {
        makeSound("mob.skeleton.step", 0.15F, 1.0F);
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);
            final MySkeleton mySkeleton = getMyPet();
            final EntityMySkeleton entityMySkeleton = this;

            this.setWither(mySkeleton.isWither());

            Bukkit.getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                public void run() {
                    if (mySkeleton.getStatus() == PetState.Here) {
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            if (mySkeleton.getEquipment(slot) != null) {
                                entityMySkeleton.setPetEquipment(slot.getSlotId(), mySkeleton.getEquipment(slot));
                            }
                        }
                    }
                }
            }, 5L);
        }
    }

    public void setPetEquipment(int slot, ItemStack itemStack) {
        ((WorldServer) this.world).getTracker().a(this, new PacketPlayOutEntityEquipment(getId(), slot, itemStack));
    }

    public MySkeleton getMyPet() {
        return (MySkeleton) myPet;
    }
}