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

package de.Keyle.MyPet.entity.types;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.api.event.MyPetSelectEvent;
import de.Keyle.MyPet.api.event.MyPetSelectEvent.NewStatus;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.entity.Player;
import org.spout.nbt.CompoundTag;

import java.util.Collection;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class MyPetList
{
    private static final BiMap<MyPetPlayer, MyPet> mActivePlayerPets = HashBiMap.create();
    private static final BiMap<MyPet, MyPetPlayer> mActivePetsPlayer = mActivePlayerPets.inverse();
    private static final ArrayListMultimap<MyPetPlayer, InactiveMyPet> mInctivePets = ArrayListMultimap.create();

    // Active -------------------------------------------------------------------

    private static MyPet getMyPetFromInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        if (inactiveMyPet.getOwner().isOnline())
        {
            MyPet activeMyPet = inactiveMyPet.getPetType().getNewMyPetInstance(inactiveMyPet.getOwner());
            activeMyPet.setUUID(inactiveMyPet.getUUID());
            activeMyPet.petName = inactiveMyPet.getPetName();
            activeMyPet.setRespawnTime(inactiveMyPet.getRespawnTime());
            activeMyPet.setSkilltree(inactiveMyPet.getSkillTree());
            activeMyPet.setWorldGroup(inactiveMyPet.getWorldGroup());
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
            Collection<ISkillInstance> skills = activeMyPet.getSkills().getSkills();
            if (skills.size() > 0)
            {
                for (ISkillInstance skill : skills)
                {
                    if (skill instanceof ISkillStorage)
                    {
                        ISkillStorage storageSkill = (ISkillStorage) skill;
                        if (inactiveMyPet.getSkills().getValue().containsKey(skill.getName()))
                        {
                            storageSkill.load((CompoundTag) inactiveMyPet.getSkills().getValue().get(skill.getName()));
                        }
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
        mActivePetsPlayer.put(myPet, myPet.getOwner());
    }

    private static void removeMyPet(MyPet myPet)
    {
        if (myPet == null)
        {
            return;
        }
        mActivePetsPlayer.remove(myPet);
    }

    public static MyPet getMyPet(Player owner)
    {
        return mActivePlayerPets.get(MyPetPlayer.getMyPetPlayer(owner));
    }

    public static MyPet getMyPet(String owner)
    {
        return mActivePlayerPets.get(MyPetPlayer.getMyPetPlayer(owner));
    }

    public static MyPet[] getAllActiveMyPets()
    {
        MyPet[] allActiveMyPets = new MyPet[mActivePetsPlayer.keySet().size()];
        int i = 0;
        for (MyPet myPet : mActivePetsPlayer.keySet())
        {
            allActiveMyPets[i++] = myPet;
        }
        return allActiveMyPets;
    }

    public static boolean hasMyPet(Player player)
    {
        return mActivePlayerPets.containsKey(MyPetPlayer.getMyPetPlayer(player));
    }

    public static boolean hasMyPet(String name)
    {
        return mActivePlayerPets.containsKey(MyPetPlayer.getMyPetPlayer(name));
    }

    // Inactive -----------------------------------------------------------------

    public static Collection<InactiveMyPet> getAllInactiveMyPets()
    {
        return mInctivePets.values();
    }

    public static boolean hasInactiveMyPets(Player player)
    {
        return mInctivePets.containsKey(MyPetPlayer.getMyPetPlayer(player));
    }

    public static boolean hasInactiveMyPets(MyPetPlayer myPetPlayer)
    {
        return mInctivePets.containsKey(myPetPlayer);
    }

    private static InactiveMyPet getInactiveMyPetFromMyPet(MyPet activeMyPet)
    {
        InactiveMyPet inactiveMyPet = new InactiveMyPet(activeMyPet.getOwner());
        inactiveMyPet.setUUID(activeMyPet.getUUID());
        inactiveMyPet.setPetName(activeMyPet.petName);
        inactiveMyPet.setExp(activeMyPet.getExperience().getExp());
        inactiveMyPet.setHealth(activeMyPet.getHealth());
        inactiveMyPet.setHungerValue(activeMyPet.getHungerValue());
        inactiveMyPet.setRespawnTime(activeMyPet.respawnTime);
        inactiveMyPet.setSkills(activeMyPet.getSkills().getSkills());
        inactiveMyPet.setInfo(activeMyPet.getExtendedInfo());
        inactiveMyPet.setPetType(activeMyPet.getPetType());
        inactiveMyPet.setSkillTree(activeMyPet.getSkillTree());
        inactiveMyPet.setWorldGroup(activeMyPet.getWorldGroup());

        return inactiveMyPet;
    }

    public static List<InactiveMyPet> getInactiveMyPets(MyPetPlayer owner)
    {
        return mInctivePets.get(owner);
    }

    public static List<InactiveMyPet> getInactiveMyPets(Player owner)
    {
        return mInctivePets.get(MyPetPlayer.getMyPetPlayer(owner));
    }

    public static void removeInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        mInctivePets.remove(inactiveMyPet.getOwner(), inactiveMyPet);
    }

    public static void addInactiveMyPet(InactiveMyPet inactiveMyPet)
    {
        if (!mInctivePets.containsEntry(inactiveMyPet.getOwner(), inactiveMyPet))
        {
            mInctivePets.put(inactiveMyPet.getOwner(), inactiveMyPet);
        }
    }

    // All ----------------------------------------------------------------------

    public static MyPet setMyPetActive(InactiveMyPet inactiveMyPet)
    {
        if (inactiveMyPet.getOwner().hasMyPet())
        {
            setMyPetInactive(inactiveMyPet.getOwner());
        }

        boolean isCancelled = false;
        if (Configuration.ENABLE_EVENTS)
        {
            MyPetSelectEvent event = new MyPetSelectEvent(inactiveMyPet, NewStatus.Active);
            getServer().getPluginManager().callEvent(event);
            isCancelled = event.isCancelled();
        }

        if (!isCancelled)
        {
            MyPet activeMyPet = getMyPetFromInactiveMyPet(inactiveMyPet);
            addMyPet(activeMyPet);
            removeInactiveMyPet(inactiveMyPet);

            DebugLogger.info("   A: " + activeMyPet);
            DebugLogger.info("   I: " + inactiveMyPet);

            return activeMyPet;
        }
        return null;
    }

    public static InactiveMyPet setMyPetInactive(MyPetPlayer owner)
    {
        if (mActivePlayerPets.containsKey(owner))
        {
            MyPet activeMyPet = owner.getMyPet();

            boolean isCancelled = false;
            if (Configuration.ENABLE_EVENTS)
            {
                MyPetSelectEvent event = new MyPetSelectEvent(activeMyPet, NewStatus.Inactive);
                getServer().getPluginManager().callEvent(event);
                isCancelled = event.isCancelled();
            }

            if (isCancelled)
            {
                return null;
            }
            activeMyPet.removePet();

            InactiveMyPet inactiveMyPet = getInactiveMyPetFromMyPet(activeMyPet);
            removeMyPet(activeMyPet);
            addInactiveMyPet(inactiveMyPet);

            DebugLogger.info("   I: " + inactiveMyPet);
            DebugLogger.info("   A: " + activeMyPet);

            return inactiveMyPet;
        }
        return null;
    }

    public static IMyPet[] getAllMyPets()
    {
        IMyPet[] allMyPets = new IMyPet[countMyPets()];
        int i = 0;
        for (MyPet myPet : mActivePetsPlayer.keySet())
        {
            allMyPets[i++] = myPet;
        }
        for (InactiveMyPet inactiveMyPet : getAllInactiveMyPets())
        {
            allMyPets[i++] = inactiveMyPet;
        }
        return allMyPets;
    }

    public static void clearList()
    {
        mActivePlayerPets.clear();
        mInctivePets.clear();
    }

    public static int countMyPets()
    {
        return countActiveMyPets() + getAllInactiveMyPets().size();
    }

    public static int countActiveMyPets()
    {
        return mActivePetsPlayer.size();
    }

    public static int countMyPets(MyPetType myPetType)
    {
        int counter = 0;
        for (MyPet myPet : mActivePetsPlayer.keySet())
        {
            if (myPet.getPetType() == myPetType)
            {
                counter++;
            }
        }
        for (InactiveMyPet inactiveMyPet : getAllInactiveMyPets())
        {
            if (inactiveMyPet.getPetType() == myPetType)
            {
                counter++;
            }
        }
        return counter;
    }
}