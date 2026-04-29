package de.Keyle.MyPet.entity.visual;

import io.papermc.paper.entity.EntitySerializationFlag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

/**
 * Captures and restores a pet's full vanilla state via Paper's
 * {@code Bukkit.getUnsafe().serializeEntity / deserializeEntity}. Vanilla NBT
 * carries every variant, attribute, equipment slot, brain memory, and
 * persistent-data field automatically, so new mob types and new variants on
 * existing types do not require MyPet code changes.
 *
 * <p><b>Persistence-flag handling.</b> MyPet pets are spawned with
 * {@code setPersistent(false)} in {@link de.Keyle.MyPet.entity.spawn.VanillaMobSpawner}
 * so vanilla doesn't write them to chunk save files (MyPet's repository owns
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
 *   <li>Install MyPet goals via {@code PetGoalInstaller.install(pet, mob)}.</li>
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
     * Serializes the mob's full vanilla state to a byte array suitable for
     * later replay through {@link #restore}. See class Javadoc for the
     * persistence-flag rationale.
     *
     * @throws IllegalArgumentException if Paper refuses to serialize the
     *         entity (see {@code UnsafeValues.serializeEntity})
     */
    public static byte[] capture(Mob mob) {
        try {
            return captureWithForce(mob);
        } catch (LinkageError forceUnavailable) {
            // LinkageError covers NoClassDefFoundError + NoSuchMethodError —
            // either means the EntitySerializationFlag enum or the flagged
            // serializeEntity overload is absent on this Paper version.
            return captureWithPersistenceToggle(mob);
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
     * Deserializes snapshot bytes into a {@link Mob} object and pre-applies
     * MyPet's persistence flags. The returned mob is NOT yet in the world —
     * Paper's {@code Bukkit.getUnsafe().deserializeEntity} returns a detached
     * entity object, and the caller must call
     * {@link Entity#spawnAt(org.bukkit.Location, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason)}
     * to actually place it. Skipping that step leaves a ghost entity (no
     * errors thrown, but the mob never appears in the world).
     *
     * <p>Persistence flags are set <em>before</em> spawn so vanilla never
     * sees the mob as persistent and never writes it to chunk save files.
     *
     * @param snapshot bytes returned by a prior {@link #capture}
     * @param world    world to deserialize into (does not have to match the
     *                 world the snapshot was taken in)
     * @return a detached {@link Mob} with {@code setPersistent(false)} and
     *         {@code setRemoveWhenFarAway(false)} applied; caller must call
     *         {@code spawnAt} to put it in the world
     * @throws IllegalArgumentException if the snapshot bytes are invalid
     * @throws IllegalStateException    if the snapshot deserialized to a
     *         non-{@link Mob} entity (e.g. a player or projectile)
     */
    public static Mob restore(byte[] snapshot, World world) {
        Entity restored = Bukkit.getUnsafe().deserializeEntity(snapshot, world, false);
        if (!(restored instanceof Mob mob)) {
            String type = restored == null ? "null" : restored.getClass().getName();
            throw new IllegalStateException(
                    "Snapshot did not deserialize to a Mob: " + type);
        }
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        return mob;
    }

    /**
     * Returns Minecraft's current data version — the integer Mojang stamps on
     * save files and uses with DataFixerUpper to dispatch forward-fixing rules
     * across version upgrades. Stamping this on every saved snapshot lets a
     * future DFU integration migrate the snapshot bytes at read time without
     * needing a one-time data migration to add the stamp retroactively.
     *
     * <p>Available on every supported Paper version (1.20.5+).
     */
    public static int currentDataVersion() {
        return Bukkit.getUnsafe().getDataVersion();
    }

    /**
     * Builds an EntitySnapshot envelope compound for storage in a pet's
     * {@code info} BLOB. Centralizes the envelope shape (schema version,
     * data-version stamps, snapshot bytes, storage sub-compound) so every
     * producer — the impl {@code MyPet#getInfo}, the leash/tame listeners,
     * and the bulk migration — emits the same format.
     *
     * @param snapshot vanilla NBT bytes from {@link #capture}, or {@code null}
     *                 if no snapshot is available (graceful-degradation path:
     *                 the pet round-trips with default visuals).
     * @param storage  pet-storage sub-compound (typically containing the
     *                 experience level). May be empty.
     */
    public static CompoundBinaryTag envelope(byte[] snapshot, CompoundBinaryTag storage) {
        CompoundBinaryTag.Builder b = CompoundBinaryTag.builder()
                .putInt("schema_version", 1)
                .putInt("mc_data_version", currentDataVersion())
                .putString("mc_version", Bukkit.getServer().getMinecraftVersion())
                .put("storage", storage);
        if (snapshot != null) {
            b.putByteArray("snapshot", snapshot);
        }
        return b.build();
    }
}
