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

package de.Keyle.MyPet.compat.v1_9_R2.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWolf;
import de.Keyle.MyPet.compat.v1_9_R2.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_9_R2.entity.ai.movement.Sit;
import net.minecraft.server.v1_9_R2.*;
import org.bukkit.DyeColor;

import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.64f)
public class EntityMyWolf extends EntityMyPet {
    private static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.h);
    protected static final DataWatcherObject<Byte> sitWatcher = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> ownerWatcher = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.m);
    private static final DataWatcherObject<Float> tailWatcher = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.c);
    private static final DataWatcherObject<Boolean> watcher = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Integer> collarWatcher = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.b);

    protected boolean shaking;
    protected boolean isWet;
    protected float shakeCounter;
    private Sit sitPathfinder;

    public EntityMyWolf(World world, MyPet myPet) {
        super(world, myPet);
    }

    public void applySitting(boolean sitting) {
        int i = this.datawatcher.get(sitWatcher);
        if (sitting) {
            this.datawatcher.set(sitWatcher, (byte) (i | 0x1));
        } else {
            this.datawatcher.set(sitWatcher, (byte) (i & 0xFFFFFFFE));
        }
    }

    public boolean canMove() {
        return !sitPathfinder.isSitting();
    }

    @Override
    protected String getDeathSound() {
        return "entity.wolf.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.wolf.hurt";
    }

    protected String getLivingSound() {
        return this.random.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "entity.wolf.whine" : "entity.wolf.pant") : "entity.wolf.ambient";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (getOwner().equals(entityhuman)) {
            if (itemStack != null && canUseItem()) {
                if (itemStack.getItem() == Items.DYE && itemStack.getData() != getMyPet().getCollarColor().getWoolData()) {
                    if (itemStack.getData() <= 15) {
                        if (getOwner().getPlayer().isSneaking()) {
                            getMyPet().setCollarColor(DyeColor.getByWoolData((byte) itemStack.getData()));
                            if (!entityhuman.abilities.canInstantlyBuild) {
                                if (--itemStack.count <= 0) {
                                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                                }
                            }
                            return true;
                        } else {
                            this.datawatcher.set(collarWatcher, 0);
                            updateVisuals();
                            return false;
                        }
                    }
                } else if (Configuration.MyPet.Wolf.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        if (--itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    getMyPet().setBaby(false);
                    return true;
                }
            }
            this.sitPathfinder.toogleSitting();
            return true;
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();

        this.datawatcher.register(ageWatcher, false);               // age
        this.datawatcher.register(sitWatcher, (byte) 0);            // tamed/angry/sitting
        this.datawatcher.register(ownerWatcher, Optional.absent()); // owner
        this.datawatcher.register(tailWatcher, 20F);                // tail height
        this.datawatcher.register(watcher, false);                  // N/A
        this.datawatcher.register(collarWatcher, 14);               // collar color
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());

        byte b0 = this.datawatcher.get(sitWatcher);
        if (getMyPet().isTamed()) {
            this.datawatcher.set(sitWatcher, (byte) (b0 | 0x4));
        } else {
            this.datawatcher.set(sitWatcher, (byte) (b0 & 0xFFFFFFFB));
        }

        b0 = this.datawatcher.get(sitWatcher);
        if (getMyPet().isAngry()) {
            this.datawatcher.set(sitWatcher, (byte) (b0 | 0x2));
        } else {
            this.datawatcher.set(sitWatcher, (byte) (b0 & 0xFFFFFFFD));
        }

        this.datawatcher.set(collarWatcher, (int) getMyPet().getCollarColor().getWoolData());
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.isWet && !this.shaking && this.onGround) {
            this.shaking = true;
            this.shakeCounter = 0.0F;
            this.world.broadcastEntityEffect(this, (byte) 8);
        }

        if (isInWater()) // -> is in water
        {
            this.isWet = true;
            this.shaking = false;
            this.shakeCounter = 0.0F;
        } else if ((this.isWet || this.shaking) && this.shaking) {
            if (this.shakeCounter == 0.0F) {
                makeSound("entity.wolf.shake", 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            this.shakeCounter += 0.05F;
            if (this.shakeCounter - 0.05F >= 2.0F) {
                this.isWet = false;
                this.shaking = false;
                this.shakeCounter = 0.0F;
            }

            if (this.shakeCounter > 0.4F) {
                float locY = (float) this.getBoundingBox().b;
                int i = (int) (MathHelper.sin((this.shakeCounter - 0.4F) * 3.141593F) * 7.0F);
                for (; i >= 0; i--) {
                    float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;
                    float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;

                    this.world.addParticle(EnumParticle.WATER_SPLASH, this.locX + offsetX, locY + 0.8F, this.locZ + offsetZ, this.motX, this.motY, this.motZ);
                }
            }
        }

        float tailHeight = 20F * (getHealth() / getMaxHealth());
        if (this.datawatcher.get(tailWatcher) != tailHeight) {
            this.datawatcher.set(tailWatcher, tailHeight); // update tail height
        }
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.wolf.step", 0.15F, 1.0F);
    }

    public void setHealth(float i) {
        super.setHealth(i);

        float tailHeight = 20F * (i / getMaxHealth());
        this.datawatcher.set(tailWatcher, tailHeight);
    }

    public MyWolf getMyPet() {
        return (MyWolf) myPet;
    }

    public void setPathfinder() {
        super.setPathfinder();
        sitPathfinder = new Sit(this);
        petPathfinderSelector.addGoal("Sit", 2, sitPathfinder);
    }
}