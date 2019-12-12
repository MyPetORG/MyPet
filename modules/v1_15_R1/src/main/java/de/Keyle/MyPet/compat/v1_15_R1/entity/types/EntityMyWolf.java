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

package de.Keyle.MyPet.compat.v1_15_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWolf;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.DyeColor;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.64f)
public class EntityMyWolf extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.i);
    protected static final DataWatcherObject<Byte> SIT_WATCHER = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Float> TAIL_WATCHER = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.c);
    private static final DataWatcherObject<Boolean> UNUSED_WATCHER = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> COLLAR_COLOR_WATCHER = DataWatcher.a(EntityMyWolf.class, DataWatcherRegistry.b);

    protected boolean shaking;
    protected boolean isWet;
    protected float shakeCounter;

    public EntityMyWolf(World world, MyPet myPet) {
        super(world, myPet);
    }

    public void applySitting(boolean sitting) {
        int i = getDataWatcher().get(SIT_WATCHER);
        if (sitting) {
            getDataWatcher().set(SIT_WATCHER, (byte) (i | 0x1));
        } else {
            getDataWatcher().set(SIT_WATCHER, (byte) (i & 0xFFFFFFFE));
        }
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
            if (itemStack != null && itemStack.getItem() != Items.AIR) {
                if (canUseItem()) {
                    if (itemStack.getItem() instanceof ItemDye && ((ItemDye) itemStack.getItem()).d().ordinal() != getMyPet().getCollarColor().ordinal()) {
                        if (getOwner().getPlayer().isSneaking()) {
                            getMyPet().setCollarColor(DyeColor.values()[((ItemDye) itemStack.getItem()).d().ordinal()]);
                            if (itemStack != ItemStack.a && !entityhuman.abilities.canInstantlyBuild) {
                                itemStack.subtract(1);
                                if (itemStack.getCount() <= 0) {
                                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                                }
                            }
                            return true;
                        } else {
                            getDataWatcher().set(COLLAR_COLOR_WATCHER, 0);
                            updateVisuals();
                            return false;
                        }
                    } else if (Configuration.MyPet.Wolf.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();

        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(SIT_WATCHER, (byte) 0);
        getDataWatcher().register(OWNER_WATCHER, Optional.empty());
        getDataWatcher().register(TAIL_WATCHER, 30F);
        getDataWatcher().register(UNUSED_WATCHER, false); // not used
        getDataWatcher().register(COLLAR_COLOR_WATCHER, 14);
    }

    protected void initAttributes() {
        super.initAttributes();
        getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(20.0D);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());

        byte b0 = getDataWatcher().get(SIT_WATCHER);
        if (getMyPet().isTamed()) {
            getDataWatcher().set(SIT_WATCHER, (byte) (b0 | 0x4));
        } else {
            getDataWatcher().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFB));
        }

        b0 = getDataWatcher().get(SIT_WATCHER);
        if (getMyPet().isAngry()) {
            getDataWatcher().set(SIT_WATCHER, (byte) (b0 | 0x2));
        } else {
            getDataWatcher().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFD));
        }

        getDataWatcher().set(COLLAR_COLOR_WATCHER, getMyPet().getCollarColor().ordinal());
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
                int i = (int) (MathHelper.sin((this.shakeCounter - 0.4F) * 3.141593F) * 7.0F);
                for (; i >= 0; i--) {
                    float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                    float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;

                    this.world.addParticle(Particles.SPLASH, this.locX() + offsetX, this.locY() + 0.8F, this.locZ() + offsetZ, this.getMot().x, this.getMot().y, this.getMot().z);
                }
            }
        }

        float tailHeight = 30F * (getHealth() / getMaxHealth());
        if (getDataWatcher().get(TAIL_WATCHER) != tailHeight) {
            getDataWatcher().set(TAIL_WATCHER, tailHeight); // update tail height
        }
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.wolf.step", 0.15F, 1.0F);
    }

    public void setHealth(float i) {
        super.setHealth(i);

        float tailHeight = 30F * (i / getMaxHealth());
        getDataWatcher().set(TAIL_WATCHER, tailHeight);
    }

    public MyWolf getMyPet() {
        return (MyWolf) myPet;
    }
}