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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPetPart;
import de.Keyle.MyPet.compat.v1_12_R1.entity.ai.attack.MeleeAttack;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.World;

@EntitySize(width = 4.F, height = 4.F)
public class EntityMyEnderDragon extends EntityMyPet {

    public EntityMyPetPart[] children = new EntityMyPetPart[8];

    public EntityMyEnderDragon(World world, MyPet myPet) {
        super(world, myPet);

        for (int i = 0; i < 8; i++) {
            this.children[i] = new EntityMyPetPart(this);
        }
    }

    @Override
    protected String getDeathSound() {
        return "entity.enderdragon.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.enderdragon.hurt";
    }

    protected String getLivingSound() {
        return "entity.enderdragon.ambient";
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 8.5, 20));
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.EnderDragon.CAN_GLIDE) {
            if (!this.onGround && this.motY < 0.0D) {
                this.motY *= 0.6D;
            }
        }
    }


    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
        if (!Configuration.MyPet.EnderDragon.CAN_GLIDE) {
            super.e(f, f1);
        }
    }

    public Entity[] bb() {
        return this.children;
    }
}