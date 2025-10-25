/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_12_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyElderGuardian;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import net.minecraft.server.v1_12_R1.World;

@EntitySize(width = 0.7F, height = 0.85F)
public class EntityMyElderGuardian extends EntityMyPet {

    public EntityMyElderGuardian(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.elder_guardian.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.elder_guardian.hurt";
    }

    protected String getLivingSound() {
        return "entity.elder_guardian.ambient";
    }

    public MyElderGuardian getMyPet() {
        return (MyElderGuardian) myPet;
    }
}