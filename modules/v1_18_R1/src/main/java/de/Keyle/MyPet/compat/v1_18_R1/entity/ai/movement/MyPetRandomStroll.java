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

package de.Keyle.MyPet.compat.v1_18_R1.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_18_R1.entity.EntityMyPet;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;

@Compat("v1_18_R1")
public class MyPetRandomStroll implements AIGoal, de.Keyle.MyPet.api.entity.ai.movement.MyPetRandomStroll {
    private final EntityMyPet petEntity;
    protected AbstractNavigation nav;
    protected Location moveTo = null;
    protected int timeToMove = 0;
    protected final int startDistance;
    protected Control controlPathfinderGoal;
    protected final Player owner;
    protected float strollChance = 0.02F;

    public MyPetRandomStroll(EntityMyPet entityMyPet, int startDistance) {
        this.petEntity = entityMyPet;
        this.nav = entityMyPet.getPetNavigation();
        this.startDistance = startDistance * startDistance;
        this.owner = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
    }

    @Override
    public boolean shouldStart() {
        if (this.petEntity.getRandom().nextFloat() >= this.strollChance) {
            return false;
        }

        if (controlPathfinderGoal == null) {
            if (petEntity.getPathfinder().hasGoal("Control")) {
                controlPathfinderGoal = (Control) petEntity.getPathfinder().getGoal("Control");
            }
        }
        if (!this.petEntity.canMove()) {
            return false;
        } else if (this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead()) {
            return false;
        } else if (this.petEntity.getOwner() == null) {
            return false;
        } else if (((FollowOwner) petEntity.getPathfinder().getGoal("FollowOwner")).setPathTimer>0) {
            return false;
        } else if (this.petEntity.distanceToSqr(owner) < this.startDistance) {
            return controlPathfinderGoal == null || controlPathfinderGoal.moveTo == null;
        } else return false;
    }

    @Override
    public boolean shouldFinish() {
        if (controlPathfinderGoal.moveTo != null) {
            return true;
        } else if (this.petEntity.getOwner() == null) {
            return true;
        } else if (this.petEntity.distanceToSqr(owner) > this.startDistance) {
            return true;
        } else if (!this.petEntity.canMove()) {
            return true;
        } else if (moveTo == null){
            return true;
        } else if (MyPetApi.getPlatformHelper().distance(petEntity.getMyPet().getLocation().get(), moveTo) < 2) {
            return true;
        }else if (timeToMove <= 0) {
            return true;
        }else return this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead();
    }

    @Override
    public void start() {
        Vec3 vec = getPosition();
        if(vec == null)
            return;
        applySpeed();
        moveTo = new Location(this.petEntity.getBukkitEntity().getWorld(), vec.x, vec.y, vec.z);
        timeToMove = (int) MyPetApi.getPlatformHelper().distance(petEntity.getMyPet().getLocation().get(), moveTo) / 3;
        timeToMove = timeToMove < 3 ? 3 : timeToMove;

        if(!nav.navigateTo(moveTo)) {
            this.moveTo = null;
        }
    }

    @Override
    public void finish() {
        nav.getParameters().removeSpeedModifier("RandomStroll");
        this.nav.stop();
    }

    protected Vec3 getPosition() {
        return LandRandomPos.getPos(this.petEntity, 5, 0);
    }

    protected void applySpeed() {
        double walkSpeed = owner.getAbilities().walkingSpeed-0.15d;
        nav.getParameters().addSpeedModifier("RandomStroll", walkSpeed);
    }

    public void schedule() {
        timeToMove--;
    }
}
