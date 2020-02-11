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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyFox;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.compat.v1_15_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyFox extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> FOX_TYPE_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Byte> ACTIONS_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Optional<UUID>> FRIEND_A_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Optional<UUID>> FRIEND_B_WATCHER = DataWatcher.a(EntityMyFox.class, DataWatcherRegistry.o);

    public EntityMyFox(World world, MyPet myPet) {
        super(world, myPet);
        this.getControllerLook().a(this, 60.0F, 30.0F);
    }

    protected String getDeathSound() {
        return "entity.fox.death";
    }

    protected String getHurtSound() {
        return "entity.fox.hurt";
    }

    protected String getLivingSound() {
        return "entity.fox.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman)) {
            if (itemStack != null && itemStack.getItem() != Items.AIR && canUseItem() && getOwner().getPlayer().isSneaking()) {
                if (itemStack.getItem() != Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                    ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand));
                    if (itemInSlot != null && itemInSlot.getItem() != Items.AIR && itemInSlot != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                        EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), itemInSlot);
                        entityitem.pickupDelay = 10;
                        entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                        this.world.addEntity(entityitem);
                    }
                    getMyPet().setEquipment(EquipmentSlot.MainHand, CraftItemStack.asBukkitCopy(itemStack));
                    if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                        itemStack.subtract(1);
                        if (itemStack.getCount() <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                        }
                    }
                    return true;
                } else if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                    if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                        itemStack.subtract(1);
                        if (itemStack.getCount() <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                        }
                    }
                    getMyPet().setBaby(false);
                    return true;
                } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
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
                    }
                    return true;
                }
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(FRIEND_A_WATCHER, Optional.empty());
        getDataWatcher().register(FRIEND_B_WATCHER, Optional.empty());
        getDataWatcher().register(FOX_TYPE_WATCHER, 0);
        getDataWatcher().register(ACTIONS_WATCHER, (byte) 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(FOX_TYPE_WATCHER, getMyPet().getType().ordinal());

        Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
            if (getMyPet().getStatus() == MyPet.PetState.Here) {
                setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand)));
            }
        }, 5L);
    }

    /*
     *  1   = sitting
     *  2   =
     *  4   = ready for jumping
     *  8   = curious
     *  16  =
     *  32  = sleeping
     *  64  = feet spasm
     *  128 =
     */
    public void updateActionsWatcher(int i, boolean flag) {
        if (flag) {
            this.datawatcher.set(ACTIONS_WATCHER, (byte) (this.datawatcher.get(ACTIONS_WATCHER) | i));
        } else {
            this.datawatcher.set(ACTIONS_WATCHER, (byte) (this.datawatcher.get(ACTIONS_WATCHER) & ~i));
        }
    }

    public void movementTick() {
        super.movementTick();
        // foxes can't look up
        this.pitch = 0F;
    }

    public void setPetEquipment(ItemStack itemStack) {
        ((WorldServer) this.world).getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutEntityEquipment(getId(), EnumItemSlot.MAINHAND, itemStack));
    }

    public ItemStack getEquipment(EnumItemSlot vanillaSlot) {
        if (Util.findClassInStackTrace(Thread.currentThread().getStackTrace(), "net.minecraft.server." + MyPetApi.getCompatUtil().getInternalVersion() + ".EntityTrackerEntry", 2)) {
            EquipmentSlot slot = EquipmentSlot.getSlotById(vanillaSlot.c());
            if (getMyPet().getEquipment(slot) != null) {
                return CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
            }
        }
        return super.getEquipment(vanillaSlot);
    }

    public MyFox getMyPet() {
        return (MyFox) myPet;
    }
}