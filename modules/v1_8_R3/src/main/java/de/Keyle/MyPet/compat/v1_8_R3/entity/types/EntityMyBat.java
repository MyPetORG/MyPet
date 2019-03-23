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

package de.Keyle.MyPet.compat.v1_8_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import net.minecraft.server.v1_8_R3.World;

@EntitySize(width = 0.5F, height = 0.45f)
public class EntityMyBat extends EntityMyPet {

    public EntityMyBat(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound() {
        return "mob.bat.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "mob.bat.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "mob.bat.idle";
    }

    public float getSoundSpeed() {
        return super.getSoundSpeed() * 0.95F;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(16, (byte) 0); // hanging
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Bat.CAN_GLIDE) {
            if (!this.onGround && this.motY < 0.0D) {
                this.motY *= 0.6D;
            }
        }
    }

    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
        if (!Configuration.MyPet.Bat.CAN_GLIDE) {
            super.e(f, f1);
        }
    }
}