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

import de.Keyle.MyPet.util.NbtUtil;
import io.papermc.paper.entity.EntitySerializationFlag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Captures and restores a pet's full vanilla state via Paper's
 * {@code Bukkit.getUnsafe().serializeEntity / deserializeEntity}. Vanilla NBT
 * carries every variant, attribute, equipment slot, brain memory, and
 * persistent-data field automatically, so new mob types and new variants on
 * existing types do not require MyPet code changes.
 *
 * <p><b>Persistence-flag handling.</b> Pet pets are spawned with
 * {@code setPersistent(false)} in {@link de.Keyle.MyPet.entity.spawn.VanillaMobSpawner}
 * so vanilla doesn't write them to chunk save files (Pet's repository owns
 * canonical state). Paper's {@code serializeEntity} refuses non-persistent
 * entities by default — on 1.21.4+ the {@link EntitySerializationFlag#FORCE}
 * flag overrides this. {@link #capture} prefers the FORCE flag and falls
 * back to a persistence toggle on 1.20.5–1.21.3 where the flag is absent.
 *
 * <p><b>Caller responsibilities after restore.</b> {@link #restore} returns
 * a detached vanilla mob (NOT in the world) with MyPet's persistence/despawn
 * flags reasserted, leaving these steps to the caller (typically
 * {@code VanillaMobSpawner}):
 * <ol>
 *   <li>Call {@code mob.spawnAt(targetLocation, SpawnReason.CUSTOM)} to
 *       actually place the entity in the world. The snapshot encodes the
 *       despawn position; spawnAt overrides that with the call location.
 *       Skipping spawnAt leaves a ghost entity that will never appear.</li>
 *   <li>{@link de.Keyle.MyPet.entity.spawn.PetEntityMarker#mark} — the
 *       {@code mypet:pet} PDC tag round-trips through the snapshot bytes
 *       on its own, but re-marking is cheap and tolerates snapshots
 *       written before the marker existed.</li>
 *   <li>Strip vanilla AI via {@code Bukkit.getMobGoals().removeAllGoals(mob)}.
 *       The snapshot restores a vanilla mob complete with its registered goals;
 *       MyPet replaces them with its own.</li>
 *   <li>Install Pet goals via {@code PetGoalInstaller.install(pet, mob)}.</li>
 * </ol>
 *
 * <p>Keeping these steps caller-side avoids coupling this utility to the
 * domain-object types and makes the round-trip easy to test in isolation.
 */
@SuppressWarnings("deprecation") // Bukkit.getUnsafe() / UnsafeValues — intentional, documented stable API
public final class PetEntitySnapshot {

    private PetEntitySnapshot() {
    }

    /**
     * Captures the mob's full vanilla state as an Adventure-NBT compound
     * suitable for later replay through {@link #restore}. Internally calls
     * Paper's {@code Bukkit.getUnsafe().serializeEntity} (which returns
     * GZIP'd Mojang NBT) and parses the result. See class Javadoc for the
     * persistence-flag rationale.
     *
     * @throws IllegalArgumentException if Paper refuses to serialize the
     *         entity (see {@code UnsafeValues.serializeEntity})
     * @throws UncheckedIOException     if the serialized bytes fail to parse
     */
    public static CompoundBinaryTag capture(Mob mob) {
        byte[] bytes;
        try {
            bytes = captureWithForce(mob);
        } catch (LinkageError forceUnavailable) {
            // LinkageError covers NoClassDefFoundError + NoSuchMethodError —
            // either means the EntitySerializationFlag enum or the flagged
            // serializeEntity overload is absent on this Paper version.
            bytes = captureWithPersistenceToggle(mob);
        }
        try {
            return NbtUtil.readCompressed(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse serialized entity NBT", e);
        }
    }

    /**
     * 1.21.4+ path. Loads {@link EntitySerializationFlag} on first invocation;
     * on older Paper versions the class is absent and the JVM throws
     * {@link NoClassDefFoundError}, which {@link #capture} catches.
     */
    private static byte[] captureWithForce(Mob mob) {
        return Bukkit.getUnsafe().serializeEntity(mob, EntitySerializationFlag.FORCE);
    }

    /**
     * 1.20.5 – 1.21.3 fallback. Toggles persistence around the no-flag
     * {@code serializeEntity} call. Briefly observable as {@code persistent=true}
     * but the typical caller follows immediately with {@code mob.remove()}.
     */
    private static byte[] captureWithPersistenceToggle(Mob mob) {
        boolean wasPersistent = mob.isPersistent();
        mob.setPersistent(true);
        try {
            return Bukkit.getUnsafe().serializeEntity(mob);
        } finally {
            mob.setPersistent(wasPersistent);
        }
    }

    /**
     * Deserializes a snapshot compound into a {@link Mob} object and
     * pre-applies MyPet's persistence flags. The returned mob is NOT yet
     * in the world — Paper's {@code Bukkit.getUnsafe().deserializeEntity}
     * returns a detached entity object, and the caller must call
     * {@link Entity#spawnAt(org.bukkit.Location, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason)}
     * to actually place it. Skipping that step leaves a ghost entity (no
     * errors thrown, but the mob never appears in the world).
     *
     * <p>Persistence flags are set <em>before</em> spawn so vanilla never
     * sees the mob as persistent and never writes it to chunk save files.
     *
     * @param snapshot compound returned by a prior {@link #capture}
     * @param world    world to deserialize into (does not have to match the
     *                 world the snapshot was taken in)
     * @return a detached {@link Mob} with {@code setPersistent(false)} and
     *         {@code setRemoveWhenFarAway(false)} applied; caller must call
     *         {@code spawnAt} to put it in the world
     * @throws IllegalArgumentException if the snapshot is invalid
     * @throws IllegalStateException    if the snapshot deserialized to a
     *         non-{@link Mob} entity (e.g. a player or projectile)
     * @throws UncheckedIOException     if re-serializing the compound for
     *         Paper fails
     */
    public static Mob restore(CompoundBinaryTag snapshot, World world) {
        byte[] bytes;
        try {
            bytes = NbtUtil.writeCompressed(snapshot);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize snapshot for deserializeEntity", e);
        }
        Entity restored = Bukkit.getUnsafe().deserializeEntity(bytes, world, false);
        if (!(restored instanceof Mob mob)) {
            String type = restored == null ? "null" : restored.getClass().getName();
            throw new IllegalStateException(
                    "Snapshot did not deserialize to a Mob: " + type);
        }
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        // Fire ticks are intentionally NOT carried across restore even though
        // the captured NBT contains the vanilla Fire tag. A pet that died
        // while burning would otherwise come back still burning, and a
        // low-max-HP pet can re-die before the timer decays (death loop).
        // VanillaMobSpawner.configureMob also clears this — kept here as
        // defense-in-depth for any future caller that uses restore directly.
        mob.setFireTicks(0);
        return mob;
    }

}
