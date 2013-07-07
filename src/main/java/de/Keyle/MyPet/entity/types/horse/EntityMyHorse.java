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
import net.minecraft.server.v1_6_R1.Block;
import net.minecraft.server.v1_6_R1.StepSound;
import net.minecraft.server.v1_6_R1.World;
import org.bukkit.Material;

@EntitySize(width = 0.3F, height = 0.7F)
public class EntityMyHorse extends EntityMyPet
{
    public static int GROW_UP_ITEM = Material.POTION.getId();

    int bP = 0;

    public EntityMyHorse(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void setHorseType(byte horseType)
    {
        this.datawatcher.watch(19, Byte.valueOf(horseType));
        ((MyHorse) myPet).horseType = horseType;
    }

    public void setVariant(int variant)
    {
        this.datawatcher.watch(20, Integer.valueOf(variant));
        ((MyHorse) myPet).variant = variant;
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
        ((MyHorse) myPet).isBaby = flag;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, Integer.valueOf(0));    // Age
        this.datawatcher.a(16, Integer.valueOf(0));    //
        this.datawatcher.a(19, Byte.valueOf((byte) 0)); // Horse type
        this.datawatcher.a(20, Integer.valueOf(0));    // variant
        this.datawatcher.a(21, String.valueOf(""));    //
        this.datawatcher.a(22, Integer.valueOf(0));    //
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