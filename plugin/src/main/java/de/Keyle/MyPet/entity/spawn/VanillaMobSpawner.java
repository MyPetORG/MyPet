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
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.entity.PetArmorApplier;
import de.Keyle.MyPet.entity.PetAttributes;
import de.Keyle.MyPet.entity.ai.BrainAccess;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHookRegistry;
import de.Keyle.MyPet.entity.model.PetModelService;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.entity.types.ModelPet;
import de.Keyle.MyPet.entity.visual.PetNoPushSuppressor;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.api.util.hooks.types.PetModelSourceHook;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spawns a Bukkit {@link Mob} for a {@link Pet}, configures it
 * (persistence, attributes, PDC marker), strips vanilla AI, and installs MyPet goals
 * via {@link PetGoalInstaller}.
 */
public final class VanillaMobSpawner {

    /**
     * Max ticks to wait for a source plugin to finish applying its model to a freshly-spawned
     * source creature before adopting it (see {@link #adoptSourceWhenModeled}). Bounded so a
     * source that never applies a model still gets adopted rather than waiting forever.
     */
    private static final int SOURCE_MODEL_WAIT_MAX_TICKS = 40;

    /**
     * Spawns the pet as a Bukkit mob. Returns the outcome of the attempt so
     * callers can distinguish a region/plugin refusal from a genuine failure.
     * On failure (no Bukkit class for this type, or no valid spawn position),
     * logs a warning.
     */
    public SpawnOutcome spawn(Pet pet, Location loc) {
        Class<? extends Mob> mobClass = pet.getPetType().getBukkitEntityClass();
        if (mobClass == null) {
            MyPetApi.getLogger().warning("No Bukkit entity class for pet type " + pet.getPetType().name());
            return SpawnOutcome.FAILED;
        }

        Location target = findValidSpawnLocation(loc, mobClass);
        if (target == null) {
            return SpawnOutcome.NO_SPACE;
        }

        // Snapshot path: deserialize the vanilla mob from captured NBT
        // bytes (every saved pet has one after the EntitySnapshot migration
        // runs at startup). Fresh-spawn fallback covers (a) brand new pets
        // that have never been saved, and (b) defensive recovery if the
        // snapshot fails to deserialize.
        byte[] snapshot = pet.consumePendingSnapshot();
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
                    return SpawnOutcome.SUCCESS;
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

        // Fresh spawn (no snapshot). A source-driven type has no renderable Model block — its model
        // rides in from the real creature — so spawn that creature via its source plugin and adopt it
        // in place. After this first materialization the model rides in the pet's snapshot PDC, so
        // later respawns take the ordinary snapshot path above.
        if (PetModelService.isSourceDriven(pet)) {
            String sourceId = PetModelService.modelIdOf(pet);
            if (sourceId != null && !sourceId.isBlank()) {
                for (PetModelSourceHook src : MyPetApi.getServiceManager().getServices(PetModelSourceHook.class)) {
                    Optional<Mob> spawned = src.spawnSource(sourceId, target);
                    if (spawned.isPresent()) {
                        // Mark BEFORE adoption so the one-shot spawn animation plays on a genuine
                        // create. convertInPlace routes to configureMob(..., true), which skips
                        // markFreshSpawn — that keeps the tame path (leashing a wild source
                        // creature) silent; only create marks it here.
                        PetModelService.markFreshSpawn(pet);
                        // The source plugin applies its model asynchronously (e.g. MythicMobs runs
                        // an on-spawn ModelEngine mechanic off-thread). Adopting immediately would
                        // strip the mob's goals mid-flight and race the renderer's navigation-wrap
                        // into an NPE. Wait until the model lands, then adopt.
                        adoptSourceWhenModeled(pet, spawned.get(), SOURCE_MODEL_WAIT_MAX_TICKS);
                        return SpawnOutcome.SUCCESS;
                    }
                }
                MyPetApi.getLogger().warning("Source-driven pet " + pet.getPetType().name()
                        + " could not spawn source '" + sourceId + "'; falling back to host mob.");
            }
        }

        // createEntity + spawnAt rather than world.spawn(..., consumer): the
        // consumer form throws IllegalArgumentException when a listener cancels
        // the CreatureSpawnEvent, while spawnAt reports it as a plain false.
        // configureMob still runs pre-add — it must, so pet.updateVisuals()
        // lands the correct colour/variant/etc. in the initial spawn packet
        // (see the comment inside configureMob) — so a refusal here has to
        // unwind everything configureMob already started, mirroring
        // PetImpl#removeEntity's teardown order. bukkitEntity.remove() itself
        // is intentionally NOT called: the mob was never added to the world
        // (spawnAt refused it), matching the snapshot-restore-refused branch
        // above, which drops its detached entity the same way.
        // Note: with PetEnvironmentListener's MONITOR un-cancel restored, this DENIED branch
        // is reachable only if a third-party MONITOR-priority listener cancels after ours
        // (intra-priority order is plugin load order) — keep the unwind; it is defense-in-depth,
        // not dead code.
        Mob fresh = target.getWorld().createEntity(target, mobClass);
        configureMob(pet, fresh, false);
        if (!fresh.spawnAt(target, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            Timer.stopPetTicking(pet);
            PetSitParticleController.stopForPet(pet);
            RideSkillFlightController.stopForPet(pet);
            PetLifecycleHookRegistry.forPet(pet).forEach(hook -> hook.onDespawn(pet));
            PetNoPushSuppressor.stopForPet(pet);
            pet.setBukkitEntity(null);
            return SpawnOutcome.DENIED;
        }
        return SpawnOutcome.SUCCESS;
    }

    /**
     * Converts an existing vanilla mob in-place into a Pet. Used during the
     * leash/tame flow and by the source-driven create path (adopting a creature
     * just spawned by its source plugin) — the mob already exists in the world,
     * so no {@code world.spawn()} call is needed. The mob's existing visual state
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
     * Adopts a source-spawned creature (e.g. a MythicMob) into {@code pet} once its source plugin
     * has applied the model, polling the mob's own region scheduler (Folia-safe). Adoption strips
     * the mob's goals; deferring it until the model is present keeps that strip from racing the
     * source renderer's asynchronous navigation-wrap, which NPEs on a goal-stripped mob. Falls
     * back to adopting bare once {@code ticksLeft} is exhausted, so a source that never applies a
     * model still yields a usable pet.
     */
    private void adoptSourceWhenModeled(Pet pet, Mob mob, int ticksLeft) {
        if (!mob.isValid()) {
            MyPetApi.getLogger().warning("Source-driven pet " + pet.getPetType().name()
                    + " lost its source creature before it could be adopted.");
            return;
        }
        if (ticksLeft <= 0 || !PetModelService.currentModels(mob).isEmpty()) {
            convertInPlace(pet, mob);
            return;
        }
        mob.getScheduler().runDelayed(MyPetApi.getPlugin(),
                task -> adoptSourceWhenModeled(pet, mob, ticksLeft - 1), null, 1L);
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
     * <p>State preserved automatically via {@link PetEntitySnapshot}'s vanilla
     * NBT round-trip:
     * <ul>
     *   <li>All per-type visual state (Villager profession/type/level/XP,
     *       Cat collar + variant, Wolf collar + variant, Sheep colour +
     *       sheared state, Mooshroom variant, Frog / Axolotl / Salmon /
     *       TropicalFish / Cow / Chicken / Pig / Panda / Fox per-type flags,
     *       Horse coat + style + armor + saddle, Llama strength + decor,
     *       Camel decor, Bee nectar + pollination cooldowns, Goat horns,
     *       Fox sleeping state, CopperGolem oxidation state, Allay inventory,
     *       Villager / Piglin inventories, etc.)</li>
     *   <li>Equipment in every slot</li>
     *   <li>Baby/adult</li>
     *   <li>Location (exact)</li>
     * </ul>
     *
     * <p>State stripped from the restored entity ({@link #stripPetCustomizations}):
     * <ul>
     *   <li>Custom name + nametag visibility</li>
     *   <li>All {@code mypet}-namespace PDC keys (the {@code PetEntityMarker}
     *       marker plus any per-pet flags like Enderman's perma-screaming)</li>
     *   <li>Tameable / owner state, sitting pose</li>
     *   <li>Wither boss-bar visibility (pet hides it; wild should show)</li>
     *   <li>All attribute modifiers + attribute base values reset to vanilla
     *       defaults (Life skill MAX_HEALTH, Sprint MOVEMENT_SPEED, Damage
     *       skill ATTACK_DAMAGE, etc. all disappear)</li>
     *   <li>Health clamped to the post-reset max</li>
     *   <li>Persistence / removeWhenFarAway / AI / canPickupItems flags
     *       reset to vanilla defaults</li>
     *   <li>Brain {@code WALK_TARGET} and {@code ATTACK_TARGET} memories
     *       (so a released villager doesn't keep targeting the owner's bed
     *       or a still-cached attack target)</li>
     * </ul>
     *
     * <p>Ownership of the {@link Pet}'s {@code bukkitEntity} reference is
     * transferred away (set to {@code null}) so a subsequent
     * {@link Pet#removePet()} does not call {@code .remove()} on either
     * the old (already-removed) mob or the new (wild) mob. The damage
     * tracker entry for the old UUID is cleaned up here because
     * {@code removePet()} will no longer see the reference.
     */
    public SpawnOutcome releaseToWild(Pet pet) {
        Mob oldMob = pet.getBukkitEntity();
        if (oldMob == null) return SpawnOutcome.FAILED;

        Location loc = oldMob.getLocation();
        UUID oldUuid = oldMob.getUniqueId();

        if (pet.getPetType().getPetClass() == ModelPet.class) {
            return releaseModelPet(pet, oldMob, loc, oldUuid);
        }

        Class<? extends Mob> mobClass = pet.getPetType().getBukkitEntityClass();
        if (mobClass == null) {
            MyPetApi.getLogger().warning("releaseToWild: no Bukkit entity class for pet type "
                    + pet.getPetType().name() + " — cannot respawn as vanilla mob");
            return SpawnOutcome.FAILED;
        }

        World world = loc.getWorld();
        if (world == null) {
            MyPetApi.getLogger().warning("releaseToWild: world unloaded before respawn for "
                    + pet.getPetType().name() + " — cannot spawn replacement");
            return SpawnOutcome.FAILED;
        }

        // --- Build the replacement WITHOUT touching the pet -------------------
        CompoundBinaryTag snapshot;
        try {
            snapshot = PetEntitySnapshot.capture(oldMob);
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("releaseToWild: failed to capture entity snapshot for "
                    + pet.getPetType().name() + " — falling back to fresh-spawn (per-type state lost). "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            snapshot = null;
        }

        Mob replacement = null;
        if (snapshot != null) {
            // Drop MyPet's attribute writes so the released mob deserializes
            // with its vanilla attributes, and its PDC so no model revives.
            snapshot = stripMyPetPdc(snapshot.remove("attributes").remove("Attributes"));
            try {
                Mob restored = PetEntitySnapshot.restore(snapshot, world);
                if (mobClass.isInstance(restored)) {
                    replacement = restored;
                } else {
                    MyPetApi.getLogger().warning("releaseToWild: snapshot restore refused for "
                            + pet.getPetType().name() + " — falling back to fresh-spawn (per-type state lost).");
                }
            } catch (Throwable t) {
                MyPetApi.getLogger().warning("releaseToWild: snapshot restore threw for "
                        + pet.getPetType().name() + " — falling back to fresh-spawn (per-type state lost). "
                        + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
        if (replacement == null) {
            replacement = world.createEntity(loc, mobClass);
        }

        // --- The one fallible step, before anything irreversible -------------
        // PetEntitySnapshot.restore deserializes with preserveUUID=false, so the
        // replacement holds a fresh UUID and may coexist with oldMob until the
        // commit block below removes it.
        if (!replacement.spawnAt(loc, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            return SpawnOutcome.DENIED;
        }

        // --- Commit point: nothing below may throw ---------------------------
        PetModelService.detachAll(pet);
        PetDamageTracker.cleanup(oldUuid);
        Timer.stopPetTicking(pet);
        PetSitParticleController.stopForPet(pet);
        PetPotionParticleController.stopForPet(pet);
        RideSkillFlightController.stopForPet(pet);
        PetLifecycleHookRegistry.forPet(pet).forEach(hook -> hook.onDespawn(pet));
        pet.setBukkitEntity(null);
        oldMob.remove();

        stripPetCustomizations(replacement);
        return SpawnOutcome.SUCCESS;
    }

    private SpawnOutcome releaseModelPet(Pet pet, Mob oldMob, Location loc, UUID oldUuid) {
        // Try to place the wild replacement BEFORE detaching the pet, so a
        // refusal leaves the pet exactly as it was. Neither call below reads
        // oldMob, so both are safe while it is still in the world.
        boolean claimed = false;
        if (loc.getWorld() != null) {
            // Source-driven type: respawn the genuine creature (e.g. the real MythicMob) via its
            // provider, keyed on the configured Model.Id — NOT the pet-type name.
            if (PetModelService.isSourceDriven(pet)) {
                String sourceId = PetModelService.modelIdOf(pet);
                if (sourceId != null && !sourceId.isBlank()) {
                    for (PetModelSourceHook src : MyPetApi.getServiceManager().getServices(PetModelSourceHook.class)) {
                        if (src.spawnSource(sourceId, loc).isPresent()) {
                            claimed = true;
                            break;
                        }
                    }
                }
            }
            // Rendered custom creature: leave a wild modeled mob (host + configured model),
            // re-leashable back into the pet.
            if (!claimed) {
                SpawnOutcome modeled = PetModelService.releaseAsModeledWild(pet, loc);
                if (modeled == SpawnOutcome.DENIED) {
                    return SpawnOutcome.DENIED;
                }
                claimed = modeled == SpawnOutcome.SUCCESS;
            }
        }

        // Commit: nothing claimed the release means "no model, no provider" —
        // the host is simply despawned, exactly as before.
        PetDamageTracker.cleanup(oldUuid);
        Timer.stopPetTicking(pet);
        PetSitParticleController.stopForPet(pet);
        PetPotionParticleController.stopForPet(pet);
        RideSkillFlightController.stopForPet(pet);
        PetLifecycleHookRegistry.forPet(pet).forEach(hook -> hook.onDespawn(pet));
        pet.setBukkitEntity(null);
        oldMob.remove();
        return SpawnOutcome.SUCCESS;
    }

    /**
     * Removes everything MyPet adds on top of a vanilla mob, so a snapshot-
     * restored entity is indistinguishable from a wild spawn. Called from
     * {@link #releaseToWild} after the new mob is in the world.
     */
    private static void stripPetCustomizations(Mob newMob) {
        // Custom name / nametag visibility.
        newMob.customName(null);
        newMob.setCustomNameVisible(false);

        // All mypet:* PDC keys (pet marker, perma-screaming flag, future
        // per-pet flags). Snapshotting collects PDC into BukkitValues, so
        // every MyPet PDC key the pet had is on the restored mob.
        PersistentDataContainer pdc = newMob.getPersistentDataContainer();
        List<NamespacedKey> mypetKeys = new ArrayList<>();
        for (NamespacedKey key : pdc.getKeys()) {
            if ("mypet".equals(key.getNamespace())) {
                mypetKeys.add(key);
            }
        }
        for (NamespacedKey key : mypetKeys) {
            pdc.remove(key);
        }

        // Tameable / owner.
        if (newMob instanceof Tameable tameable) {
            tameable.setOwner(null);
            tameable.setTamed(false);
        }
        // Sit pose (released mob should not spawn sitting).
        if (newMob instanceof Sittable sittable) {
            sittable.setSitting(false);
        }
        // Wither boss bar — pet hides it via PetVisualSyncer; wild should show.
        if (newMob instanceof Wither wither) {
            try {
                wither.getBossBar().setVisible(true);
            } catch (Throwable ignored) {
            }
        }

        newMob.setHealth(Math.min(newMob.getHealth(), newMob.getMaxHealth()));

        // Wild defaults for persistence / AI flags. PetEntitySnapshot.restore
        // pre-applies setPersistent(false) + setRemoveWhenFarAway(false) for
        // pet-side use; reverse them here so the wild mob is treated like
        // any naturally-spawned mob.
        newMob.setPersistent(true);
        newMob.setRemoveWhenFarAway(true);
        newMob.setAI(true);
        newMob.setCanPickupItems(true);

        // Clear brain memories that may have leaked from the pet's state —
        // a released villager shouldn't keep pathing toward the owner's bed,
        // a released breeze shouldn't keep the owner as an attack target.
        BrainAccess.clearAttackTarget(newMob);
        BrainAccess.clearWalkTarget(newMob);
    }

    /**
     * Removes MyPet's PDC keys from a captured snapshot so the restored mob
     * enters the world bare. Mirrors {@link #stripPetCustomizations}, but acts
     * on the NBT before the spawn rather than the entity after it — a model
     * plugin reading PDC on spawn must not find a live model id.
     */
    private static CompoundBinaryTag stripMyPetPdc(CompoundBinaryTag snapshot) {
        CompoundBinaryTag values = snapshot.getCompound("BukkitValues");
        if (values.size() == 0) {
            return snapshot;
        }
        CompoundBinaryTag.Builder kept = CompoundBinaryTag.builder();
        for (String key : values.keySet()) {
            if (!key.startsWith("mypet:")) {
                kept.put(key, values.get(key));
            }
        }
        return snapshot.put("BukkitValues", kept.build());
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

        // Lock/unlock vanilla aging. A per-pet owner override (set in-game with a
        // golden dandelion) wins; absent that, the per-type PreventNaturalGrowup
        // config applies (default true → no auto-grow). The grow-up item is
        // unaffected either way — it matures via setBaby(false) regardless.
        if (pet instanceof PetBaby baby && mob instanceof Ageable ageable) {
            Boolean override = PetGrowthLock.getOverride(mob);
            boolean frozen = override != null ? override : baby.preventNaturalGrowup();
            ageable.setAgeLock(frozen);
        }

        AttributeInstance health = mob.getAttribute(PetAttributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(1.0, pet.getMaxHealth()));
            mob.setHealth(Math.max(1.0, Math.min(pet.getHealth(), health.getValue())));
        }

        AttributeInstance speed = mob.getAttribute(PetAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(MyPetApi.getPetInfo().getSpeed(pet.getPetType()));
        }

        // Armor skill: ARMOR / ARMOR_TOUGHNESS attribute modifiers. Idempotent
        // (remove-then-add by key), so snapshot-restores and respawns never stack.
        PetArmorApplier.update(pet);

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

        // Folia: start per-pet scheduler tasks. Potion particles have no
        // spawn-time task — PetPotionParticleController starts lazily on show().
        Timer.startPetTicking(pet);
        PetSitParticleController.startForPet(pet);
        RideSkillFlightController.startForPet(pet);
        // A genuine fresh summon (not a snapshot-restore / tame in-place) gets a one-shot
        // spawn animation, played by PetModelService once the model is confirmed present.
        if (!mobHasPersistentState) {
            PetModelService.markFreshSpawn(pet);
        }
        PetLifecycleHookRegistry.forPet(pet).forEach(hook -> hook.onSpawn(pet));
        PetNoPushSuppressor.startForPet(pet);
    }

    /**
     * Pre-spawn position search
     */
    private Location findValidSpawnLocation(Location origin, Class<? extends Mob> mobClass) {
        if (origin != null && origin.getWorld() != null && canSpawnIn(origin.getBlock())) {
            return origin;
        }
        Location loc = origin.clone().subtract(1, 0, 1);
        for (double x = 0; x <= 2; x += 0.5) {
            for (double z = 0; z <= 2; z += 0.5) {
                if (x != 1 && z != 1) {
                    if (loc.getWorld() != null && canSpawnIn(loc.getBlock())) {
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

    /**
     * Whether a pet can spawn inside this block without suffocating. Passable
     * blocks (air, water, plants) qualify, and so do thin or partial blocks
     * like carpet, snow layers and slabs — these report isPassable() == false
     * only because of their slim collision box. Only full, vision-occluding
     * blocks (stone, dirt, …) are rejected so pets never spawn trapped in terrain.
     */
    public static boolean canSpawnIn(Block block) {
        return block.isPassable() || !block.getType().isOccluding();
    }
}
