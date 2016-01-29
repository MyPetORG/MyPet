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

package de.Keyle.MyPet.entity.types.pigzombie;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.MyPetVersion;
import de.Keyle.MyPet.util.Util;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;

@EntitySize(width = 0.6F, length = 0.6F, height = 1.9F)
public class EntityMyPigZombie extends EntityMyPet {
    public EntityMyPigZombie(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.zombiepig.zpigdeath";
    }

    @Override
    protected String getHurtSound() {
        return "mob.zombiepig.zpighurt";
    }

    protected String getLivingSound() {
        return "mob.zombiepig.zpig";
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
                EquipmentSlot slot = EquipmentSlot.getSlotById(c(itemStack));
                ItemStack itemInSlot = getMyPet().getEquipment(slot);
                if (itemInSlot != null && !entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                getMyPet().setEquipment(slot, itemStack);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (MyPigZombie.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
        getDataWatcher().a(12, new Byte((byte) 0)); // is baby
    }

    /**
     * Returns the speed of played sounds
     * The faster the higher the sound will be
     */
    public float getSoundSpeed() {
        return super.getSoundSpeed() + 0.4F;
    }

    public void setBaby(boolean flag) {
        getDataWatcher().watch(12, (byte) (flag ? 1 : 0));
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);
            final MyPigZombie myPigZombie = getMyPet();
            final EntityMyPigZombie entityMyPigZombie = this;

            this.setBaby(myPigZombie.isBaby());

            Bukkit.getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                public void run() {
                    if (myPigZombie.getStatus() == PetState.Here) {
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            if (myPigZombie.getEquipment(slot) != null) {
                                entityMyPigZombie.setPetEquipment(slot.getSlotId(), myPigZombie.getEquipment(slot));
                            }
                        }
                    }
                }
            }, 5L);
        }
    }

    public MyPigZombie getMyPet() {
        return (MyPigZombie) myPet;
    }

    public void setPetEquipment(int slot, ItemStack itemStack) {
        ((WorldServer) this.world).getTracker().a(this, new PacketPlayOutEntityEquipment(getId(), slot, itemStack));
    }

    public ItemStack getEquipment(int i) {
        if (Util.findClassInStackTrace(Thread.currentThread().getStackTrace(), "net.minecraft.server." + MyPetVersion.getBukkitPacket() + ".EntityTrackerEntry", 2)) {
            EquipmentSlot slot = EquipmentSlot.getSlotById(i);
            if (getMyPet().getEquipment(slot) != null) {
                return getMyPet().getEquipment(slot);
            }
        }
        return super.getEquipment(i);
    }
}