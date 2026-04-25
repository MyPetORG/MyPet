package de.Keyle.MyPet.entity.spawn;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.util.Timer;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.entity.visual.CreakingActivationSuppressor;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.entity.visual.PetVisualSyncer;
import de.Keyle.MyPet.entity.visual.WitherAutonomousAttackSuppressor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Spawns a Bukkit {@link Mob} for a {@link MyPet}, configures it
 * (persistence, attributes, PDC marker), strips vanilla AI, and installs MyPet goals
 * via {@link PetGoalInstaller}.
 */
public final class VanillaMobSpawner {

    /**
     * Spawns the pet as a Bukkit mob. Returns true on success.
     * On failure (no Bukkit class for this type, or no valid spawn position),
     * logs a warning and returns false.
     */
    public boolean spawn(MyPet pet, Location loc) {
        Class<? extends Mob> mobClass = pet.getPetType().getBukkitEntityClass();
        if (mobClass == null) {
            MyPetApi.getLogger().warning("No Bukkit entity class for pet type " + pet.getPetType().name());
            return false;
        }

        Location target = findValidSpawnLocation(loc, mobClass);
        if (target == null) {
            return false;
        }

        Mob spawned = target.getWorld().spawn(target, mobClass, CreatureSpawnEvent.SpawnReason.CUSTOM, mob -> {
            configureMob(pet, mob);
        });

        return true;
    }

    /**
     * Converts an existing vanilla mob in-place into a MyPet. Used during the
     * leash/tame flow — the mob already exists in the world, so no
     * {@code world.spawn()} call is needed. The mob's existing visual state
     * (colour, variant, etc.) is preserved because the entity is the same.
     *
     * <p>Responsibilities: strip vanilla AI, install MyPet goals, configure
     * attributes, mark with PDC, wire the mob into the MyPet domain object.
     */
    public void convertInPlace(MyPet pet, Mob mob) {
        configureMob(pet, mob);
    }

    /**
     * Releases a MyPet back to the wild as a fresh vanilla mob with full
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
     *   <li>Visual state applied via {@link PetVisualSyncer#sync(MyPet, Mob, boolean)}
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
     * <p>Ownership of the {@link MyPet}'s {@code bukkitEntity} reference is
     * transferred away (set to {@code null}) so a subsequent
     * {@link MyPet#removePet()} does not call {@code .remove()} on either
     * the old (already-removed) mob or the new (wild) mob. The damage
     * tracker entry for the old UUID is cleaned up here because
     * {@code removePet()} will no longer see the reference.
     */
    public void releaseToWild(MyPet pet) {
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

        // Detach MyPet state BEFORE any potentially-throwing operation below.
        // If world.spawn() throws (plugin-cancelled CreatureSpawnEvent,
        // world-unloaded races, etc.), the MyPet domain object is already in
        // a consistent "detached" state, so the caller's exception handler
        // just needs to finish the repository/worldgroup cleanup — it does
        // not inherit a dead entity reference.
        PetDamageTracker.cleanup(oldUuid);
        Timer.stopPetTicking(pet);
        PetSitParticleController.stopForPet(pet);
        PetPotionParticleController.stopForPet(pet);
        RideSkillFlightController.stopForPet(pet);
        CreakingActivationSuppressor.stopForPet(pet);
        WitherAutonomousAttackSuppressor.stopForPet(pet);
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

    private void configureMob(MyPet pet, Mob mob) {
        // Wire the mob into the MyPet domain object FIRST, so
        // pet.getPetNavigation() returns a valid PaperNavigation when the goal
        // classes fetch it during construction below. Doing this after goal
        // install caused goals to store a null nav reference and NPE on tick.
        pet.setBukkitEntity(mob);

        // persistent=false means the entity is NOT written to the world save
        // file on chunk unload — MyPet owns canonical pet state in its repo,
        // and the entity is re-spawned from that state on owner relogin.
        // Combined with removeWhenFarAway=false so the entity doesn't
        // auto-despawn while its chunk is loaded and the owner is online.
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        mob.setAI(true);
        mob.setCanPickupItems(false);
        PetEntityMarker.mark(mob);

        AttributeInstance health = mob.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(1.0, pet.getMaxHealth()));
            mob.setHealth(Math.max(1.0, Math.min(pet.getHealth(), health.getValue())));
        }

        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(MyPetApi.getMyPetInfo().getSpeed(pet.getPetType()));
        }

        // Initial visual state — applied inside the spawn consumer so the
        // correct colour/variant/profession/etc. lands in the initial spawn
        // packet (no default-flash). Must happen before goal install so goals
        // that inspect mob state see the final values.
        PetVisualSyncer.sync(pet, mob);
        pet.updateNameTag();

        PetGoalInstaller.install(pet, mob);

        // Equipment sync — lifted from plugin/entity/MyPet.java:645-650.
        if (pet instanceof MyPetEquipment equipmentPet) {
            EntityEquipment eq = mob.getEquipment();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                eq.setItem(slot, equipmentPet.getEquipment(slot));
            }
        }

        // Folia: start per-pet scheduler tasks.
        Timer.startPetTicking(pet);
        PetSitParticleController.startForPet(pet);
        PetPotionParticleController.startForPet(pet);
        RideSkillFlightController.startForPet(pet);
        CreakingActivationSuppressor.startForPet(pet);
        WitherAutonomousAttackSuppressor.startForPet(pet);
    }

    /**
     * Pre-spawn position search
     */
    private Location findValidSpawnLocation(Location origin, Class<? extends Mob> mobClass) {
        if (MyPetApi.getPlatformHelper().canSpawn(origin, mobClass)) {
            return origin;
        }
        Location loc = origin.clone().subtract(1, 0, 1);
        for (double x = 0; x <= 2; x += 0.5) {
            for (double z = 0; z <= 2; z += 0.5) {
                if (x != 1 && z != 1) {
                    if (MyPetApi.getPlatformHelper().canSpawn(loc, mobClass)) {
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
