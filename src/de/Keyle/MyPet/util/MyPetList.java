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

import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import org.bukkit.entity.Player;

import java.util.*;

public class MyPetList
{
    private static final Map<MyPetPlayer, MyPet> mActivePets = new HashMap<MyPetPlayer, MyPet>();
    private static final List<MyPet> lActivePets = new ArrayList<MyPet>();

    private static final Map<MyPetPlayer, InactiveMyPet> mInctivePets = new HashMap<MyPetPlayer, InactiveMyPet>();
    private static final List<InactiveMyPet> lInactivePets = new ArrayList<InactiveMyPet>();

    // Active -------------------------------------------------------------------

    public static MyPet getMyPet(InactiveMyPet IMPet)
    {
        if (IMPet.getOwner().isOnline())
        {
            MyPet AMPet = IMPet.getType().getNewMyPetInstance(IMPet.getOwner());
            AMPet.setHealth(IMPet.getHealth());
            AMPet.setLocation(IMPet.getLocation());
            AMPet.Name = IMPet.getName();
            AMPet.RespawnTime = IMPet.getRespawnTime();
            AMPet.setExtendedInfo(IMPet.getInfo());

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

    public static void addMyPet(MyPet MP)
    {
        mActivePets.put(MP.getOwner(), MP);
        lActivePets.add(MP);
    }

    public static void removeMyPet(MyPet MP)
    {
        lActivePets.remove(MP);
        mActivePets.remove(MP.getOwner());
    }

    public static MyPet getMyPet(int EntityID)
    {
        for (MyPet pet : lActivePets)
        {
            if (pet.Status == PetState.Here && pet.getPet().getEntityId() == EntityID)
            {
                return pet;
            }
        }
        return null;
    }

    public static MyPet getMyPet(Player owner)
    {
        for (MyPetPlayer myPetPlayer : mActivePets.keySet())
        {
            if (myPetPlayer.equals(owner))
            {
                return mActivePets.get(myPetPlayer);
            }
        }
        return null;
    }

    public static MyPet getMyPet(String owner)
    {
        if (hasMyPet(owner))
        {
            return mActivePets.get(MyPetPlayer.getMyPetPlayer(owner));
        }
        return null;
    }

    public static List<MyPet> getMyPetList()
    {
        return lActivePets;
    }

    public static boolean hasMyPet(Player player)
    {
        for (MyPetPlayer myPetPlayer : mActivePets.keySet())
        {
            if (myPetPlayer.equals(player))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean hasMyPet(MyPetPlayer myPetPlayer)
    {
        return mActivePets.containsKey(myPetPlayer);
    }

    public static boolean hasMyPet(String name)
    {
        return MyPetPlayer.isMyPetPlayer(name) && mActivePets.containsKey(MyPetPlayer.getMyPetPlayer(name));
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

    public static boolean hasInactiveMyPet(Player player)
    {
        for (MyPetPlayer myPetPlayer : mInctivePets.keySet())
        {
            if (myPetPlayer.equals(player))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean hasInactiveMyPet(MyPetPlayer myPetPlayer)
    {
        return mInctivePets.containsKey(myPetPlayer);
    }

    public static boolean hasInactiveMyPet(String name)
    {
        return MyPetPlayer.isMyPetPlayer(name) && mInctivePets.containsKey(MyPetPlayer.getMyPetPlayer(name));
    }

    public static InactiveMyPet getInactiveMyPet(MyPet AMPet)
    {
        InactiveMyPet IAMPet = new InactiveMyPet(AMPet.getOwner());
        IAMPet.setName(AMPet.Name);
        IAMPet.setExp(AMPet.getExperience().getExp());
        IAMPet.setHealth(AMPet.getHealth());
        IAMPet.setLocation(AMPet.getLocation());
        IAMPet.setRespawnTime(AMPet.RespawnTime);
        IAMPet.setSitting(IAMPet.isSitting());
        IAMPet.setSkills(AMPet.getSkillSystem().getSkills());
        IAMPet.setInfo(AMPet.getExtendedInfo());

        return IAMPet;
    }

    public static InactiveMyPet getInactiveMyPet(Player owner)
    {
        for (MyPetPlayer myPetPlayer : mActivePets.keySet())
        {
            if (myPetPlayer.equals(owner))
            {
                return mInctivePets.get(myPetPlayer);
            }
        }
        return null;
    }

    public static InactiveMyPet getInactiveMyPet(String owner)
    {
        if (hasInactiveMyPet(owner))
        {
            return mInctivePets.get(MyPetPlayer.getMyPetPlayer(owner));
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

    public static void setMyPetActive(Player Owner, boolean activate)
    {
        MyPetUtil.getDebugLogger().info("Set MyPet active: " + activate);
        if (activate)
        {
            for (MyPetPlayer myPetPlayer : mInctivePets.keySet())
            {
                if (myPetPlayer.equals(Owner) && mInctivePets.containsKey(myPetPlayer) && myPetPlayer.isOnline())
                {
                    InactiveMyPet IMPet = mInctivePets.get(myPetPlayer);
                    MyPet AMPet = getMyPet(IMPet);
                    addMyPet(AMPet);
                    removeInactiveMyPet(IMPet);
                    MyPetUtil.getDebugLogger().info("   A: " + AMPet);
                    MyPetUtil.getDebugLogger().info("   I: " + IMPet);
                    return;
                }
            }
        }
        else
        {
            for (MyPetPlayer myPetPlayer : mActivePets.keySet())
            {
                if (myPetPlayer.equals(Owner) && mActivePets.containsKey(myPetPlayer))
                {
                    MyPet AMPet = mActivePets.get(myPetPlayer);
                    InactiveMyPet IMPet = getInactiveMyPet(AMPet);
                    AMPet.removePet();
                    removeMyPet(AMPet);
                    addInactiveMyPet(IMPet);
                    MyPetUtil.getDebugLogger().info("   I: " + IMPet);
                    MyPetUtil.getDebugLogger().info("   A: " + AMPet);
                    return;
                }
            }
        }
    }

    public static void clearList()
    {
        mActivePets.clear();
        lActivePets.clear();
        mInctivePets.clear();
        lInactivePets.clear();
    }

    public static int countMyPets()
    {
        return mActivePets.size() + mInctivePets.size();
    }

    public static int countMyPets(MyPetType myPetType)
    {
        int counter = 0;
        for (MyPet myPet : lActivePets)
        {
            if (myPet.getPetType() == myPetType)
            {
                counter++;
            }
        }
        for (InactiveMyPet inactiveMyPet : lInactivePets)
        {
            if (inactiveMyPet.getType() == myPetType)
            {
                counter++;
            }
        }
        return counter;
    }
}