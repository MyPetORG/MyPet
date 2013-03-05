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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.entity.types.IMyPet;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.event.MyPetSelectEvent;
import de.Keyle.MyPet.event.MyPetSelectEvent.NewStatus;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import org.bukkit.entity.Player;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class MyPetList
{
    private static final Map<MyPetPlayer, MyPet> mActivePets = new HashMap<MyPetPlayer, MyPet>();
    private static final List<MyPet> lActivePets = new ArrayList<MyPet>();

    private static final Map<MyPetPlayer, List<InactiveMyPet>> mInctivePets = new HashMap<MyPetPlayer, List<InactiveMyPet>>();
    private static final List<InactiveMyPet> lInactivePets = new ArrayList<InactiveMyPet>();

    // Active -------------------------------------------------------------------

    private static MyPet getMyPetFromInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        if (inactiveMyPet.getOwner().isOnline())
        {
            MyPet activeMyPet = inactiveMyPet.getPetType().getNewMyPetInstance(inactiveMyPet.getOwner());
            activeMyPet.setUUID(inactiveMyPet.getUUID());
            activeMyPet.setLocation(inactiveMyPet.getLocation() == null ? inactiveMyPet.getOwner().getPlayer().getLocation() : inactiveMyPet.getLocation());
            activeMyPet.petName = inactiveMyPet.getPetName();
            activeMyPet.respawnTime = inactiveMyPet.getRespawnTime();
            activeMyPet.setSkilltree(inactiveMyPet.getSkillTree());
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
            Collection<MyPetGenericSkill> skills = activeMyPet.getSkills().getSkills();
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

    private static void addMyPet(MyPet myPet)
    {
        mActivePets.put(myPet.getOwner(), myPet);
        lActivePets.add(myPet);
    }

    private static void removeMyPet(MyPet myPet)
    {
        if (myPet == null)
        {
            return;
        }
        lActivePets.remove(myPet);
        if (mActivePets.containsKey(myPet.getOwner()))
        {
            mActivePets.remove(myPet.getOwner());
        }
    }

    public static MyPet getMyPet(Player owner)
    {
        if (mActivePets.containsKey(MyPetPlayer.getMyPetPlayer(owner)))
        {
            return mActivePets.get(MyPetPlayer.getMyPetPlayer(owner));
        }
        return null;
    }

    public static MyPet getMyPet(String owner)
    {
        if (mActivePets.containsKey(MyPetPlayer.getMyPetPlayer(owner)))
        {
            return mActivePets.get(MyPetPlayer.getMyPetPlayer(owner));
        }
        return null;
    }

    public static MyPet[] getAllActiveMyPets()
    {
        MyPet[] allActiveMyPets = new MyPet[lActivePets.size()];
        int i = 0;
        for (MyPet myPet : lActivePets)
        {
            allActiveMyPets[i++] = myPet;
        }
        return allActiveMyPets;
    }

    public static boolean hasMyPet(Player player)
    {
        return mActivePets.containsKey(MyPetPlayer.getMyPetPlayer(player));
    }

    public static boolean hasMyPet(String name)
    {
        return mActivePets.containsKey(MyPetPlayer.getMyPetPlayer(name));
    }

    // Inactive -----------------------------------------------------------------

    public static List<InactiveMyPet> getAllInactiveMyPets()
    {
        return lInactivePets;
    }

    public static boolean hasInactiveMyPets(Player player)
    {
        return mInctivePets.containsKey(MyPetPlayer.getMyPetPlayer(player));
    }

    public static boolean hasInactiveMyPets(MyPetPlayer myPetPlayer)
    {
        return mInctivePets.containsKey(myPetPlayer);
    }

    public static boolean hasInactiveMyPets(String name)
    {
        return MyPetPlayer.isMyPetPlayer(name) && mInctivePets.containsKey(MyPetPlayer.getMyPetPlayer(name));
    }

    private static InactiveMyPet getInactiveMyPetFromMyPet(MyPet activeMyPet)
    {
        InactiveMyPet inactiveMyPet = new InactiveMyPet(activeMyPet.getOwner());
        inactiveMyPet.setUUID(activeMyPet.getUUID());
        inactiveMyPet.setPetName(activeMyPet.petName);
        inactiveMyPet.setExp(activeMyPet.getExperience().getExp());
        inactiveMyPet.setHealth(activeMyPet.getHealth());
        inactiveMyPet.setHungerValue(activeMyPet.getHungerValue());
        inactiveMyPet.setLocation(activeMyPet.getLocation());
        inactiveMyPet.setRespawnTime(activeMyPet.respawnTime);
        inactiveMyPet.setSkills(activeMyPet.getSkills().getSkills());
        inactiveMyPet.setInfo(activeMyPet.getExtendedInfo());
        inactiveMyPet.setPetType(activeMyPet.getPetType());
        inactiveMyPet.setSkillTree(activeMyPet.getSkillTree());

        return inactiveMyPet;
    }

    public static List<InactiveMyPet> getInactiveMyPets(Player owner)
    {
        if (mInctivePets.containsKey(MyPetPlayer.getMyPetPlayer(owner)))
        {
            return mInctivePets.get(MyPetPlayer.getMyPetPlayer(owner));
        }
        return null;
    }

    public static InactiveMyPet[] getInactiveMyPets(String owner)
    {
        if (mInctivePets.containsKey(MyPetPlayer.getMyPetPlayer(owner)))
        {
            InactiveMyPet[] allInactiveMyPets = new InactiveMyPet[mInctivePets.get(MyPetPlayer.getMyPetPlayer(owner)).size()];
            int i = 0;
            for (InactiveMyPet myPet : mInctivePets.get(MyPetPlayer.getMyPetPlayer(owner)))
            {
                allInactiveMyPets[i++] = myPet;
            }
            return allInactiveMyPets;
        }
        return null;
    }

    public static void removeInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        if (inactiveMyPet == null)
        {
            return;
        }
        lInactivePets.remove(inactiveMyPet);
        if (mInctivePets.containsKey(inactiveMyPet.getOwner()))
        {
            List<InactiveMyPet> myPetList = mInctivePets.get(inactiveMyPet.getOwner());
            if (myPetList.contains(inactiveMyPet))
            {
                myPetList.remove(inactiveMyPet);
            }
            if (myPetList.size() == 0)
            {
                mInctivePets.remove(inactiveMyPet.getOwner());
            }
        }
    }

    public static void addInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        lInactivePets.add(inactiveMyPet);
        if (mInctivePets.containsKey(inactiveMyPet.getOwner()))
        {
            List<InactiveMyPet> inactiveMyPetList = mInctivePets.get(inactiveMyPet.getOwner());
            if (!inactiveMyPetList.contains(inactiveMyPet))
            {
                inactiveMyPetList.add(inactiveMyPet);
            }
        }
        else
        {
            List<InactiveMyPet> inactiveMyPetList = new ArrayList<InactiveMyPet>();
            inactiveMyPetList.add(inactiveMyPet);
            mInctivePets.put(inactiveMyPet.getOwner(), inactiveMyPetList);
        }
    }

    // All ----------------------------------------------------------------------

    public static MyPet setMyPetActive(InactiveMyPet inactiveMyPet)
    {
        if (hasMyPet(inactiveMyPet.getPetName()))
        {
            setMyPetInactive(inactiveMyPet.getOwner().getPlayer());
        }

        MyPet activeMyPet = getMyPetFromInactiveMyPet(inactiveMyPet);
        addMyPet(activeMyPet);
        removeInactiveMyPet(inactiveMyPet);

        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            MyPetSelectEvent event = new MyPetSelectEvent(inactiveMyPet, NewStatus.Active);
            getServer().getPluginManager().callEvent(event);
        }

        inactiveMyPet.getOwner().setLastActiveMyPetUUID(activeMyPet.getUUID());

        MyPetUtil.getDebugLogger().info("   A: " + activeMyPet);
        MyPetUtil.getDebugLogger().info("   I: " + inactiveMyPet);
        return activeMyPet;
    }

    public static InactiveMyPet setMyPetInactive(Player owner)
    {
        if (mActivePets.containsKey(MyPetPlayer.getMyPetPlayer(owner)))
        {
            MyPet activeMyPet = getMyPet(owner);

            activeMyPet.removePet();
            InactiveMyPet inactiveMyPet = getInactiveMyPetFromMyPet(activeMyPet);
            removeMyPet(activeMyPet);
            addInactiveMyPet(inactiveMyPet);

            if (MyPetConfiguration.ENABLE_EVENTS)
            {
                MyPetSelectEvent event = new MyPetSelectEvent(activeMyPet, NewStatus.Inactive);
                getServer().getPluginManager().callEvent(event);
            }

            MyPetUtil.getDebugLogger().info("   I: " + inactiveMyPet);
            MyPetUtil.getDebugLogger().info("   A: " + activeMyPet);
            return inactiveMyPet;
        }
        return null;
    }

    public static IMyPet getLastActiveMyPet(MyPetPlayer petPlayer)
    {
        if (petPlayer.hasCustomData())
        {
            UUID lastActiveMyPetUUID = petPlayer.getLastActiveMyPetUUID();
            if (lastActiveMyPetUUID != null)
            {
                for (IMyPet myPet : getAllMyPets())
                {
                    if (myPet.getUUID().equals(lastActiveMyPetUUID))
                    {
                        return myPet;
                    }
                }
            }
        }
        return null;
    }

    public static IMyPet[] getAllMyPets()
    {
        IMyPet[] allMyPets = new IMyPet[countMyPets()];
        int i = 0;
        for (MyPet myPet : lActivePets)
        {
            allMyPets[i++] = myPet;
        }
        for (InactiveMyPet inactiveMyPet : lInactivePets)
        {
            allMyPets[i++] = inactiveMyPet;
        }
        return allMyPets;
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
        return lActivePets.size() + lInactivePets.size();
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