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

package de.Keyle.MyPet.compat.v1_13_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyIronGolem;
import de.Keyle.MyPet.compat.v1_13_R2.entity.EntityMyPet;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

@EntitySize(width = 1.4F, height = 2.7F)
public class EntityMyIronGolem extends EntityMyPet {

    protected static final DataWatcherObject<Byte> UNUSED_WATCHER = DataWatcher.a(EntityMyIronGolem.class, DataWatcherRegistry.a);

    int flowerCounter = 0;
    boolean flower = false;

    public EntityMyIronGolem(World world, MyPet myPet) {
        super(EntityTypes.IRON_GOLEM, world, myPet);
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            this.world.broadcastEntityEffect(this, (byte) 4);
            flag = super.attack(entity);
            if (Configuration.MyPet.IronGolem.CAN_TOSS_UP && flag) {
                entity.motY += 0.4000000059604645D;
                this.makeSound("entity.iron_golem.attack", 1.0F, 1.0F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        return "entity.iron_golem.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.iron_golem.hurt";
    }

    protected String getLivingSound() {
        return null;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(UNUSED_WATCHER, (byte) 0); // N/A
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Blocks.POPPY.getItem() && !getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
                getMyPet().setFlower(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
                EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getFlower()));
                entityitem.pickupDelay = 10;
                entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                this.world.addEntity(entityitem);

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setFlower(null);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            }
        }
        return false;
    }

    @Override
    public void updateVisuals() {
        flower = getMyPet().hasFlower();
        flowerCounter = 0;
    }

    public MyIronGolem getMyPet() {
        return (MyIronGolem) myPet;
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.iron_golem.step", 1.0F, 1.0F);
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.flower && this.flowerCounter-- <= 0) {
            this.world.broadcastEntityEffect(this, (byte) 11);
            flowerCounter = 300;
        }
    }
}