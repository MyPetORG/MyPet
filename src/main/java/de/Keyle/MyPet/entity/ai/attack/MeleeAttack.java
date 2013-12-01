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

package de.Keyle.MyPet.entity.ai.attack;

import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.IMyPetEquipment;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_7_R1.EntityLiving;

public class MeleeAttack extends AIGoal {
    MyPet myPet;
    EntityMyPet petEntity;
    EntityLiving targetEntity;
    double range;
    float walkSpeedModifier;
    private int ticksUntilNextHitLeft = 0;
    private int ticksUntilNextHit;
    private int timeUntilNextNavigationUpdate;

    public MeleeAttack(EntityMyPet petEntity, float walkSpeedModifier, double range, int ticksUntilNextHit) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.walkSpeedModifier = walkSpeedModifier;
        this.range = range;
        this.ticksUntilNextHit = ticksUntilNextHit;
    }

    @Override
    public boolean shouldStart() {
        if (myPet.getDamage() <= 0) {
            return false;
        }
        EntityLiving targetEntity = this.petEntity.getGoalTarget();
        if (targetEntity == null) {
            return false;
        }
        if (!targetEntity.isAlive()) {
            return false;
        }
        if (petEntity.getMyPet().getRangedDamage() > 0 && this.petEntity.e(targetEntity.locX, targetEntity.boundingBox.b, targetEntity.locZ) >= 16) {
            return false;
        }
        this.targetEntity = targetEntity;
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (this.petEntity.getGoalTarget() == null || !this.targetEntity.isAlive()) {
            return true;
        } else if (this.targetEntity != this.petEntity.getGoalTarget()) {
            return true;
        }
        if (petEntity.getMyPet().getRangedDamage() > 0 && this.petEntity.e(targetEntity.locX, targetEntity.boundingBox.b, targetEntity.locZ) >= 16) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.petEntity.petNavigation.getParameters().addSpeedModifier("MeleeAttack", walkSpeedModifier);
        this.petEntity.petNavigation.navigateTo(this.targetEntity);
        this.timeUntilNextNavigationUpdate = 0;
    }

    @Override
    public void finish() {
        this.petEntity.petNavigation.getParameters().removeSpeedModifier("MeleeAttack");
        this.targetEntity = null;
        this.petEntity.petNavigation.stop();
    }

    @Override
    public void tick() {
        this.petEntity.getControllerLook().a(targetEntity, 30.0F, 30.0F);
        if (--this.timeUntilNextNavigationUpdate <= 0) {
            this.timeUntilNextNavigationUpdate = (4 + this.petEntity.getRandom().nextInt(7));
            this.petEntity.petNavigation.navigateTo(targetEntity);
        }
        if (Math.sqrt(this.petEntity.e(targetEntity.locX, targetEntity.boundingBox.b, targetEntity.locZ)) - (targetEntity.length * (2. / 3.)) <= this.range && this.ticksUntilNextHitLeft-- <= 0) {
            this.ticksUntilNextHitLeft = ticksUntilNextHit;
            if (this.petEntity instanceof IMyPetEquipment) {
                if (((IMyPetEquipment) this.petEntity).getEquipment(EquipmentSlot.Weapon) != null) {
                    this.petEntity.aR(); // -> swingItem()
                }
            }
            this.petEntity.attack(targetEntity);
        }
    }
}