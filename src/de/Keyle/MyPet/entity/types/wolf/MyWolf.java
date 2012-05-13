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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyWolfSpoutEventReason;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillSystem;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.util.*;
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

    private int SitTimer = MyPetConfig.SitdownTime;
    private boolean isSitting = false;

    public PetState Status = PetState.Despawned;

    private Location Location;

    public MyPetSkillTree skillTree = null;
    final public MyPetSkillSystem skillSystem;
    public final MyPetExperience Experience;

    public MyWolf(OfflinePlayer Owner)
    {
        this.Owner = Owner;

        if (MyPetSkillTreeConfigLoader.getSkillTreeNames().length > 0)
        {
            for (String ST : MyPetSkillTreeConfigLoader.getSkillTreeNames())
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyWolf.custom.skilltree." + ST))
                {
                    this.skillTree = MyPetSkillTreeConfigLoader.getSkillTree(ST);
                    break;
                }
            }
        }
        if (this.skillTree == null)
        {
            this.skillTree = new MyPetSkillTree("%+-%NoNe%-+%");
        }
        skillSystem = new MyPetSkillSystem(this);
        Experience = new MyPetExperience(this);
    }

    public void setName(String Name)
    {
        this.Name = Name;
        MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(this, MyWolfSpoutEventReason.Name));
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
            sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_OnRespawn")).replace("%wolfname%", Name));
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
        return MyPetConfig.StartHP + (skillSystem.hasSkill("HP") ? skillSystem.getSkill("HP").getLevel() : 0);
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
        SitTimer = MyPetConfig.SitdownTime;
    }

    public void scheduleTask()
    {
        if (Status != PetState.Despawned && getOwner() != null)
        {
            if (skillSystem.getSkills().size() > 0)
            {
                for (MyPetGenericSkill skill : skillSystem.getSkills())
                {
                    skill.schedule();
                }
            }
            if (Status == PetState.Here)
            {
                if (MyPetConfig.SitdownTime > 0 && SitTimer <= 0)
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
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + Name + ", exp=" + Experience.getExp() + "/" + Experience.getRequiredExp() + ", lv=" + Experience.getLevel() + ", status=" + Status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}