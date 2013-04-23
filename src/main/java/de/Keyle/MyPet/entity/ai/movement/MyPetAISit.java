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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.entity.ai.MyPetAIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;

public class MyPetAISit extends MyPetAIGoal
{
    private EntityMyPet entityMyPet;
    private boolean sitting = false;

    public MyPetAISit(EntityMyPet entityMyPet)
    {
        this.entityMyPet = entityMyPet;
    }

    public boolean shouldStart()
    {
        if (!(this.entityMyPet instanceof EntityMyOcelot) && !(this.entityMyPet instanceof EntityMyWolf))
        {
            return false;
        }
        else if (this.entityMyPet.H())
        {
            return false;
        }
        else if (!this.entityMyPet.onGround)
        {
            return false;
        }
        return this.sitting;
    }

    @Override
    public void start()
    {
        this.entityMyPet.petNavigation.stop();
        if (this.entityMyPet instanceof EntityMyOcelot)
        {
            ((EntityMyOcelot) this.entityMyPet).applySitting(true);
        }
        if (this.entityMyPet instanceof EntityMyWolf)
        {
            ((EntityMyWolf) this.entityMyPet).applySitting(true);
        }
        entityMyPet.setGoalTarget(null);
    }

    @Override
    public void finish()
    {

        if (this.entityMyPet instanceof EntityMyOcelot)
        {
            ((EntityMyOcelot) this.entityMyPet).applySitting(false);
        }
        if (this.entityMyPet instanceof EntityMyWolf)
        {
            ((EntityMyWolf) this.entityMyPet).applySitting(false);
        }
    }

    public void setSitting(boolean sitting)
    {
        this.sitting = sitting;
    }

    public boolean isSitting()
    {
        return this.sitting;
    }

    public void toogleSitting()
    {
        this.sitting = !this.sitting;
    }
}