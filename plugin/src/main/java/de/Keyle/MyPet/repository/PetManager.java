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
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.PetActivatedEvent;
import de.Keyle.MyPet.api.event.PetLoadEvent;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.entity.PersistedMyPet;
import de.Keyle.MyPet.entity.PetInfoAccess;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import de.Keyle.MyPet.entity.visual.CreakingActivationSuppressor;
import de.Keyle.MyPet.entity.visual.PetEnderDragonHoverController;
import de.Keyle.MyPet.entity.visual.PetNoPushSuppressor;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.entity.visual.WitherAutonomousAttackSuppressor;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.util.Timer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PetManager extends de.Keyle.MyPet.api.repository.PetManager {


    // Inactive -----------------------------------------------------------------

    @Override
    public PersistedMyPet snapshot(MyPet myPet) {
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
                .skillInfo(PetInfoAccess.readSkillInfo(myPet))
                .info(PetInfoAccess.read(myPet))
                .build();
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

        Event event = new PetLoadEvent(storedMyPet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        MyPet myPet = createMyPetInstance(storedMyPet.getPetType(), storedMyPet.getOwner());
        if (myPet == null) {
            return Optional.empty();
        }
        myPet.setUUID(storedMyPet.getUUID());
        myPet.setPetName(storedMyPet.getPetName());
        myPet.setRespawnTime(storedMyPet.getRespawnTime());
        myPet.setWorldGroup(storedMyPet.getWorldGroup());
        PetInfoAccess.write(myPet, PetInfoAccess.read(storedMyPet));
        myPet.setLastUsed(storedMyPet.getLastUsed());
        myPet.setWantsToRespawn(storedMyPet.wantsToRespawn());
        myPet.getExperience().setExp(storedMyPet.getExp());
        myPet.setSkilltree(storedMyPet.getSkilltree());
        Collection<Skill> skills = myPet.getSkills().all();
        if (!skills.isEmpty()) {
            CompoundBinaryTag skillInfo = PetInfoAccess.readSkillInfo(storedMyPet);
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


        event = new PetActivatedEvent(myPet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        return Optional.of(myPet);
    }

    @Override
    public boolean deactivateMyPet(MyPetPlayer owner, boolean update) {
        if (mActivePlayerPets.containsKey(owner)) {
            final MyPet myPet = owner.getMyPet();

            PetSaveEvent event = new PetSaveEvent(myPet);
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

    /**
     * Re-types a live, active MyPet to a new {@link MyPetType} by binding it
     * to the entity vanilla just produced from a transformation (Hoglin →
     * Zoglin, Piglin/PiglinBrute → ZombifiedPiglin). The old {@link MyPet}
     * domain object is discarded; a fresh instance of the new type takes
     * its place in the active-pets map with the same UUID, name, XP, skill
     * state, owner, and persisted database row.
     *
     * <p>Caller responsibility: invoke from inside the
     * {@code EntityTransformEvent} handler WITHOUT cancelling the event, so
     * vanilla discards the source entity and adds the new entity to the
     * world after the handler returns. This method binds the new entity
     * pre-spawn (it isn't in the world yet at event time) — Paper's MobGoals
     * and entity setters operate on the entity object directly, so the
     * configuration applies before the spawn packet is sent.
     *
     * <p>State copy is parallel to {@link #activateMyPet}'s
     * {@link StoredMyPet} → {@link MyPet} hydration, minus the snapshot
     * (the new entity replaces it). Health is clamped to the new type's
     * max via {@link MyPet#setHealth} after the status flips to
     * {@link PetState#Here}.
     *
     * <p>Does not fire {@code PetSaveEvent} or {@code PetRemoveEvent}
     * for the old pet — this is a transformation, not a removal — but does
     * fire {@link PetActivatedEvent} for the new pet so listeners that
     * track active pets see the swap.
     *
     * @return the new {@link MyPet} on success, or empty if the new
     *         instance could not be created or the types are equal
     */
    public Optional<MyPet> convertPetType(MyPet oldPet, MyPetType newType, Mob newEntity) {
        if (oldPet == null || newType == null || newEntity == null) {
            return Optional.empty();
        }
        if (oldPet.getPetType().equals(newType)) {
            return Optional.empty();
        }

        MyPet newPet = createMyPetInstance(newType, oldPet.getOwner());
        if (newPet == null) {
            return Optional.empty();
        }

        // Copy persistent state (everything that survives a /petsendaway
        // round-trip). Order doesn't matter here since the new entity
        // isn't bound yet — these are pure field assignments on newPet.
        newPet.setUUID(oldPet.getUUID());
        newPet.setPetName(oldPet.getPetName());
        newPet.setRespawnTime(oldPet.getRespawnTime());
        newPet.setWorldGroup(oldPet.getWorldGroup());
        newPet.setLastUsed(oldPet.getLastUsed());
        newPet.setWantsToRespawn(oldPet.wantsToRespawn());
        newPet.getExperience().setExp(oldPet.getExp());
        newPet.setSkilltree(oldPet.getSkilltree());

        Collection<Skill> newSkills = newPet.getSkills().all();
        if (!newSkills.isEmpty()) {
            CompoundBinaryTag skillInfo = PetInfoAccess.readSkillInfo(oldPet);
            for (Skill skill : newSkills) {
                if (skill instanceof NBTStorage storageSkill
                        && skillInfo.keySet().contains(skill.getName())) {
                    storageSkill.load(skillInfo.getCompound(skill.getName()));
                }
            }
        }

        // Detach the OLD pet's tickers and entity reference. Vanilla will
        // discard the source entity right after the EntityTransformEvent
        // handler returns; without this detach, oldPet.removePet (if it
        // ever runs) would try to capture a snapshot from an already-dead
        // entity and clean up the entity we're binding to newPet.
        UUID oldEntityUuid = oldPet.getBukkitEntity() != null
                ? oldPet.getBukkitEntity().getUniqueId() : null;
        PetSitParticleController.stopForPet(oldPet);
        PetPotionParticleController.stopForPet(oldPet);
        RideSkillFlightController.stopForPet(oldPet);
        CreakingActivationSuppressor.stopForPet(oldPet);
        WitherAutonomousAttackSuppressor.stopForPet(oldPet);
        PetEnderDragonHoverController.stopForPet(oldPet);
        PetNoPushSuppressor.stopForPet(oldPet);
        Timer.stopPetTicking(oldPet);
        if (oldEntityUuid != null) {
            PetDamageTracker.cleanup(oldEntityUuid);
        }
        oldPet.setBukkitEntity(null);

        // Swap the active-pets registry. mActivePetsPlayer is the inverse
        // view of mActivePlayerPets, so updating one updates both.
        mActivePetsPlayer.remove(oldPet);
        mActivePetsPlayer.put(newPet, newPet.getOwner());

        // Apply the MyPet pipeline to the new entity: marks the entity,
        // strips vanilla AI, installs MyPet goals, configures attributes
        // and persistence flags, starts the per-pet tickers.
        new VanillaMobSpawner().convertInPlace(newPet, newEntity);

        // Promote status without going through createEntity (the entity is
        // already bound by convertInPlace). Mirrors the post-spawn block in
        // MyPet#respawnPet so plugin hooks see the same metadata. The
        // updateStatus cast is the same shape as EntityListener:378 — the
        // method lives on the plugin's concrete MyPet, not the api facet.
        ((de.Keyle.MyPet.entity.MyPet) newPet).updateStatus(PetState.Here);
        newEntity.setMetadata("MyPet", new FixedMetadataValue(MyPetApi.getPlugin(), true));

        // Carry health/saturation across — must happen AFTER status flips
        // because MyPet#setHealth only writes through to the live entity
        // when status == Here. Health is clamped against the new type's
        // max inside setHealth.
        newPet.setHealth(oldPet.getHealth());
        newPet.setSaturation(oldPet.getSaturation());

        // Persist the new type. Same UUID → repository-side UPDATE, not
        // INSERT — the database row's `type` column flips while everything
        // else stays linked.
        MyPetPlugin.getInstance().getRepository().updatePet(newPet);

        Bukkit.getServer().getPluginManager().callEvent(new PetActivatedEvent(newPet));

        return Optional.of(newPet);
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
