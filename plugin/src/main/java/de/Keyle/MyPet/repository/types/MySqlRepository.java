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

package de.Keyle.MyPet.repository.types;

import com.zaxxer.hikari.HikariDataSource;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.util.VersionUtil;
import de.Keyle.MyPet.repository.RepositoryInitException;
import de.Keyle.MyPet.util.NbtUtil;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MySqlRepository extends AbstractSqlRepository {

    private static final Pattern VALID_PREFIX = Pattern.compile("^[A-Za-z0-9_]*$");

    private HikariDataSource dataSource;

    @Override
    protected ConnectionHolder acquireConnection() throws SQLException {
        Connection c = dataSource.getConnection();
        return new ConnectionHolder() {
            @Override public Connection connection() { return c; }
            @Override public void close() {
                try { c.close(); } catch (SQLException e) {
                    reportError(e);
                }
            }
        };
    }

    @Override
    protected void disableBackend() { dataSource.close(); }

    private boolean schemaExists(Connection c) throws SQLException {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT * FROM " + MyPetGlobal.Repository.MySQL.PREFIX.get() + "info;");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        } catch (SQLSyntaxErrorException e) {
            return false;
        }
    }

    @Override
    protected String qualifyTable(String baseName) {
        return MyPetGlobal.Repository.MySQL.PREFIX.get() + baseName;
    }

    @Override
    protected void bindBlob(PreparedStatement s, int idx, byte[] data) throws SQLException {
        s.setBlob(idx, new ByteArrayInputStream(data));
    }

    @Override
    protected byte[] readBlob(ResultSet rs, String col) throws SQLException {
        Blob blob = rs.getBlob(col);
        if (blob == null) return null;
        return blob.getBytes(1, (int) blob.length());
    }

    @Override
    protected void bindPetName(PreparedStatement s, int idx, String name) throws SQLException {
        s.setBinaryStream(idx, new ByteArrayInputStream(
                name.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    protected String readPetName(ResultSet rs, String col) throws SQLException {
        return readStreamAsString(rs.getBinaryStream(col), StandardCharsets.UTF_8);
    }

    private static String readStreamAsString(InputStream is, Charset charset) {
        try {
            return new String(is.readAllBytes(), charset);
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    protected String dbLabel() { return "MySQL"; }

    @Override
    public Connection openIsolatedConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void init() throws RepositoryInitException {
        String prefix = MyPetGlobal.Repository.MySQL.PREFIX.get();
        if (!VALID_PREFIX.matcher(prefix).matches()) {
            throw new RepositoryInitException(new IllegalArgumentException(
                    "Invalid MySQL table prefix \"" + prefix + "\". "
                            + "Prefix must match [A-Za-z0-9_]* to prevent SQL injection / schema corruption."));
        }
        this.dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://" +
                MyPetGlobal.Repository.MySQL.HOST.get() + ":" + MyPetGlobal.Repository.MySQL.PORT.get() + "/" +
                MyPetGlobal.Repository.MySQL.DATABASE.get() + (MyPetGlobal.Repository.MySQL.DATABASE.get().contains("?") ? "&" : "?") + "useUnicode=true&characterEncoding=" + MyPetGlobal.Repository.MySQL.CHARACTER_ENCODING.get());
        dataSource.setUsername(MyPetGlobal.Repository.MySQL.USER.get());
        dataSource.setPassword(MyPetGlobal.Repository.MySQL.PASSWORD.get());
        dataSource.setMaximumPoolSize(MyPetGlobal.Repository.MySQL.POOL_SIZE.get());
        dataSource.addDataSourceProperty("cachePrepStmts", true);
        dataSource.setLeakDetectionThreshold(10000);

        this.executor = Executors.newFixedThreadPool(
                Math.max(1, MyPetGlobal.Repository.MySQL.POOL_SIZE.get()),
                r -> {
                    Thread t = new Thread(r, "MyPet-MySQL");
                    t.setDaemon(true);
                    return t;
                });

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + MyPetGlobal.Repository.MySQL.PREFIX.get() + "info;");
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                updateInfo();
            } else {
                initStructure();
            }
        } catch (SQLSyntaxErrorException e) {
            initStructure();
        } catch (Exception e) {
            throw new RepositoryInitException(e);
        }

        startPeriodicFlush();
    }

    private void initStructure() {
        try (Connection connection = dataSource.getConnection();
             Statement create = connection.createStatement()) {

            create.executeUpdate("CREATE TABLE " + MyPetGlobal.Repository.MySQL.PREFIX.get() + "pets (" +
                    "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "owner_uuid VARCHAR(36) NOT NULL , " +
                    "exp DOUBLE, " +
                    "health DOUBLE, " +
                    "respawn_time INTEGER, " +
                    "name VARBINARY(1024), " +
                    "type VARCHAR(20), " +
                    "last_used BIGINT, " +
                    "hunger INTEGER, " +
                    "world_group VARCHAR(255), " +
                    "wants_to_spawn BOOLEAN, " +
                    "skilltree VARCHAR(255), " +
                    "skills BLOB, " +
                    "info BLOB, " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY ( uuid ), " +
                    "INDEX `owner_uuid` (`owner_uuid`)" +
                    ")");

            create.executeUpdate("CREATE TABLE " + MyPetGlobal.Repository.MySQL.PREFIX.get() + "players (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "auto_respawn BOOLEAN, " +
                    "auto_respawn_min INTEGER , " +
                    "capture_mode BOOLEAN, " +
                    "health_bar INTEGER, " +
                    "pet_idle_volume FLOAT, " +
                    "extended_info BLOB, " +
                    "multi_world VARCHAR(2000), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY ( uuid )" +
                    ")");

            create.executeUpdate("CREATE TABLE " + MyPetGlobal.Repository.MySQL.PREFIX.get() + "info (" +
                    "mypet_version VARCHAR(20), " +
                    "mypet_build VARCHAR(20), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + MyPetGlobal.Repository.MySQL.PREFIX.get() + "info (mypet_version, mypet_build) VALUES (?,?);")) {
                insert.setString(1, VersionUtil.getVersion());
                insert.setString(2, VersionUtil.getBuild());
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            reportError(e);
        }
    }

    // Players ---------------------------------------------------------------------------------------------------------

    @Override
    protected void readPlayerMultiWorld(ResultSet rs, MyPetPlayerImpl player) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int column = rs.findColumn("multi_world");
        switch (metaData.getColumnTypeName(column)) {
            case "BLOB":
                try {
                    CompoundBinaryTag worldGroups = NbtUtil.readCompressed(
                            rs.getBlob(column).getBinaryStream());
                    for (String worldGroupName : worldGroups.keySet()) {
                        String petUUID = worldGroups.getString(worldGroupName);
                        player.setPetForWorldGroup(worldGroupName, UUID.fromString(petUUID));
                    }
                } catch (IOException e) {
                    MyPetApi.getLogger().warning("Multiworld info of player (" + player.getUniqueId()
                            + ") could not be loaded!");
                }
                break;
            case "VARCHAR":
            default:
                super.readPlayerMultiWorld(rs, player);
        }
    }

}
