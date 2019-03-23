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

package de.Keyle.MyPet.compat.v1_10_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_10_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_10_R1.entity.ai.attack.MeleeAttack;
import net.minecraft.server.v1_10_R1.World;

@EntitySize(width = 4.F, height = 4.F)
public class EntityMyGhast extends EntityMyPet {

    public EntityMyGhast(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.ghast.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.ghast.hurt";
    }

    protected String getLivingSound() {
        return "entity.ghast.ambient";
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 5.5, 20));
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Ghast.CAN_GLIDE) {
            if (!this.onGround && this.motY < 0.0D) {
                this.motY *= 0.6D;
            }
        }
    }

    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
        if (!Configuration.MyPet.Ghast.CAN_GLIDE) {
            super.e(f, f1);
        }
    }
}