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

package de.Keyle.MyPet.entity.spawn;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.entity.PetAttributes;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHookRegistry;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.entity.visual.PetNoPushSuppressor;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.entity.visual.PetVisualSyncer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Spawns a Bukkit {@link Mob} for a {@link Pet}, configures it
 * (persistence, attributes, PDC marker), strips vanilla AI, and installs MyPet goals
 * via {@link PetGoalInstaller}.
 */
public final class VanillaMobSpawner {

    /**
     * Spawns the pet as a Bukkit mob. Returns true on success.
     * On failure (no Bukkit class for this type, or no valid spawn position),
     * logs a warning and returns false.
     */
    public boolean spawn(Pet pet, Location loc) {
        Class<? extends Mob> mobClass = pet.getPetType().getBukkitEntityClass();
        if (mobClass == null) {
            MyPetApi.getLogger().warning("No Bukkit entity class for pet type " + pet.getPetType().name());
            return false;
        }

        Location target = findValidSpawnLocation(loc, mobClass);
        if (target == null) {
            return false;
        }

        // Snapshot path: deserialize the vanilla mob from a captured NBT
        // compound (every saved pet has one after the EntitySnapshot migration
        // runs at startup). Fresh-spawn fallback covers (a) brand new pets
        // that have never been saved, and (b) defensive recovery if the
        // snapshot fails to deserialize.
        CompoundBinaryTag snapshot = pet.consumePendingSnapshot();
        if (snapshot != null) {
            try {
                Mob restored = PetEntitySnapshot.restore(snapshot, target.getWorld());
                if (!mobClass.isInstance(restored)) {
                    // Snapshot disagrees with stored pet type (e.g. /petadmin
                    // changed the type after the snapshot was taken). Discard
                    // the deserialized object (never entered the world — see
                    // PetEntitySnapshot.restore) and fall through to fresh-spawn.
                    MyPetApi.getLogger().warning("Snapshot type mismatch for pet "
                            + pet.getUUID() + " — expected " + mobClass.getSimpleName()
                            + " but got " + restored.getClass().getSimpleName()
                            + ". Falling back to fresh-spawn.");
                } else if (restored.spawnAt(target, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
                    // deserializeEntity returns a detached Entity object
                    // NOT in the world; spawnAt is what actually places it.
                    // Skipping this call would leave a ghost entity (no errors,
                    // pet just never appears). spawnAt also fires CreatureSpawnEvent.
                    configureMob(pet, restored, true);
                    return true;
                } else {
                    // spawnAt returned false — another plugin canceled the
                    // CreatureSpawnEvent, or the entity was already spawned/despawned.
                    MyPetApi.getLogger().warning("Snapshot-restored mob refused to spawn for pet "
                            + pet.getUUID() + " — falling back to fresh-spawn.");
                }
            } catch (Throwable t) {
                MyPetApi.getLogger().warning("Failed to restore EntitySnapshot for pet "
                        + pet.getUUID() + " — falling back to fresh-spawn. " + t.getMessage());
            }
        }

        target.getWorld().spawn(target, mobClass, CreatureSpawnEvent.SpawnReason.CUSTOM, mob -> {
            configureMob(pet, mob, false);
        });
        return true;
    }

    /**
     * Converts an existing vanilla mob in-place into a Pet. Used during the
     * leash/tame flow — the mob already exists in the world, so no
     * {@code world.spawn()} call is needed. The mob's existing visual state
     * (colour, variant, etc.) is preserved because the entity is the same.
     *
     * <p>Responsibilities: strip vanilla AI, install Pet goals, configure
     * attributes, mark with PDC, wire the mob into the Pet domain object.
     */
    public void convertInPlace(Pet pet, Mob mob) {
        // Tame path: the wild mob already exists with its visual state intact;
        // treat it like a snapshot-restored pet so we don't clobber equipment.
        configureMob(pet, mob, true);
    }

    /**
     * Releases a Pet back to the wild as a fresh vanilla mob with full
     * vanilla AI intact. Implemented as destroy-and-respawn because Paper's
     * {@code MobGoals} API has no way to reinstate vanilla goals once they
     * have been stripped by {@link PetGoalInstaller#install}: the vanilla
     * goal list is built inside NMS's {@code Mob#registerGoals()} which only
     * runs at spawn time, and every path to obtaining a {@code Goal<T>}
     * instance via the public API yields a wrapper bound to a specific mob
     * The only working path is to ask Minecraft to build a brand-new mob via
     * {@link org.bukkit.World#spawn}.
     *
     * <p>State preserved across the respawn:
     * <ul>
     *   <li>Location (exact)</li>
     *   <li>Visual state applied via {@link PetVisualSyncer#sync(Pet, Mob, boolean)}
     *       with {@code applyTameable=false} — colour, variant, profession,
     *       baby/adult, etc., but NOT tamed/owner (the mob is wild) and NOT
     *       sit pose (a released mob should not spawn sitting)</li>
     *   <li>Equipment (items in all {@link EquipmentSlot}s)</li>
     * </ul>
     *
     * <p>State deliberately dropped: custom name/nametag visibility,
     * MyPet PDC marker, pet-scale MAX_HEALTH, pet-scale MOVEMENT_SPEED,
     * {@code setPersistent(false)} / {@code removeWhenFarAway(false)}.
     *
     * <p>Ownership of the {@link Pet}'s {@code bukkitEntity} reference is
     * transferred away (set to {@code null}) so a subsequent
     * {@link Pet#removePet()} does not call {@code .remove()} on either
     * the old (already-removed) mob or the new (wild) mob. The damage
     * tracker entry for the old UUID is cleaned up here because
     * {@code removePet()} will no longer see the reference.
     */
    public void releaseToWild(Pet pet) {
        Mob oldMob = pet.getBukkitEntity();
        if (oldMob == null) return;

        Location loc = oldMob.getLocation();
        UUID oldUuid = oldMob.getUniqueId();
        Class<? extends Mob> mobClass = pet.getPetType().getBukkitEntityClass();
        if (mobClass == null) {
            MyPetApi.getLogger().warning("releaseToWild: no Bukkit entity class for pet type "
                    + pet.getPetType().name() + " — cannot respawn as vanilla mob");
            PetDamageTracker.cleanup(oldUuid);
            pet.setBukkitEntity(null);
            return;
        }

        World world = loc.getWorld();
        if (world == null) {
            MyPetApi.getLogger().warning("releaseToWild: world unloaded before respawn for "
                    + pet.getPetType().name() + " — cannot spawn replacement");
            PetDamageTracker.cleanup(oldUuid);
            pet.setBukkitEntity(null);
            return;
        }

        // Capture equipment from the old mob BEFORE removing it.
        EntityEquipment oldEq = oldMob.getEquipment();
        ItemStack[] capturedEquipment = null;
        if (oldEq != null) {
            EquipmentSlot[] slots = EquipmentSlot.values();
            capturedEquipment = new ItemStack[slots.length];
            for (int i = 0; i < slots.length; i++) {
                capturedEquipment[i] = oldEq.getItem(slots[i]);
            }
        }

        // Detach Pet state BEFORE any potentially-throwing operation below.
        // If world.spawn() throws (plugin-cancelled CreatureSpawnEvent,
        // world-unloaded races, etc.), the Pet domain object is already in
        // a consistent "detached" state, so the caller's exception handler
        // just needs to finish the repository/worldgroup cleanup — it does
        // not inherit a dead entity reference.
        PetDamageTracker.cleanup(oldUuid);
        Timer.stopPetTicking(pet);
        PetSitParticleController.stopForPet(pet);
        PetPotionParticleController.stopForPet(pet);
        RideSkillFlightController.stopForPet(pet);
        PetLifecycleHookRegistry.forPet(pet).forEach(hook -> hook.onDespawn(pet));
        pet.setBukkitEntity(null);

        // Safe to destroy the old mob now — MyPet already released the reference.
        oldMob.remove();

        // Spawn a fresh vanilla mob at the same location — this runs Minecraft's
        // native Mob#registerGoals() and gives the new entity a full set of
        // vanilla goals.
        Mob newMob = world.spawn(loc, mobClass, CreatureSpawnEvent.SpawnReason.CUSTOM);

        // Apply visual state (colour, variant, profession, baby/adult) but NOT
        // tamed/owner and NOT sit pose — the released mob is wild.
        PetVisualSyncer.sync(pet, newMob, false);

        // Drop the custom nametag.
        newMob.customName(null);
        newMob.setCustomNameVisible(false);

        // Transfer equipment from the old mob to the new one.
        if (capturedEquipment != null && newMob.getEquipment() != null) {
            EntityEquipment newEq = newMob.getEquipment();
            EquipmentSlot[] slots = EquipmentSlot.values();
            for (int i = 0; i < slots.length; i++) {
                ItemStack item = capturedEquipment[i];
                if (item != null && !item.getType().isAir()) {
                    newEq.setItem(slots[i], item);
                }
            }
        }
    }

    /**
     * @param mobHasPersistentState {@code true} if {@code mob} carries trustworthy
     *        prior state — the snapshot-restore path or the
     *        {@link #convertInPlace} tame path. We then preserve the mob's
     *        existing equipment instead of re-applying the (possibly empty)
     *        domain-side equipment cache. {@code false} for fresh world.spawn,
     *        in which case the domain cache is the only source of truth.
     */
    private void configureMob(Pet pet, Mob mob, boolean mobHasPersistentState) {
        // Wire the mob into the Pet domain object FIRST, so
        // pet.getPetNavigation() returns a valid PaperNavigation when the goal
        // classes fetch it during construction below. Doing this after goal
        // install caused goals to store a null nav reference and NPE on tick.
        pet.setBukkitEntity(mob);

        // persistent=false means the entity is NOT written to the world save
        // file on chunk unload — Pet owns canonical pet state in its repo,
        // and the entity is re-spawned from that state on owner relogin.
        // Combined with removeWhenFarAway=false so the entity doesn't
        // auto-despawn while its chunk is loaded and the owner is online.
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        mob.setAI(true);
        mob.setCanPickupItems(false);
        // Scrub vanilla state that, if carried across death/respawn via
        // PetEntitySnapshot's full-NBT round-trip, would re-apply lethal
        // damage on the next tick and trap the pet in a death loop:
        //   - Fire tag                  → fire damage tick
        //   - FallDistance + Motion.y   → fall damage tick
        //   - TicksFrozen               → freeze damage tick
        // Clearing unconditionally is intentional: a pet whose chunk
        // unloaded mid-burn / mid-fall / mid-freeze reappears in the clean
        // state, which is a purely cosmetic trade-off for a barely-observable
        // edge case — well worth avoiding a spawner-side
        // death-vs-reload discriminator.
        mob.setFireTicks(0);
        mob.setFallDistance(0f);
        mob.setVelocity(new Vector(0, 0, 0));
        mob.setFreezeTicks(0);
        PetEntityMarker.mark(mob);

        AttributeInstance health = mob.getAttribute(PetAttributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(1.0, pet.getMaxHealth()));
            mob.setHealth(Math.max(1.0, Math.min(pet.getHealth(), health.getValue())));
        }

        AttributeInstance speed = mob.getAttribute(PetAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(MyPetApi.getPetInfo().getSpeed(pet.getPetType()));
        }

        // FLYING_SPEED attribute is intentionally left at vanilla's baseValue.
        // The Ride controller (RideSkillFlightController.resolveBaseSpeed)
        // either reads the live attribute (scaled by vanilla-physics
        // conversion) or uses PetInfo.getFlySpeed verbatim when
        // isOverrideFlySpeed is true — either way, third-party plugins
        // tuning the FLYING_SPEED attribute see no MyPet-side interference,
        // and the MyPet override stays inside MyPet's own resolution chain.

        // Initial visual state — applied inside the spawn consumer so the
        // correct colour/variant/profession/etc. lands in the initial spawn
        // packet (no default-flash). Goes through pet.updateVisuals() rather
        // than PetVisualSyncer.sync() directly so per-type overrides
        // (e.g. PetEnderman's permaScreaming reassertion) fire on respawn.
        pet.updateVisuals();
        pet.updateNameTag();

        PetGoalInstaller.install(pet, mob);

        // Equipment sync: only when the mob lacks trustworthy persistent
        // equipment of its own. Snapshot-restored and convert-in-place mobs
        // carry the canonical equipment in their inventory, and the domain
        // cache may be empty across server-restart cycles — applying it
        // unconditionally would clobber the snapshot.
        if (!mobHasPersistentState && pet instanceof PetEquipment equipmentPet) {
            EntityEquipment eq = mob.getEquipment();
            if (eq != null) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    eq.setItem(slot, equipmentPet.getEquipment(slot));
                }
            }
        }

        // Folia: start per-pet scheduler tasks.
        Timer.startPetTicking(pet);
        PetSitParticleController.startForPet(pet);
        PetPotionParticleController.startForPet(pet);
        RideSkillFlightController.startForPet(pet);
        PetLifecycleHookRegistry.forPet(pet).forEach(hook -> hook.onSpawn(pet));
        PetNoPushSuppressor.startForPet(pet);
    }

    /**
     * Pre-spawn position search
     */
    private Location findValidSpawnLocation(Location origin, Class<? extends Mob> mobClass) {
        if (origin != null && origin.getWorld() != null && origin.getBlock().isPassable()) {
            return origin;
        }
        Location loc = origin.clone().subtract(1, 0, 1);
        for (double x = 0; x <= 2; x += 0.5) {
            for (double z = 0; z <= 2; z += 0.5) {
                if (x != 1 && z != 1) {
                    if (loc.getWorld() != null && loc.getBlock().isPassable()) {
                        Block below = loc.getBlock().getRelative(BlockFace.DOWN);
                        if (below.getType().isSolid()) {
                            return loc;
                        }
                    }
                }
                loc.add(0, 0, 0.5);
            }
            loc.subtract(0, 0, 2);
            loc.add(0.5, 0, 0);
        }
        return null;
    }
}
