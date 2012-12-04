/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalControl;
import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalSit;
import de.Keyle.MyPet.entity.pathfinder.target.*;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;

public class EntityMyOcelot extends EntityMyPet
{
    private PathfinderGoalSit sitPathfinder;

    public EntityMyOcelot(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/ozelot.png";
        this.a(0.6F, 0.8F);

        if (this.sitPathfinder == null)
        {
            this.sitPathfinder = new PathfinderGoalSit(this);
        }
        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, this.sitPathfinder);
        this.goalSelector.a(5, controlPathfinder);
        this.goalSelector.a(7, new PathfinderGoalFollowOwner(this, this.walkSpeed, 10.0F, 5.0F, controlPathfinder));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));

        if (MyPet.getStartDamage(MyOcelot.class) > 0)
        {
            this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, this.walkSpeed + 0.1F, true));
            this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
            this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
            this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
            this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinder, 1));
            this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 15));
            this.targetSelector.a(6, new PathfinderGoalFarmTarget(myPet, 15));
        }
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setSitting(((MyOcelot) myPet).isSitting());
            this.setBaby(((MyOcelot) myPet).isBaby());
            this.setCatType(((MyOcelot) myPet).getCatType());
        }
    }

    public boolean canMove()
    {
        return !isSitting();
    }

    public void setSitting(boolean flag)
    {
        if (this.sitPathfinder == null)
        {
            this.sitPathfinder = new PathfinderGoalSit(this);
        }
        this.sitPathfinder.setSitting(flag);
    }

    public boolean isSitting()
    {
        return this.sitPathfinder.isSitting();
    }

    public void applySitting(boolean flag)
    {
        int i = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyOcelot) myPet).isSitting = flag;
    }

    public int getCatType()
    {
        return this.datawatcher.getByte(18);
    }

    public void setCatType(int value)
    {
        this.datawatcher.watch(18, (byte) value);
        ((MyOcelot) myPet).catType = value;
    }

    public boolean isBaby()
    {
        return this.datawatcher.getInt(12) < 0;
    }

    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, -1);
        }
        else
        {
            this.datawatcher.watch(12, 0);
        }
        ((MyOcelot) myPet).isBaby = flag;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyOcelot(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // tamed/sitting
        this.datawatcher.a(18, new Byte((byte) 0)); // cat type

    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a(EntityHuman entityhuman)
    {
        if (super.a(entityhuman))
        {
            return true;
        }

        if (entityhuman.name.equalsIgnoreCase(this.myPet.getOwner().getName()) && !this.world.isStatic)
        {
            this.sitPathfinder.toogleSitting();
            this.bE = false;
        }
        return false;
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return this.random.nextInt(4) == 0 ? "mob.cat.purreow" : "mob.cat.meow";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String aZ()
    {
        return "mob.cat.hitt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String ba()
    {
        return "mob.cat.hitt";
    }
}