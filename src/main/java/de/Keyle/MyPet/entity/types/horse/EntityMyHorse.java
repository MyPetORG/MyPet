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
import net.minecraft.server.v1_6_R1.*;
import org.bukkit.Material;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyHorse extends EntityMyPet
{
    public static int GROW_UP_ITEM = Material.BREAD.getId();

    int bP = 0;

    public EntityMyHorse(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setAge(((MyHorse) myPet).getAge());
            this.setHorseType(((MyHorse) myPet).getHorseType());
            this.setVariant(((MyHorse) myPet).getVariant());
            this.setSaddle(((MyHorse) myPet).hasSaddle());
            this.setChest(((MyHorse) myPet).hasChest());
        }
    }

    public void setChest(boolean flag)
    {
        applySaddleChest();
        ((MyHorse) myPet).chest = flag;
    }

    public boolean hasChest()
    {
        return ((MyHorse) myPet).chest;
    }

    public void setSaddle(boolean flag)
    {
        applySaddleChest();
        ((MyHorse) myPet).saddle = flag;
    }

    public boolean hasSaddle()
    {
        return ((MyHorse) myPet).saddle;
    }

    private void applySaddleChest()
    {
        int saddleChest = 0;
        if (hasChest())
        {
            saddleChest += 8;
        }
        if (hasSaddle())
        {
            saddleChest += 4;
        }
        this.datawatcher.watch(16, Integer.valueOf(saddleChest));
    }

    public void setHorseType(byte horseType)
    {
        this.datawatcher.watch(19, Byte.valueOf(horseType));
        ((MyHorse) myPet).horseType = horseType;
    }

    public void setArmor(int value)
    {
        this.datawatcher.watch(22, Integer.valueOf(value));
        ((MyHorse) myPet).armor = value;
    }

    public int getArmor()
    {
        return ((MyHorse) myPet).armor;
    }

    public void setVariant(int variant)
    {
        this.datawatcher.watch(20, Integer.valueOf(variant));
        ((MyHorse) myPet).variant = variant;
    }

    public int getVariant()
    {
        return ((MyHorse) myPet).variant;
    }

    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, Integer.valueOf(-24000));
            ((MyHorse) myPet).age = -24000;
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
            ((MyHorse) myPet).age = 0;
        }
    }

    public boolean isBaby()
    {
        return ((MyHorse) myPet).age < 0;
    }


    public void setAge(int value)
    {
        value = Math.min(0, (Math.max(-24000, value)));
        value = value - (value % 1000);
        ((MyHorse) myPet).age = value;
        this.datawatcher.watch(12, new Integer(value));
    }

    public int getAge()
    {
        return ((MyHorse) myPet).age;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, Integer.valueOf(0));     // Age
        this.datawatcher.a(16, Integer.valueOf(0));     // Saddle & Chest
        this.datawatcher.a(19, Byte.valueOf((byte) 0)); // Horse type
        this.datawatcher.a(20, Integer.valueOf(0));     // Variant
        this.datawatcher.a(21, String.valueOf(""));     // N/A
        this.datawatcher.a(22, Integer.valueOf(0));     // Armor
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a(EntityHuman entityhuman)
    {
        try
        {
            if (super.a(entityhuman))
            {
                return true;
            }
            ItemStack itemStack = entityhuman.inventory.getItemInHand();

            if (itemStack != null)
            {
                if (itemStack.id == 329 && getOwner().getPlayer().isSneaking() && !hasSaddle() && getAge() >= 0 && canEquip())
                {
                    setSaddle(true);
                    if (!entityhuman.abilities.canInstantlyBuild)
                    {
                        if (--itemStack.count <= 0)
                        {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    return true;
                }
                else if (itemStack.id == 54 && getOwner().getPlayer().isSneaking() && !hasChest() && getAge() >= 0 && canEquip())
                {
                    setChest(true);
                    if (!entityhuman.abilities.canInstantlyBuild)
                    {
                        if (--itemStack.count <= 0)
                        {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    return true;
                }
                else if (itemStack.id >= 417 && itemStack.id <= 419 && getOwner().getPlayer().isSneaking() && canEquip())
                {
                    if (getArmor() > 0 && !entityhuman.abilities.canInstantlyBuild)
                    {
                        EntityItem entityitem = this.a(new ItemStack(416 + getArmor(), 1, 0), 1F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    setArmor(itemStack.id - 416);
                    if (!entityhuman.abilities.canInstantlyBuild)
                    {
                        if (--itemStack.count <= 0)
                        {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    return true;
                }
                else if (itemStack.id == Item.SHEARS.id && getOwner().getPlayer().isSneaking() && canEquip())
                {
                    if (getArmor() > 0 && !entityhuman.abilities.canInstantlyBuild)
                    {
                        setArmor(0);
                        EntityItem entityitem = this.a(new ItemStack(416 + getArmor(), 1, 0), 1F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    if (hasChest() && !entityhuman.abilities.canInstantlyBuild)
                    {
                        setChest(false);
                        EntityItem entityitem = this.a(new ItemStack(Block.CHEST), 1F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    if (hasSaddle() && !entityhuman.abilities.canInstantlyBuild)
                    {
                        setSaddle(false);
                        EntityItem entityitem = this.a(new ItemStack(Item.SADDLE), 1F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    return true;
                }
                else if (itemStack.id == GROW_UP_ITEM && getOwner().getPlayer().isSneaking() && canUseItem())
                {
                    if (isBaby())
                    {
                        if (!entityhuman.abilities.canInstantlyBuild)
                        {
                            if (--itemStack.count <= 0)
                            {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        this.setAge(getAge() + 1000);
                        return true;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void a(int i, int j, int k, int l)
    {
        StepSound localStepSound = Block.byId[l].stepSound;
        if (this.world.getTypeId(i, j + 1, k) == Block.SNOW.id)
        {
            localStepSound = Block.SNOW.stepSound;
        }
        if (!Block.byId[l].material.isLiquid())
        {
            int horseType = ((MyHorse) myPet).horseType;
            if ((this.passenger != null) && (horseType != 1) && (horseType != 2))
            {
                this.bP += 1;
                if ((this.bP > 5) && (this.bP % 3 == 0))
                {
                    makeSound("mob.horse.gallop", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
                    if ((horseType == 0) && (this.random.nextInt(10) == 0))
                    {
                        makeSound("mob.horse.breathe", localStepSound.getVolume1() * 0.6F, localStepSound.getVolume2());
                    }
                }
                else if (this.bP <= 5)
                {
                    makeSound("mob.horse.wood", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
                }
            }
            else if (localStepSound == Block.h)
            {
                makeSound("mob.horse.soft", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
            }
            else
            {
                makeSound("mob.horse.wood", localStepSound.getVolume1() * 0.15F, localStepSound.getVolume2());
            }
        }
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aK()
    {
        int horseType = ((MyHorse) myPet).horseType;
        if (horseType == 3)
        {
            return "mob.horse.zombie.hit";
        }
        if (horseType == 4)
        {
            return "mob.horse.skeleton.hit";
        }
        if ((horseType == 1) || (horseType == 2))
        {
            return "mob.horse.donkey.hit";
        }
        return "mob.horse.hit";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aL()
    {
        int horseType = ((MyHorse) myPet).horseType;
        if (horseType == 3)
        {
            return "mob.horse.zombie.death";
        }
        if (horseType == 4)
        {
            return "mob.horse.skeleton.death";
        }
        if ((horseType == 1) || (horseType == 2))
        {
            return "mob.horse.donkey.death";
        }
        return "mob.horse.death";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String r()
    {
        if (playIdleSound())
        {
            int horseType = ((MyHorse) myPet).horseType;
            if (horseType == 3)
            {
                return "mob.horse.zombie.idle";
            }
            if (horseType == 4)
            {
                return "mob.horse.skeleton.idle";
            }
            if ((horseType == 1) || (horseType == 2))
            {
                return "mob.horse.donkey.idle";
            }
            return "mob.horse.idle";
        }
        return "";
    }
}