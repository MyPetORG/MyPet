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

package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetZombifiable;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wither;


/**
 * Applies the universal MyPet domain flags onto the live Bukkit {@link Mob}:
 * baby/adult, sitting pose, tamed-owner relationship, and the Wither boss-bar
 * suppression.
 *
 * <p>Per-type visual state (collar colour, variant, saddle, etc.) is handled
 * directly on the live entity via Bukkit setters at the call site — no field
 * cache exists on the {@code My{Type}} domain objects to push through this
 * class.
 */
public final class PetVisualSyncer {

    private PetVisualSyncer() {
    }

    /**
     * Synchronises universal MyPet flags onto the Bukkit mob. Called from
     * {@code VanillaMobSpawner.configureMob} pre-spawn and from
     * {@code plugin/entity/Pet.updateVisuals} post-spawn.
     */
    public static void sync(Pet pet, Mob mob) {
        sync(pet, mob, true);
    }

    /**
     * Overload that allows the caller to skip the tameable/owner application.
     * Used by {@code VanillaMobSpawner#releaseToWild} when respawning the pet
     * as a wild vanilla mob — the mob is no longer owned by the player, so
     * {@code setTamed/setOwner} must not be called.
     *
     * @param applyTameable when {@code false}, skip the {@code Tameable} block
     *                      so the synced mob remains un-tamed. The sit pose
     *                      is also suppressed since a released mob should not
     *                      spawn in the sitting animation.
     */
    public static void sync(Pet pet, Mob mob, boolean applyTameable) {
        if (pet == null || mob == null) return;

        if (pet instanceof PetBaby baby && mob instanceof Ageable ageable) {
            if (baby.isBaby()) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }
        if (mob instanceof Sittable sittable) {
            sittable.setSitting(applyTameable && pet.isSitting());
        }
        if (applyTameable && mob instanceof Tameable tameable) {
            tameable.setTamed(true);
            if (pet.getOwner() != null && pet.getOwner().getPlayer() != null) {
                tameable.setOwner(pet.getOwner().getPlayer());
            }
        }
        // Hide the Wither's auto-managed boss bar. setVisible(false) flips the
        // NMS ServerBossEvent visibility flag; startSeenByPlayer checks it before
        // broadcasting add-packets, so the suppression persists across players
        // entering range. Called pre-spawn from VanillaMobSpawner.configureMob
        // so the bar is hidden before the initial spawn packet is sent.
        if (mob instanceof Wither wither) {
            try {
                wither.getBossBar().setVisible(false);
            } catch (Throwable ignored) {}
        }
        // Suppress vanilla's Overworld-conversion timer for nether-native pets
        // (Hoglin → Zoglin, Piglin → ZombifiedPiglin, PiglinBrute →
        // ZombifiedPiglin). Without this, vanilla discards the original entity
        // and spawns a wild copy — Pet would then respawn the original type,
        // leaving a non-Pet zombified mob loose in the world. Pushed every
        // sync so a /reload that flips AllowZombification takes effect on the
        // next visual update without requiring a despawn/respawn round-trip.
        if (pet instanceof PetZombifiable zombifiable) {
            boolean immune = !zombifiable.allowZombification();
            if (mob instanceof Hoglin hoglin) {
                hoglin.setImmuneToZombification(immune);
            } else if (mob instanceof PiglinAbstract piglin) {
                piglin.setImmuneToZombification(immune);
            }
        }
    }
}
