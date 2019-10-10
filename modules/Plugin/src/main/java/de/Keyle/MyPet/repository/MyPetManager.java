/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.repository;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetActivatedEvent;
import de.Keyle.MyPet.api.event.MyPetLoadEvent;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.entity.MyPetClass;
import de.keyle.knbt.TagCompound;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.Optional;

public class MyPetManager extends de.Keyle.MyPet.api.repository.MyPetManager {


    // Inactive -----------------------------------------------------------------

    public StoredMyPet getInactiveMyPetFromMyPet(StoredMyPet myPet) {
        InactiveMyPet inactiveMyPet = new InactiveMyPet(myPet.getOwner());
        inactiveMyPet.setUUID(myPet.getUUID());
        inactiveMyPet.setPetName(myPet.getPetName());
        inactiveMyPet.setWorldGroup(myPet.getWorldGroup());
        inactiveMyPet.setExp(myPet.getExp());
        inactiveMyPet.setHealth(myPet.getHealth());
        inactiveMyPet.setSaturation(myPet.getSaturation());
        inactiveMyPet.setRespawnTime(myPet.getRespawnTime());
        inactiveMyPet.setSkills(myPet.getSkillInfo());
        inactiveMyPet.setInfo(myPet.getInfo());
        inactiveMyPet.setPetType(myPet.getPetType());
        inactiveMyPet.setSkilltree(myPet.getSkilltree());
        inactiveMyPet.setLastUsed(myPet.getLastUsed());
        inactiveMyPet.wantsToRespawn = myPet.wantsToRespawn();

        return inactiveMyPet;
    }

    // All ----------------------------------------------------------------------

    public Optional<MyPet> activateMyPet(StoredMyPet storedMyPet) {
        if (storedMyPet == null) {
            return Optional.empty();
        }
        
        if (!storedMyPet.getOwner().isOnline()) {
            return Optional.empty();
        }

        if (storedMyPet.getOwner().hasMyPet()) {
            if (!deactivateMyPet(storedMyPet.getOwner(), true)) {
                return Optional.empty();
            }
        }

        Event event = new MyPetLoadEvent(storedMyPet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        MyPet myPet = MyPetClass.getByMyPetType(storedMyPet.getPetType()).getNewMyPetInstance(storedMyPet.getOwner());
        myPet.setUUID(storedMyPet.getUUID());
        myPet.setPetName(storedMyPet.getPetName());
        myPet.setRespawnTime(storedMyPet.getRespawnTime());
        myPet.setWorldGroup(storedMyPet.getWorldGroup());
        myPet.setInfo(storedMyPet.getInfo());
        myPet.setLastUsed(storedMyPet.getLastUsed());
        myPet.setWantsToRespawn(storedMyPet.wantsToRespawn());
        myPet.getExperience().setExp(storedMyPet.getExp());
        myPet.setSkilltree(storedMyPet.getSkilltree());
        Collection<Skill> skills = myPet.getSkills().all();
        if (skills.size() > 0) {
            for (Skill skill : skills) {
                if (skill instanceof NBTStorage) {
                    NBTStorage storageSkill = (NBTStorage) skill;
                    if (storedMyPet.getSkillInfo().getCompoundData().containsKey(skill.getName())) {
                        storageSkill.load(storedMyPet.getSkillInfo().getAs(skill.getName(), TagCompound.class));
                    }
                }
            }
        }
        myPet.setHealth(storedMyPet.getHealth());
        myPet.setSaturation(storedMyPet.getSaturation());

        mActivePetsPlayer.put(myPet, myPet.getOwner());


        event = new MyPetActivatedEvent(myPet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        return Optional.of(myPet);
    }
}