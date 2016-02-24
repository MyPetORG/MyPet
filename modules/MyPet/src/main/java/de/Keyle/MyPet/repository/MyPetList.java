/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.repository;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetSelectEvent;
import de.Keyle.MyPet.api.event.MyPetSelectEvent.NewStatus;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.entity.MyPetClass;
import de.keyle.knbt.TagCompound;

import java.util.Collection;

import static org.bukkit.Bukkit.getServer;

public class MyPetList extends de.Keyle.MyPet.api.repository.MyPetList {


    // Inactive -----------------------------------------------------------------

    public MyPet getInactiveMyPetFromMyPet(MyPet activeMyPet) {
        InactiveMyPet inactiveMyPet = new InactiveMyPet(activeMyPet.getOwner());
        inactiveMyPet.setUUID(activeMyPet.getUUID());
        inactiveMyPet.setPetName(activeMyPet.getPetName());
        inactiveMyPet.setExp(activeMyPet.getExp());
        inactiveMyPet.setHealth(activeMyPet.getHealth());
        inactiveMyPet.setHungerValue(activeMyPet.getHungerValue());
        inactiveMyPet.setRespawnTime(activeMyPet.getRespawnTime());
        inactiveMyPet.setSkills(activeMyPet.getSkillInfo());
        inactiveMyPet.setInfo(activeMyPet.getInfo());
        inactiveMyPet.setPetType(activeMyPet.getPetType());
        inactiveMyPet.setSkilltree(activeMyPet.getSkilltree());
        inactiveMyPet.setWorldGroup(activeMyPet.getWorldGroup());
        inactiveMyPet.setLastUsed(activeMyPet.getLastUsed());
        inactiveMyPet.wantsToRespawn = activeMyPet.wantsToRespawn();

        return inactiveMyPet;
    }

    // All ----------------------------------------------------------------------

    public ActiveMyPet activateMyPet(MyPet inactiveMyPet) {
        if (!inactiveMyPet.getOwner().isOnline()) {
            return null;
        }

        if (inactiveMyPet.getOwner().hasMyPet()) {
            if (!deactivateMyPet(inactiveMyPet.getOwner(), true)) {
                return null;
            }
        }

        MyPetSelectEvent event = new MyPetSelectEvent(inactiveMyPet, NewStatus.Active);
        getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            ActiveMyPet activeMyPet = MyPetClass.getByMyPetType(inactiveMyPet.getPetType()).getNewMyPetInstance(inactiveMyPet.getOwner());
            activeMyPet.setUUID(inactiveMyPet.getUUID());
            activeMyPet.setPetName(inactiveMyPet.getPetName());
            activeMyPet.setRespawnTime(inactiveMyPet.getRespawnTime());
            activeMyPet.setWorldGroup(inactiveMyPet.getWorldGroup());
            activeMyPet.setInfo(inactiveMyPet.getInfo());
            activeMyPet.setLastUsed(inactiveMyPet.getLastUsed());
            activeMyPet.setWantsToRespawn(inactiveMyPet.wantsToRespawn());

            activeMyPet.getExperience().setExp(inactiveMyPet.getExp());
            activeMyPet.setSkilltree(inactiveMyPet.getSkilltree());
            Collection<SkillInstance> skills = activeMyPet.getSkills().getSkills();
            if (skills.size() > 0) {
                for (SkillInstance skill : skills) {
                    if (skill instanceof NBTStorage) {
                        NBTStorage storageSkill = (NBTStorage) skill;
                        if (inactiveMyPet.getSkillInfo().getCompoundData().containsKey(skill.getName())) {
                            storageSkill.load(inactiveMyPet.getSkillInfo().getAs(skill.getName(), TagCompound.class));
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
}