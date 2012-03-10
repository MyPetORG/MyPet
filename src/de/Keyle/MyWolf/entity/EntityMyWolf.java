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

package de.Keyle.MyWolf.entity;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.skill.skills.Behavior;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.*;
import org.bukkit.Location;

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

    public boolean isMyWolf()
    {
        return isMyWolf;
    }

    public void e(NBTTagCompound nbttagcompound)
    {
        if (!isMyWolf)
        {
            super.d(nbttagcompound);
            EntityWolf entityWolf = new EntityWolf(world);
            entityWolf.d(nbttagcompound);
            this.getBukkitEntity().remove();
            MyWolfUtil.getLogger().severe("If there is a unnormal messege around here, please contact the developer and inform him about this!");
        }
        else
        {
            super.d(nbttagcompound);
        }
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
                this.world.broadcastEntityEffect(this, (byte) 7);
            }
        }
    }

    public int getMaxHealth()
    {
        if (isMyWolf)
        {
            return MyWolfConfig.StartHP + (MWolf.SkillSystem.hasSkill("HP") ? MWolf.SkillSystem.getSkill("HP").getLevel() : 0);
        }
        else
        {
            return super.getMaxHealth();
        }
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
                    else if (behavior.getBehavior() == Behavior.BehaviorState.Aggressive && !this.isSitting())
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
        return super.findTarget();
    }

    public boolean b(EntityHuman entityhuman)
    {
        if (isMyWolf() && entityhuman.name.equalsIgnoreCase(this.getOwnerName()))
        {
            if (MWolf.SkillSystem.hasSkill("Control") && MWolf.SkillSystem.getSkill("Control").getLevel() > 0)
            {
                if (MWolf.getOwner().getPlayer().getItemInHand().getType() == MyWolfConfig.ControlItem)
                {
                    return true;
                }
            }
        }
        return super.b(entityhuman);
    }

    public boolean a(Entity entity)
    {
        int i = this.isTamed() ? 4 : 2;
        i += (isMyWolf && MWolf.SkillSystem.hasSkill("Demage")) ? MWolf.SkillSystem.getSkill("Demage").getLevel() : 0;

        return entity.damageEntity(DamageSource.mobAttack(this), i);
    }

    public EntityAnimal createChild(EntityAnimal entityanimal)
    {
        return null;
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
            this.bukkitEntity = new CraftMyWolf(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    public MyWolf getMyWolf()
    {
        return MWolf;
    }
}
