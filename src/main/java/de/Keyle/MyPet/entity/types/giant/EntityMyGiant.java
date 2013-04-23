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

package de.Keyle.MyPet.entity.types.giant;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.MyPetAIMeleeAttack;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_5_R2.World;

@EntitySize(width = 5.5f, height = 5.5F)
public class EntityMyGiant extends EntityMyPet
{
    public EntityMyGiant(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/zombie.png";
    }

    public void setMyPet(MyPet myPet)
    {

        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.height *= 6.0F;
        }
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.replaceGoal("MeleeAttack", new MyPetAIMeleeAttack(this, 0.1F, 8, 20));
        }
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.zombie.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String bb()
    {
        return !playIdleSound() ? "" : "mob.zombie.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String bc()
    {
        return "mob.zombie.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String bd()
    {
        return "mob.zombie.death";
    }
}