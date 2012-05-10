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

package de.Keyle.MyWolf.entity.types.wolf;

import de.Keyle.MyWolf.entity.types.MyPet;
import de.Keyle.MyWolf.entity.types.MyPetType;
import de.Keyle.MyWolf.event.MyWolfSpoutEvent;
import de.Keyle.MyWolf.event.MyWolfSpoutEvent.MyWolfSpoutEventReason;
import de.Keyle.MyWolf.skill.MyWolfExperience;
import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.skill.MyWolfSkillSystem;
import de.Keyle.MyWolf.skill.MyWolfSkillTree;
import de.Keyle.MyWolf.util.*;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MyWolf extends MyPet
{
    public String Name = "Wolf";
    public final OfflinePlayer Owner;
    private int Health;
    public CraftMyWolf Wolf;
    public int RespawnTime = 0;

    private int SitTimer = MyWolfConfig.SitdownTime;
    private boolean isSitting = false;

    public PetState Status = PetState.Despawned;

    private Location Location;

    public MyWolfSkillTree SkillTree = null;
    final public MyWolfSkillSystem SkillSystem;
    public final MyWolfExperience Experience;

    public MyWolf(OfflinePlayer Owner)
    {
        this.Owner = Owner;

        if (MyWolfSkillTreeConfigLoader.getSkillTreeNames().length > 0)
        {
            for (String ST : MyWolfSkillTreeConfigLoader.getSkillTreeNames())
            {
                if (MyWolfPermissions.has(Owner.getPlayer(), "MyWolf.custom.skilltree." + ST))
                {
                    this.SkillTree = MyWolfSkillTreeConfigLoader.getSkillTree(ST);
                    break;
                }
            }
        }
        if (this.SkillTree == null)
        {
            this.SkillTree = new MyWolfSkillTree("%+-%NoNe%-+%");
        }
        SkillSystem = new MyWolfSkillSystem(this);
        Experience = new MyWolfExperience(this);
    }

    public void setName(String Name)
    {
        this.Name = Name;
        MyWolfUtil.getServer().getPluginManager().callEvent(new MyWolfSpoutEvent(this, MyWolfSpoutEventReason.Name));
    }

    public void removeWolf()
    {
        if (Status == PetState.Here)
        {
            isSitting = Wolf.isSitting();
            Health = Wolf.getHealth();
            Location = Wolf.getLocation();
            if (Location == null && getOwner().isOnline())
            {
                Location = getOwner().getPlayer().getLocation();
            }
            Status = PetState.Despawned;
            Wolf.remove();
        }
    }

    void RespawnWolf()
    {
        if (Status != PetState.Here)
        {
            Location = getOwner().getPlayer().getLocation();
            sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_OnRespawn")).replace("%wolfname%", Name));
            createWolf(false);
            RespawnTime = 0;
            Health = getMaxHealth();
        }
    }

    public void createWolf(boolean sitting)
    {
        if (Status == PetState.Here || getOwner() == null)
        {
        }
        else
        {
            if (RespawnTime <= 0)
            {
                net.minecraft.server.World mcWorld = ((CraftWorld) Location.getWorld()).getHandle();
                EntityMyWolf MWentityMyWolf = new EntityMyWolf(mcWorld, this);
                MWentityMyWolf.setLocation(Location);
                mcWorld.addEntity(MWentityMyWolf, CreatureSpawnEvent.SpawnReason.CUSTOM);
                Wolf = (CraftMyWolf) MWentityMyWolf.getBukkitEntity();
                Wolf.setSitting(sitting);
                Status = PetState.Here;
            }
        }
    }

    public void createWolf(Wolf wolf)
    {
        net.minecraft.server.World mcWorld = ((CraftWorld) wolf.getWorld()).getHandle();
        EntityMyWolf MWentityMyWolf = new EntityMyWolf(mcWorld, this);
        MWentityMyWolf.setLocation(wolf.getLocation());
        mcWorld.addEntity(MWentityMyWolf, CreatureSpawnEvent.SpawnReason.CUSTOM);
        wolf.remove();
        Wolf = (CraftMyWolf) MWentityMyWolf.getBukkitEntity();
        Location = Wolf.getLocation();
        Wolf.setSitting(true);
        Status = PetState.Here;
    }

    public void setHealth(int d)
    {
        if (d > getMaxHealth())
        {
            Health = getMaxHealth();
        }
        else
        {
            Health = d;
        }
        if (Status == PetState.Here)
        {
            Wolf.setHealth(Health);
        }
    }

    public int getHealth()
    {

        if (Status == PetState.Here)
        {
            return Wolf.getHealth();
        }
        else
        {
            return Health;
        }
    }

    public int getMaxHealth()
    {
        return MyWolfConfig.StartHP + (SkillSystem.hasSkill("HP") ? SkillSystem.getSkill("HP").getLevel() : 0);
    }

    public Location getLocation()
    {
        if (Status == PetState.Here)
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
        if (Status == PetState.Here)
        {
            Wolf.teleport(loc);
        }
    }

    public boolean isSitting()
    {
        if (Status == PetState.Here)
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
        if (Status == PetState.Here)
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
        SitTimer = MyWolfConfig.SitdownTime;
    }

    public void scheduleTask()
    {
        if (Status != PetState.Despawned && getOwner() != null)
        {
            if (SkillSystem.getSkills().size() > 0)
            {
                for (MyWolfGenericSkill skill : SkillSystem.getSkills())
                {
                    skill.schedule();
                }
            }
            if (Status == PetState.Here)
            {
                if (MyWolfConfig.SitdownTime > 0 && SitTimer <= 0)
                {
                    Wolf.setSitting(true);
                    ResetSitTimer();
                }
                SitTimer--;
            }
            else if (Status == PetState.Dead)
            {
                RespawnTime--;
                if (RespawnTime <= 0)
                {
                    RespawnWolf();
                }
            }
        }
    }

    public OfflinePlayer getOwner()
    {
        if (Owner.isOnline())
        {
            return Owner.getPlayer();
        }
        return Owner;
    }

    public void sendMessageToOwner(String Text)
    {
        if (Owner.isOnline())
        {
            getOwner().getPlayer().sendMessage(Text);
        }
    }

    public MyPetType getPetType()
    {
        return MyPetType.Wolf;
    }

    @Override
    public String toString()
    {
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + Name + ", exp=" + Experience.getExp() + "/" + Experience.getRequiredExp() + ", lv=" + Experience.getLevel() + ", status=" + Status.name() + ", skilltree=" + SkillTree.getName() + "}";
    }
}