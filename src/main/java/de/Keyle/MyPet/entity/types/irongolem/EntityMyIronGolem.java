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

package de.Keyle.MyPet.entity.types.irongolem;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.Entity;
import net.minecraft.server.v1_6_R2.World;

@EntitySize(width = 1.4F, height = 2.9F)
public class EntityMyIronGolem extends EntityMyPet
{
    public static boolean CAN_THROW_UP = true;

    public EntityMyIronGolem(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public boolean attack(Entity entity)
    {
        boolean flag = false;
        try
        {
            this.world.broadcastEntityEffect(this, (byte) 4);
            flag = super.attack(entity);
            if (CAN_THROW_UP && flag)
            {
                entity.motY += 0.4000000059604645D;
                this.world.makeSound(this, "mob.irongolem.throw", 1.0F, 1.0F);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return flag;
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        petPathfinderSelector.removeGoal("Float");
    }

    protected void setPlayerCreated(boolean flag)
    {
        byte b0 = this.datawatcher.getByte(16);

        if (flag)
        {
            this.datawatcher.watch(16, (byte) (b0 | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & 0xFFFFFFFE));
        }
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // flower???
    }

    @Override
    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.irongolem.walk", 1.0F, 1.0F);
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aN()
    {
        return "mob.irongolem.hit";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aO()
    {
        return "mob.irongolem.death";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String r()
    {
        return "";
    }
}