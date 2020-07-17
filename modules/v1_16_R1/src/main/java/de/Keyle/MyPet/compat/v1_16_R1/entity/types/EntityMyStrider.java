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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyStrider;
import de.Keyle.MyPet.compat.v1_16_R1.entity.EntityMyPet;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;

import static de.Keyle.MyPet.compat.v1_16_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.9F, height = 1.7F)
public class EntityMyStrider extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyStrider.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> BOOST_TICKS_WATCHER = DataWatcher.a(EntityMyStrider.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Boolean> HAS_RIDER_WATCHER = DataWatcher.a(EntityMyStrider.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Boolean> SADDLE_WATCHER = DataWatcher.a(EntityMyStrider.class, DataWatcherRegistry.i);

    public EntityMyStrider(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.strider.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.strider.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.strider.ambient";
    }

    @Override
    public EnumInteractionResult handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
            return EnumInteractionResult.CONSUME;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
                getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                return EnumInteractionResult.CONSUME;
            } else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
                EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
                entityitem.pickupDelay = 10;
                entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
                this.world.addEntity(entityitem);

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setSaddle(null);
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
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

                return EnumInteractionResult.CONSUME;
            } else if (Configuration.MyPet.Strider.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                getMyPet().setBaby(false);
                return EnumInteractionResult.CONSUME;
            }
        }
        return EnumInteractionResult.PASS;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(BOOST_TICKS_WATCHER, 0);
        getDataWatcher().register(HAS_RIDER_WATCHER, false);
        getDataWatcher().register(SADDLE_WATCHER, false);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(SADDLE_WATCHER, getMyPet().hasSaddle());
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.strider.step", 0.15F, 1.0F);
    }

    @Override
    public MyStrider getMyPet() {
        return (MyStrider) myPet;
    }
}