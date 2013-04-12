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

package de.Keyle.MyPet.entity.ai;

import net.minecraft.server.v1_5_R2.PathfinderGoal;

public abstract class EntityAIGoal extends PathfinderGoal
{
    public abstract boolean shouldStart();

    public boolean shouldFinish()
    {
        return shouldStart();
    }

    public void start()
    {
    }

    public void finish()
    {
    }

    public void tick()
    {
    }

    @Override
    public boolean a()
    {
        //MyPetLogger.write("GT:" + this.getClass().getSimpleName() + " shouldStart");
        return shouldStart();
    }

    @Override
    public boolean b()
    {
        //MyPetLogger.write("GT:" + this.getClass().getSimpleName() + " shouldFinish");
        return shouldFinish();
    }

    @Override
    public void c()
    {
        //MyPetLogger.write("GT:" + this.getClass().getSimpleName() + " start");
        start();
    }

    @Override
    public void d()
    {
        //MyPetLogger.write("GT:" + this.getClass().getSimpleName() + " finish");
        finish();
    }

    @Override
    public void e()
    {
        //MyPetLogger.write("GT:" + this.getClass().getSimpleName() + " schedule");
        tick();
    }
}
