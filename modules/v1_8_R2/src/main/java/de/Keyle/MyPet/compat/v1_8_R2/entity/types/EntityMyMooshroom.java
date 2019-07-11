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

package de.Keyle.MyPet.compat.v1_8_R2.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyMooshroom;
import de.Keyle.MyPet.compat.v1_8_R2.entity.EntityMyPet;
import net.minecraft.server.v1_8_R2.EntityHuman;
import net.minecraft.server.v1_8_R2.ItemStack;
import net.minecraft.server.v1_8_R2.Items;
import net.minecraft.server.v1_8_R2.World;
import org.bukkit.Bukkit;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyMooshroom extends EntityMyPet {
    public EntityMyMooshroom(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.cow.hurt";
    }

    @Override
    protected String getHurtSound() {
        return "mob.cow.hurt";
    }

    protected String getLivingSound() {
        return "mob.cow.say";
    }

    public boolean handlePlayerInteraction(final EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack != null) {
            if (itemStack.getItem().equals(Items.BOWL)) {
                if (!getOwner().equals(entityhuman) || !canUseItem() || !Configuration.MyPet.Mooshroom.CAN_GIVE_SOUP) {
                    final int itemInHandIndex = entityhuman.inventory.itemInHandIndex;
                    ItemStack is = new ItemStack(Items.MUSHROOM_STEW);
                    final ItemStack oldIs = entityhuman.inventory.getItem(itemInHandIndex);
                    entityhuman.inventory.setItem(itemInHandIndex, is);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(MyPetApi.getPlugin(), () -> entityhuman.inventory.setItem(itemInHandIndex, oldIs), 2L);

                } else {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, new ItemStack(Items.MUSHROOM_STEW));
                    } else {
                        if (!entityhuman.inventory.pickup(new ItemStack(Items.MUSHROOM_STEW))) {
                            entityhuman.drop(new ItemStack(Items.GLASS_BOTTLE), true);
                        }
                    }
                    return true;
                }
            }
            if (getOwner().equals(entityhuman) && canUseItem()) {
                if (Configuration.MyPet.Mooshroom.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        if (--itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    getMyPet().setBaby(false);
                    return true;
                }
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, (byte) 0); // age
    }

    @Override
    public void updateVisuals() {
        if (getMyPet().isBaby()) {
            this.datawatcher.watch(12, Byte.MIN_VALUE);
        } else {
            this.datawatcher.watch(12, (byte) 0);
        }
    }

    public void playStepSound() {
        makeSound("mob.cow.step", 0.15F, 1.0F);
    }

    public MyMooshroom getMyPet() {
        return (MyMooshroom) myPet;
    }
}