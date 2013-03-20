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

package de.Keyle.MyPet.entity.types.wither;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_5_R2.World;

@EntitySize(width = 0.9F, height = 4.0F)
public class EntityMyWither extends EntityMyPet
{
    public EntityMyWither(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/wither.png";
    }


    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Integer(300));   // Healthbar
        this.datawatcher.a(17, new Integer(0));     // target EntityID
        this.datawatcher.a(18, new Integer(0));     // N/A
        this.datawatcher.a(19, new Integer(0));     // N/A
        this.datawatcher.a(20, new Integer(0));     // blue (1/0)
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String bb()
    {
        return !playIdleSound() ? "" : "mob.wither.idle";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String bc()
    {
        return "mob.wither.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String bd()
    {
        return "mob.wither.death";
    }

    @Override
    protected void bp()
    {
        this.datawatcher.watch(16, (int) (300. * getHealth() / getMaxHealth())); // update healthbar
    }
}