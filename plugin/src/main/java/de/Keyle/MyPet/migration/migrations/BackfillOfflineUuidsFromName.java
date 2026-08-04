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

package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.migration.DatabaseMigration;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.SqlMigrationContext;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Fills {@code players.mojang_uuid} for rows left NULL by an offline-mode MyPet 3
 * install, deriving each player's offline UUID from their stored name.
 * <p>
 * MyPet 3 only wrote {@code mojang_uuid} when the joining player's UUID differed
 * from their name-derived offline UUID. That test is per-player, not per-server —
 * {@code d21a5d100} deliberately replaced an {@code isInOnlineMode()} gate with it so
 * auth-injecting proxies (FastLogin and similar), which hand a nominally offline-mode
 * server real premium UUIDs for some players, record those correctly. So a NULL here
 * means precisely "this player last joined presenting their offline UUID", which is the
 * one thing the derivation needs. Do not re-gate this on {@link Bukkit#getOnlineMode()}.
 * <p>
 * Downstream migrations treat NULL as unmigratable and drop the row, losing the player
 * and orphaning their pets.
 * <p>
 * Must run BEFORE {@code TranslatePetOwnerUuidToMojang}, which declares the
 * dependency. Rows with no usable name cannot be recovered and are left for
 * {@code RebuildPlayersSchemaOnMojangUuid} to drop.
 */
@Migration(
        version = "4.0.0",
        description = "Derive offline-mode UUIDs from stored player names"
)
public class BackfillOfflineUuidsFromName implements DatabaseMigration {

    private static final int SAMPLE_LIMIT = 10;
    private static final Logger LOG = Logger.getLogger("MyPet");

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        // Usernames are client-supplied on an offline-mode server, so every write below
        // goes through a PreparedStatement. Neither SqlMigrationContext#execute nor
        // #queryAndMap accepts bind parameters, so the Connection is needed for the
        // parameterized UPDATE — same as RebuildPlayersSchemaOnMojangUuid. Reads with no
        // untrusted input still go through the interface (see queryAndMap below).
        if (!(ctx instanceof SqlMigrationContextImpl impl)) {
            throw new MigrationException("SqlMigrationContext is not a SqlMigrationContextImpl; "
                    + "cannot reach underlying Connection for parameterized updates.");
        }
        Connection connection = impl.getConnection();
        String playersTable = ctx.getTablePrefix() + "players";

        try {
            boolean hasMojangUuid = hasColumn(connection, playersTable, "mojang_uuid");
            boolean hasName = hasColumn(connection, playersTable, "name");
            if (!hasMojangUuid && !hasName) {
                LOG.info("players table is already on the v4 shape — nothing to backfill.");
                return;
            } else if (!hasMojangUuid || !hasName) {
                // Partial-rebuild state, not a clean v4 shape. In practice only one half of
                // this is reachable: RebuildPlayersSchemaOnMojangUuid#rebuildMysql drops "name"
                // first, then internal_uuid, then renames mojang_uuid — so an interrupted rebuild
                // leaves mojang_uuid without name. The mirror state cannot occur, because once
                // mojang_uuid is renamed internal_uuid is already gone, making the install
                // NORMAL_4X rather than UPGRADE_3X, so this migration is COMPLETE and never
                // re-runs to observe it. The condition still covers both: nothing is derivable
                // without a name either way, and skipping beats failing on a SELECT against a
                // column that is not there.
                String missing = hasName ? "mojang_uuid" : "name";
                LOG.warning("players table is missing column \"" + missing + "\" but not the "
                        + "other legacy column — this indicates a partially-completed "
                        + "RebuildPlayersSchemaOnMojangUuid rebuild. Nothing to backfill without "
                        + "a name; skipping. The pre-migration backup is the only route back to "
                        + "this data.");
                return;
            }
        } catch (SQLException e) {
            throw new MigrationException("Failed to inspect players columns", e);
        }

        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new MigrationException("Failed to begin transaction for the offline UUID backfill", e);
        }

        // Hoisted out of the try block so the post-condition below can compare against it —
        // the two classifications must be the exact same count, not just SQL string semantics
        // that happen to agree with the Java isBlank() skip.
        long unresolvableCount = 0;
        boolean committed = false;

        try {
            Map<String, String> resolvable = new LinkedHashMap<>(); // internal_uuid -> name
            List<String> unresolvableSamples = new ArrayList<>();

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT internal_uuid, name FROM " + playersTable
                                 + " WHERE mojang_uuid IS NULL")) {
                while (rs.next()) {
                    String internalUuid = rs.getString("internal_uuid");
                    String name = rs.getString("name");
                    if (name == null || name.isBlank()) {
                        unresolvableCount++;
                        if (unresolvableSamples.size() < SAMPLE_LIMIT) {
                            unresolvableSamples.add(internalUuid);
                        }
                        continue;
                    }
                    resolvable.put(internalUuid, name);
                }
            }

            // No dedup guard on the derived UUID, deliberately. offlineUuid() is a pure function
            // of the name, so two rows sharing a name would derive the same UUID and collide.
            // players.name is declared UNIQUE in the 3.x CREATE TABLE on both backends (since
            // 0e8bc2fdd, 2016), and MyPet 3 only ever inserted after a `mojang_uuid=? OR name=?`
            // lookup missed, so duplicates are very hard to produce — but not impossible: a MySQL
            // install upgraded through MySqlRepository#updateToV6 gets the constraint via
            // `ALTER IGNORE TABLE ... ADD UNIQUE (name)`, and ALTER IGNORE was removed in MySQL
            // 5.7. There the statement throws, the error is swallowed, and the schema version is
            // bumped regardless — leaving a "current" table with no UNIQUE on name.
            //
            // A collision therefore surfaces as a UNIQUE violation, a failed migration, and a
            // pre-migration backup on disk. That is the intended outcome: two players claiming
            // one identity is ambiguous, and silently discarding or fusing one of them is worse
            // than stopping and saying so. Note the failure recurs every boot until an admin
            // intervenes, since a FAILED record is retried rather than skipped.
            if (!resolvable.isEmpty()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE " + playersTable + " SET mojang_uuid=? WHERE internal_uuid=?")) {
                    int i = 0;
                    for (Map.Entry<String, String> entry : resolvable.entrySet()) {
                        ps.setString(1, offlineUuid(entry.getValue()).toString());
                        ps.setString(2, entry.getKey());
                        ps.addBatch();
                        // Chunk every 500 rows — matches AbstractSqlRepository#addMyPetPlayers.
                        if (++i % 500 == 0 && i != resolvable.size()) {
                            ps.executeBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }

            connection.commit();
            committed = true;

            LOG.info("Offline-mode UUID backfill complete. Server online-mode: "
                    + Bukkit.getOnlineMode() + ".");
            LOG.info("  Players given a derived offline UUID: " + resolvable.size() + ".");
            if (unresolvableCount > 0) {
                LOG.warning("" + unresolvableCount + " player rows have no Mojang UUID and no "
                        + "usable name — nothing identifies them, so they cannot be recovered and "
                        + "will be dropped by the players-rebuild migration. Sample internal_uuid "
                        + "values: " + unresolvableSamples
                        + (unresolvableCount > unresolvableSamples.size()
                                ? " (showing first " + unresolvableSamples.size() + ")" : ""));
            }
        } catch (SQLException e) {
            throw new MigrationException("Offline UUID backfill failed — rolled back", e);
        } finally {
            // A non-SQLException escaping the try body would otherwise skip straight to
            // setAutoCommit(true) below, which per the JDBC contract commits whatever is
            // still open. Roll back first whenever we didn't reach a successful commit.
            if (!committed) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LOG.warning("Failed to roll back the offline UUID backfill transaction: "
                            + rollbackEx.getMessage());
                }
            }
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // Leaving autoCommit in its new state is not worth failing the migration for.
            }
        }

        // Post-condition: every row still NULL must be exactly one we skipped. Compared
        // against the Java-side count, so no SQL string semantics are involved and the two
        // classifications cannot drift (e.g. "\t" is blank to Java's isBlank() but not to
        // SQLite/MySQL TRIM(), which strip only U+0020).
        long stillNull = ctx.queryAndMap(
                "SELECT COUNT(*) FROM " + playersTable + " WHERE mojang_uuid IS NULL",
                rs -> rs.next() ? rs.getLong(1) : -1L);
        if (stillNull != unresolvableCount) {
            throw new MigrationException("Post-condition failed: " + stillNull
                    + " player rows still have no UUID but only " + unresolvableCount
                    + " were unrecoverable.");
        }
    }

    /**
     * Bukkit's offline-mode UUID for a username. Byte-for-byte identical to
     * {@code CraftServer#getOfflinePlayer(String)} — no trimming, no case folding,
     * because in offline mode the exact name string is the identity.
     */
    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }
        // Case sensitivity on some backends — try lowercase table name too.
        try (ResultSet rs = meta.getColumns(null, null, table.toLowerCase(), column)) {
            return rs.next();
        }
    }
}
