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

import de.Keyle.MyWolf.Entity.CraftMyWolf;
import de.Keyle.MyWolf.Entity.EntityMyWolf;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.Skill.MyWolfSkillSystem;
import de.Keyle.MyWolf.Skill.MyWolfSkillTree;
import de.Keyle.MyWolf.util.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Collection;

public class MyWolf
{
    public String Name = "Wolf";
    public final String Owner;
    private int ID;
    public int HealthNow;
    public int HealthMax = 20;
    public CraftMyWolf Wolf;
    public int RespawnTime = 0;

    private int Timer = -1;

    private int SitTimer = MyWolfConfig.SitdownTime;
    private boolean isSitting = false;

    public WolfState Status = WolfState.Despawned;

    private Location Location;

    public MyWolfSkillTree SkillTree = null;
    public MyWolfSkillSystem SkillSystem;
    public final MyWolfExperience Experience;

    public static enum WolfState
    {
        Dead, Despawned, Here
    }

    public MyWolf(String Owner)
    {
        this.Owner = Owner;

        if(MyWolfSkillTreeConfigLoader.getSkillTreeNames().length > 0)
        {
            for(String ST : MyWolfSkillTreeConfigLoader.getSkillTreeNames())
            {
                if(MyWolfPermissions.has(Owner, "MyWolf.user.skilltree." + ST))
                {
                    this.SkillTree = MyWolfSkillTreeConfigLoader.getSkillTree(ST);
                    break;
                }
            }
        }
        if(this.SkillTree == null)
        {
            this.SkillTree = new MyWolfSkillTree("%+-%NoNe%-+%");
        }

        SkillSystem = new MyWolfSkillSystem(this);
        Experience = new MyWolfExperience(this);
    }

    public void SetName(String Name)
    {
        this.Name = Name;
    }

    public void removeWolf()
    {
        StopTimer();
        isSitting = Wolf.isSitting();
        HealthNow = Wolf.getHealth();
        Location = Wolf.getLocation();
        Status = WolfState.Despawned;
        Wolf.remove();
    }

    void RespawnWolf()
    {
        if (Status != WolfState.Here)
        {
            Location = getOwner().getLocation();
            getOwner().sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_OnRespawn")).replace("%wolfname%", Name));
            createWolf(false);
            RespawnTime = 0;
            HealthNow = HealthMax;
        }
    }

    public void createWolf(boolean sitting)
    {
        if (Status == WolfState.Here || getOwner() == null)
        {
        }
        else
        {
            if (RespawnTime <= 0)
            {
                net.minecraft.server.World mcWorld = ((CraftWorld)Location.getWorld()).getHandle();
                EntityMyWolf MWentityMyWolf = new EntityMyWolf(mcWorld);
                MWentityMyWolf.setMyWolf(this);
                MWentityMyWolf.setLocation(Location);
                mcWorld.addEntity(MWentityMyWolf, CreatureSpawnEvent.SpawnReason.CUSTOM);
                Wolf = (CraftMyWolf) MWentityMyWolf.getBukkitEntity();
                ID = Wolf.getEntityId();
                Wolf.setSitting(sitting);
                Status = WolfState.Here;
            }
            Timer();
        }
    }

    public void createWolf(Wolf wolf)
    {
        net.minecraft.server.World mcWorld = ((CraftWorld)wolf.getWorld()).getHandle();
        EntityMyWolf MWentityMyWolf = new EntityMyWolf(mcWorld);
        MWentityMyWolf.setMyWolf(this);
        MWentityMyWolf.setLocation(wolf.getLocation());
        mcWorld.addEntity(MWentityMyWolf, CreatureSpawnEvent.SpawnReason.CUSTOM);
        wolf.remove();
        Wolf = (CraftMyWolf) MWentityMyWolf.getBukkitEntity();
        ID = Wolf.getEntityId();
        Location = Wolf.getLocation();
        Status = WolfState.Here;
        Wolf.setSitting(true);
        Timer();
    }

    public void setHealth(int d)
    {
        if (d > HealthMax)
        {
            HealthNow = HealthMax;
        }
        else
        {
            HealthNow = d;
        }
        if (Status == WolfState.Here)
        {
            Wolf.setHealth(HealthNow);
        }
    }

    public int getHealth()
    {

        if (Status == WolfState.Here)
        {
            return Wolf.getHealth();
        }
        else
        {
            return HealthNow;
        }
    }

    public int getEntityId()
    {
        if (Status == WolfState.Here)
        {
            return Wolf.getEntityId();
        }
        else
        {
            return ID;
        }
    }

    public Location getLocation()
    {
        if (Status == WolfState.Here)
        {
            return Wolf.getLocation();
        }
        else
        {
            return Location;
        }
    }

    public void setLocation(Location loc)
    {
        this.Location = loc;
        if (Status == WolfState.Here)
        {
            Wolf.teleport(loc);
        }
    }

    public boolean isSitting()
    {
        if (Status == WolfState.Here)
        {
            return Wolf.isSitting();
        }
        else
        {
            return isSitting;
        }
    }

    public void setSitting(boolean sitting)
    {
        if (Status == WolfState.Here)
        {
            Wolf.setSitting(sitting);
            this.isSitting = sitting;
        }
        else
        {
            this.isSitting = sitting;
        }
    }

    public void ResetSitTimer()
    {
        if(MyWolfConfig.SitdownTime > 0)
        {
            SitTimer = MyWolfConfig.SitdownTime;
        }
    }

    public void StopTimer()
    {
        if (Timer != -1)
        {
            MyWolfPlugin.Plugin.getServer().getScheduler().cancelTask(Timer);
            Timer = -1;
        }
    }

    public void Timer()
    {
    	
        if (Status != WolfState.Despawned)
        {
            if (Timer != -1)
            {
                StopTimer();
            }

            Timer = MyWolfPlugin.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWolfPlugin.Plugin, new Runnable()
            {
                Collection<MyWolfGenericSkill> Skills = SkillSystem.getSkills();

                public void run()
                {
                    if (Status == WolfState.Despawned || getOwner() == null)
                    {
                        StopTimer();
                    }
                    else
                    {

                        if(Skills.size() > 0)
                        {
                            for(MyWolfGenericSkill skill : Skills)
                            {
                                skill.schedule();
                            }
                        }
                        if (Status == WolfState.Here)
                        {

                            SitTimer--;
                            if (MyWolfConfig.SitdownTime > 0 && SitTimer <= 0)
                            {
                                Wolf.setSitting(true);
                                SitTimer = MyWolfConfig.SitdownTime;
                            }
                        }
                        else if (Status == WolfState.Dead)
                        {
                            RespawnTime--;
                            if (RespawnTime <= 0)
                            {
                                RespawnWolf();
                            }
                        }
                    }
                }
            }, 0L, 20L);
        }
    }

    public Player getOwner()
    {
        Player p = MyWolfPlugin.Plugin.getServer().getPlayer(Owner);
        if(p != null && p.isOnline())
        {
            return p;
        }
        return null;
    }
    
    public String getOwnerName()
    {
        return this.Owner;
    }

    public void sendMessageToOwner(String Text)
    {
        if (getOwner() != null)
        {
            getOwner().sendMessage(Text);
        }
    }
}
