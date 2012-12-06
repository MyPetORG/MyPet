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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.entity.pathfinder.MyPetPathfinderGoalSelector;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.skill.skills.Ride;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import java.util.List;

public abstract class EntityMyPet extends EntityCreature implements IMonster
{
    protected float e;
    protected boolean h;
    protected boolean g;
    protected float j;
    protected float i;
    public MyPetPathfinderGoalSelector petPathfinderSelector, petTargetSelector;
    public EntityLiving goalTarget = null;
    protected float walkSpeed = 0.3F;
    protected boolean isRidden = false;
    protected boolean isMyPet = false;
    protected MyPet myPet;

    // This Constructor should be never called!!!
    public EntityMyPet(World world)
    {
        super(world);
        MyPetUtil.getLogger().severe("Don't try to get a MyPet this way!");
        MyPetUtil.getDebugLogger().severe("Default Entity constructor called!!!");
    }

    public EntityMyPet(World world, MyPet myPet)
    {
        super(world);
        setMyPet(myPet);
        myPet.craftMyPet = (CraftMyPet) this.getBukkitEntity();

        this.petPathfinderSelector = new MyPetPathfinderGoalSelector(this.goalSelector);
        this.petTargetSelector = new MyPetPathfinderGoalSelector(this.targetSelector);

        this.getNavigation().b(true);
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

            this.setHealth(myPet.getHealth() >= myPet.getMaxHealth() ? myPet.getMaxHealth() : myPet.getHealth());
        }
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
            if (myPet.getSkillSystem().getSkillLevel("Ride") > 0)
            {
                if (itemStack.id == Ride.item.getId() && canMove())
                {
                    this.getOwner().mount(this);
                    return true;
                }
            }
            else if (myPet.getSkillSystem().getSkillLevel("Control") > 0)
            {
                if (itemStack.id == Control.item.getId())
                {
                    return true;
                }
            }
        }
        if (canEat(itemStack))
        {
            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                this.heal(3, RegainReason.EATING);
                myPet.setHungerValue(myPet.getHungerValue() + 3);
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                this.tamedEffect(true);
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
     * N.A.
     */
    public float aV()
    {
        return 0.4F;
    }

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
     * N.A.
     */
    public void c()
    {
        super.c();
        if (!this.world.isStatic && this.h && !this.g && !this.H() && this.onGround)
        {
            this.g = true;
            this.j = 0.0F;
            this.i = 0.0F;
            this.world.broadcastEntityEffect(this, (byte) 8);
        }
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean m(Entity entity)
    {
        int damage = isMyPet() ? myPet.getDamage() : MyPet.getStartDamage(MyPetType.getMyPetTypeByEntityClass(this.getClass()).getMyPetClass());
        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}