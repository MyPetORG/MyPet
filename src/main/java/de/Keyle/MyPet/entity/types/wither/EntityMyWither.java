/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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
import net.minecraft.server.v1_7_R4.World;

@EntitySize(width = 0.9F, height = 4.0F)
public class EntityMyWither extends EntityMyPet {
    public EntityMyWither(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.wither.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.wither.hurt";
    }

    protected String getLivingSound() {
        return "mob.wither.idle";
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(17, new Integer(0));  // target entityID
        this.datawatcher.a(18, new Integer(0));  // N/A
        this.datawatcher.a(19, new Integer(0));  // N/A
        this.datawatcher.a(20, new Integer(0));  // blue (1/0)
    }
}