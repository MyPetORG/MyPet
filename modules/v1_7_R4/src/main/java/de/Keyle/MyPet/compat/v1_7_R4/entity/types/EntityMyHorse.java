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

package de.Keyle.MyPet.compat.v1_7_R4.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyHorse;
import de.Keyle.MyPet.compat.v1_7_R4.entity.EntityMyPet;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyHorse extends EntityMyPet {

    int soundCounter = 0;
    int rearCounter = -1;
    int ageCounter = -1;
    int ageFailCounter = 1;

    public EntityMyHorse(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Possible visual horse effects:
     * 4 saddle
     * 8 chest
     * 32 head down
     * 64 rear
     * 128 mouth open
     */
    private void applyVisual(int value, boolean flag) {
        int i = this.datawatcher.getInt(16);
        if (flag) {
            this.datawatcher.watch(16, i | value);
        } else {
            this.datawatcher.watch(16, i & (~value));
        }
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            flag = super.attack(entity);
            if (flag) {
                applyVisual(64, true);
                rearCounter = 10;
                if (getMyPet().getHorseType() == 0) {
                    this.world.makeSound(this, "mob.horse.angry", 1.0F, 1.0F);
                } else if (getMyPet().getHorseType() == 2 || getMyPet().getHorseType() == 1) {
                    this.world.makeSound(this, "mob.horse.donkey.angry", 1.0F, 1.0F);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        int horseType = getMyPet().getHorseType();
        if (horseType == 3) {
            return "mob.horse.zombie.death";
        }
        if (horseType == 4) {
            return "mob.horse.skeleton.death";
        }
        if ((horseType == 1) || (horseType == 2)) {
            return "mob.horse.donkey.death";
        }
        return "mob.horse.death";
    }

    @Override
    protected String getHurtSound() {
        int horseType = ((MyHorse) myPet).getHorseType();
        if (horseType == 3) {
            return "mob.horse.zombie.hit";
        }
        if (horseType == 4) {
            return "mob.horse.skeleton.hit";
        }
        if ((horseType == 1) || (horseType == 2)) {
            return "mob.horse.donkey.hit";
        }
        return "mob.horse.hit";
    }

    protected String getLivingSound() {
        if (playIdleSound()) {
            int horseType = ((MyHorse) myPet).getHorseType();
            if (horseType == 3) {
                return "mob.horse.zombie.idle";
            }
            if (horseType == 4) {
                return "mob.horse.skeleton.idle";
            }
            if ((horseType == 1) || (horseType == 2)) {
                return "mob.horse.donkey.idle";
            }
            return "mob.horse.idle";
        }
        return null;
    }


    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }
        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Item.getItemOf(Blocks.CHEST) && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
                getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (getHorseArmorId(CraftItemStack.asBukkitCopy(itemStack)) > 0 && !getMyPet().hasArmor() && getMyPet().getHorseType() == 0 && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setArmor(CraftItemStack.asBukkitCopy(itemStack));
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getMyPet().hasArmor()) {
                    EntityItem entityitem = this.a(CraftItemStack.asNMSCopy(getMyPet().getArmor()), 1F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                if (getMyPet().hasChest()) {
                    EntityItem entityitem = this.a(CraftItemStack.asNMSCopy(getMyPet().getChest()), 1F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                if (getMyPet().hasSaddle()) {
                    EntityItem entityitem = this.a(CraftItemStack.asNMSCopy(getMyPet().getSaddle()), 1F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }

                makeSound("mob.sheep.shear", 1.0F, 1.0F);
                getMyPet().setChest(null);
                getMyPet().setSaddle(null);
                getMyPet().setArmor(null);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Horse.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                getMyPet().setBaby(false);
                return true;
            }
            if (itemStack.getItem() == Items.BREAD ||
                    itemStack.getItem() == Items.WHEAT ||
                    itemStack.getItem() == Items.GOLDEN_APPLE ||
                    itemStack.getItem() == Item.getItemOf(Blocks.HAY_BLOCK) ||
                    itemStack.getItem() == Items.CARROT_GOLDEN ||
                    itemStack.getItem() == Items.APPLE ||
                    itemStack.getItem() == Items.SUGAR) {
                ageCounter = 5;
            }
        }
        return false;
    }

    private int getHorseArmorId(org.bukkit.inventory.ItemStack itemstack) {
        if (itemstack == null) {
            return 0;
        }
        Material item = itemstack.getType();

        return item == Material.DIAMOND_BARDING ? 3 : item == Material.GOLD_BARDING ? 2 : item == Material.IRON_BARDING ? 1 : 0;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, 0);        // age
        this.datawatcher.a(16, 0);        // saddle & chest
        this.datawatcher.a(19, (byte) 0); // horse type
        this.datawatcher.a(20, 0);        // variant
        this.datawatcher.a(21, "");       // owner
        this.datawatcher.a(22, 0);        // armor
    }

    @Override
    public void updateVisuals() {
        if (getMyPet().isBaby()) {
            this.datawatcher.watch(12, MathHelper.a(-24000, -1, 1));
        } else {
            this.datawatcher.watch(12, 0);
        }
        this.datawatcher.watch(22, getHorseArmorId(getMyPet().getArmor()));
        this.datawatcher.watch(19, getMyPet().getHorseType());
        this.datawatcher.watch(20, getMyPet().getVariant());
        applyVisual(8, getMyPet().hasChest());
        applyVisual(4, getMyPet().hasSaddle());
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!hasRider) {
            if (rearCounter > -1 && rearCounter-- == 0) {
                applyVisual(64, false);
                rearCounter = -1;
            }
        }
        if (ageCounter > -1 && ageCounter-- == 0) {
            this.datawatcher.watch(12, MathHelper.a(ageFailCounter++, -1, 1));
            ageCounter = -1;
            ageFailCounter %= 1000;
        }
    }

    @Override
    public void playStepSound(int i, int j, int k, Block block) {
        StepSound localStepSound = block.stepSound;
        if (this.world.getType(i, j + 1, k) == Blocks.SNOW) {
            localStepSound = Blocks.SNOW.stepSound;
        }
        if (!block.getMaterial().isLiquid()) {
            int horseType = getMyPet().getHorseType();
            if ((this.passenger != null) && (horseType != 1) && (horseType != 2)) {
                this.soundCounter += 1;
                if ((this.soundCounter > 5) && (this.soundCounter % 3 == 0)) {
                    makeSound("mob.horse.gallop", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
                    if ((horseType == 0) && (this.random.nextInt(10) == 0)) {
                        makeSound("mob.horse.breathe", localStepSound.getVolume1() * 0.6F, localStepSound.getVolume2());
                    }
                } else if (this.soundCounter <= 5) {
                    makeSound("mob.horse.wood", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
                }
            } else if (localStepSound == Block.f) {
                makeSound("mob.horse.wood", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
            } else {
                makeSound("mob.horse.soft", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
            }
        }
    }

    public MyHorse getMyPet() {
        return (MyHorse) myPet;
    }
}