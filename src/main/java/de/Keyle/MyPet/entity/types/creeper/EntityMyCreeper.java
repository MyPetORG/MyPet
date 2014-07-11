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

package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_7_R4.World;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyCreeper extends EntityMyPet {
    public EntityMyCreeper(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.creeper.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.creeper.say";
    }

    @Override
    protected String getLivingSound() {
        return null;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(16, new Byte((byte) -1)); // fuse
        this.datawatcher.a(17, new Byte((byte) 0));  // powered
    }

    public void setPowered(boolean powered) {
        if (!powered) {
            this.datawatcher.watch(17, (byte) 0);
        } else {
            this.datawatcher.watch(17, (byte) 1);
        }
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setPowered(getMyPet().isPowered());
        }
    }

    public MyCreeper getMyPet() {
        return (MyCreeper) myPet;
    }
}