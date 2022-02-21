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

package de.Keyle.MyPet.compat.v1_16_R1.entity.types;

import com.mojang.datafixers.util.Pair;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVindicator;
import de.Keyle.MyPet.compat.v1_16_R1.CompatManager;
import de.Keyle.MyPet.compat.v1_16_R1.entity.EntityMyPet;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@EntitySize(width = 0.6F, height = 1.95F)
public class EntityMyVindicator extends EntityMyPet {

    protected static final DataWatcherObject<Boolean> RAID_WATCHER = DataWatcher.a(EntityMyVindicator.class, DataWatcherRegistry.i);

    public EntityMyVindicator(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound() {
        return "entity.vindicator.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.vindicator.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    @Override
    protected String getLivingSound() {
        return "entity.vindicator.ambient";
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    @Override
    public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
            return EnumInteractionResult.CONSUME;
        }

        if (getOwner().equals(entityhuman) && itemStack != null) {
            if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                boolean hadEquipment = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
                    if (itemInSlot != null && itemInSlot.getItem() != Items.AIR) {
                        EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), itemInSlot);
                        entityitem.pickupDelay = 10;
                        entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                        this.world.addEntity(entityitem);
                        getMyPet().setEquipment(slot, null);
                        hadEquipment = true;
                    }
                }
                if (hadEquipment) {
                    if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                        try {
                            itemStack.damage(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastItemBreak(enumhand));
                        } catch (Error e) {
                            // TODO REMOVE
                            itemStack.damage(1, entityhuman, (entityhuman1) -> {
                                try {
                                    CompatManager.ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
                                } catch (IllegalAccessException | InvocationTargetException ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                    }
                }
                return EnumInteractionResult.CONSUME;
            } else if (MyPetApi.getPlatformHelper().isEquipment(CraftItemStack.asBukkitCopy(itemStack)) && getOwner().getPlayer().isSneaking() && canEquip()) {
                EquipmentSlot slot = EquipmentSlot.getSlotById(j(itemStack).c());
                if (slot == EquipmentSlot.MainHand) {
                    ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
                    if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && itemInSlot != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                        EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), itemInSlot);
                        entityitem.pickupDelay = 10;
                        entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                        this.world.addEntity(entityitem);
                    }
                    getMyPet().setEquipment(slot, CraftItemStack.asBukkitCopy(itemStack));
                    if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                        itemStack.subtract(1);
                        if (itemStack.getCount() <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                        }
                    }
                    return EnumInteractionResult.CONSUME;
                }
            } else if (itemStack.getItem() instanceof ItemBanner && getOwner().getPlayer().isSneaking() && canEquip()) {
                ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.Helmet));
                if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && itemInSlot != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), itemInSlot);
                    entityitem.pickupDelay = 10;
                    entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                    this.world.addEntity(entityitem);
                }
                getMyPet().setEquipment(EquipmentSlot.Helmet, CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                return EnumInteractionResult.CONSUME;
            }
        }
        return EnumInteractionResult.PASS;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(RAID_WATCHER, false);
    }

    @Override
    public void updateVisuals() {
        Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
            if (getMyPet().getStatus() == MyPet.PetState.Here) {
                setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand)), EnumItemSlot.MAINHAND);
                setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.Helmet)), EnumItemSlot.HEAD);

                ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand));
                if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && !this.isAggressive()) {
                    this.setAggressive(true);
                }
                if (itemInSlot == null || itemInSlot.getItem() == Items.AIR && this.isAggressive()) {
                    this.setAggressive(false);
                }
            }
        }, 5L);
    }

    @Override
    public MyVindicator getMyPet() {
        return (MyVindicator) myPet;
    }

    public void setPetEquipment(ItemStack itemStack, EnumItemSlot slot) {
        ((WorldServer) this.world).getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutEntityEquipment(getId(), Arrays.asList(new Pair<>(slot, itemStack))));
    }

    @Override
    public ItemStack getEquipment(EnumItemSlot vanillaSlot) {
        if (Util.findClassInStackTrace(Thread.currentThread().getStackTrace(), "net.minecraft.server." + MyPetApi.getCompatUtil().getInternalVersion() + ".EntityTrackerEntry", 2)) {
            EquipmentSlot slot = EquipmentSlot.getSlotById(vanillaSlot.c());
            if (getMyPet().getEquipment(slot) != null) {
                return CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
            }
        }
        return super.getEquipment(vanillaSlot);
    }
}