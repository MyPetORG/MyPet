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

package de.Keyle.MyPet.entity.types.wolf;

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

public class EntityMyWolf extends EntityMyPet
{
    private PathfinderGoalSit sitPathfinder;

    public EntityMyWolf(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/wolf.png";
        this.a(0.6F, 0.8F);

        if (this.sitPathfinder == null)
        {
            this.sitPathfinder = new PathfinderGoalSit(this);
        }
        PathfinderGoalControl controlPathfinder = new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, this.sitPathfinder);
        this.goalSelector.a(5, controlPathfinder);
        this.goalSelector.a(7, new PathfinderGoalFollowOwner(this, this.walkSpeed, 5.0F, 2.0F, 20F, controlPathfinder));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));

        if (MyPet.getStartDamage(MyWolf.class) > 0)
        {
            this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, this.walkSpeed, true));
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

            this.setSitting(((MyWolf) myPet).isSitting());
            this.setCollarColor(((MyWolf) myPet).getCollarColor());
            this.setTamed(((MyWolf) myPet).isTamed());
        }
    }

    public void setHealth(int i)
    {
        super.setHealth(i);
        this.bm();
    }

    public boolean canMove()
    {
        return !isSitting();
    }

    public void setSitting(boolean sitting)
    {
        if (this.sitPathfinder == null)
        {
            this.sitPathfinder = new PathfinderGoalSit(this);
        }
        this.sitPathfinder.setSitting(sitting);
    }

    public boolean isSitting()
    {
        return this.sitPathfinder.isSitting();
    }

    public void applySitting(boolean sitting)
    {
        int i = this.datawatcher.getByte(16);
        if (sitting)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyWolf) myPet).isSitting = sitting;
    }

    public boolean isTamed()
    {
        return (this.datawatcher.getByte(16) & 0x4) != 0;
    }

    public void setTamed(boolean flag)
    {
        int i = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (i | 0x4));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFB));
        }
        ((MyWolf) myPet).isTamed = flag;
    }

    public boolean isAngry()
    {
        return (this.datawatcher.getByte(16) & 0x2) != 0;
    }

    public void setAngry(boolean flag)
    {
        byte b0 = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (b0 | 0x2));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & 0xFFFFFFFD));
        }
        ((MyWolf) myPet).isAngry = flag;
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
        ((MyWolf) myPet).isBaby = flag;
    }

    public int getCollarColor()
    {
        return this.datawatcher.getByte(20) & 0xF;
    }

    public void setCollarColor(int value)
    {
        this.datawatcher.watch(20, (byte) (value & 0xF));
        ((MyWolf) myPet).collarColor = value;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyWolf(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0));               // tamed/angry/sitting
        this.datawatcher.a(17, new String(""));                   // wolf owner name
        this.datawatcher.a(18, new Integer(this.getHealth()));    // tail height
        this.datawatcher.a(12, new Integer(0));                   // age
        this.datawatcher.a(19, new Byte((byte) 0));
        this.datawatcher.a(20, new Byte((byte) BlockCloth.e_(1))); // collar color
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

    @Override
    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.wolf.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return (this.random.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "mob.wolf.whine" : "mob.wolf.panting") : "mob.wolf.bark");
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.wolf.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.wolf.death";
    }

    @Override
    protected void bm()
    {
        this.datawatcher.watch(18, (int) (25. * myPet.getHealth() / myPet.getMaxHealth())); // update tail height
    }
}