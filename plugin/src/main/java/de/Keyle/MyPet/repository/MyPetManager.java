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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetActivatedEvent;
import de.Keyle.MyPet.api.event.MyPetLoadEvent;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.entity.PersistedMyPet;
import de.Keyle.MyPet.util.CompatUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MyPetManager extends de.Keyle.MyPet.api.repository.MyPetManager {


    // Inactive -----------------------------------------------------------------

    @Override
    public PersistedMyPet getInactiveMyPetFromMyPet(StoredMyPet myPet) {
        return PersistedMyPet.builder(myPet.getOwner())
                .uuid(myPet.getUUID())
                .petType(myPet.getPetType())
                .petName(myPet.getPetName())
                .worldGroup(myPet.getWorldGroup())
                .exp(myPet.getExp())
                .health(myPet.getHealth())
                .saturation(myPet.getSaturation())
                .respawnTime(myPet.getRespawnTime())
                .wantsToRespawn(myPet.wantsToRespawn())
                .lastUsed(myPet.getLastUsed())
                .skilltree(myPet.getSkilltree())
                .skillInfo(myPet.getSkillInfo())
                .info(myPet.getInfo())
                .build();
    }

    // All ----------------------------------------------------------------------

    public Optional<MyPet> activateMyPet(StoredMyPet storedMyPet) {
        if (storedMyPet == null) {
            return Optional.empty();
        }

        if (storedMyPet.getPetType().equals(MyPetType.byName("EnderDragon")) && CompatUtil.minecraftVersionEqualsOrAbove("1.21.4"))
            return Optional.empty();

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

        MyPet myPet = createMyPetInstance(storedMyPet.getPetType(), storedMyPet.getOwner());
        if (myPet == null) {
            return Optional.empty();
        }
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
        if (!skills.isEmpty()) {
            CompoundBinaryTag skillInfo = storedMyPet.getSkillInfo();
            for (Skill skill : skills) {
                if (skill instanceof NBTStorage storageSkill) {
                    if (skillInfo.keySet().contains(skill.getName())) {
                        storageSkill.load(skillInfo.getCompound(skill.getName()));
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

    @Override
    public boolean deactivateMyPet(MyPetPlayer owner, boolean update) {
        if (mActivePlayerPets.containsKey(owner)) {
            final MyPet myPet = owner.getMyPet();

            MyPetSaveEvent event = new MyPetSaveEvent(myPet);
            Bukkit.getServer().getPluginManager().callEvent(event);

            myPet.removePet();
            if (update) {
                MyPetPlugin.getInstance().getRepository().updatePet(myPet);
            }
            mActivePetsPlayer.remove(myPet);
            return true;
        }
        return false;
    }

    @Override
    public CompletableFuture<List<StoredMyPet>> getStoredPets(MyPetPlayer owner) {
        return MyPetPlugin.getInstance().getRepository().getPets(owner);
    }

    private static MyPet createMyPetInstance(MyPetType type, MyPetPlayer owner) {
        String className = "de.Keyle.MyPet.entity.types.My" + type.name();
        try {
            Class<?> clazz = Class.forName(className);
            return (MyPet) clazz.getConstructor(MyPetPlayer.class).newInstance(owner);
        } catch (Exception e) {
            ErrorUtil.reportError("Failed to create MyPet instance for " + type.name(), e);
            return null;
        }
    }
}
