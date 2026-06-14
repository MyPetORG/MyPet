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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.PetExperience;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.Scheduler;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.Optional;

/**
 * Active, in-world pet. Extends the read-only {@link StoredPet} contract
 * with the mutators the live entity needs as it takes damage, gains XP,
 * eats, and is renamed. Mutations on the persisted snapshot type
 * {@code PersistedPet} use {@code withX} / {@code Builder} instead — only
 * the active pet (this interface) is genuinely mutable.
 */
public non-sealed interface Pet extends StoredPet, Scheduler {

    // ─── Mutators (previously inherited from StoredPet) ───

    void setUUID(UUID uuid);

    void setOwner(MyPetPlayer owner);

    /** Updates the pet's display name (MiniMessage format). */
    void setPetName(String petName);

    void setPetType(PetType petType);

    void setWorldGroup(String worldGroup);

    /**
     * Sets the pet's current health, clamped to
     * {@code [0, getMaxHealth()]}. Setting to 0 triggers death handling.
     */
    void setHealth(double health);

    /** Sets saturation (1–100). Reaching 1 disables hunger drain. */
    void setSaturation(double saturation);

    /** Sets the respawn countdown in seconds. Zero means alive. */
    void setRespawnTime(int respawnTime);

    void setLastUsed(long lastUsed);

    void setWantsToRespawn(boolean wantsToRespawn);

    /** Sets raw XP. Triggers level-up/down logic internally. */
    void setExp(double exp);

    /**
     * Single-arg form that updates the skilltree without firing
     * {@code PetSelectSkilltreeEvent}. Used during pet activation when the
     * skilltree is being restored from persistence rather than chosen.
     */
    void setSkilltree(Skilltree skilltree);

    /** Returns the experience tracker managing XP gain and level curves. */
    PetExperience getExperience();

    /**
     * Despawns and deactivates the pet. Equivalent to
     * {@code removePet(true)} — the pet will attempt to respawn on the
     * next call/login.
     */
    void removePet();

    /**
     * Despawns and deactivates the pet.
     *
     * @param wantsToRespawn if {@code true}, the pet will auto-spawn on
     *                       the owner's next login or call command
     */
    void removePet(boolean wantsToRespawn);

    /**
     * Attempts to spawn the pet at the owner's location.
     *
     * @return result flag indicating success or the reason for failure
     */
    SpawnFlags createEntity();

    /**
     * Attempts to spawn the pet at the specified location.
     *
     * @return result flag indicating success or the reason for failure
     */
    SpawnFlags createEntity(Location spawnLocation);

    /** Returns the current lifecycle state of this pet. */
    PetState getStatus();

    /**
     * Returns the last cached pet status without touching the Bukkit entity.
     * Safe to call from any thread on Folia. Use this when ticking from a thread
     * that does not own the pet's region.
     */
    PetState getCachedStatus();

    void setStatus(PetState status);

    /**
     * Returns the pet's current world location, or empty if the Bukkit
     * entity is not spawned.
     */
    Optional<Location> getLocation();

    /**
     * Returns the current max health (base and Life skill bonus). May
     * differ from the starting HP configured in {@link PetInfo}.
     */
    double getMaxHealth();

    /** Returns the skill container holding all active skill instances. */
    Skills getSkills();

    /**
     * Attempts to auto-assign a skilltree based on permission or config
     * rules. Called on first activation if no tree is already set.
     *
     * @return {@code true} if a tree was assigned
     */
    boolean autoAssignSkilltree();

    /**
     * Returns the underlying Bukkit mob, or {@code null} if the pet is
     * not currently spawned (dead/despawned).
     */
    Mob getBukkitEntity();

    /** Binds a freshly spawned Bukkit mob to this pet instance. */
    void setBukkitEntity(Mob mob);

    /** Returns the navigation controller for this pet's movement. */
    AbstractNavigation getPetNavigation();

    /** Returns mêlée damage dealt by this pet (base and Damage skill). */
    double getDamage();

    /** Returns ranged damage for projectile-shooting pets. */
    double getRangedDamage();

    /**
     * Returns {@code true} if the pet's skilltree has no damage-dealing
     * skills active — the pet will not attack even if provoked.
     */
    boolean isPassive();

    /** Returns {@code true} if the pet has an active combat target. */
    boolean hasTarget();

    /** Reduces saturation by the given amount (hunger tick). */
    void decreaseSaturation(double value);

    /**
     * Sets the skilltree and fires {@link PetSelectSkilltreeEvent},
     * allowing cancellation. Use this for player-initiated selections.
     *
     * @param source who/what triggered the selection
     * @return {@code true} if the tree was applied (event not canceled)
     */
    boolean setSkilltree(Skilltree skilltree, PetSelectSkilltreeEvent.Source source);

    /** Returns {@code true} if the pet is in its sitting/stay pose. */
    boolean isSitting();

    /** Puts the pet into or takes it out of its sitting/stay pose. */
    void setSitting(boolean sitting);

    /** Returns the current combat target, or {@code null} if none. */
    LivingEntity getPetTarget();

    /** Sets a combat target with {@link TargetPriority#Overwrite}. */
    void setTarget(LivingEntity target);

    /**
     * Sets a combat target if the given priority is higher than the
     * current one. Lower-priority calls are ignored.
     */
    void setTarget(LivingEntity target, TargetPriority priority);

    /** Clears the current target and resets priority to None. */
    void forgetTarget();

    /** Returns the priority level of whoever set the current target. */
    TargetPriority getTargetPriority();

    /**
     * Returns {@code true} if the pet is able to move (spawned, not
     * sitting, not riding, entity is alive).
     */
    boolean canMove();

    /**
     * Removes the Bukkit entity from the world without changing pet
     * state. Used during chunk-unload or teleport transitions.
     */
    void removeEntity();

    /**
     * Calls the pet to its owner. If the pet is already spawned, despawns
     * it first (preserving auto-respawn intent) and then re-spawns at the
     * owner's location. Returns the spawn result for the caller to handle.
     */
    SpawnFlags callToOwner();

    /**
     * Sends the pet away — equivalent to {@code removePet(false)}.
     * The pet stays owned but will not auto-spawn on the next login or
     * call until it is summoned again.
     */
    void sendAway();

    /** Re-renders the pet's name tag from the current state and config. */
    default void updateNameTag() {
    }

    /** Pushes all visual states (variant, baby, collar, etc.) to the mob. */
    default void updateVisuals() {
    }

    /**
     * Hook fired by {@code PetMeleeAttackGoal} immediately after a successful
     * melee hit (one that reduced the target's health). Default no-op;
     * pet implementations override to apply post-hit effects that vanilla
     * normally runs inside the mob's {@code doHurtTarget} (e.g., IronGolem
     * toss-up).
     */
    default void onMeleeHitLanded(LivingEntity target) {
    }

    /**
     * Ticks the respawn countdown for a dead pet. Must be invoked from the owner's
     * scheduler (not the pet entity's), because the dead Bukkit mob is removed from
     * the world shortly after death and its per-entity scheduler stops firing.
     */
    default void tickRespawnTimer() {
    }

    /** Displays a coloured potion-swirl particle above the pet. */
    default void showPotionParticles(Color color) {
    }

    /** Stops any active potion particle effect. */
    default void hidePotionParticles() {
    }

    /**
     * Plays a sound at the pet's location. Silently ignores invalid
     * sound names (e.g., sounds added in newer MC versions).
     */
    default void makeSound(String sound, float volume, float pitch) {
        Mob mob = getBukkitEntity();
        if (mob != null) {
            Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(sound));
            if (s != null) {
                mob.getWorld().playSound(mob.getLocation(), s, volume, pitch);
            }
        }
    }

    /** Async-teleports the pet to the given location. */
    default void setLocation(Location loc) {
        Mob mob = getBukkitEntity();
        if (mob != null) mob.teleportAsync(loc);
    }

    /** Returns {@code true} if a player is currently riding this pet. */
    default boolean hasPetRider() {
        Mob mob = getBukkitEntity();
        return mob != null && !mob.getPassengers().isEmpty();
    }

    /**
     * Called when a player right-clicks the pet. Implementations handle
     * feeding, equipment, and skill interactions.
     *
     * @return {@code true} if the interaction was consumed
     */
    default boolean onInteract(Player player,
                               ItemStack item,
                               EquipmentSlot hand) {
        return false;
    }

    /**
     * Single-shot read of the snapshot captured at the pet's last despawn,
     * used by the spawner to deserialize a vanilla mob with the pet's prior
     * visual state. Default {@code null} — only the active impl overrides
     * with real semantics (read-and-clear).
     */
    default CompoundBinaryTag consumePendingSnapshot() {
        return null;
    }

    /** Lifecycle state of a pet. */
    enum PetState {
        /** Killed — waiting on respawn timer. */
        Dead,
        /** Removed from the world by owner or system (call/chunk-unload). */
        Despawned,
        /** Transitional state during spawn pipeline. */
        PetState,
        /** Alive and present in the world. */
        Here
    }

    /** Result codes returned by {@link #createEntity}. */
    enum SpawnFlags {
        Success, NoSpace, AlreadyHere, Dead, Canceled, OwnerDead, Flying, Spectator, WrongWorldGroup, NotAllowed, InvalidPosition
    }
}
