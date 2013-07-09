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

package de.Keyle.MyPet.entity.types.ghast;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.attack.MyPetAIMeleeAttack;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.World;

@EntitySize(width = 4.F, height = 4.F)
public class EntityMyGhast extends EntityMyPet
{
    public EntityMyGhast(World world, MyPet myPet)
    {
        super(world, myPet);
        this.height = 3.5F;
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MyPetAIMeleeAttack(this, 0.1F, 5.5, 20));
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aN()
    {
        return "mob.ghast.scream";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aO()
    {
        return "mob.ghast.death";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String r()
    {
        return !playIdleSound() ? "" : "mob.ghast.moan";
    }
}