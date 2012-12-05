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

    public static MyPet getMyPet(InactiveMyPet inactiveMyPet)
    {
        if (inactiveMyPet.getPetOwner().isOnline())
        {
            MyPet activeMyPet = inactiveMyPet.getPetType().getNewMyPetInstance(inactiveMyPet.getPetOwner());
            activeMyPet.setLocation(inactiveMyPet.getLocation());
            activeMyPet.petName = inactiveMyPet.getPetName();
            activeMyPet.respawnTime = inactiveMyPet.getRespawnTime();
            activeMyPet.setExtendedInfo(inactiveMyPet.getInfo());

            if (activeMyPet.respawnTime > 0)
            {
                activeMyPet.status = PetState.Dead;
            }
            else
            {
                activeMyPet.status = PetState.Despawned;
            }

            activeMyPet.getExperience().setExp(inactiveMyPet.getExp());
            Collection<MyPetGenericSkill> skills = activeMyPet.getSkillSystem().getSkills();
            if (skills.size() > 0)
            {
                for (MyPetGenericSkill skill : skills)
                {
                    if (inactiveMyPet.getSkills().hasKey(skill.getName()))
                    {
                        skill.load(inactiveMyPet.getSkills().getCompound(skill.getName()));
                    }
                }
            }
            activeMyPet.setHealth(inactiveMyPet.getHealth());
            activeMyPet.setHungerValue(inactiveMyPet.getHungerValue());
            return activeMyPet;
        }
        return null;
    }

    public static void addMyPet(MyPet myPet)
    {
        mActivePets.put(myPet.getOwner(), myPet);
        lActivePets.add(myPet);
    }

    public static void removeMyPet(MyPet myPet)
    {
        lActivePets.remove(myPet);
        mActivePets.remove(myPet.getOwner());
    }

    public static MyPet getMyPet(int entityID)
    {
        for (MyPet pet : lActivePets)
        {
            if (pet.status == PetState.Here && pet.getCraftPet().getEntityId() == entityID)
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

    public static boolean isMyPet(int enityID)
    {
        return getMyPet(enityID) != null;
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

    public static InactiveMyPet getInactiveMyPet(MyPet activeMyPet)
    {
        InactiveMyPet inactiveMyPet = new InactiveMyPet(activeMyPet.getOwner());
        inactiveMyPet.setPetName(activeMyPet.petName);
        inactiveMyPet.setExp(activeMyPet.getExperience().getExp());
        inactiveMyPet.setHealth(activeMyPet.getHealth());
        inactiveMyPet.setHungerValue(activeMyPet.getHungerValue());
        inactiveMyPet.setLocation(activeMyPet.getLocation());
        inactiveMyPet.setRespawnTime(activeMyPet.respawnTime);
        inactiveMyPet.setSkills(activeMyPet.getSkillSystem().getSkills());
        inactiveMyPet.setInfo(activeMyPet.getExtendedInfo());

        return inactiveMyPet;
    }

    public static InactiveMyPet getInactiveMyPet(Player owner)
    {
        for (MyPetPlayer myPetPlayer : mInctivePets.keySet())
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

    public static void removeInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        mInctivePets.remove(inactiveMyPet.getPetOwner());
        lInactivePets.remove(inactiveMyPet);
    }

    public static void addInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        mInctivePets.put(inactiveMyPet.getPetOwner(), inactiveMyPet);
        lInactivePets.add(inactiveMyPet);
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
                    InactiveMyPet inactiveMyPet = mInctivePets.get(myPetPlayer);
                    MyPet activeMyPet = getMyPet(inactiveMyPet);
                    addMyPet(activeMyPet);
                    removeInactiveMyPet(inactiveMyPet);
                    MyPetUtil.getDebugLogger().info("   A: " + activeMyPet);
                    MyPetUtil.getDebugLogger().info("   I: " + inactiveMyPet);
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
                    MyPet activeMyPet = mActivePets.get(myPetPlayer);
                    InactiveMyPet inactiveMyPet = getInactiveMyPet(activeMyPet);
                    activeMyPet.removePet();
                    removeMyPet(activeMyPet);
                    addInactiveMyPet(inactiveMyPet);
                    MyPetUtil.getDebugLogger().info("   I: " + inactiveMyPet);
                    MyPetUtil.getDebugLogger().info("   A: " + activeMyPet);
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
            if (inactiveMyPet.getPetType() == myPetType)
            {
                counter++;
            }
        }
        return counter;
    }
}