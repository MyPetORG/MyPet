/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.entity.ai.MyPetEntityAISelector;
import de.Keyle.MyPet.entity.ai.movement.EntityAIControl;
import de.Keyle.MyPet.entity.ai.movement.EntityAIFollowOwner;
import de.Keyle.MyPet.entity.ai.movement.EntityAIMeleeAttack;
import de.Keyle.MyPet.entity.ai.movement.EntityAIRide;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.skill.skills.Ride;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import net.minecraft.server.v1_4_6.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import java.util.List;

public abstract class EntityMyPet extends EntityCreature implements IMonster
{
    protected float e;
    protected boolean h;
    protected boolean g;
    protected float j;
    protected float i;
    public MyPetEntityAISelector petPathfinderSelector, petTargetSelector;
    public EntityLiving goalTarget = null;
    protected float walkSpeed = 0.3F;
    protected boolean isRidden = false;
    protected boolean isMyPet = false;
    protected MyPet myPet;
    protected int idleSoundTimer = 0;

    // This Constructor should be never called!!!
    public EntityMyPet(World world)
    {
        super(world);
        MyPetLogger.write(ChatColor.RED + "Don't try to get a MyPet this way!");
        MyPetUtil.getDebugLogger().severe("Default Entity constructor called!!!");
    }

    public EntityMyPet(World world, MyPet myPet)
    {
        super(world);
        setMyPet(myPet);
        myPet.craftMyPet = (CraftMyPet) this.getBukkitEntity();

        this.petPathfinderSelector = new MyPetEntityAISelector(this.goalSelector);
        this.petTargetSelector = new MyPetEntityAISelector(this.targetSelector);

        this.getNavigation().b(true);

        Float[] entitySize = MyPet.getEntitySize(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
        this.a(entitySize[0], entitySize[1]);
        this.walkSpeed = MyPet.getStartSpeed(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
        this.setPathfinder();
    }

    public boolean isMyPet()
    {
        return isMyPet;
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.myPet = myPet;
            isMyPet = true;

            this.setHealth(myPet.getHealth());
        }
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new PathfinderGoalFloat(this));
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed, Ride.speedPerLevel));
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.addGoal("LeapAtTarget", new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, this.walkSpeed, 3, 20));
            petTargetSelector.addGoal("OwnerHurtByTarget", new EntityAIOwnerHurtByTarget(this));
            petTargetSelector.addGoal("OwnerHurtTarget", new EntityAIOwnerHurtTarget(myPet));
            petTargetSelector.addGoal("HurtByTarget", new EntityAIHurtByTarget(this, true));
            petTargetSelector.addGoal("ControlTarget", new EntityAIControlTarget(myPet, 1));
            petTargetSelector.addGoal("AggressiveTarget", new EntityAIAggressiveTarget(myPet, 15));
            petTargetSelector.addGoal("FarmTarget", new EntityAIFarmTarget(myPet, 15));
        }
        petPathfinderSelector.addGoal("Control", new EntityAIControl(myPet, this.walkSpeed + 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new EntityAIFollowOwner(this, this.walkSpeed, 10.0F, 5.0F, 20F));
        petPathfinderSelector.addGoal("LookAtPlayer", false, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new PathfinderGoalRandomLookaround(this));
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public void setHealth(int i)
    {
        if (i > this.getMaxHealth())
        {
            i = this.getMaxHealth();
        }
        else if (i < 0)
        {
            i = 0;
        }
        this.health = i;
    }

    public int getMaxHealth()
    {
        if (isMyPet())
        {
            return myPet.getMaxHealth();
        }
        else
        {
            return MyPet.getStartHP(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
        }
    }

    public boolean isRidden()
    {
        return isRidden;
    }

    public void setRidden(boolean flag)
    {
        isRidden = flag;
    }

    public void setLocation(Location loc)
    {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    public boolean canMove()
    {
        return true;
    }

    public float getWalkSpeed()
    {
        return walkSpeed;
    }

    public boolean canEat(ItemStack itemstack)
    {
        List<Material> foodList = MyPet.getFood(myPet.getClass());
        for (Material foodItem : foodList)
        {
            if (itemstack.id == foodItem.getId())
            {
                return true;
            }
        }
        return false;
    }

    public boolean playIdleSound()
    {
        if (idleSoundTimer-- <= 0)
        {
            idleSoundTimer = 5;
            return true;
        }
        return false;
    }

    public EntityLiving getOwner()
    {
        return this.world.a(myPet.getOwner().getName());
    }

    public boolean damageEntity(DamageSource damagesource, int i)
    {
        Entity entity = damagesource.getEntity();

        if (entity != null && !(entity instanceof EntityHuman) && !(entity instanceof EntityArrow))
        {
            i = (i + 1) / 2;
        }

        return super.damageEntity(damagesource, i);
    }

    protected void tamedEffect(boolean tamed)
    {
        String str = tamed ? "heart" : "smoke";
        for (int i = 0 ; i < 7 ; i++)
        {
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            double d3 = this.random.nextGaussian() * 0.02D;
            this.world.addParticle(str, this.locX + this.random.nextFloat() * this.width * 2.0F - this.width, this.locY + 0.5D + this.random.nextFloat() * this.length, this.locZ + this.random.nextFloat() * this.width * 2.0F - this.width, d1, d2, d3);
        }
    }

    public abstract org.bukkit.entity.Entity getBukkitEntity();

    // Obfuscated Methods -------------------------------------------------------------------------------------------

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

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack == null)
        {
            return false;
        }


        if (isMyPet() && entityhuman.name.equalsIgnoreCase(myPet.getOwner().getName()))
        {
            if (this.isRidden())
            {
                this.getOwner().mount(null);
                return true;
            }
            if (myPet.getSkills().getSkillLevel("Ride") > 0)
            {
                if (itemStack.id == Ride.item.getId() && canMove())
                {
                    this.getOwner().mount(this);
                    return true;
                }
            }
            else if (myPet.getSkills().getSkillLevel("Control") > 0)
            {
                if (itemStack.id == Control.item.getId())
                {
                    return true;
                }
            }
        }
        if (canEat(itemStack))
        {
            int addHunger = 6;
            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                addHunger -= Math.min(3, getMaxHealth() - getHealth()) * 2;
                this.heal(Math.min(3, getMaxHealth() - getHealth()), RegainReason.EATING);
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                this.tamedEffect(true);
            }
            else if (myPet.getHungerValue() < 100)
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
            }
            if (addHunger > 0 && myPet.getHungerValue() < 100)
            {
                myPet.setHungerValue(myPet.getHungerValue() + addHunger);
                addHunger = 0;
            }
            if (addHunger < 6)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected abstract String aY();

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected abstract String aZ();

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected abstract String ba();

    /**
     * Set weather the "new" AI is used
     */
    public boolean be()
    {
        return true;
    }

    protected boolean bj()
    {
        return false;
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean m(Entity entity)
    {
        int damage = isMyPet() ? myPet.getDamage() : MyPet.getStartDamage(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
        if (entity instanceof EntityPlayer)
        {
            Player victim = (Player) entity.getBukkitEntity();
            if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), victim))
            {
                if (myPet.hasTarget())
                {
                    myPet.getCraftPet().getHandle().b((EntityLiving) null);
                }
                return false;
            }
        }
        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}