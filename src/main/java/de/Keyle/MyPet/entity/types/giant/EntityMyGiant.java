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

package de.Keyle.MyPet.entity.types.giant;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_7_R4.World;

@EntitySize(width = 5.5f, height = 5.5F)
public class EntityMyGiant extends EntityMyPet {
    public EntityMyGiant(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.zombie.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.zombie.hurt";
    }

    protected String getLivingSound() {
        return "mob.zombie.say";
    }

    public void playStepSound() {
        makeSound("mob.zombie.step", 0.15F, 1.0F);
    }

    public void setMyPet(MyPet myPet) {

        if (myPet != null) {
            super.setMyPet(myPet);

            this.height *= 6.0F;
        }
    }

    public void setPathfinder() {
        super.setPathfinder();
        if (myPet.getDamage() > 0) {
            petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 8, 20));
        }
    }
}