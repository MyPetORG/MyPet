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

package de.Keyle.MyWolf;

import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Collection;

public class InactiveMyWolf
{
    private String Name;
    private final OfflinePlayer Owner;
    private int Health;
    private int RespawnTime;
    private boolean isSitting;
    private Location Location;
    private double Exp;

    private NBTTagCompound NBTSkills = new NBTTagCompound("Skills");

    public InactiveMyWolf(OfflinePlayer Owner)
    {
        this.Owner = Owner;
    }

    public void setSkills(Collection<MyWolfGenericSkill> Skills)
    {
        if (Skills.size() > 0)
        {
            for (MyWolfGenericSkill Skill : Skills)
            {
                NBTTagCompound s = Skill.save();
                if (s != null)
                {
                    this.NBTSkills.set(Skill.getName(), s);
                }
            }
        }
    }

    public void setSkills(NBTTagCompound Skills)
    {
        NBTSkills = Skills;
    }

    public NBTTagCompound getSkills()
    {
        return NBTSkills;
    }

    public void setHealth(int Health)
    {
        this.Health = Health;
    }

    public int getHealth()
    {
        return Health;
    }

    public void setExp(double Exp)
    {
        this.Exp = Exp;
    }

    public double getExp()
    {
        return Exp;
    }

    public void setName(String Name)
    {
        this.Name = Name;
    }

    public String getName()
    {
        return Name;
    }

    public void setRespawnTime(int RespawnTime)
    {
        this.RespawnTime = RespawnTime;
    }

    public int getRespawnTime()
    {
        return RespawnTime;
    }

    public Location getLocation()
    {
        return Location;
    }

    public void setLocation(Location Location)
    {
        this.Location = Location;
    }

    public boolean isSitting()
    {
        return isSitting;
    }

    public void setSitting(boolean isSitting)
    {
        this.isSitting = isSitting;
    }

    public OfflinePlayer getOwner()
    {
        if (Owner.isOnline())
        {
            return Owner.getPlayer();
        }
        return Owner;
    }

    @Override
    public String toString()
    {
        return "InactiveMyWolf{owner=" + getOwner().getName() + ", name=" + Name + ", exp=" + getExp() + ", health=" + getHealth() + ", sitting=" + isSitting() + "}";
    }
}
