/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_8_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_8_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_8_R1.entity.ai.attack.MeleeAttack;
import net.minecraft.server.v1_8_R1.World;

@EntitySize(width = 8.F, height = 8.F)
public class EntityMyEnderDragon extends EntityMyPet {
    public EntityMyEnderDragon(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.enderdragon.growl";
    }

    @Override
    protected String getHurtSound() {
        return "mob.enderdragon.growl";
    }

    protected String getLivingSound() {
        return "mob.enderdragon.hit";
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 8.5, 20));
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }
    }


    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
    }
}