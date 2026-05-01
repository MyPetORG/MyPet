package de.Keyle.MyPet.repository.types;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.util.VersionUtil;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.NbtUtil;
import de.Keyle.MyPet.entity.PersistedMyPet;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base class for SQL-backed {@link Repository} implementations (SQLite, MySQL).
 * Owns all shared CRUD logic, in-memory write queues, and the executor on which
 * every async operation runs. Subclasses supply only dialect-specific bits via
 * the SPI hooks declared below: connection lifecycle, DDL, table qualification,
 * binary column binding, and an error label.
 *
 * <p><b>Threading.</b> Every {@link CompletableFuture}-returning method pins its
 * work to {@link #executor}, which the subclass creates in its own {@code init()}
 * (a single thread for SQLite — the JDBC driver is not connection-safe; a pool
 * sized from config for MySQL). Callers must not touch JDBC state off-executor.
 *
 * <p><b>Pending-write safety net.</b> {@link #petsToBeSaved} and
 * {@link #playersToBeSaved} hold entries from the moment a mutating async
 * operation is submitted until it observes success. Reads ({@link #getPet})
 * consult them first so a read that races a failed write still sees the
 * intended state. Periodic and shutdown saves flush both maps.
 *
 * <p><b>Failure conventions for {@code CompletableFuture}-returning methods.</b>
 * <ul>
 *   <li>{@code Boolean}-returning writes ({@code addMyPet}, {@code updateMyPet},
 *       {@code removeMyPet}, etc.) return {@code false} on error and log.</li>
 *   <li>{@code Integer} counters ({@code countPets}) throw {@link CompletionException}
 *       on error — {@code 0} would be ambiguous with "no rows".</li>
 *   <li>{@code Integer cleanup} returns {@code 0} on error — {@code 0} genuinely
 *       matches "nothing deleted".</li>
 *   <li>Domain-object reads that can legitimately return {@code null} for
 *       "not found" ({@code getMyPet}, {@code getMyPetPlayer}, {@code getPets})
 *       throw {@link CompletionException} on error to distinguish from empty.</li>
 *   <li>Boolean existence checks ({@code hasPets}, {@code isMyPetPlayer}) throw
 *       {@link CompletionException} — a silent {@code false} could lead to data
 *       loss (e.g. believing a player has no saved pets).</li>
 * </ul>
 */
public abstract class AbstractSqlRepository implements Repository {

    protected final Gson gson = new Gson();

    /**
     * Pets enqueued for UPDATE by {@link #updatePet}, cleared on success.
     * Entries here take precedence over the DB row in {@link #getPet} and
     * are flushed by {@link #savePets} during periodic / shutdown saves.
     */
    protected final Map<UUID, StoredMyPet> petsToBeSaved = new ConcurrentHashMap<>();

    /** Same pattern as {@link #petsToBeSaved} but for player rows. */
    protected final Map<UUID, MyPetPlayer> playersToBeSaved = new ConcurrentHashMap<>();

    /**
     * Executor that serializes all async CRUD. Subclass assigns in its
     * {@code init()} — single-thread for SQLite, fixed pool for MySQL.
     */
    protected ExecutorService executor;

    // --- Connection lifecycle ---

    /**
     * Borrow a JDBC connection for a single unit of work. The returned
     * {@link ConnectionHolder} must be used in try-with-resources.
     *
     * <p><b>SQLite caveat.</b> SQLite's implementation returns the single shared
     * connection and a no-op {@code close()}; calling this inside an already-open
     * {@link Statement} / {@link ResultSet} produces a second Statement on the
     * same Connection, which SQLite JDBC does not support and which will
     * silently invalidate the outer cursor. Fetch any dependent data up front
     * instead of nesting acquires (see {@link #getAllPets}).
     */
    protected abstract ConnectionHolder acquireConnection() throws SQLException;

    /** Close the backing connection or pool. Called from {@link #disable()} after the executor drains. */
    protected abstract void disableBackend();

    // --- SQL shaping ---

    /**
     * Resolve a logical table name (e.g. {@code "pets"}) to the fully-qualified
     * form used in SQL. SQLite returns the name unchanged; MySQL prepends
     * {@code Configuration.Repository.MySQL.PREFIX} (validated at init time
     * against {@code [A-Za-z0-9_]*}).
     */
    protected abstract String qualifyTable(String baseName);

    // --- Binary column glue ---

    /**
     * Bind a byte array to a {@code BLOB} parameter. SQLite uses {@code setBytes};
     * MySQL uses {@code setBlob(new ByteArrayInputStream(...))} since its JDBC
     * driver prefers streams for large values.
     */
    protected abstract void bindBlob(PreparedStatement s, int idx, byte[] data) throws SQLException;

    /** Read a {@code BLOB} column as a byte array. Dialect-specific for the same reason as {@link #bindBlob}. */
    protected abstract byte[] readBlob(ResultSet rs, String col) throws SQLException;

    /**
     * Serialize a pet's vanilla-NBT entity snapshot for the {@code info}
     * BLOB column. Empty compounds (graceful-degradation pets, shop
     * templates) become zero-length byte arrays.
     */
    private static byte[] serializeInfo(StoredMyPet pet) throws IOException {
        CompoundBinaryTag info = pet.getInfo();
        return info.keySet().isEmpty() ? new byte[0] : NbtUtil.writeCompressed(info);
    }

    /**
     * Bind a pet name to the {@code name} column, which is {@code VARCHAR} in SQLite
     * but {@code VARBINARY} in MySQL (to be byte-exact with Minecraft's UTF-8
     * display names without charset surprises).
     *
     * <p>Note: {@code addMyPet} and {@code addPets} bypass this hook and use
     * {@code setString} directly on INSERT — that's the pre-existing asymmetry
     * with the UPDATE path and is preserved by this refactor.
     */
    protected abstract void bindPetName(PreparedStatement s, int idx, String name) throws SQLException;

    /** Read the {@code name} column as a {@code String}. Inverse of {@link #bindPetName}. */
    protected abstract String readPetName(ResultSet rs, String col) throws SQLException;

    // --- Misc ---

    /** Short human label for error messages — {@code "SQLite"} or {@code "MySQL"}. */
    protected abstract String dbLabel();

    /**
     * Open a fresh JDBC connection dedicated to the caller (must be closed by
     * the caller). Used by the migration service to run on its own thread
     * without contending with the repository's managed connection / pool.
     * SQLite opens a new file connection; MySQL borrows from the pool.
     */
    public abstract Connection openIsolatedConnection() throws SQLException;

    /**
     * Populate a player's world-group-to-pet map from the {@code multi_world}
     * column. Default implementation reads a JSON string (the current format);
     * MySQL overrides to first detect and migrate rows still using the legacy
     * NBT-compressed {@code BLOB} format.
     */
    protected void readPlayerMultiWorld(ResultSet rs, MyPetPlayerImpl player) throws SQLException {
        try {
            JsonObject jsonObject = gson.fromJson(rs.getString("multi_world"), JsonObject.class);
            if (jsonObject == null) return;
            for (String uuid : jsonObject.keySet()) {
                String petUUID = jsonObject.get(uuid).getAsString();
                player.setMyPetForWorldGroup(uuid, UUID.fromString(petUUID));
            }
        } catch (JsonParseException e) {
            reportError(e);
        }
    }

    /**
     * Write a corrupted column's raw bytes to {@code <dataFolder>/corrupted/} for
     * post-mortem inspection. Called when NBT decompression fails during a load —
     * the pet is recovered with an empty compound so the server stays up, but the
     * original bytes are preserved so an admin can salvage them.
     */
    protected void backupCorruptedData(UUID petUuid, MyPetPlayer owner, String fieldName, byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            Path corruptedDir = MyPetApi.getPlugin().getDataFolder().toPath().resolve("corrupted");
            Files.createDirectories(corruptedDir);
            String filename = owner.getUniqueId() + "_" + petUuid + "_" + fieldName + ".dat";
            Path backupFile = corruptedDir.resolve(filename);
            Files.write(backupFile, data);
            MyPetApi.getLogger().info("Corrupted data backed up to: " + backupFile);
        } catch (IOException e) {
            MyPetApi.getLogger().warning("Failed to backup corrupted data for pet " + petUuid + ": " + e.getMessage());
        }
    }

    /**
     * Log a backend error with a dialect-tagged prefix (e.g.
     * {@code "SQLite database operation failed"}) that server admins grep for.
     */
    protected void reportError(Throwable t) {
        ErrorUtil.reportError(dbLabel() + " database operation failed", t);
    }

    /**
     * Stop accepting new async work and wait up to 30 seconds for in-flight tasks
     * to finish. On timeout or interruption, forcibly cancel remaining tasks so
     * {@link #disable()} can return and the plugin can unload cleanly.
     */
    protected void shutdownExecutor() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown sequence: flush pending saves synchronously (on the calling
     * thread), then drain the executor, then close the backend. Order matters —
     * saves must complete before the executor stops accepting work, and the
     * backend must stay open until the executor's last task returns.
     */
    @Override
    public void disable() {
        saveData();
        shutdownExecutor();
        disableBackend();
    }

    /**
     * Delete every pet row whose {@code last_used} is older than {@code timestamp},
     * excluding any pet currently active in memory. Returns the number of rows
     * deleted, or {@code 0} on error (matches the "nothing to clean up" case).
     *
     * <p>Active pets are skipped because their {@code last_used} reflects the time
     * of activation (set in {@code createEntity}) and is not refreshed while the
     * pet is in use, so a long-lived active pet looks "unused" to a time-based
     * cutoff. Deleting the DB row of an active pet would orphan the in-memory
     * state — the Bukkit entity stays in the world, {@code mActivePlayerPets}
     * still holds the pet, and the owner's world-group still points at the UUID.
     * That state only unwinds through proper deactivation (e.g. {@code /petstore}
     * or {@code /petadmin remove}), and {@code /petswitch} shows {@code -1/max}
     * in the meantime.
     */
    @Override
    public CompletableFuture<Integer> cleanup(final long timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            MyPet[] activePets = MyPetApi.getMyPetManager().getAllActiveMyPets();
            StringBuilder sql = new StringBuilder("DELETE FROM ")
                    .append(qualifyTable("pets"))
                    .append(" WHERE last_used<?");
            if (activePets.length > 0) {
                sql.append(" AND uuid NOT IN (");
                for (int i = 0; i < activePets.length; i++) {
                    if (i > 0) sql.append(',');
                    sql.append('?');
                }
                sql.append(')');
            }
            sql.append(';');
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(sql.toString())) {
                stmt.setLong(1, timestamp);
                for (int i = 0; i < activePets.length; i++) {
                    stmt.setString(2 + i, activePets[i].getUUID().toString());
                }
                return stmt.executeUpdate();
            } catch (SQLException e) {
                reportError(e);
                return 0;
            }
        }, executor);
    }

    /**
     * Total pet count across all owners. Throws {@link CompletionException} on
     * error — a silent {@code 0} would be indistinguishable from "no pets".
     */
    @Override
    public CompletableFuture<Integer> countPets() {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT COUNT(uuid) FROM " + qualifyTable("pets") + ";");
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /** Count pets of a specific type. Same failure semantics as {@link #countPets()}. */
    @Override
    public CompletableFuture<Integer> countPets(final MyPetType type) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT COUNT(uuid) FROM " + qualifyTable("pets") + " WHERE type=?;")) {
                stmt.setString(1, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Delete a pet row by UUID. Returns {@code true} if the row existed and was
     * removed, {@code false} if the row was missing or the delete failed.
     */
    @Override
    public CompletableFuture<Boolean> removePet(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "DELETE FROM " + qualifyTable("pets") + " WHERE uuid=?;")) {
                stmt.setString(1, uuid.toString());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                reportError(e);
                return false;
            }
        }, executor);
    }

    /** Convenience overload; delegates to {@link #removePet(UUID)}. */
    @Override
    public CompletableFuture<Boolean> removePet(final StoredMyPet storedMyPet) {
        return removePet(storedMyPet.getUUID());
    }

    /** Delete a player row. Same failure semantics as {@link #removePet(UUID)}. */
    @Override
    public CompletableFuture<Boolean> removeMyPetPlayer(final MyPetPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "DELETE FROM " + qualifyTable("players") + " WHERE uuid=?;")) {
                stmt.setString(1, player.getUniqueId().toString());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                reportError(e);
                return false;
            }
        }, executor);
    }

    /**
     * Does this player have at least one saved pet? Returns an
     * already-completed {@code false} if {@code myPetPlayer} is {@code null}.
     * Throws {@link CompletionException} on error — a silent {@code false}
     * could mislead callers into discarding stored pets.
     */
    @Override
    public CompletableFuture<Boolean> hasPets(final MyPetPlayer myPetPlayer) {
        if (myPetPlayer == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT COUNT(uuid) FROM " + qualifyTable("pets") + " WHERE owner_uuid=?;")) {
                stmt.setString(1, myPetPlayer.getUniqueId().toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Does a player row exist for this Bukkit player? Same failure semantics
     * as {@link #hasPets}.
     */
    @Override
    public CompletableFuture<Boolean> isMyPetPlayer(final Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT COUNT(uuid) FROM " + qualifyTable("players") + " WHERE uuid=?;")) {
                stmt.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    /**
     * Builds one {@link PersistedMyPet} from the ResultSet's current row. Caller
     * must have already advanced the cursor with rs.next(). Returns null if the
     * row's type is unknown (pet type no longer registered).
     */
    protected PersistedMyPet petFromRow(MyPetPlayer owner, ResultSet rs) throws SQLException {
        MyPetType type = MyPetType.byNameOrNull(rs.getString("type"));
        if (type == null) return null;

        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String petName = readPetName(rs, "name");

        Skilltree skilltree = null;
        String skillTreeName = rs.getString("skilltree");
        if (skillTreeName != null) {
            skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skillTreeName);
        }

        byte[] skillsData = readBlob(rs, "skills");
        CompoundBinaryTag skillInfo;
        try {
            skillInfo = NbtUtil.readCompressed(skillsData);
        } catch (IOException e) {
            MyPetApi.getLogger().warning("Failed to load skills for " + owner.getName()
                    + "'s Pet " + petName + " - the data was likely corrupted.");
            backupCorruptedData(uuid, owner, "skills", skillsData);
            skillInfo = CompoundBinaryTag.empty();
        }

        byte[] infoData = readBlob(rs, "info");
        CompoundBinaryTag info;
        if (infoData == null || infoData.length == 0) {
            info = CompoundBinaryTag.empty();
        } else {
            try {
                info = NbtUtil.readCompressed(infoData);
            } catch (IOException e) {
                MyPetApi.getLogger().warning("Failed to load info for " + owner.getName()
                        + "'s Pet " + petName + " - the data was likely corrupted.");
                backupCorruptedData(uuid, owner, "info", infoData);
                info = CompoundBinaryTag.empty();
            }
        }

        return PersistedMyPet.builder(owner)
                .uuid(uuid)
                .petType(type)
                .petName(petName)
                .worldGroup(rs.getString("world_group"))
                .exp(rs.getDouble("exp"))
                .health(rs.getDouble("health"))
                .saturation(rs.getDouble("hunger"))
                .respawnTime(rs.getInt("respawn_time"))
                .wantsToRespawn(rs.getBoolean("wants_to_spawn"))
                .lastUsed(rs.getLong("last_used"))
                .skilltree(skilltree)
                .skillInfo(skillInfo)
                .info(info)
                .build();
    }

    /**
     * Iterate the remaining rows of {@code rs} and build a list of pets, all
     * owned by {@code owner}. Rows with unknown pet types are skipped silently.
     * SQL errors during iteration are logged and the partial list is returned.
     */
    protected List<StoredMyPet> petsFromResultSet(MyPetPlayer owner, ResultSet rs) {
        List<StoredMyPet> pets = new ArrayList<>();
        try {
            while (rs.next()) {
                StoredMyPet pet = petFromRow(owner, rs);
                if (pet != null) pets.add(pet);
            }
        } catch (SQLException e) {
            reportError(e);
        }
        return pets;
    }

    /**
     * Load every pet row whose owner is also loadable (rows with an orphan
     * {@code owner_uuid} are skipped). Used by the data migration and admin
     * tooling; runs synchronously on the calling thread.
     *
     * <p>The {@link #getAllMyPetPlayers()} call happens before opening the pets
     * cursor so SQLite's single shared connection is not holding two concurrent
     * statements — see the note on {@link #acquireConnection()}.
     */
    @Override
    public List<StoredMyPet> getAllPets() {
        List<MyPetPlayer> playerList = getAllMyPetPlayers();
        Map<UUID, MyPetPlayer> owners = new HashMap<>();
        for (MyPetPlayer p : playerList) owners.put(p.getUniqueId(), p);

        try (ConnectionHolder h = acquireConnection();
             Statement stmt = h.connection().createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM " + qualifyTable("pets") + ";")) {
            List<StoredMyPet> pets = new ArrayList<>();
            while (rs.next()) {
                UUID ownerId = UUID.fromString(rs.getString("owner_uuid"));
                MyPetPlayer owner = owners.get(ownerId);
                if (owner == null) continue;
                StoredMyPet pet = petFromRow(owner, rs);
                if (pet != null) pets.add(pet);
            }
            return pets;
        } catch (SQLException e) {
            reportError(e);
        }
        return new ArrayList<>();
    }

    /**
     * Load every pet belonging to {@code owner}. Returns an already-completed
     * empty list if {@code owner} is {@code null}. Throws
     * {@link CompletionException} on query error.
     */
    @Override
    public CompletableFuture<List<StoredMyPet>> getPets(final MyPetPlayer owner) {
        if (owner == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT * FROM " + qualifyTable("pets") + " WHERE owner_uuid=?;")) {
                stmt.setString(1, owner.getUniqueId().toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return petsFromResultSet(owner, rs);
                }
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Load a single pet by UUID. Returns {@code null} if the pet doesn't exist,
     * its owner isn't currently in the player manager, or the plugin is
     * disabling (so callback chains don't trigger during shutdown).
     *
     * <p>Consults {@link #petsToBeSaved} first — a read that races a pending
     * update returns the in-flight value rather than a stale DB row.
     * Throws {@link CompletionException} on query error (distinguishing
     * database failure from a legitimate null).
     */
    @Override
    public CompletableFuture<StoredMyPet> getPet(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!MyPetApi.getPlugin().isEnabled()) return null;
            StoredMyPet pending = petsToBeSaved.get(uuid);
            if (pending != null) return pending;
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT * FROM " + qualifyTable("pets") + " WHERE uuid=?;")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(
                                UUID.fromString(rs.getString("owner_uuid")));
                        if (owner != null) {
                            return petFromRow(owner, rs);
                        }
                    }
                }
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
            return null;
        }, executor);
    }

    // Players ---------------------------------------------------------------------------------------------------------

    /**
     * Read the next row of {@code rs} into a {@link MyPetPlayer}. Returns
     * {@code null} when the cursor is exhausted — {@link #getAllMyPetPlayers}
     * uses that to terminate its loop. SQL errors are logged and {@code null}
     * is returned (the caller can't distinguish error from end-of-cursor; the
     * log line is the signal).
     */
    protected MyPetPlayer resultSetToMyPetPlayer(ResultSet rs) {
        try {
            if (rs.next()) {
                UUID mojangUUID = UUID.fromString(rs.getString("uuid"));
                MyPetPlayerImpl petPlayer = new MyPetPlayerImpl(mojangUUID);

                petPlayer.setAutoRespawnEnabled(rs.getBoolean("auto_respawn"));
                petPlayer.setAutoRespawnMin(rs.getInt("auto_respawn_min"));
                petPlayer.setCaptureHelperActive(rs.getBoolean("capture_mode"));
                petPlayer.setHealthBarActive(rs.getBoolean("health_bar"));
                petPlayer.setPetLivingSoundVolume(rs.getFloat("pet_idle_volume"));
                try {
                    byte[] extended = readBlob(rs, "extended_info");
                    petPlayer.setExtendedInfo(NbtUtil.readCompressed(extended));
                } catch (IOException e) {
                    MyPetApi.getLogger().warning("Extended info of player (" + mojangUUID + ") could not be loaded!");
                }

                readPlayerMultiWorld(rs, petPlayer);
                return petPlayer;
            }
        } catch (SQLException e) {
            reportError(e);
        }
        return null;
    }

    /**
     * Load every player row. Runs synchronously on the calling thread; used
     * during startup to hydrate the player manager and from {@link #getAllPets}
     * to resolve pet owners.
     */
    @Override
    public List<MyPetPlayer> getAllMyPetPlayers() {
        try (ConnectionHolder h = acquireConnection();
             Statement stmt = h.connection().createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM " + qualifyTable("players") + ";")) {
            List<MyPetPlayer> players = new ArrayList<>();
            MyPetPlayer player;
            while ((player = resultSetToMyPetPlayer(rs)) != null) {
                players.add(player);
            }
            return players;
        } catch (SQLException e) {
            reportError(e);
        }
        return new ArrayList<>();
    }

    /**
     * Load a single player row by Mojang UUID. Returns {@code null} when no
     * row exists; throws {@link CompletionException} on query error.
     */
    @Override
    public CompletableFuture<MyPetPlayer> getMyPetPlayer(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "SELECT * FROM " + qualifyTable("players") + " WHERE uuid=?;")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return resultSetToMyPetPlayer(rs);
                }
            } catch (SQLException e) {
                reportError(e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /** Convenience overload; delegates to {@link #getMyPetPlayer(UUID)}. */
    @Override
    public CompletableFuture<MyPetPlayer> getMyPetPlayer(final Player player) {
        return getMyPetPlayer(player.getUniqueId());
    }

    // Pets (writes) ---------------------------------------------------------------------------------------------------

    /**
     * Synchronous UPDATE of one pet row. Shared by {@link #savePet} (async
     * single write) and {@link #savePets} (batch during periodic / shutdown
     * saves). Returns {@code true} if the UPDATE touched a row.
     */
    protected boolean savePetSync(StoredMyPet myPet) {
        try (ConnectionHolder h = acquireConnection();
             PreparedStatement stmt = h.connection().prepareStatement(
                     "UPDATE " + qualifyTable("pets") + " SET " +
                             "owner_uuid=?, exp=?, health=?, respawn_time=?, name=?, type=?, " +
                             "last_used=?, hunger=?, world_group=?, wants_to_spawn=?, " +
                             "skilltree=?, skills=?, info=? " +
                             "WHERE uuid=?;")) {
            stmt.setString(1, myPet.getOwner().getUniqueId().toString());
            stmt.setDouble(2, myPet.getExp());
            stmt.setDouble(3, myPet.getHealth());
            stmt.setInt(4, myPet.getRespawnTime());
            bindPetName(stmt, 5, myPet.getPetName());
            stmt.setString(6, myPet.getPetType().name());
            stmt.setLong(7, myPet.getLastUsed());
            stmt.setDouble(8, myPet.getSaturation());
            stmt.setString(9, myPet.getWorldGroup());
            stmt.setBoolean(10, myPet.wantsToRespawn());
            stmt.setString(11, myPet.getSkilltree() != null ? myPet.getSkilltree().getName() : null);
            bindBlob(stmt, 12, NbtUtil.writeCompressed(myPet.getSkillInfo()));
            bindBlob(stmt, 13, serializeInfo(myPet));
            stmt.setString(14, myPet.getUUID().toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException | IOException e) {
            reportError(e);
            return false;
        }
    }

    /** Async wrapper over {@link #savePetSync}. */
    @Override
    public CompletableFuture<Boolean> savePet(StoredMyPet myPet) {
        return CompletableFuture.supplyAsync(() -> savePetSync(myPet), executor);
    }

    /**
     * INSERT a new pet row. Returns {@code true} if one row was inserted,
     * {@code false} on any error (including primary-key collision — which in
     * practice means the caller should have used {@link #updatePet}).
     */
    @Override
    public CompletableFuture<Boolean> addPet(final StoredMyPet storedMyPet) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "INSERT INTO " + qualifyTable("pets") + " (uuid, owner_uuid, exp, health, " +
                                 "respawn_time, name, type, last_used, hunger, world_group, " +
                                 "wants_to_spawn, skilltree, skills, info) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                stmt.setString(1, storedMyPet.getUUID().toString());
                stmt.setString(2, storedMyPet.getOwner().getUniqueId().toString());
                stmt.setDouble(3, storedMyPet.getExp());
                stmt.setDouble(4, storedMyPet.getHealth());
                stmt.setInt(5, storedMyPet.getRespawnTime());
                stmt.setString(6, storedMyPet.getPetName());
                stmt.setString(7, storedMyPet.getPetType().name());
                stmt.setLong(8, storedMyPet.getLastUsed());
                stmt.setDouble(9, storedMyPet.getSaturation());
                stmt.setString(10, storedMyPet.getWorldGroup());
                stmt.setBoolean(11, storedMyPet.wantsToRespawn());
                stmt.setString(12, storedMyPet.getSkilltree() != null
                        ? storedMyPet.getSkilltree().getName() : null);
                bindBlob(stmt, 13, NbtUtil.writeCompressed(storedMyPet.getSkillInfo()));
                bindBlob(stmt, 14, serializeInfo(storedMyPet));
                return stmt.executeUpdate() > 0;
            } catch (SQLException | IOException e) {
                reportError(e);
                return false;
            }
        }, executor);
    }

    /**
     * UPDATE an existing pet row. Enqueues the pet in {@link #petsToBeSaved}
     * before submitting the async write; the entry is removed only when the
     * UPDATE confirms a row was touched. Entries that survive a failed UPDATE
     * are retried on the next periodic save (see {@link #savePets}).
     *
     * <p>Returns {@code false} on any backend error. Never throws — even though
     * the underlying operation is async, callers don't need
     * {@code .exceptionally()} to avoid uncaught exceptions.
     */
    @Override
    public CompletableFuture<Boolean> updatePet(final StoredMyPet storedMyPet) {
        petsToBeSaved.put(storedMyPet.getUUID(), storedMyPet);
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "UPDATE " + qualifyTable("pets") + " SET " +
                                 "owner_uuid=?, exp=?, health=?, respawn_time=?, name=?, type=?, " +
                                 "last_used=?, hunger=?, world_group=?, wants_to_spawn=?, " +
                                 "skilltree=?, skills=?, info=? " +
                                 "WHERE uuid=?;")) {
                stmt.setString(1, storedMyPet.getOwner().getUniqueId().toString());
                stmt.setDouble(2, storedMyPet.getExp());
                stmt.setDouble(3, storedMyPet.getHealth());
                stmt.setInt(4, storedMyPet.getRespawnTime());
                bindPetName(stmt, 5, storedMyPet.getPetName());
                stmt.setString(6, storedMyPet.getPetType().name());
                stmt.setLong(7, storedMyPet.getLastUsed());
                stmt.setDouble(8, storedMyPet.getSaturation());
                stmt.setString(9, storedMyPet.getWorldGroup());
                stmt.setBoolean(10, storedMyPet.wantsToRespawn());
                stmt.setString(11, storedMyPet.getSkilltree() != null
                        ? storedMyPet.getSkilltree().getName() : null);
                bindBlob(stmt, 12, NbtUtil.writeCompressed(storedMyPet.getSkillInfo()));
                bindBlob(stmt, 13, serializeInfo(storedMyPet));
                stmt.setString(14, storedMyPet.getUUID().toString());
                int result = stmt.executeUpdate();
                if (result > 0) {
                    petsToBeSaved.remove(storedMyPet.getUUID());
                }
                return result > 0;
            } catch (SQLException | IOException e) {
                reportError(e);
                return false;
            }
        }, executor);
    }

    // Players (writes) ------------------------------------------------------------------------------------------------

    /**
     * Synchronous UPDATE of one player row. Shared by {@link #updateMyPetPlayer}
     * (async single write), {@link #savePlayer} (called from the batch saver),
     * and external callers that hold the executor themselves.
     * Returns {@code true} if the UPDATE touched a row.
     */
    public boolean updatePlayer(final MyPetPlayer player) {
        try (ConnectionHolder h = acquireConnection();
             PreparedStatement stmt = h.connection().prepareStatement(
                     "UPDATE " + qualifyTable("players") + " SET " +
                             "auto_respawn=?, auto_respawn_min=?, capture_mode=?, health_bar=?, " +
                             "pet_idle_volume=?, extended_info=?, multi_world=? " +
                             "WHERE uuid=?;")) {
            stmt.setBoolean(1, player.hasAutoRespawnEnabled());
            stmt.setInt(2, player.getAutoRespawnMin());
            stmt.setBoolean(3, player.isCaptureHelperActive());
            stmt.setBoolean(4, player.isHealthBarActive());
            stmt.setFloat(5, player.getPetLivingSoundVolume());
            bindBlob(stmt, 6, NbtUtil.writeCompressed(player.getExtendedInfo()));

            JsonObject multiWorldObject = new JsonObject();
            for (String g : player.getMyPetsForWorldGroups().keySet()) {
                multiWorldObject.addProperty(g, player.getMyPetsForWorldGroups().get(g).toString());
            }
            stmt.setString(7, gson.toJson(multiWorldObject));
            stmt.setString(8, player.getUniqueId().toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException | IOException e) {
            reportError(e);
            return false;
        }
    }

    /**
     * Async UPDATE of a player row. Uses the same enqueue-before-write pattern
     * as {@link #updatePet} via {@link #playersToBeSaved}, so failed writes
     * are retried on the next periodic save.
     */
    @Override
    public CompletableFuture<Boolean> updateMyPetPlayer(final MyPetPlayer player) {
        playersToBeSaved.put(player.getUniqueId(), player);
        return CompletableFuture.supplyAsync(() -> {
            boolean ok = updatePlayer(player);
            if (ok) playersToBeSaved.remove(player.getUniqueId());
            return ok;
        }, executor);
    }

    private void savePlayer(MyPetPlayer player) {
        updatePlayer(player);
    }

    /**
     * INSERT a new player row. Returns {@code false} on primary-key collision
     * or any other backend error — caller should use {@link #updateMyPetPlayer}
     * for existing rows.
     */
    @Override
    public CompletableFuture<Boolean> addMyPetPlayer(final MyPetPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            try (ConnectionHolder h = acquireConnection();
                 PreparedStatement stmt = h.connection().prepareStatement(
                         "INSERT INTO " + qualifyTable("players") + " (uuid, auto_respawn, " +
                                 "auto_respawn_min, capture_mode, health_bar, pet_idle_volume, " +
                                 "extended_info, multi_world) VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setBoolean(2, player.hasAutoRespawnEnabled());
                stmt.setInt(3, player.getAutoRespawnMin());
                stmt.setBoolean(4, player.isCaptureHelperActive());
                stmt.setBoolean(5, player.isHealthBarActive());
                stmt.setFloat(6, player.getPetLivingSoundVolume());
                bindBlob(stmt, 7, NbtUtil.writeCompressed(player.getExtendedInfo()));
                JsonObject multiWorldObject = new JsonObject();
                for (String g : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldObject.addProperty(g, player.getMyPetsForWorldGroups().get(g).toString());
                }
                stmt.setString(8, gson.toJson(multiWorldObject));
                return stmt.executeUpdate() > 0;
            } catch (SQLException | IOException e) {
                reportError(e);
                return false;
            }
        }, executor);
    }

    // Save / batch ----------------------------------------------------------------------------------------------------

    /** {@link Repository#save()} entry point; delegates to {@link #saveData()}. */
    @Override
    public void save() { saveData(); }

    /**
     * Flush all live and pending state to the database synchronously:
     * {@code info} version row, every active + pending pet, every active +
     * pending player. Called from {@link #disable()} and from the scheduled
     * periodic save task. Runs on the calling thread (not the executor), so
     * it's safe to invoke during shutdown after the executor has drained.
     */
    public void saveData() {
        updateInfo();
        savePets();
        savePlayers();
    }

    /**
     * Stamp the {@code info} row with the running plugin version and build
     * number. Runs every {@link #saveData()} so operators can confirm which
     * version last touched the database.
     */
    protected void updateInfo() {
        try (ConnectionHolder h = acquireConnection();
             PreparedStatement stmt = h.connection().prepareStatement(
                     "UPDATE " + qualifyTable("info") + " SET mypet_version=?, mypet_build=?;")) {
            stmt.setString(1, VersionUtil.getVersion());
            stmt.setString(2, VersionUtil.getBuild());
            stmt.executeUpdate();
        } catch (SQLException e) {
            reportError(e);
        }
    }

    private void savePets() {
        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            savePetSync(myPet);
        }
        for (StoredMyPet myPet : petsToBeSaved.values()) {
            savePetSync(myPet);
        }
    }

    private void savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
            savePlayer(player);
        }
        for (MyPetPlayer player : playersToBeSaved.values()) {
            savePlayer(player);
        }
    }

    /**
     * Bulk INSERT every pet in {@code pets} as a single JDBC batch, flushing
     * every 500 rows to bound memory. Used by {@code Converter} when migrating
     * between backends. Synchronous on the calling thread.
     * Returns {@code true} only if every batch executed without error.
     */
    public boolean addPets(List<StoredMyPet> pets) {
        try (ConnectionHolder h = acquireConnection();
             PreparedStatement stmt = h.connection().prepareStatement(
                     "INSERT INTO " + qualifyTable("pets") + " (uuid, owner_uuid, exp, health, " +
                             "respawn_time, name, type, last_used, hunger, world_group, " +
                             "wants_to_spawn, skilltree, skills, info) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            int i = 0;
            for (StoredMyPet storedMyPet : pets) {
                stmt.setString(1, storedMyPet.getUUID().toString());
                stmt.setString(2, storedMyPet.getOwner().getUniqueId().toString());
                stmt.setDouble(3, storedMyPet.getExp());
                stmt.setDouble(4, storedMyPet.getHealth());
                stmt.setInt(5, storedMyPet.getRespawnTime());
                stmt.setString(6, storedMyPet.getPetName());
                stmt.setString(7, storedMyPet.getPetType().name());
                stmt.setLong(8, storedMyPet.getLastUsed());
                stmt.setDouble(9, storedMyPet.getSaturation());
                stmt.setString(10, storedMyPet.getWorldGroup());
                stmt.setBoolean(11, storedMyPet.wantsToRespawn());
                stmt.setString(12, storedMyPet.getSkilltree() != null
                        ? storedMyPet.getSkilltree().getName() : null);
                bindBlob(stmt, 13, NbtUtil.writeCompressed(storedMyPet.getSkillInfo()));
                bindBlob(stmt, 14, serializeInfo(storedMyPet));
                stmt.addBatch();
                if (++i % 500 == 0 && i != pets.size()) {
                    stmt.executeBatch();
                }
            }
            stmt.executeBatch();
            return true;
        } catch (SQLException | IOException e) {
            reportError(e);
        }
        return false;
    }

    /**
     * Bulk INSERT every player in {@code players} as a single JDBC batch,
     * flushing every 500 rows. Skips rows with a null UUID or duplicates
     * within the batch (both are logged). Used by {@code Converter}.
     */
    public boolean addMyPetPlayers(List<MyPetPlayer> players) {
        try (ConnectionHolder h = acquireConnection();
             PreparedStatement stmt = h.connection().prepareStatement(
                     "INSERT INTO " + qualifyTable("players") + " (uuid, auto_respawn, " +
                             "auto_respawn_min, capture_mode, health_bar, pet_idle_volume, " +
                             "extended_info, multi_world) VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
            int i = 0;
            HashSet<UUID> playerUUIDs = new HashSet<>();
            for (MyPetPlayer player : players) {
                UUID mojangUUID = player.getUniqueId();
                if (mojangUUID == null) {
                    MyPetApi.getLogger().warning("Skipping player with no uuid: " + player);
                    continue;
                }
                if (playerUUIDs.contains(mojangUUID)) {
                    MyPetApi.getLogger().info("Found duplicate Player: " + player);
                    continue;
                }
                playerUUIDs.add(mojangUUID);

                stmt.setString(1, mojangUUID.toString());
                stmt.setBoolean(2, player.hasAutoRespawnEnabled());
                stmt.setInt(3, player.getAutoRespawnMin());
                stmt.setBoolean(4, player.isCaptureHelperActive());
                stmt.setBoolean(5, player.isHealthBarActive());
                stmt.setFloat(6, player.getPetLivingSoundVolume());
                bindBlob(stmt, 7, NbtUtil.writeCompressed(player.getExtendedInfo()));
                JsonObject multiWorldObject = new JsonObject();
                for (String g : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldObject.addProperty(g, player.getMyPetsForWorldGroups().get(g).toString());
                }
                stmt.setString(8, gson.toJson(multiWorldObject));
                stmt.addBatch();
                if (++i % 500 == 0 && i != players.size()) {
                    stmt.executeBatch();
                }
            }
            stmt.executeBatch();
            return true;
        } catch (SQLException | IOException e) {
            reportError(e);
        }
        return false;
    }
}
