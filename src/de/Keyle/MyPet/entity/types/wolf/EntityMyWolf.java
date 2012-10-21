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

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalSit;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMyWolf extends EntityMyPet
{
    private PathfinderGoalSit sitPathfinderGoal;

    public EntityMyWolf(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/wolf.png";
        this.a(0.6F, 0.8F);
        this.getNavigation().a(true);

        if (this.sitPathfinderGoal == null)
        {
            this.sitPathfinderGoal = new PathfinderGoalSit(this);
        }
        PathfinderGoalControl controlPathfinderGoal = new PathfinderGoalControl(myPet, 0.4F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, this.sitPathfinderGoal);
        this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, 0.3F, true));
        this.goalSelector.a(5, controlPathfinderGoal);
        this.goalSelector.a(7, new PathfinderGoalFollowOwner(this, 0.3F, 5.0F, 2.0F, controlPathfinderGoal));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinderGoal, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 10));
    }

    @Override
    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.myPet = myPet;
            isMyPet = true;

            this.setPathEntity(null);
            this.setSitting(((MyWolf) myPet).isSitting());
            this.setHealth(myPet.getHealth() >= getMaxHealth() ? getMaxHealth() : myPet.getHealth());
            setTamed(true);
        }
    }

    public void setSitting(boolean sitting)
    {
        if (this.sitPathfinderGoal == null)
        {
            this.sitPathfinderGoal = new PathfinderGoalSit(this);
        }
        this.sitPathfinderGoal.setSitting(sitting);
    }

    public boolean isSitting()
    {
        return this.sitPathfinderGoal.isSitting();
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
    }

    public int getMaxHealth()
    {
        return MyWolf.getStartHP() + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean c(EntityHuman entityhuman)
    {
        super.c(entityhuman);

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack != null && this.b(itemStack))
        {
            ItemFood itemfood = (ItemFood) Item.byId[itemStack.id];

            if (getHealth() < getMaxHealth())
            {
                //don't remove item in creative gamemode
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                this.heal(itemfood.getNutrition(), RegainReason.EATING);
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                this.tamedEffect(true);
                return true;
            }
        }
        else if (entityhuman.name.equalsIgnoreCase(this.myPet.getOwner().getName()) && !this.world.isStatic)
        {
            //sit down
            this.sitPathfinderGoal.toogleSitting();
            this.bu = false;
            this.setPathEntity(null);
        }

        return false;
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean k(Entity entity)
    {
        int damage = 4 + (isMyPet && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
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

    // Vanilla Methods -----------------------------------------------------------------------------------------------------

    /**
     * Returns the default sound of the MyPet
     */
    protected String aQ()
    {
        return (this.random.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "mob.wolf.whine" : "mob.wolf.panting") : "mob.wolf.bark");
    }

    @Override
    protected void bd()
    {
        this.datawatcher.watch(18, this.getHealth());
    }

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, (byte) 0);         // tamed/angry/sitting
        this.datawatcher.a(18, this.getHealth()); // tail height
        this.datawatcher.a(12, 0);                // age
    }

    public void setTamed(boolean tamed)
    {
        int i = this.datawatcher.getByte(16);
        if (tamed)
        {
            this.datawatcher.watch(16, (byte) (i | 0x4));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFB));
        }
    }

    public int getAge()
    {
        return this.datawatcher.getInt(12);
    }

    public void setAge(int i)
    {
        this.datawatcher.watch(12, i);
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aR()
    {
        return "mob.wolf.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String aS()
    {
        return "mob.wolf.death";
    }
}