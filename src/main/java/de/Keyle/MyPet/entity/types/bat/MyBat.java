/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.bat;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.MyPetWorldGroup;
import de.Keyle.MyPet.util.support.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;

import static org.bukkit.Material.SPIDER_EYE;

@MyPetInfo(food = {SPIDER_EYE})
public class MyBat extends MyPet
{
    boolean hanging = false;

    public MyBat(MyPetPlayer petOwner)
    {
        super(petOwner);
    }

    @Override
    public SpawnFlags createPet()
    {
        if (status != PetState.Here && getOwner().isOnline())
        {
            if (respawnTime <= 0)
            {
                Location loc = petOwner.getPlayer().getLocation();
                net.minecraft.server.v1_6_R2.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
                EntityMyPet petEntity = getPetType().getNewEntityInstance(mcWorld, this);
                craftMyPet = (CraftMyPet) petEntity.getBukkitEntity();
                loc = loc.add(0, 1, 0);
                petEntity.setLocation(loc);
                if (!MyPetBukkitUtil.canSpawn(loc, petEntity))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NoSpace;
                }

                if (Minigames.DISABLE_PETS_IN_MINIGAMES && Minigames.isInMinigame(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (PvPArena.DISABLE_PETS_IN_ARENA && PvPArena.isInPvPArena(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (MobArena.DISABLE_PETS_IN_ARENA && MobArena.isInMobArena(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (BattleArena.DISABLE_PETS_IN_ARENA && BattleArena.isInBattleArena(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (SurvivalGames.DISABLE_PETS_IN_SURVIVAL_GAMES && SurvivalGames.isInSurvivalGames(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }

                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.Canceled;
                }
                craftMyPet.setMetadata("MyPet", new FixedMetadataValue(MyPetPlugin.getPlugin(), this));
                status = PetState.Here;

                if (worldGroup == null || worldGroup.equals(""))
                {
                    setWorldGroup(MyPetWorldGroup.getGroup(craftMyPet.getWorld().getName()).getName());
                }

                autoAssignSkilltree();

                return SpawnFlags.Success;
            }
        }
        if (status == PetState.Dead)
        {
            return SpawnFlags.Dead;
        }
        else
        {
            return SpawnFlags.AlreadyHere;
        }
    }

    public void setHanging(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyBat) getCraftPet().getHandle()).setHanging(flag);
        }
        this.hanging = flag;
    }

    public boolean ishanging()
    {
        return hanging;
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Hanging", new ByteTag("Hanging", ishanging()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Hanging"))
        {
            setHanging(((ByteTag) info.getValue().get("Hanging")).getBooleanValue());
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Bat;
    }

    @Override
    public String toString()
    {
        return "MyBat{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ",hanging=" + ishanging() + "}";
    }
}
