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

package de.Keyle.MyPet.entity.types.bat;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_5_R2.World;


@EntitySize(width = 0.5F, height = 0.9F)
public class EntityMyBat extends EntityMyPet
{
    public EntityMyBat(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/bat.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setHanging(((MyBat) myPet).ishanging());
        }
    }

    public void setHanging(boolean flags)
    {
        int i = this.datawatcher.getByte(16);
        if (flags)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyBat) myPet).hanging = flags;
    }

    public boolean isHanging()
    {
        return ((MyBat) myPet).hanging;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // hanging
    }

    /**
     * Returns the speed of played sounds
     */
    protected float aY()
    {
        return super.aY() * 0.95F;
    }

    @Override
    protected String bb()
    {
        return !playIdleSound() ? "" : "mob.bat.idle";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String bc()
    {
        return "mob.bat.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String bd()
    {
        return "mob.bat.death";
    }

    public void c()
    {
        super.c();

        if (!this.onGround && this.motY < 0.0D)
        {
            this.motY *= 0.6D;
        }
    }

    public void l_()
    {
        super.l_();
        if (!world.getMaterial((int) locX, (int) locY, (int) locZ).isLiquid() && !world.getMaterial((int) locX, (int) (locY + 1.), (int) locZ).isSolid())
        {
            this.locY += 0.65;
        }
    }
}