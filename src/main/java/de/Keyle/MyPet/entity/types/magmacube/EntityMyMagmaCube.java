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

package de.Keyle.MyPet.entity.types.magmacube;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.attack.MyPetAIMeleeAttack;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R1.PathEntity;
import net.minecraft.server.v1_6_R1.World;

@EntitySize(width = 0.6F, height = 0.6F)
public class EntityMyMagmaCube extends EntityMyPet
{
    int jumpDelay;
    PathEntity lastPathEntity = null;

    public EntityMyMagmaCube(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            setSize(((MyMagmaCube) myPet).getSize());
        }
    }

    public int getSize()
    {
        return ((MyMagmaCube) myPet).size;
    }

    public void setSize(int value)
    {
        value = Math.max(1, value);
        this.datawatcher.watch(16, new Byte((byte) value));
        EntitySize es = EntityMyMagmaCube.class.getAnnotation(EntitySize.class);
        if (es != null)
        {
            this.a(es.height() * value, es.width() * value);
        }
        if (petPathfinderSelector != null && petPathfinderSelector.hasGoal("MeleeAttack"))
        {
            petPathfinderSelector.replaceGoal("MeleeAttack", new MyPetAIMeleeAttack(this, 0.1F, 2 + getSize(), 20));
        }
        ((MyMagmaCube) myPet).size = value;
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MyPetAIMeleeAttack(this, 0.1F, 2 + getSize(), 20));
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 1)); //size
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aK()
    {
        return aL();
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aL()
    {
        return "mob.magmacube." + (getSize() > 1 ? "big" : "small");
    }

    /**
     * Method is called when pet moves
     * Is used to create the hopping motion
     */
    public void l_()
    {
        try
        {
            super.l_();

            if (this.onGround && jumpDelay-- <= 0 && lastPathEntity != getNavigation().e())
            {
                getControllerJump().a();
                jumpDelay = (this.random.nextInt(20) + 10);
                lastPathEntity = getNavigation().e();
                makeSound("mob.magmacube." + (getSize() > 1 ? "big" : "small"), aW(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String r()
    {
        return "";
    }
}