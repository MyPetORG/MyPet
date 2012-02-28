/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.Entity;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.Skill.Skills.Behavior;
import de.Keyle.MyWolf.util.MyWolfConfig;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.List;

public class EntityMyWolf extends EntityWolf
{
    boolean isMyWolf = false;
    MyWolf MWolf;

    public EntityMyWolf(World world)
    {
        super(world);
    }

    public EntityMyWolf(World world, MyWolf MWolf)
    {
        super(world);
        setMyWolf(MWolf);
    }

    public void setMyWolf(MyWolf MWolf)
    {
        if (MWolf != null)
        {
            this.MWolf = MWolf;
            isMyWolf = true;
            if (!isTamed())
            {
                this.setTamed(true);
                this.setPathEntity(null);
                this.setSitting(MWolf.isSitting());
                this.setHealth(getMaxHealth());
                this.setOwnerName(MWolf.getOwner().getName());
                this.world.a(this, (byte) 7);
            }
        }
    }

    public int getMaxHealth()
    {
        if(isMyWolf)
        {
            return MyWolfConfig.StartHP + (MWolf.SkillSystem.hasSkill("HP")?MWolf.SkillSystem.getSkill("HP").getLevel():0);
        }
        else
        {
            return this.isTamed() ? 20 : 8;
        }
    }

    public void b(NBTTagCompound nbttagcompound)
    {
    }

    public void m_()
    {
        super.m_();
    }

    protected Entity findTarget()
    {
        if (isMyWolf)
        {
            if (MWolf.SkillSystem.hasSkill("Behavior"))
            {
                Behavior behavior = (Behavior) MWolf.SkillSystem.getSkill("Behavior");
                if (behavior.getLevel() > 0)
                {
                    if (behavior.getBehavior() == Behavior.BehaviorState.Friendly)
                    {
                        return null;
                    }
                    else if (behavior.getBehavior() == Behavior.BehaviorState.Aggressive)
                    {
                        List list = this.world.a(EntityLiving.class, AxisAlignedBB.b(this.locX, this.locY, this.locZ, this.locX + 1.0D, this.locY + 1.0D, this.locZ + 1.0D).grow(16.0D, 4.0D, 16.0D));
                        EntityHuman owner = this.world.a(this.getOwnerName());

                        if (!list.isEmpty())
                        {
                            for (Object o : list)
                            {
                                Entity e = (Entity) o;
                                if (e != owner && e != this)
                                {
                                    return e;
                                }
                            }
                        }
                    }
                }
            }
        }
        return this.isAngry() ? this.world.findNearbyPlayer(this, 16.0D) : null;
    }

    @Override
    public boolean b(EntityHuman entityhuman)
    {
        ItemStack itemstack = entityhuman.inventory.getItemInHand();

        if (!this.isTamed())
        {
            if (itemstack != null && itemstack.id == Item.BONE.id && !this.isAngry())
            {
                --itemstack.count;
                if (itemstack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }

                if (!this.world.isStatic)
                {
                    // CraftBukkit start - added event call and isCancelled check.
                    if (this.random.nextInt(3) == 0 && !CraftEventFactory.callEntityTameEvent(this, entityhuman).isCancelled())
                    {
                        // CraftBukkit end
                        this.setTamed(true);
                        this.setPathEntity(null);
                        this.setSitting(true);
                        this.setHealth(20);
                        this.setOwnerName(entityhuman.name);
                        this.world.a(this, (byte) 7);
                    }
                    else
                    {
                        this.world.a(this, (byte) 6);
                    }
                }

                return true;
            }
        }
        else
        {
            if (itemstack != null && Item.byId[itemstack.id] instanceof ItemFood)
            {
                ItemFood itemfood = (ItemFood) Item.byId[itemstack.id];

                if (itemfood.q() && this.datawatcher.getInt(18) < 20)
                {
                    --itemstack.count;
                    this.heal(itemfood.getNutrition(), EntityRegainHealthEvent.RegainReason.EATING); // CraftBukkit
                    if (itemstack.count <= 0)
                    {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }

                    return true;
                }
            }

            if (entityhuman.name.equalsIgnoreCase(this.getOwnerName()))
            {
                if (!this.world.isStatic)
                {
                    if (isMyWolf && MWolf.SkillSystem.hasSkill("Control") && MWolf.SkillSystem.getSkill("Control").getLevel() > 0)
                    {
                        if (MWolf.getOwner().getPlayer().getItemInHand().getType() != MyWolfConfig.ControlItem)
                        {
                            this.setSitting(!this.isSitting());
                            this.aZ = false;
                            this.setPathEntity(null);
                        }
                    }
                    else
                    {
                        this.setSitting(!this.isSitting());
                        this.aZ = false;
                        this.setPathEntity(null);
                    }
                }
                return true;
            }
        }
        return super.b(entityhuman);
    }

    public void setLocation(Location loc)
    {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    public void setHealth(int i)
    {
        if (i > this.getMaxHealth())
        {
            i = this.getMaxHealth();
        }
        this.health = i;
    }

    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyWolf(this.world.getServer(),this);
        }
        return this.bukkitEntity;
    }
}
