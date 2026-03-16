/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_21_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyAllay;
import de.Keyle.MyPet.compat.v1_21_R3.entity.EntityMyFlyingPet;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.4F, height = 0.8F)
public class EntityMyAllay extends EntityMyFlyingPet {

    public EntityMyAllay(Level world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getMyPetDeathSound() {
        return "entity.allay.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.allay.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    @Override
    protected String getLivingSound() {
        if (hasItemInSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) {
            return "entity.allay.ambient_with_item";
        }
        return "entity.allay.ambient_without_item";
    }

    @Override
    protected void playUnequipSound() {
        getBukkitEntity().getWorld().playSound(getBukkitEntity().getLocation(), "entity.allay.item_thrown", 1.0f, 1.0f);
    }

    @Override
    public MyAllay getMyPet() {
        return (MyAllay) myPet;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Allay.CAN_GLIDE) {
            if (!this.onGround() && this.getDeltaMovement().y() < 0.0D) {
                this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
            }
        }

        this.updateVisuals();
    }

    @Override
    protected boolean checkInteractCooldown() {
        boolean val = super.checkInteractCooldown();
        this.interactCooldown = 5;
        return val;
    }
}
