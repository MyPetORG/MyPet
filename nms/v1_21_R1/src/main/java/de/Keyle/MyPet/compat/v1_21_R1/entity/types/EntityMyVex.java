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

package de.Keyle.MyPet.compat.v1_21_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVex;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyFlyingPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.4F, height = 0.8F)
public class EntityMyVex extends EntityMyFlyingPet {

    protected static final EntityDataAccessor<Byte> CHARGING_WATCHER = SynchedEntityData.defineId(EntityMyVex.class, EntityDataSerializers.BYTE);

    protected boolean isAggressive = false;

    public EntityMyVex(Level world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getMyPetDeathSound() {
        return "entity.vex.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.vex.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    @Override
    protected String getLivingSound() {
        return "entity.vex.ambient";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING_WATCHER, (byte) 0);
    }

    @Override
    public void updateVisuals() {
        getEntityData().set(CHARGING_WATCHER, (byte) (getMyPet().isGlowing() || isAggressive ? 1 : 0));

        super.updateVisuals();
    }

    @Override
    public MyVex getMyPet() {
        return (MyVex) myPet;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Vex.CAN_GLIDE) {
            if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
                this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
            }
        }
    }

    @Override
    protected void doMyPetTick() {
        super.doMyPetTick();
        BehaviorImpl skill = getMyPet().getSkills().get(BehaviorImpl.class);
        Behavior.BehaviorMode behavior = skill.getBehavior();
        if (behavior == Behavior.BehaviorMode.Aggressive) {
            if (!isAggressive) {
                isAggressive = true;
                this.updateVisuals();
            }
        } else {
            if (isAggressive) {
                isAggressive = false;
                this.updateVisuals();
            }
        }
    }
}
