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

package de.Keyle.MyPet.compat.v1_14_R1.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_14_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.SprintImpl;
import net.minecraft.server.v1_14_R1.EntityLiving;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftLivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

@Compat("v1_14_R1")
public class Sprint implements AIGoal {

    private MyPet myPet;
    private final EntityMyPet petEntity;
    private float walkSpeedModifier;
    private AbstractNavigation nav;
    private EntityLiving lastTarget = null;

    public Sprint(EntityMyPet entityMyPet, float walkSpeedModifier) {
        this.petEntity = entityMyPet;
        this.walkSpeedModifier = walkSpeedModifier;
        this.nav = entityMyPet.getPetNavigation();
        myPet = entityMyPet.getMyPet();
    }

    @Override
    public boolean shouldStart() {
        if (!myPet.getSkills().isActive(SprintImpl.class)) {
            return false;
        }
        if (petEntity.getMyPet().getDamage() <= 0) {
            return false;
        }
        if (!this.petEntity.hasTarget()) {
            return false;
        }

        EntityLiving targetEntity = ((CraftLivingEntity) this.petEntity.getTarget()).getHandle();

        if (!targetEntity.isAlive()) {
            return false;
        }
        if (lastTarget == targetEntity) {
            return false;
        }
        if (petEntity.getMyPet().getRangedDamage() > 0 && this.petEntity.h(targetEntity) >= 16) {
            return false;
        }
        this.lastTarget = targetEntity;
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (this.petEntity.getOwner() == null) {
            return true;
        } else if (this.petEntity.h(this.lastTarget) < 16) {
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
        new BukkitRunnable() {
            public void run() {
                petEntity.setSprinting(false);
            }
        }.runTaskLater(MyPetApi.getPlugin(), 25L);
    }
}