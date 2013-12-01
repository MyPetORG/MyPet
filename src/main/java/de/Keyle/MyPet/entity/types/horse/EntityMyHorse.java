/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types.horse;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.ConfigItem;
import net.minecraft.server.v1_7_R1.*;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyHorse extends EntityMyPet {
    public static ConfigItem GROW_UP_ITEM;
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
            this.datawatcher.watch(16, Integer.valueOf(i | value));
        } else {
            this.datawatcher.watch(16, Integer.valueOf(i & (~value)));
        }
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            flag = super.attack(entity);
            if (flag) {
                applyVisual(64, true);
                rearCounter = 10;
                if (getHorseType() == 0) {
                    this.world.makeSound(this, "mob.horse.angry", 1.0F, 1.0F);
                } else if (getHorseType() == 2 || getHorseType() == 1) {
                    this.world.makeSound(this, "mob.horse.donkey.angry", 1.0F, 1.0F);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public int getAge() {
        return ((MyHorse) myPet).age;
    }

    public void setAge(int value) {
        value = Math.min(0, (Math.max(-24000, value)));
        value -= value % 1000;
        ((MyHorse) myPet).age = value;
        this.datawatcher.watch(12, new Integer(value));
    }

    public int getArmor() {
        return ((MyHorse) myPet).armor;
    }

    public void setArmor(int value) {
        this.datawatcher.watch(22, Integer.valueOf(value));
        ((MyHorse) myPet).armor = value;
    }

    @Override
    protected String getDeathSound() {
        int horseType = ((MyHorse) myPet).horseType;
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

    public byte getHorseType() {
        return ((MyHorse) myPet).horseType;
    }

    public void setHorseType(byte horseType) {
        this.datawatcher.watch(19, Byte.valueOf(horseType));
        ((MyHorse) myPet).horseType = horseType;
    }

    @Override
    protected String getHurtSound() {
        int horseType = ((MyHorse) myPet).horseType;
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
            int horseType = ((MyHorse) myPet).horseType;
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

    public int getVariant() {
        return ((MyHorse) myPet).variant;
    }

    public void setVariant(int variant) {
        this.datawatcher.watch(20, Integer.valueOf(variant));
        ((MyHorse) myPet).variant = variant;
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }
        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.SADDLE && getOwner().getPlayer().isSneaking() && !hasSaddle() && getAge() >= 0 && canEquip()) {
                setSaddle(true);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Item.getItemOf(Blocks.CHEST) && getOwner().getPlayer().isSneaking() && !hasChest() && getAge() >= 0 && canEquip()) {
                setChest(true);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                return true;
            } else if (getHorseArmorId(itemStack) > 0 && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getHorseType() == 0) {
                    if (getArmor() > 0 && !entityhuman.abilities.canInstantlyBuild) {
                        EntityItem entityitem = this.a(new ItemStack(Item.d(416 + getArmor()), 1, 0), 1F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    setArmor(getHorseArmorId(itemStack));
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        if (--itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    return true;
                }
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getArmor() > 0 && !entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = this.a(new ItemStack(Item.d(416 + getArmor()), 1, 0), 1F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                if (hasChest() && !entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = this.a(new ItemStack(Blocks.CHEST), 1F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                if (hasSaddle() && !entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = this.a(new ItemStack(Items.SADDLE), 1F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                setChest(false);
                setSaddle(false);
                setArmor(0);
                return true;
            } else if (GROW_UP_ITEM.compare(itemStack)) {
                if (isBaby()) {
                    if (getOwner().getPlayer().isSneaking()) {
                        if (!entityhuman.abilities.canInstantlyBuild) {
                            if (--itemStack.count <= 0) {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        this.setAge(getAge() + 3000);
                        return true;
                    }
                }
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

    private int getHorseArmorId(ItemStack itemstack) {
        if (itemstack == null) {
            return 0;
        }
        Item item = itemstack.getItem();

        return item == Items.HORSE_ARMOR_DIAMOND ? 3 : item == Items.HORSE_ARMOR_GOLD ? 2 : item == Items.HORSE_ARMOR_IRON ? 1 : 0;
    }

    public boolean hasChest() {
        return ((MyHorse) myPet).chest;
    }

    public boolean hasSaddle() {
        return ((MyHorse) myPet).saddle;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, Integer.valueOf(0));     // age
        this.datawatcher.a(16, Integer.valueOf(0));     // saddle & chest
        this.datawatcher.a(19, Byte.valueOf((byte) 0)); // horse type
        this.datawatcher.a(20, Integer.valueOf(0));     // variant
        this.datawatcher.a(21, String.valueOf(""));     // N/A
        this.datawatcher.a(22, Integer.valueOf(0));     // armor
    }

    public boolean isBaby() {
        return ((MyHorse) myPet).age < 0;
    }

    public void setBaby(boolean flag) {
        if (flag) {
            this.datawatcher.watch(12, Integer.valueOf(-24000));
            ((MyHorse) myPet).age = -24000;
        } else {
            this.datawatcher.watch(12, new Integer(0));
            ((MyHorse) myPet).age = 0;
        }
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (rearCounter > -1 && rearCounter-- == 0) {
            applyVisual(64, false);
            rearCounter = -1;
        }
        if (ageCounter > -1 && ageCounter-- == 0) {
            this.datawatcher.watch(12, new Integer(getAge() + ageFailCounter++));
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
            int horseType = ((MyHorse) myPet).horseType;
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

    public void setChest(boolean flag) {
        applyVisual(8, flag);
        ((MyHorse) myPet).chest = flag;
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setAge(((MyHorse) myPet).getAge());
            this.setHorseType(((MyHorse) myPet).getHorseType());
            this.setVariant(((MyHorse) myPet).getVariant());
            this.setSaddle(((MyHorse) myPet).hasSaddle());
            this.setChest(((MyHorse) myPet).hasChest());
            this.setArmor(((MyHorse) myPet).getArmor());
        }
    }

    public void setSaddle(boolean flag) {
        applyVisual(4, flag);
        ((MyHorse) myPet).saddle = flag;
    }
}