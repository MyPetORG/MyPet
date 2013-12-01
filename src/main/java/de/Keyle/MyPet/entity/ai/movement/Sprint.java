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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_7_R1.EntityLiving;

public class Sprint extends AIGoal {
    private MyPet myPet;
    private final EntityMyPet petEntity;
    private float walkSpeedModifier;
    private AbstractNavigation nav;
    private EntityLiving lastTarget = null;

    public Sprint(EntityMyPet entityMyPet, float walkSpeedModifier) {
        this.petEntity = entityMyPet;
        this.walkSpeedModifier = walkSpeedModifier;
        this.nav = entityMyPet.petNavigation;
        myPet = entityMyPet.getMyPet();
    }

    @Override
    public boolean shouldStart() {
        if (!myPet.getSkills().isSkillActive(de.Keyle.MyPet.skill.skills.implementation.Sprint.class)) {
            return false;
        }
        if (petEntity.getMyPet().getDamage() <= 0) {
            return false;
        }
        EntityLiving targetEntity = this.petEntity.getGoalTarget();

        if (targetEntity == null) {
            return false;
        }
        if (!targetEntity.isAlive()) {
            return false;
        }
        if (lastTarget == targetEntity) {
            return false;
        }
        if (petEntity.getMyPet().getRangedDamage() > 0 && this.petEntity.e(targetEntity) >= 16) {
            return false;
        }
        this.lastTarget = targetEntity;
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (this.petEntity.getOwner() == null) {
            return true;
        } else if (this.petEntity.e(this.lastTarget) < 4) {
            return true;
        } else if (!this.petEntity.canMove()) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        nav.getParameters().addSpeedModifier("Sprint", walkSpeedModifier);
        petEntity.setSprinting(true);
    }

    @Override
    public void finish() {
        nav.getParameters().removeSpeedModifier("Sprint");
        MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
            public void run() {
                petEntity.setSprinting(false);
            }
        }, 25L);
    }
}