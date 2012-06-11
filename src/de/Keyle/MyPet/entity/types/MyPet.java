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

import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillSystem;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.util.*;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

public abstract class MyPet
{
    public static enum PetState
    {
        Dead, Despawned, Here
    }

    protected CraftMyPet Pet;
    public String Name = "Pet";
    protected final OfflinePlayer Owner;
    protected int Health;
    public int RespawnTime = 0;

    protected int SitTimer = MyPetConfig.SitdownTime;
    protected boolean isSitting = false;

    public PetState Status = PetState.Despawned;

    protected Location Location;

    protected MyPetSkillTree skillTree = null;
    protected MyPetSkillSystem skillSystem;
    protected MyPetExperience experience;

    public static int startHP = 10;

    public MyPet(OfflinePlayer Owner)
    {
        this.Owner = Owner;

        if (MyPetSkillTreeConfigLoader.getSkillTreeNames().length > 0)
        {
            for (String ST : MyPetSkillTreeConfigLoader.getSkillTreeNames())
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyPet.custom.skilltree." + ST))
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
        experience = new MyPetExperience(this);
    }

    public void setName(String Name)
    {
        this.Name = Name;
        MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(this, MyPetSpoutEventReason.Name));
    }

    public void removePet()
    {
        if (Status == PetState.Here)
        {
            isSitting = Pet.isSitting();
            Health = Pet.getHealth();
            Location = Pet.getLocation();
            if (Location == null && getOwner().isOnline())
            {
                Location = getOwner().getPlayer().getLocation();
            }
            Status = PetState.Despawned;
            Pet.remove();
        }
    }

    protected void RespawnPet()
    {
        if (Status != PetState.Here)
        {
            Location = getOwner().getPlayer().getLocation();
            sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_OnRespawn")).replace("%petname%", Name));
            createPet();
            RespawnTime = 0;
            Health = getMaxHealth();
        }
    }

    public void createPet()
    {
        if (Status == PetState.Here || getOwner() == null)
        {
        }
        else
        {
            if (RespawnTime <= 0)
            {
                net.minecraft.server.World mcWorld = ((CraftWorld) Location.getWorld()).getHandle();
                EntityMyPet entityMyPet = getPetType().getNewEntityInstance(mcWorld, this);
                entityMyPet.setLocation(Location);
                if (!Location.getChunk().isLoaded())
                {
                    Location.getChunk().load();
                }
                if (!mcWorld.addEntity(entityMyPet, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    return;
                }
                Pet = (CraftMyPet) entityMyPet.getBukkitEntity();
                Pet.setSitting(isSitting);
                Status = PetState.Here;
            }
        }
    }

    public void createPet(Location loc)
    {
        if (Status == PetState.Here || getOwner() == null)
        {
        }
        else
        {
            if (RespawnTime <= 0)
            {
                this.Location = loc;
                net.minecraft.server.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
                EntityMyPet entityMyPet = getPetType().getNewEntityInstance(mcWorld, this);
                entityMyPet.setLocation(loc);
                if (!Location.getChunk().isLoaded())
                {
                    Location.getChunk().load();
                }
                if (!mcWorld.addEntity(entityMyPet, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    return;
                }
                Pet = (CraftMyPet) entityMyPet.getBukkitEntity();
                Pet.setSitting(isSitting);
                Status = PetState.Here;
            }
        }
    }

    public CraftMyPet getPet()
    {
        return Pet;
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
            Pet.setHealth(Health);
        }
    }

    public int getHealth()
    {

        if (Status == PetState.Here)
        {
            return Pet.getHealth();
        }
        else
        {
            return Health;
        }
    }

    public int getMaxHealth()
    {
        return startHP + (skillSystem.hasSkill("HP") ? skillSystem.getSkill("HP").getLevel() : 0);
    }

    public MyPetSkillSystem getSkillSystem()
    {
        return skillSystem;
    }

    public MyPetExperience getExperience()
    {
        return experience;
    }

    public MyPetSkillTree getSkillTree()
    {
        return skillTree;
    }

    public Location getLocation()
    {
        if (Status == PetState.Here)
        {
            return Pet.getLocation();
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
            Pet.teleport(loc);
        }
    }

    public boolean isSitting()
    {
        if (Status == PetState.Here)
        {
            return Pet.isSitting();
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
            Pet.setSitting(sitting);
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
                    Pet.setSitting(true);
                    ResetSitTimer();
                }
                SitTimer--;
            }
            else if (Status == PetState.Dead)
            {
                RespawnTime--;
                if (RespawnTime <= 0)
                {
                    RespawnPet();
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

    public int getStartHP()
    {
        return startHP;
    }

    public abstract MyPetType getPetType();

    public NBTTagCompound getExtendedInfo()
    {
        return new NBTTagCompound("Info");
    }

    public void setExtendedInfo(NBTTagCompound info)
    {
    }

    @Override
    public String toString()
    {
        return "MyPet{owner=" + getOwner().getName() + ", name=" + Name + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + Status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}
