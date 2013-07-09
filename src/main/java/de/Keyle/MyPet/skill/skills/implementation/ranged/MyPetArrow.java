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

package de.Keyle.MyPet.skill.skills.implementation.ranged;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import net.minecraft.server.v1_6_R2.EntityArrow;
import net.minecraft.server.v1_6_R2.EntityLiving;
import net.minecraft.server.v1_6_R2.NBTTagCompound;
import net.minecraft.server.v1_6_R2.World;

import java.lang.reflect.Field;

public class MyPetArrow extends EntityArrow
{
    private Field inGround = null;

    public MyPetArrow(World world, EntityMyPet entityMyPet, EntityLiving target, float v, int i)
    {
        super(world, entityMyPet, target, v, i);
    }

    @Override
    public void a(NBTTagCompound nbtTagCompound)
    {
    }

    @Override
    public void b(NBTTagCompound nbtTagCompound)
    {
    }

    public void l_()
    {
        try
        {
            super.l_();
            if (inGround == null)
            {
                try
                {
                    inGround = EntityArrow.class.getDeclaredField("inGround");
                    inGround.setAccessible(true);
                }
                catch (NoSuchFieldException e)
                {
                    e.printStackTrace();
                }
            }
            try
            {
                if (inGround != null)
                {
                    if (inGround.getBoolean(this))
                    {
                        die();
                    }
                }
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
