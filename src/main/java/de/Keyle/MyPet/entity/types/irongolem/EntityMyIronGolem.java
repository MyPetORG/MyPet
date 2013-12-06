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

package de.Keyle.MyPet.entity.types.irongolem;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.logger.DebugLogger;
import net.minecraft.server.v1_7_R1.Entity;
import net.minecraft.server.v1_7_R1.World;

@EntitySize(width = 1.4F, height = 2.9F)
public class EntityMyIronGolem extends EntityMyPet {
    public EntityMyIronGolem(World world, MyPet myPet) {
        super(world, myPet);
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            this.world.broadcastEntityEffect(this, (byte) 4);
            flag = super.attack(entity);
            if (MyIronGolem.CAN_THROW_UP && flag) {
                entity.motY += 0.4000000059604645D;
                this.world.makeSound(this, "mob.irongolem.throw", 1.0F, 1.0F);
            }
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        return "mob.irongolem.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.irongolem.hit";
    }

    protected String getLivingSound() {
        return null;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(16, new Byte((byte) 0)); // flower???
    }

    @Override
    public void playStepSound() {
        makeSound("mob.irongolem.walk", 1.0F, 1.0F);
    }
}