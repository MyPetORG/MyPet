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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class MyPetList
{
    private static final Map<OfflinePlayer, MyWolf> mActiveWolves = new HashMap<OfflinePlayer, MyWolf>();
    private static final List<MyWolf> lActivePets = new ArrayList<MyWolf>();

    private static final Map<OfflinePlayer, InactiveMyPet> mInctivePets = new HashMap<OfflinePlayer, InactiveMyPet>();
    private static final List<InactiveMyPet> lInactivePets = new ArrayList<InactiveMyPet>();

    // Active -------------------------------------------------------------------

    public static MyWolf getMyPet(InactiveMyPet IMPet)
    {
        if (IMPet.getOwner().isOnline())
        {
            MyWolf AMPet = new MyWolf(IMPet.getOwner());
            AMPet.setHealth(IMPet.getHealth());
            AMPet.setLocation(IMPet.getLocation());
            AMPet.Name = IMPet.getName();
            AMPet.RespawnTime = IMPet.getRespawnTime();

            if (AMPet.RespawnTime > 0)
            {
                AMPet.Status = PetState.Dead;
            }
            else
            {
                AMPet.Status = PetState.Despawned;
            }

            AMPet.getExperience().setExp(IMPet.getExp());
            Collection<MyPetGenericSkill> skills = AMPet.getSkillSystem().getSkills();
            if (skills.size() > 0)
            {
                for (MyPetGenericSkill skill : skills)
                {
                    if (IMPet.getSkills().hasKey(skill.getName()))
                    {
                        skill.load(IMPet.getSkills().getCompound(skill.getName()));
                    }
                }
            }
            return AMPet;
        }
        return null;
    }

    public static void addMyPet(MyWolf MW)
    {
        mActiveWolves.put(MW.getOwner(), MW);
        lActivePets.add(MW);
    }

    public static void removeMyPet(MyWolf MW)
    {
        lActivePets.remove(MW);
        mActiveWolves.remove(MW.getOwner());
    }

    public static void removeMyPet(OfflinePlayer Owner)
    {
        lActivePets.remove(mActiveWolves.get(Owner));
        mActiveWolves.remove(Owner);
    }

    public static MyWolf getMyPet(int EntityID)
    {
        for (MyWolf pet : lActivePets)
        {
            if (pet.Status == PetState.Here && pet.Wolf.getEntityId() == EntityID)
            {
                return pet;
            }
        }
        return null;
    }

    public static MyWolf getMyPet(OfflinePlayer owner)
    {
        if (mActiveWolves.containsKey(owner))
        {
            return mActiveWolves.get(owner);
        }
        return null;
    }

    public static List<MyWolf> getMyPetList()
    {
        return lActivePets;
    }

    public static boolean hasMyPet(OfflinePlayer player)
    {
        return mActiveWolves.containsKey(player);
    }

    public static boolean isMyPet(int EnityID)
    {
        return getMyPet(EnityID) != null;
    }

    // Inactive -----------------------------------------------------------------

    public static List<InactiveMyPet> getInactiveMyPetList()
    {
        return lInactivePets;
    }

    public static boolean hasInactiveMypet(OfflinePlayer player)
    {
        return mInctivePets.containsKey(player);
    }

    public static InactiveMyPet getInactiveMyPet(MyWolf AMPet)
    {
        InactiveMyPet IAMPet = new InactiveMyPet(MyPetPlugin.getPlugin().getServer().getOfflinePlayer(AMPet.getOwner().getName()));
        IAMPet.setName(AMPet.Name);
        IAMPet.setExp(AMPet.getExperience().getExp());
        IAMPet.setHealth(AMPet.getHealth());
        IAMPet.setLocation(AMPet.getLocation());
        IAMPet.setRespawnTime(AMPet.RespawnTime);
        IAMPet.setSitting(IAMPet.isSitting());
        IAMPet.setSkills(AMPet.getSkillSystem().getSkills());

        return IAMPet;
    }

    public static InactiveMyPet getInactiveMyPet(OfflinePlayer owner)
    {
        if (mInctivePets.containsKey(owner))
        {
            return mInctivePets.get(owner);
        }
        return null;
    }

    public static void removeInactiveMyPet(InactiveMyPet IMPet)
    {
        mInctivePets.remove(IMPet.getOwner());
        lInactivePets.remove(IMPet);
    }

    public static void addInactiveMyPet(InactiveMyPet IMPet)
    {
        mInctivePets.put(IMPet.getOwner(), IMPet);
        lInactivePets.add(IMPet);
    }

    // All ----------------------------------------------------------------------

    public static void setMyPetActive(OfflinePlayer Owner, boolean activate)
    {
        MyPetUtil.getDebugLogger().info("Set MyPet active: " + activate);
        if (activate)
        {
            if (mInctivePets.containsKey(Owner) && mInctivePets.get(Owner).getOwner().isOnline())
            {
                InactiveMyPet IMPet = mInctivePets.get(Owner);
                MyWolf AMPet = getMyPet(IMPet);
                addMyPet(AMPet);
                removeInactiveMyPet(IMPet);
                MyPetUtil.getDebugLogger().info("   A: " + AMPet);
                MyPetUtil.getDebugLogger().info("   I: " + IMPet);
            }
        }
        else
        {
            if (mActiveWolves.containsKey(Owner))
            {
                MyWolf AMPet = mActiveWolves.get(Owner);
                InactiveMyPet IMPet = getInactiveMyPet(AMPet);
                AMPet.removePet();
                removeMyPet(AMPet);
                addInactiveMyPet(IMPet);
                MyPetUtil.getDebugLogger().info("   I: " + IMPet);
                MyPetUtil.getDebugLogger().info("   A: " + AMPet);
            }
        }
    }

    public static void clearList()
    {
        mActiveWolves.clear();
        lActivePets.clear();
        mInctivePets.clear();
        lInactivePets.clear();
    }

    public static int getMyPetCount()
    {
        return mActiveWolves.size() + mInctivePets.size();
    }
}