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

package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_R1.World;


@EntitySize(width = 0.6F, height = 0.9F)
public class EntityMyCreeper extends EntityMyPet
{
    public EntityMyCreeper(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/creeper.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setPowered(((MyCreeper) myPet).isPowered());
        }
    }

    public void setPowered(boolean powered)
    {
        if (!powered)
        {
            this.datawatcher.watch(17, (byte) 0);
        }
        else
        {
            this.datawatcher.watch(17, (byte) 1);
        }
        ((MyCreeper) myPet).isPowered = powered;
    }

    public boolean isPowered()
    {
        return this.datawatcher.getByte(17) == 1;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) -1)); // N/A
        this.datawatcher.a(17, new Byte((byte) 0));  // powered
    }

    @Override
    protected String aY()
    {
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.creeper.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.creeper.death";
    }
}