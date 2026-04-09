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

package de.Keyle.MyPet.compat.v1_21_R2.entity;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.target.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

@EntitySize(width = 0.5F, height = 0.3f)
public abstract class EntityMyFlyingPet extends EntityMyPet {

    protected MyPetFlyingMovementGoal flyingMovementGoal;

    public EntityMyFlyingPet(Level world, MyPet myPet) {
        super(world, myPet);
        // Forwarding MoveControl: NMS PathNavigation.tick() calls setWantedPosition on
        // this.moveControl each tick of an active path. We relay that call to the Paper
        // goal (which is the real movement pipeline when usesPaperMovement() == true)
        // while keeping the NMS fields in sync for any code that reads them.
        // flyingMovementGoal was populated by setPathfinder() during super(...) above,
        // so the anonymous class below can read it at call time.
        this.moveControl = new FlyingMoveControl(this, (int) maxTurn, true) {
            @Override
            public void setWantedPosition(double x, double y, double z, double speedModifier) {
                super.setWantedPosition(x, y, z, speedModifier);
                flyingMovementGoal.setWantedPosition(x, y, z, speedModifier);
            }
        };
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0f);
    }

    @Override
    public boolean usesPaperMovement() {
        return true;
    }

    @Override
    protected PathNavigation setSpecialNav() {
        return new FlyingPathNavigation(this, this.level());
    }

    //Disengage FallDamage
    @Override
    protected void checkFallDamage(double d0, boolean flag, BlockState iblockdata, BlockPos blockposition) {
    }

    @Override
    public void travel(Vec3 vec3d) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater() || this.isInLava() || hasRider || this.isVehicle()) {
                super.travel(vec3d);
                return;
            } else {
                float f = 0.91F;

                if (this.onGround()) {
                    f = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
                }

                float f1 = 0.16277137F / (f * f * f);

                f = 0.91F;
                if (this.onGround()) {
                    f = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
                }

                float flySpeed = this.onGround() ? 0.1F * f1
                        : (float) this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                this.moveRelative(flySpeed, vec3d);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(f));
            }
        }

        this.calculateEntityAnimation(false);
    }

    @Override
    public void setPathfinder() {
        super.setPathfinder();

        var mobGoals = org.bukkit.Bukkit.getMobGoals();
        MyPetBukkitEntity bukkit = getBukkitEntity();
        org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) bukkit;

        // Create the Paper flying movement goal here (not in the constructor).
        // setPathfinder() is invoked from EntityMyPet.<init> via super(...) before
        // the EntityMyFlyingPet constructor body runs, so the constructor cannot
        // create this instance ahead of time — `this.flyingMovementGoal` would
        // still be null. Creating it here and assigning the field ensures the
        // forwarding FlyingMoveControl (installed after super() returns) can
        // read it at call time.
        this.flyingMovementGoal = new MyPetFlyingMovementGoal(bukkit, this.maxTurn);
        mobGoals.addGoal(mob, -1, flyingMovementGoal);

        mobGoals.addGoal(mob, 5, new de.Keyle.MyPet.entity.ai.attack.PetMeleeAttackGoal(bukkit, 0.7F, this.getBbWidth() + 1.3, 20));
        mobGoals.addGoal(mob, 7, new PetRandomFlyGoal(bukkit));

        // Register flying Control goal via Paper MobGoals (higher speed than ground pets)
        PetControlGoal controlGoal = new PetControlGoal(bukkit, 0.8F);
        mobGoals.addGoal(mob, 2, controlGoal);
        PetControlTargetGoal controlTargetGoal = new PetControlTargetGoal(bukkit, this.getBbWidth() + 2.5F);
        controlTargetGoal.setControlGoal(controlGoal);
        mobGoals.addGoal(mob, 12, controlTargetGoal);
    }
}
