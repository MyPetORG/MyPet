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

package de.Keyle.MyPet.entity.types.squid;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.BukkitUtil;
import net.minecraft.server.v1_6_R2.World;

@EntitySize(width = 0.95F, height = 0.95F)
public class EntityMySquid extends EntityMyPet {
    public EntityMySquid(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return null;
    }

    @Override
    protected String getHurtSound() {
        return null;
    }

    protected String getLivingSound() {
        return null;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.random.nextBoolean()) {
            BukkitUtil.playParticleEffect(myPet.getLocation().add(0, 0.7, 0), "splash", 0.2F, 0.2F, 0.2F, 0.5F, 10, 20);
        }
    }
}