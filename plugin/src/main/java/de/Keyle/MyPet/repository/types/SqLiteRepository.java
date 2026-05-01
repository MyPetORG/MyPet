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

package de.Keyle.MyPet.repository.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.util.VersionUtil;
import de.Keyle.MyPet.api.repository.RepositoryInitException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.Executors;

public class SqLiteRepository extends AbstractSqlRepository {

    private Connection connection;

    @Override
    protected ConnectionHolder acquireConnection() {
        // Single shared connection; close() is a no-op. SQLite JDBC is not
        // thread-safe, so only the single-threaded executor touches this.
        return new ConnectionHolder() {
            @Override public Connection connection() { return connection; }
            @Override public void close() { /* connection outlives individual calls */ }
        };
    }

    @Override
    protected void disableBackend() {
        try {
            connection.close();
        } catch (SQLException e) {
            reportError(e);
        }
    }

    private boolean schemaExists(Connection c) throws SQLException {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='info';");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        }
    }

    @Override
    protected String qualifyTable(String baseName) { return baseName; }

    @Override
    protected void bindBlob(PreparedStatement s, int idx, byte[] data) throws SQLException {
        s.setBytes(idx, data);
    }

    @Override
    protected byte[] readBlob(ResultSet rs, String col) throws SQLException {
        return rs.getBytes(col);
    }

    @Override
    protected void bindPetName(PreparedStatement s, int idx, String name) throws SQLException {
        s.setBytes(idx, name.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected String readPetName(ResultSet rs, String col) throws SQLException {
        byte[] bytes = rs.getBytes(col);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    protected String dbLabel() { return "SQLite"; }

    @Override
    public Connection openIsolatedConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    private File dbFile;

    @Override
    public void init() throws RepositoryInitException {
        try {
            dbFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "pets.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // Single-thread executor so the JDBC Connection is only touched by one thread.
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MyPet-SQLite");
                t.setDaemon(true);
                return t;
            });

            try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='info';");
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    updateInfo();
                } else {
                    initStructure();
                }
            }
        } catch (Exception e) {
            reportError(e);
            throw new RepositoryInitException(e);
        }
    }

    private void initStructure() {
        try (Statement create = connection.createStatement()) {
            create.executeUpdate("CREATE TABLE pets (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36) NOT NULL , " +
                    "exp DOUBLE, " +
                    "health DOUBLE, " +
                    "respawn_time INTEGER, " +
                    "name VARCHAR(1024), " +
                    "type VARCHAR(20), " +
                    "last_used BIGINT, " +
                    "hunger INTEGER, " +
                    "world_group VARCHAR(255), " +
                    "wants_to_spawn BOOLEAN, " +
                    "skilltree VARCHAR(255), " +
                    "skills BLOB, " +
                    "info BLOB, " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            createTimestampTrigger("pets", "last_update", "uuid");

            create.executeUpdate("CREATE TABLE players (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "auto_respawn BOOLEAN, " +
                    "auto_respawn_min INTEGER , " +
                    "capture_mode BOOLEAN, " +
                    "health_bar INTEGER, " +
                    "pet_idle_volume FLOAT, " +
                    "extended_info BLOB, " +
                    "multi_world VARCHAR(2000), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            createTimestampTrigger("players", "last_update", "uuid");

            create.executeUpdate("CREATE TABLE info (" +
                    "mypet_version VARCHAR(20), " +
                    "mypet_build VARCHAR(20), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO info (mypet_version, mypet_build) VALUES (?,?);")) {
                insert.setString(1, VersionUtil.getVersion());
                insert.setString(2, VersionUtil.getBuild());
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            reportError(e);
        }
    }

    private void createTimestampTrigger(String table, String column, String id) {
        try (Statement create = connection.createStatement()) {
            create.execute("CREATE TRIGGER [update_time_trigger_" + table + "] " +
                    "AFTER UPDATE ON " + table + " FOR EACH ROW " +
                    "WHEN NEW." + column + " < OLD." + column + " " +
                    "BEGIN " +
                    "  UPDATE " + table +
                    "    SET " + column + "=CURRENT_TIMESTAMP " +
                    "    WHERE NEW." + id + "=OLD." + id + ";" +
                    "END;");
        } catch (SQLException e) {
            reportError(e);
        }
    }

}
