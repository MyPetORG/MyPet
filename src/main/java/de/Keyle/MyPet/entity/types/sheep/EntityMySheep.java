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

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.MyPetAIEatGrass;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.*;
import org.bukkit.DyeColor;
import org.bukkit.Material;

@EntitySize(width = 0.9F, height = 1.3F)
public class EntityMySheep extends EntityMyPet
{
    public static boolean CAN_BE_SHEARED = true;
    public static boolean CAN_REGROW_WOOL = true;
    public static int GROW_UP_ITEM = Material.POTION.getId();

    public EntityMySheep(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        petPathfinderSelector.addGoal("EatGrass", new MyPetAIEatGrass(this, 0.02));
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setColor(((MySheep) myPet).getColor());
            this.setSheared(((MySheep) myPet).isSheared());
            this.setBaby(((MySheep) myPet).isBaby());
        }
    }

    public DyeColor getColor()
    {
        return ((MySheep) myPet).color;
    }

    public void setColor(DyeColor color)
    {
        setColor(color.getWoolData());
    }

    public void setColor(byte color)
    {
        this.datawatcher.watch(16, color);
        ((MySheep) myPet).color = DyeColor.getByWoolData(color);
    }

    public boolean isSheared()
    {
        return ((MySheep) myPet).isSheared;
    }

    public void setSheared(boolean flag)
    {

        byte b0 = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (b0 | 16));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & -17));
        }
        ((MySheep) myPet).isSheared = flag;
    }

    public boolean isBaby()
    {
        return this.datawatcher.getInt(12) < 0;
    }

    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MySheep) myPet).isBaby = flag;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // color/sheared
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

            if (getOwner().equals(entityhuman) && itemStack != null && canUseItem())
            {
                if (itemStack.id == 351 && itemStack.getData() != ((MySheep) myPet).getColor().getDyeData())
                {
                    if (itemStack.getData() <= 15)
                    {
                        setColor(DyeColor.getByDyeData((byte) itemStack.getData()));
                        if (!entityhuman.abilities.canInstantlyBuild)
                        {
                            if (--itemStack.count <= 0)
                            {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        return true;
                    }
                }
                else if (itemStack.id == Item.SHEARS.id && CAN_BE_SHEARED && !((MySheep) myPet).isSheared())
                {
                    if (!this.world.isStatic)
                    {
                        ((MySheep) myPet).setSheared(true);
                        int i = 1 + this.random.nextInt(3);

                        for (int j = 0 ; j < i ; ++j)
                        {
                            EntityItem entityitem = this.a(new ItemStack(Block.WOOL.id, 1, ((MySheep) myPet).getColor().getDyeData()), 1.0F);

                            entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                            entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                            entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        }
                        makeSound("mob.sheep.shear", 1.0F, 1.0F);
                    }
                    itemStack.damage(1, entityhuman);
                    return true;
                }
                else if (itemStack.id == GROW_UP_ITEM && getOwner().getPlayer().isSneaking())
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
                        this.setBaby(false);
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

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.sheep.step", 0.15F, 1.0F);
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aN()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aO()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String r()
    {
        return !playIdleSound() ? "" : "mob.sheep.say";
    }
}