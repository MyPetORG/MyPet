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

import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public abstract class EntityMyPet extends EntityCreature implements IMonster
{
    protected float e;
    protected boolean h;
    protected boolean g;
    protected float j;
    protected float i;
    public EntityLiving goalTarget = null;
    protected float walkSpeed = 0.3F;

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

            this.setHealth(myPet.getHealth() >= getMaxHealth() ? getMaxHealth() : myPet.getHealth());
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

    public abstract int getMaxHealth();

    public void setLocation(Location loc)
    {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    public boolean canMove()
    {
        return true;
    }

    public abstract boolean canEat(ItemStack itemstack);

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
     * Returns the default sound of the MyPet
     */
    protected abstract String aW();

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected abstract String aX();

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected abstract String aY();

    /**
     * N.A.
     */
    public float aV()
    {
        return 0.4F;
    }

    protected boolean bg()
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
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean c(EntityHuman entityhuman)
    {
        super.c(entityhuman);

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (isMyPet() && entityhuman.name.equalsIgnoreCase(myPet.getOwner().getName()))
        {
            if (myPet.getSkillSystem().hasSkill("Control") && myPet.getSkillSystem().getSkill("Control").getLevel() > 0)
            {
                if (itemStack.id == Control.item.getId())
                {
                    return true;
                }
            }
        }
        if (itemStack != null && canEat(itemStack))
        {
            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                this.heal(3, RegainReason.EATING);
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
     * N.A.
     */
    public void j_()
    {
        super.j_();
        if (this.b)
        {
            this.e += (1.0F - this.e) * 0.4F;
        }
        else
        {
            this.e += (0.0F - this.e) * 0.4F;
        }
        if (this.G())
        {
            this.h = true;
            this.g = false;
            this.j = 0.0F;
            this.i = 0.0F;
        }
    }

    /**
     * Is called when a MyPet attemps to do damge to another entity
     */
    public boolean l(Entity entity)
    {
        int damage = MyPet.getStartDamage(this.myPet.getClass()) + (isMyPet() && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }
}