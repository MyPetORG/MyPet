/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2015 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.repository;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.event.MyPetSelectEvent;
import de.Keyle.MyPet.api.event.MyPetSelectEvent.NewStatus;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagCompound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class MyPetList {
    private static final BiMap<MyPetPlayer, MyPet> mActivePlayerPets = HashBiMap.create();
    private static final BiMap<MyPet, MyPetPlayer> mActivePetsPlayer = mActivePlayerPets.inverse();

    // Active -------------------------------------------------------------------

    public static MyPet getMyPet(MyPetPlayer owner) {
        return mActivePlayerPets.get(owner);
    }

    public static MyPet getMyPet(Player owner) {
        return mActivePlayerPets.get(PlayerList.getMyPetPlayer(owner));
    }

    public static MyPet[] getAllActiveMyPets() {
        MyPet[] allActiveMyPets = new MyPet[mActivePetsPlayer.keySet().size()];
        int i = 0;
        for (MyPet myPet : mActivePetsPlayer.keySet()) {
            allActiveMyPets[i++] = myPet;
        }
        return allActiveMyPets;
    }

    public static boolean hasMyPet(MyPetPlayer player) {
        return mActivePlayerPets.containsKey(player);
    }

    public static boolean hasMyPet(Player player) {
        if (PlayerList.isMyPetPlayer(player)) {
            MyPetPlayer petPlayer = PlayerList.getMyPetPlayer(player);
            return hasMyPet(petPlayer);
        }
        return false;
    }

    public static boolean hasMyPet(String name) {
        if (PlayerList.isMyPetPlayer(name)) {
            MyPetPlayer petPlayer = PlayerList.getMyPetPlayer(name);
            return hasMyPet(petPlayer);
        }
        return false;
    }

    // Inactive -----------------------------------------------------------------

    public static Collection<InactiveMyPet> getAllInactiveMyPets() {
        return MyPetPlugin.getPlugin().getRepository().getAllMyPets();
    }

    public static boolean hasInactiveMyPets(Player player) {
        return PlayerList.isMyPetPlayer(player) && hasMyPet(PlayerList.getMyPetPlayer(player));
    }

    public static boolean hasInactiveMyPets(MyPetPlayer myPetPlayer) {
        return MyPetPlugin.getPlugin().getRepository().hasMyPets(myPetPlayer);
    }

    public static InactiveMyPet getInactiveMyPetFromMyPet(MyPet activeMyPet) {
        InactiveMyPet inactiveMyPet = new InactiveMyPet(activeMyPet.getOwner());
        inactiveMyPet.setUUID(activeMyPet.getUUID());
        inactiveMyPet.setPetName(activeMyPet.getPetName());
        inactiveMyPet.setExp(activeMyPet.getExperience().getExp());
        inactiveMyPet.setHealth(activeMyPet.getHealth());
        inactiveMyPet.setHungerValue(activeMyPet.getHungerValue());
        inactiveMyPet.setRespawnTime(activeMyPet.getRespawnTime());
        inactiveMyPet.setSkills(activeMyPet.getSkills().getSkills());
        inactiveMyPet.setInfo(activeMyPet.writeExtendedInfo());
        inactiveMyPet.setPetType(activeMyPet.getPetType());
        inactiveMyPet.setSkillTree(activeMyPet.getSkillTree());
        inactiveMyPet.setWorldGroup(activeMyPet.getWorldGroup());
        inactiveMyPet.setLastUsed(activeMyPet.getLastUsed());
        inactiveMyPet.wantsToRespawn = activeMyPet.wantToRespawn();

        return inactiveMyPet;
    }

    public static List<InactiveMyPet> getInactiveMyPets(MyPetPlayer owner) {
        return MyPetPlugin.getPlugin().getRepository().getMyPets(owner);
    }

    public static void removeInactiveMyPet(InactiveMyPet inactiveMyPet) {
        MyPetPlugin.getPlugin().getRepository().removeMyPet(inactiveMyPet);
    }

    public static void addInactiveMyPet(InactiveMyPet inactiveMyPet) {
        MyPetPlugin.getPlugin().getRepository().addMyPet(inactiveMyPet);
    }

    // All ----------------------------------------------------------------------

    public static MyPet activateMyPet(InactiveMyPet inactiveMyPet) {
        if (!inactiveMyPet.getOwner().isOnline()) {
            return null;
        }

        if (inactiveMyPet.getOwner().hasMyPet()) {
            if (!deactivateMyPet(inactiveMyPet.getOwner())) {
                return null;
            }
        }

        MyPetSelectEvent event = new MyPetSelectEvent(inactiveMyPet, NewStatus.Active);
        getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            MyPet activeMyPet = inactiveMyPet.getPetType().getNewMyPetInstance(inactiveMyPet.getOwner());
            activeMyPet.setUUID(inactiveMyPet.getUUID());
            activeMyPet.setPetName(inactiveMyPet.getPetName());
            activeMyPet.setRespawnTime(inactiveMyPet.getRespawnTime());
            activeMyPet.setWorldGroup(inactiveMyPet.getWorldGroup());
            activeMyPet.readExtendedInfo(inactiveMyPet.getInfo());
            activeMyPet.setLastUsed(inactiveMyPet.getLastUsed());
            activeMyPet.setWantsToRespawn(inactiveMyPet.wantsToRespawn);

            activeMyPet.getExperience().setExp(inactiveMyPet.getExp());
            activeMyPet.setSkilltree(inactiveMyPet.getSkillTree());
            Collection<ISkillInstance> skills = activeMyPet.getSkills().getSkills();
            if (skills.size() > 0) {
                for (ISkillInstance skill : skills) {
                    if (skill instanceof ISkillStorage) {
                        ISkillStorage storageSkill = (ISkillStorage) skill;
                        if (inactiveMyPet.getSkills().getCompoundData().containsKey(skill.getName())) {
                            storageSkill.load(inactiveMyPet.getSkills().getAs(skill.getName(), TagCompound.class));
                        }
                    }
                }
            }
            activeMyPet.setHealth(inactiveMyPet.getHealth());
            activeMyPet.setHungerValue(inactiveMyPet.getHungerValue());

            mActivePetsPlayer.put(activeMyPet, activeMyPet.getOwner());
            return activeMyPet;
        }
        return null;
    }

    public static boolean deactivateMyPet(MyPetPlayer owner) {
        if (mActivePlayerPets.containsKey(owner)) {
            MyPet activeMyPet = owner.getMyPet();

            MyPetSelectEvent event = new MyPetSelectEvent(activeMyPet, NewStatus.Inactive);
            getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            activeMyPet.removePet();
            MyPetPlugin.getPlugin().getRepository().updateMyPet(activeMyPet);
            mActivePetsPlayer.remove(activeMyPet);
            return true;
        }
        return false;
    }

    public static void clearList() {
        mActivePlayerPets.clear();
        MyPetPlugin.getPlugin().getRepository().disable();
    }

    public static int countMyPets() {
        return MyPetPlugin.getPlugin().getRepository().countMyPets();
    }

    public static int countActiveMyPets() {
        return mActivePetsPlayer.size();
    }

    public static int countMyPets(MyPetType myPetType) {
        return MyPetPlugin.getPlugin().getRepository().countMyPets(myPetType);
    }
}