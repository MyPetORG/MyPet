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
import de.Keyle.MyPet.api.entity.types.MyHorse;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.compat.v1_15_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 1.3965F, height = 1.6F)
public class EntityMyHorse extends EntityMyPet implements IJumpable {

    protected static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.i);
    protected static final DataWatcherObject<Byte> SADDLE_CHEST_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.b);

    int soundCounter = 0;
    int rearCounter = -1;

    public EntityMyHorse(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Possible visual horse effects:
     * 4 saddle
     * 8 chest
     * 32 head down
     * 64 rear
     */
    protected void applyVisual(int value, boolean flag) {
        int i = getDataWatcher().get(SADDLE_CHEST_WATCHER);
        if (flag) {
            getDataWatcher().set(SADDLE_CHEST_WATCHER, (byte) (i | value));
        } else {
            getDataWatcher().set(SADDLE_CHEST_WATCHER, (byte) (i & (~value)));
        }
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            flag = super.attack(entity);
            if (flag) {
                applyVisual(64, true);
                rearCounter = 10;
                this.makeSound("entity.horse.angry", 1.0F, 1.0F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        return "entity.horse.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.horse.hurt";
    }

    protected String getLivingSound() {
        return "entity.horse.ambient";
    }


    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (itemStack != null && canUseItem()) {
            org.bukkit.inventory.ItemStack is = CraftItemStack.asBukkitCopy(itemStack);
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setSaddle(is);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (!getMyPet().hasArmor() && getOwner().getPlayer().isSneaking() && canEquip() && isArmor(is)) {
                getMyPet().setArmor(is);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getMyPet().hasArmor()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getArmor()));
                    entityitem.pickupDelay = 10;
                    entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasChest()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getChest()));
                    entityitem.pickupDelay = 10;
                    entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasSaddle()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
                    entityitem.pickupDelay = 10;
                    entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                    this.world.addEntity(entityitem);
                }

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setChest(null);
                getMyPet().setSaddle(null);
                getMyPet().setArmor(null);
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

                return true;
            } else if (Configuration.MyPet.Horse.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
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

    protected boolean isArmor(org.bukkit.inventory.ItemStack item) {
        if (item != null) {
            switch (item.getType()) {
                case LEATHER_HORSE_ARMOR:
                case IRON_HORSE_ARMOR:
                case GOLDEN_HORSE_ARMOR:
                case DIAMOND_HORSE_ARMOR:
                    return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(SADDLE_CHEST_WATCHER, (byte) 0);
        getDataWatcher().register(OWNER_WATCHER, Optional.empty());
        getDataWatcher().register(VARIANT_WATCHER, 0);
    }

    protected void initAttributes() {
        super.initAttributes();
        this.getAttributeMap().b(EntityHorseAbstract.attributeJumpStrength);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(VARIANT_WATCHER, getMyPet().getVariant());
        applyVisual(4, getMyPet().hasSaddle());
        Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
            if (getMyPet().getStatus() == MyPet.PetState.Here) {
                setPetEquipment(EquipmentSlot.Chestplate, CraftItemStack.asNMSCopy(getMyPet().getArmor()));
            }
        }, 5L);
    }

    public void onLivingUpdate() {
        boolean oldRiding = hasRider;
        super.onLivingUpdate();
        if (!hasRider) {
            if (rearCounter > -1 && rearCounter-- == 0) {
                applyVisual(64, false);
                rearCounter = -1;
            }
        }
        if (oldRiding != hasRider) {
            if (hasRider) {
                applyVisual(4, true);
            } else {
                applyVisual(4, getMyPet().hasSaddle());
            }
        }
    }

    @Override
    public void playStepSound(BlockPosition blockposition, IBlockData blockdata) {
        if (!blockdata.getMaterial().isLiquid()) {
            IBlockData blockdataUp = this.world.getType(blockposition.up());
            SoundEffectType soundeffecttype = blockdata.r();
            if (blockdataUp.getBlock() == Blocks.SNOW) {
                soundeffecttype = blockdata.r();
            }
            if (this.isVehicle()) {
                ++this.soundCounter;
                if (this.soundCounter > 5 && this.soundCounter % 3 == 0) {
                    this.a(SoundEffects.ENTITY_HORSE_GALLOP, soundeffecttype.a() * 0.15F, soundeffecttype.b());
                } else if (this.soundCounter <= 5) {
                    this.a(SoundEffects.ENTITY_HORSE_STEP_WOOD, soundeffecttype.a() * 0.15F, soundeffecttype.b());
                }
            } else if (!blockdata.getMaterial().isLiquid()) {
                this.soundCounter += 1;
                a(SoundEffects.ENTITY_HORSE_STEP_WOOD, soundeffecttype.a() * 0.15F, soundeffecttype.b());
            } else {
                a(SoundEffects.ENTITY_HORSE_STEP, soundeffecttype.a() * 0.15F, soundeffecttype.b());
            }
        }
    }

    public void setPetEquipment(EquipmentSlot slot, ItemStack itemStack) {
        ((WorldServer) this.world).getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutEntityEquipment(getId(), EnumItemSlot.values()[slot.get19Slot()], itemStack));
    }

    public ItemStack getEquipment(EnumItemSlot vanillaSlot) {
        if (Util.findClassInStackTrace(Thread.currentThread().getStackTrace(), "net.minecraft.server." + MyPetApi.getCompatUtil().getInternalVersion() + ".EntityTrackerEntry", 2)) {
            EquipmentSlot slot = EquipmentSlot.getSlotById(vanillaSlot.c());
            if (slot == EquipmentSlot.Chestplate && getMyPet().getArmor() != null) {
                return CraftItemStack.asNMSCopy(getMyPet().getArmor());
            }
        }
        return super.getEquipment(vanillaSlot);
    }

    public MyHorse getMyPet() {
        return (MyHorse) myPet;
    }

    /* Jump power methods */
    @Override
    public boolean G_() {
        return true;
    }

    @Override
    public void b(int i) {
        this.jumpPower = i;
    }

    @Override
    public void c() {
    }
}