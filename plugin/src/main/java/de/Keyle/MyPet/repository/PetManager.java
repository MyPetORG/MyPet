/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.event.PetActivatedEvent;
import de.Keyle.MyPet.api.event.PetLoadEvent;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.PetInfoAccess;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHookRegistry;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import de.Keyle.MyPet.entity.visual.PetNoPushSuppressor;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.util.Timer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PetManager extends de.Keyle.MyPet.api.repository.PetManager {


    // Inactive -----------------------------------------------------------------

    @Override
    public PersistedPet snapshot(Pet pet) {
        return PersistedPet.builder(pet.getOwner())
                .uuid(pet.getUUID())
                .petType(pet.getPetType())
                .petName(pet.getPetName())
                .worldGroup(pet.getWorldGroup())
                .exp(pet.getExp())
                .health(pet.getHealth())
                .saturation(pet.getSaturation())
                .respawnTime(pet.getRespawnTime())
                .wantsToRespawn(pet.wantsToRespawn())
                .lastUsed(pet.getLastUsed())
                .skilltree(pet.getSkilltree())
                .skillInfo(PetInfoAccess.readSkillInfo(pet))
                .info(PetInfoAccess.read(pet))
                .build();
    }

    // All ----------------------------------------------------------------------

    public Optional<Pet> activatePet(StoredPet storedPet) {
        if (storedPet == null) {
            return Optional.empty();
        }

        if (!storedPet.getOwner().isOnline()) {
            return Optional.empty();
        }

        if (storedPet.getOwner().hasPet()) {
            if (!deactivatePet(storedPet.getOwner(), true)) {
                return Optional.empty();
            }
        }

        Event event = new PetLoadEvent(storedPet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        Pet pet = createMyPetInstance(storedPet.getPetType(), storedPet.getOwner());
        if (pet == null) {
            return Optional.empty();
        }
        pet.setUUID(storedPet.getUUID());
        pet.setPetName(storedPet.getPetName());
        pet.setRespawnTime(storedPet.getRespawnTime());
        pet.setWorldGroup(storedPet.getWorldGroup());
        PetInfoAccess.write(pet, PetInfoAccess.read(storedPet));
        pet.setLastUsed(storedPet.getLastUsed());
        pet.setWantsToRespawn(storedPet.wantsToRespawn());
        pet.getExperience().setExp(storedPet.getExp());
        pet.setSkilltree(storedPet.getSkilltree());
        Collection<Skill> skills = pet.getSkills().all();
        if (!skills.isEmpty()) {
            CompoundBinaryTag skillInfo = PetInfoAccess.readSkillInfo(storedPet);
            SkillManager mgr = MyPetApi.getSkillManager();
            for (Skill skill : skills) {
                if (skillInfo.keySet().contains(skill.getName())) {
                    mgr.loadSkillState(skill, skillInfo.getCompound(skill.getName()));
                }
            }
        }
        pet.setHealth(storedPet.getHealth());
        pet.setSaturation(storedPet.getSaturation());

        mActivePetsPlayer.put(pet, pet.getOwner());


        event = new PetActivatedEvent(pet);
        Bukkit.getServer().getPluginManager().callEvent(event);

        return Optional.of(pet);
    }

    @Override
    public boolean deactivatePet(MyPetPlayer owner, boolean update) {
        if (mActivePlayerPets.containsKey(owner)) {
            final Pet pet = owner.getPet();

            PetSaveEvent event = new PetSaveEvent(pet);
            Bukkit.getServer().getPluginManager().callEvent(event);

            pet.removePet();
            if (update) {
                MyPetPlugin.getInstance().getRepository().updatePet(pet);
            }
            mActivePetsPlayer.remove(pet);
            return true;
        }
        return false;
    }

    @Override
    public CompletableFuture<List<StoredPet>> getStoredPets(MyPetPlayer owner) {
        return MyPetPlugin.getInstance().getRepository().getPets(owner);
    }

    /**
     * Re-types a live, active Pet to a new {@link PetType} by binding it
     * to the entity vanilla just produced from a transformation (Hoglin →
     * Zoglin, Piglin/PiglinBrute → ZombifiedPiglin). The old {@link Pet}
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
     * <p>State copy is parallel to {@link #activatePet}'s
     * {@link StoredPet} → {@link Pet} hydration, minus the snapshot
     * (the new entity replaces it). Health is clamped to the new type's
     * max via {@link Pet#setHealth} after the status flips to
     * {@link PetState#Here}.
     *
     * <p>Does not fire {@code PetSaveEvent} or {@code PetRemoveEvent}
     * for the old pet — this is a transformation, not a removal — but does
     * fire {@link PetActivatedEvent} for the new pet so listeners that
     * track active pets see the swap.
     *
     * @return the new {@link Pet} on success, or empty if the new
     *         instance could not be created or the types are equal
     */
    public Optional<Pet> convertPetType(Pet oldPet, PetType newType, Mob newEntity) {
        if (oldPet == null || newType == null || newEntity == null) {
            return Optional.empty();
        }
        if (oldPet.getPetType().equals(newType)) {
            return Optional.empty();
        }

        Pet newPet = createMyPetInstance(newType, oldPet.getOwner());
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
            SkillManager mgr = MyPetApi.getSkillManager();
            for (Skill skill : newSkills) {
                if (skillInfo.keySet().contains(skill.getName())) {
                    mgr.loadSkillState(skill, skillInfo.getCompound(skill.getName()));
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
        PetLifecycleHookRegistry.forPet(oldPet).forEach(hook -> hook.onDespawn(oldPet));
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

        // Apply the Pet pipeline to the new entity: marks the entity,
        // strips vanilla AI, installs Pet goals, configures attributes
        // and persistence flags, starts the per-pet tickers.
        new VanillaMobSpawner().convertInPlace(newPet, newEntity);

        // Promote status without going through createEntity (the entity is
        // already bound by convertInPlace). Mirrors the post-spawn block in
        // Pet#respawnPet so plugin hooks see the same metadata. The
        // updateStatus cast is the same shape as EntityListener:378 — the
        // method lives on the plugin's concrete PetImpl, not the api facet.
        ((PetImpl) newPet).updateStatus(PetState.Here);
        newEntity.setMetadata("MyPet", new FixedMetadataValue(MyPetApi.getPlugin(), true));

        // Carry health/saturation across — must happen AFTER status flips
        // because Pet#setHealth only writes through to the live entity
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

    private static Pet createMyPetInstance(PetType type, MyPetPlayer owner) {
        try {
            Constructor<? extends Pet> ctor = type.getPetClass().getConstructor(MyPetPlayer.class);
            return ctor.newInstance(owner);
        } catch (Exception e) {
            ErrorUtil.reportError("Failed to create PetImpl instance for " + type.name(), e);
            return null;
        }
    }
}
