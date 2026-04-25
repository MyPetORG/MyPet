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
package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.Scheduler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface MyPet extends StoredMyPet, Scheduler {
    MyPetExperience getExperience();

    void removePet();

    void removePet(boolean wantsToRespawn);

    SpawnFlags createEntity();

    SpawnFlags createEntity(Location spawnLocation);

    PetState getStatus();

    /**
     * Returns the last cached pet status without touching the Bukkit entity.
     * Safe to call from any thread on Folia. Use this when ticking from a thread
     * that does not own the pet's region.
     */
    PetState getCachedStatus();

    void setStatus(PetState status);

    Optional<Location> getLocation();

    double getMaxHealth();

    Skills getSkills();

    boolean autoAssignSkilltree();

    Mob getBukkitEntity();

    void setBukkitEntity(Mob mob);

    AbstractNavigation getPetNavigation();

    /**
     * @deprecated Returns an Optional wrapping the Bukkit Mob. Skills should switch to
     * {@link #getBukkitEntity()} with null-checks. Default impl provided so existing skill
     * code that calls {@code .ifPresent(...)} keeps working through stages B/C.
     */
    @Deprecated
    default Optional<Mob> getEntity() {
        return Optional.ofNullable(getBukkitEntity());
    }

    double getDamage();

    double getRangedDamage();

    boolean isPassiv();

    boolean hasTarget();

    void decreaseSaturation(double value);

    boolean setSkilltree(Skilltree skilltree, MyPetSelectSkilltreeEvent.Source source);

    boolean isSitting();

    void setSitting(boolean sitting);

    LivingEntity getMyPetTarget();

    void setTarget(LivingEntity target);

    void setTarget(LivingEntity target, TargetPriority priority);

    void forgetTarget();

    TargetPriority getTargetPriority();

    boolean canMove();

    void removeEntity();

    default void updateNameTag() {
    }

    default void updateVisuals() {
    }

    /**
     * Ticks the respawn countdown for a dead pet. Must be invoked from the owner's
     * scheduler (not the pet entity's), because the dead Bukkit mob is removed from
     * the world shortly after death and its per-entity scheduler stops firing.
     */
    default void tickRespawnTimer() {
    }

    default void showPotionParticles(Color color) {
    }

    default void hidePotionParticles() {
    }

    default void makeSound(String sound, float volume, float pitch) {
        Mob mob = getBukkitEntity();
        if (mob != null) {
            try {
                Sound s = Sound.valueOf(sound);
                mob.getWorld().playSound(mob.getLocation(), s, volume, pitch);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    default void setLocation(Location loc) {
        Mob mob = getBukkitEntity();
        if (mob != null) mob.teleportAsync(loc);
    }

    // ─── Passenger state ───
    default boolean hasMyPetRider() {
        Mob mob = getBukkitEntity();
        return mob != null && !mob.getPassengers().isEmpty();
    }

    default boolean onInteract(Player player,
                               ItemStack item,
                               EquipmentSlot hand) {
        return false;
    }

    enum PetState {
        Dead, Despawned, PetState, Here
    }

    enum SpawnFlags {
        Success, NoSpace, AlreadyHere, Dead, Canceled, OwnerDead, Flying, Spectator, WrongWorldGroup, NotAllowed, InvalidPosition
    }
}
