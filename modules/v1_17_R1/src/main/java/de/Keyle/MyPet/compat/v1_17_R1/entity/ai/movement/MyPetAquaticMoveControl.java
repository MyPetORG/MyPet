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

package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.movement;

import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class MyPetAquaticMoveControl extends MoveControl{
	private final EntityMyPet fish;

	public MyPetAquaticMoveControl(EntityMyPet entityfish) {
        super(entityfish);
        this.fish = entityfish;
    }

    @Override
    public void tick() {
    	
    	
        if (this.operation == MoveControl.Operation.MOVE_TO && !this.fish.getNavigation().isDone()) {
            float f = (float) (this.speedModifier * this.fish.getAttributes().getInstance(Attributes.MOVEMENT_SPEED).getValue());
            
            double d0 = this.wantedX - this.fish.getX();
            double d1 = this.wantedY - this.fish.getY();
            double d2 = this.wantedZ - this.fish.getZ();
            
            this.fish.setSpeed(Mth.lerp(0.125F, this.fish.getSpeed(), f));
            
            if(this.fish.isInWaterOrBubble()) {
                float f3 = Mth.sin(this.mob.getXRot() * 0.017453292F);
                this.fish.yya = -f3 * f;
                this.fish.setDeltaMovement(this.fish.getDeltaMovement().multiply(f, 1, f));
            }
            
            if (d0 != 0.0D || d2 != 0.0D) {
                float f1 = (float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F;

                this.fish.setYRot(this.rotlerp(this.fish.getYRot(), f1, 90.0F));
                this.fish.yBodyRot = this.fish.getYRot();
            }
            
            if (d1 != 0.0D) {
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(0.0D, (double) this.fish.getSpeed() * (d1 / d3) * 0.1D, 0.0D));
            }
        } else {
        	if(this.fish.isInWaterOrBubble()) {
        		this.fish.setDeltaMovement(this.fish.getDeltaMovement().x,-0.0045,this.fish.getDeltaMovement().z);
        	}
            this.fish.setSpeed(0.0F);
        }
    }
}
