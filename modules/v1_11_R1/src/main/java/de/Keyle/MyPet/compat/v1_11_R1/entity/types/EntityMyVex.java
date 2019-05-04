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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVex;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_11_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;

@EntitySize(width = 0.4F, height = 0.8F)
public class EntityMyVex extends EntityMyPet {

    protected static final DataWatcherObject<Byte> CHARGING_WATCHER = DataWatcher.a(EntityMyVex.class, DataWatcherRegistry.a);

    protected boolean isAggressive = false;

    public EntityMyVex(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound() {
        return "entity.vex.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.vex.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String getLivingSound() {
        return "entity.vex.ambient";
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null) {
            if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                boolean hadEquipment = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
                    if (itemInSlot != null) {
                        EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, itemInSlot);
                        entityitem.pickupDelay = 10;
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        this.world.addEntity(entityitem);
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
            } else if (MyPetApi.getPlatformHelper().isEquipment(CraftItemStack.asBukkitCopy(itemStack)) && getOwner().getPlayer().isSneaking() && canEquip()) {
                EquipmentSlot slot = EquipmentSlot.getSlotById(d(itemStack).c());
                if (slot == EquipmentSlot.MainHand) {
                    ItemStack itemInSlot = CraftItemStack.asNMSCopy(getMyPet().getEquipment(slot));
                    if (itemInSlot != null && !entityhuman.abilities.canInstantlyBuild) {
                        EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, itemInSlot);
                        entityitem.pickupDelay = 10;
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        this.world.addEntity(entityitem);
                    }
                    getMyPet().setEquipment(slot, CraftItemStack.asBukkitCopy(itemStack));
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        itemStack.subtract(1);
                        if (itemStack.getCount() <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
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
        getDataWatcher().register(CHARGING_WATCHER, (byte) 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(CHARGING_WATCHER, (byte) (getMyPet().isGlowing() || isAggressive ? 1 : 0));

        Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
            if (getMyPet().getStatus() == MyPet.PetState.Here) {
                setPetEquipment(CraftItemStack.asNMSCopy(getMyPet().getEquipment(EquipmentSlot.MainHand)));
            }
        }, 5L);
    }

    public MyVex getMyPet() {
        return (MyVex) myPet;
    }

    public void setPetEquipment(ItemStack itemStack) {
        ((WorldServer) this.world).getTracker().a(this, new PacketPlayOutEntityEquipment(getId(), EnumItemSlot.MAINHAND, itemStack));
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

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Vex.CAN_GLIDE)
        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }
    }

    protected void doMyPetTick() {
        super.doMyPetTick();
        BehaviorImpl skill = getMyPet().getSkills().get(BehaviorImpl.class);
        Behavior.BehaviorMode behavior = skill.getBehavior();
        if (behavior == Behavior.BehaviorMode.Aggressive) {
            if (!isAggressive) {
                isAggressive = true;
                this.updateVisuals();
            }
        } else {
            if (isAggressive) {
                isAggressive = false;
                this.updateVisuals();
            }
        }
    }

    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
        if (!Configuration.MyPet.Vex.CAN_GLIDE) {
            super.e(f, f1);
        }
    }
}