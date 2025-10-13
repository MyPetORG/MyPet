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

package de.Keyle.MyPet.compat.v1_21_R5.entity.ai.movement;

import de.Keyle.MyPet.compat.v1_21_R5.entity.EntityMyPet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class MyPetFlyingMoveControl extends MoveControl{

    private final float maxTurn;

    public MyPetFlyingMoveControl(EntityMyPet entitywinged, float maxTurn) {
        super(entitywinged);
        this.maxTurn = maxTurn;
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            float speed = (float) this.mob.getAttributes().getInstance(Attributes.MOVEMENT_SPEED).getValue();

            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedY - this.mob.getY();
            double d2 = this.wantedZ - this.mob.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            double d4 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

            if (d3 < 2.500000277905201E-7D) {
                this.mob.setZza(0.0F);
                return;
            }

            if (Math.abs(d3) > 9.999999747378752E-6D) {
                float f = (float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F;

                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, 90.0F));

                float f4 = (float) (-(Mth.atan2(-d1, d3) * 57.2957763671875D));

                this.mob.setXRot(rotlerp(this.mob.getXRot(), f4, maxTurn));
                this.mob.setSpeed(speed);

                this.mob.setDeltaMovement(this.mob.getDeltaMovement().scale(speed));
            }

            if (d1 != 0.0D) {
                if (this.mob.onGround())
                    speed = 0.47F;
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0D, (double) speed * (d1 / d4) * 0.1D, 0.0D));
            }
        } else {
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }
    }
}
