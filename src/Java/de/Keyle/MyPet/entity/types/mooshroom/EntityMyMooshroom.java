/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.mooshroom;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_6.World;

@EntitySize(width = 0.9F, height = 1.3F)
public class EntityMyMooshroom extends EntityMyPet
{
    public EntityMyMooshroom(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/redcow.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setBaby(((MyMooshroom) myPet).isBaby());
        }
    }

    public boolean isBaby()
    {
        return this.datawatcher.getInt(12) < 0;
    }

    @SuppressWarnings("boxing")
    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, new Integer(-24000));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyMooshroom) myPet).isBaby = flag;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, new Integer(0));
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.cow.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.cow.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.cow.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.cow.hurt";
    }
}