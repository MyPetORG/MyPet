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

package de.Keyle.MyPet.compat.v1_8_R1.entity.ai.target;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.compat.v1_8_R1.entity.EntityMyPet;
import net.minecraft.server.v1_8_R1.EntityLiving;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;

public class OwnerHurtTarget extends AIGoal {

    EntityMyPet petEntity;
    EntityLiving target;
    MyPet myPet;

    public OwnerHurtTarget(EntityMyPet petEntity) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
    }

    @Override
    public boolean shouldStart() {
        if (!petEntity.canMove()) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (!this.petEntity.hasTarget() || this.petEntity.getTargetPriority() != TargetPriority.OwnerHurts) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (!petEntity.canMove()) {
            return true;
        }
        if (!petEntity.hasTarget()) {
            return true;
        }

        EntityLiving target = ((CraftLivingEntity) this.petEntity.getTarget()).getHandle();

        if (target.world != petEntity.world) {
            return true;
        } else if (petEntity.h(target) > 400) {
            return true;
        } else if (petEntity.h(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        petEntity.setTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.OwnerHurts);
    }

    @Override
    public void finish() {
        petEntity.forgetTarget();
    }
}