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
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

public abstract class MyPet
{
    public static enum PetState
    {
        Dead, Despawned, Here
    }

    protected CraftMyPet craftPet;
    public String petName = "Pet";
    protected final MyPetPlayer petOwner;
    protected int health;
    public int respawnTime = 0;

    protected boolean isSitting = false;

    public PetState status = PetState.Despawned;

    protected Location petLocation;

    protected MyPetSkillTree skillTree = null;
    protected MyPetSkillSystem skillSystem;
    protected MyPetExperience experience;

    protected static int startHP = 10;

    public MyPet(MyPetPlayer Owner)
    {
        this.petOwner = Owner;

        if (MyPetSkillTreeConfigLoader.getSkillTreeNames(this.getPetType()).size() > 0)
        {
            for (String skillTreeName : MyPetSkillTreeConfigLoader.getSkillTreeNames(this.getPetType()))
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyPet.custom.skilltree." + skillTreeName))
                {
                    this.skillTree = MyPetSkillTreeConfigLoader.getMobType(this.getPetType().getTypeName()).getSkillTree(skillTreeName);
                    break;
                }
            }
        }
        if (this.skillTree == null)
        {
            for (String skillTreeName : MyPetSkillTreeConfigLoader.getSkillTreeNames("default"))
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyPet.custom.skilltree." + skillTreeName))
                {
                    this.skillTree = MyPetSkillTreeConfigLoader.getMobType("default").getSkillTree(skillTreeName);
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

    public void setPetName(String Name)
    {
        this.petName = Name;
        MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(this, MyPetSpoutEventReason.Name));
    }

    public void removePet()
    {
        if (status == PetState.Here)
        {
            isSitting = craftPet.isSitting();
            health = craftPet.getHealth();
            petLocation = craftPet.getLocation();
            if (petLocation == null && getOwner().isOnline())
            {
                petLocation = getOwner().getPlayer().getLocation();
            }
            status = PetState.Despawned;
            craftPet.remove();
        }
    }

    protected void respawnPet()
    {
        if (status != PetState.Here)
        {
            petLocation = getOwner().getPlayer().getLocation();
            sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_OnRespawn")).replace("%petname%", petName));
            createPet();
            respawnTime = 0;
            health = getMaxHealth();
        }
    }

    public void createPet()
    {
        if (status == PetState.Here || getOwner() == null)
        {
        }
        else
        {
            if (respawnTime <= 0)
            {
                net.minecraft.server.World mcWorld = ((CraftWorld) petLocation.getWorld()).getHandle();
                EntityMyPet petEntity = getPetType().getNewEntityInstance(mcWorld, this);
                petEntity.setLocation(petLocation);
                if (!petLocation.getChunk().isLoaded())
                {
                    petLocation.getChunk().load();
                }
                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    return;
                }
                craftPet = (CraftMyPet) petEntity.getBukkitEntity();
                craftPet.setSitting(isSitting);
                status = PetState.Here;
            }
        }
    }

    public void createPet(Location loc)
    {
        if (status == PetState.Here || getOwner() == null)
        {
        }
        else
        {
            if (respawnTime <= 0)
            {
                this.petLocation = loc;
                net.minecraft.server.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
                EntityMyPet petEntity = getPetType().getNewEntityInstance(mcWorld, this);
                petEntity.setLocation(loc);
                if (!petLocation.getChunk().isLoaded())
                {
                    petLocation.getChunk().load();
                }
                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    return;
                }
                craftPet = (CraftMyPet) petEntity.getBukkitEntity();
                craftPet.setSitting(isSitting);
                status = PetState.Here;
            }
        }
    }

    public CraftMyPet getPet()
    {
        return craftPet;
    }

    public void setHealth(int d)
    {
        if (d > getMaxHealth())
        {
            health = getMaxHealth();
        }
        else
        {
            health = d;
        }
        if (status == PetState.Here)
        {
            craftPet.setHealth(health);
        }
    }

    public int getHealth()
    {

        if (status == PetState.Here)
        {
            return craftPet.getHealth();
        }
        else
        {
            return health;
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
        if (status == PetState.Here)
        {
            return craftPet.getLocation();
        }
        else
        {
            return petLocation;
        }
    }

    public void setLocation(Location loc)
    {
        this.petLocation = loc;
        if (status == PetState.Here)
        {
            craftPet.teleport(loc);
        }
    }

    public boolean isSitting()
    {
        return false;
    }

    public void setSitting(boolean sitting)
    {
    }

    public void scheduleTask()
    {
        if (status != PetState.Despawned && getOwner() != null)
        {
            if (skillSystem.getSkills().size() > 0)
            {
                for (MyPetGenericSkill skill : skillSystem.getSkills())
                {
                    skill.schedule();
                }
            }
            else if (status == PetState.Dead)
            {
                respawnTime--;
                if (respawnTime <= 0)
                {
                    respawnPet();
                }
            }
        }
    }

    public MyPetPlayer getOwner()
    {
        return petOwner;
    }

    public void sendMessageToOwner(String text)
    {
        if (petOwner.isOnline())
        {
            getOwner().getPlayer().sendMessage(text);
        }
    }

    public static int getStartHP()
    {
        return startHP;
    }

    public static void setStartHP(int hp)
    {
        startHP = hp;
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
        return "MyPet{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}
