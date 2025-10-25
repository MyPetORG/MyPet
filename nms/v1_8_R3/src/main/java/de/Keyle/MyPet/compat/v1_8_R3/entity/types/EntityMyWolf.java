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

package de.Keyle.MyPet.compat.v1_8_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWolf;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.DyeColor;

@EntitySize(width = 0.6F, height = 0.64f)
public class EntityMyWolf extends EntityMyPet {
    protected boolean shaking;
    protected boolean isWet;
    protected float shakeCounter;

    public EntityMyWolf(World world, MyPet myPet) {
        super(world, myPet);
    }

    public void applySitting(boolean sitting) {
        int i = this.datawatcher.getByte(16);
        if (sitting) {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        } else {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
    }

    @Override
    protected String getDeathSound() {
        return "mob.wolf.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.wolf.hurt";
    }

    protected String getLivingSound() {
        return this.random.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "mob.wolf.whine" : "mob.wolf.panting") : "mob.wolf.bark";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }
        ItemStack itemStack = entityhuman.inventory.getItemInHand();

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
                            this.datawatcher.watch(20, (byte) 0);
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
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, (byte) 0);     // age
        this.datawatcher.a(16, (byte) 0);     // tamed/angry/sitting
        this.datawatcher.a(17, "");           // wolf owner name
        this.datawatcher.a(18, 30F);          // tail height
        this.datawatcher.a(19, (byte) 0);     // N/A
        this.datawatcher.a(20, (byte) 14);    // collar color
    }

    @Override
    public void updateVisuals() {
        if (getMyPet().isBaby()) {
            this.datawatcher.watch(12, Byte.MIN_VALUE);
        } else {
            this.datawatcher.watch(12, (byte) 0);
        }

        int i = this.datawatcher.getByte(16);
        if (getMyPet().isTamed()) {
            this.datawatcher.watch(16, (byte) (i | 0x4));
        } else {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFB));
        }

        byte b0 = this.datawatcher.getByte(16);
        if (getMyPet().isAngry()) {
            this.datawatcher.watch(16, (byte) (b0 | 0x2));
        } else {
            this.datawatcher.watch(16, (byte) (b0 & 0xFFFFFFFD));
        }

        this.datawatcher.watch(20, getMyPet().getCollarColor().getWoolData());
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.isWet && !this.shaking && this.onGround) {
            this.shaking = true;
            this.shakeCounter = 0.0F;
            this.world.broadcastEntityEffect(this, (byte) 8);
        }

        if (U()) // -> is in water
        {
            this.isWet = true;
            this.shaking = false;
            this.shakeCounter = 0.0F;
        } else if ((this.isWet || this.shaking) && this.shaking) {
            if (this.shakeCounter == 0.0F) {
                makeSound("mob.wolf.shake", 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
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

        float tailHeight = 30F * getHealth() / getMaxHealth();
        if (this.datawatcher.getFloat(18) != tailHeight) {
            this.datawatcher.watch(18, tailHeight); // update tail height
        }
    }

    @Override
    public void playStepSound() {
        makeSound("mob.wolf.step", 0.15F, 1.0F);
    }

    public void setHealth(float i) {
        super.setHealth(i);
        float tailHeight = 30F * getHealth() / getMaxHealth();
        this.datawatcher.watch(18, tailHeight);
    }

    public MyWolf getMyPet() {
        return (MyWolf) myPet;
    }
}