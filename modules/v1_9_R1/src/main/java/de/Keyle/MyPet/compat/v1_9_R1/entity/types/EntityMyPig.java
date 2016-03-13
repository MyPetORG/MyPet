/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_9_R1.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPig;
import de.Keyle.MyPet.compat.v1_9_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.Ride;
import net.minecraft.server.v1_9_R1.*;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@EntitySize(width = 0.7F, height = 0.9F)
public class EntityMyPig extends EntityMyPet {
    private static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityMyPig.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Boolean> saddleWatcher = DataWatcher.a(EntityMyPig.class, DataWatcherRegistry.h);

    public EntityMyPig(World world, MyPet myPet) {
        super(world, myPet);
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
                    entityhuman.a(EnumHand.OFF_HAND, null);
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

        if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
            if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(itemStack)) {
                if (myPet.getSkills().isSkillActive(Ride.class) && canMove()) {
                    if (itemStack.getItem() == Items.LEAD) {
                        ((WorldServer) this.world).getTracker().a(this, new PacketPlayOutAttachEntity(this, null));
                        entityhuman.a(EnumHand.MAIN_HAND, null);
                        new BukkitRunnable() {
                            public void run() {
                                if (entityhuman instanceof EntityPlayer) {
                                    entityhuman.a(EnumHand.MAIN_HAND, itemStack);
                                    Player p = (Player) entityhuman.getBukkitEntity();
                                    if (!p.isOnline()) {
                                        p.saveData();
                                    }
                                }
                            }
                        }.runTaskLater(MyPetApi.getPlugin(), 5);
                    }
                    getMyPet().getOwner().sendMessage("Ironically pigs can not be ridden right now (Minecraft 1.9 problem)");
                    return true;
                }
            }
        }

        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
                getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
                EntityItem entityitem = this.a(CraftItemStack.asNMSCopy(getMyPet().getSaddle()), 1.0F);
                entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setSaddle(null);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Pig.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
        this.datawatcher.register(ageWatcher, false);    // age
        this.datawatcher.register(saddleWatcher, false); // saddle
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());
        this.datawatcher.set(saddleWatcher, getMyPet().hasSaddle());
    }

    public void playPetStepSound() {
        makeSound("entity.pig.step", 0.15F, 1.0F);
    }

    public MyPig getMyPet() {
        return (MyPig) myPet;
    }
}