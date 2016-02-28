/*
 * This file is part of mypet-compat
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-compat is licensed under the GNU Lesser General Public License.
 *
 * mypet-compat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-compat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_7_R4.entity.types;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.types.MyCreeper;
import de.Keyle.MyPet.compat.v1_7_R4.entity.EntityMyPet;
import net.minecraft.server.v1_7_R4.World;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyCreeper extends EntityMyPet {
    public EntityMyCreeper(World world, ActiveMyPet myPet) {
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

    @Override
    public void updateVisuals() {
        if (getMyPet().isPowered()) {
            this.datawatcher.watch(17, (byte) 0);
        } else {
            this.datawatcher.watch(17, (byte) 1);
        }
    }

    public MyCreeper getMyPet() {
        return (MyCreeper) myPet;
    }
}