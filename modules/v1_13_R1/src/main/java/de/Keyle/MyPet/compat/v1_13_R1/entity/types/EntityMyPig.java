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

package de.Keyle.MyPet.compat.v1_13_R1.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPig;
import de.Keyle.MyPet.compat.v1_13_R1.entity.EntityMyPet;
import net.minecraft.server.v1_13_R1.*;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@EntitySize(width = 0.7F, height = 0.9F)
public class EntityMyPig extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyPig.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Boolean> SADDLE_WATCHER = DataWatcher.a(EntityMyPig.class, DataWatcherRegistry.i);

    public EntityMyPig(World world, MyPet myPet) {
        super(EntityTypes.PIG, world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.pig.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.pig.hurt";
    }

    protected String getLivingSound() {
        return "entity.pig.ambient";
    }

    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (enumhand == EnumHand.OFF_HAND) {
            if (itemStack != null) {
                if (itemStack.getItem() == Items.LEAD) {
                    ((WorldServer) this.world).getTracker().a(this, new PacketPlayOutAttachEntity(this, null));
                    entityhuman.a(EnumHand.OFF_HAND, ItemStack.a);
                    new BukkitRunnable() {
                        public void run() {
                            if (entityhuman instanceof EntityPlayer) {
                                entityhuman.a(EnumHand.OFF_HAND, itemStack);
                                Player p = (Player) entityhuman.getBukkitEntity();
                                if (!p.isOnline()) {
                                    p.saveData();
                                }
                            }
                        }
                    }.runTaskLater(MyPetApi.getPlugin(), 5);
                }
            }
            return true;
        }

        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
                getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
                EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
                entityitem.pickupDelay = 10;
                entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                this.world.addEntity(entityitem);

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setSaddle(null);
                if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Pig.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(AGE_WATCHER, false);
        this.datawatcher.register(SADDLE_WATCHER, false);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(AGE_WATCHER, getMyPet().isBaby());
        this.datawatcher.set(SADDLE_WATCHER, getMyPet().hasSaddle());
    }

    public void playPetStepSound() {
        makeSound("entity.pig.step", 0.15F, 1.0F);
    }

    public MyPig getMyPet() {
        return (MyPig) myPet;
    }
}