/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zaxxer.hikari.HikariDataSource;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.repository.RepositoryInitException;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagStream;
import de.keyle.knbt.TagString;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipException;

public class MySqlRepository implements Repository {

    protected Gson gson = new Gson();
    private HikariDataSource dataSource;
    private int version = 10;

    @Override
    public void disable() {
        saveData();
        dataSource.close();
    }

    @Override
    public void save() {
        saveData();
    }

    @Override
    public void init() throws RepositoryInitException {
        this.dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://" +
                Configuration.Repository.MySQL.HOST + ":" + Configuration.Repository.MySQL.PORT + "/" +
                Configuration.Repository.MySQL.DATABASE + (Configuration.Repository.MySQL.DATABASE.contains("?") ? "&" : "?") + "useUnicode=true&characterEncoding=" + Configuration.Repository.MySQL.CHARACTER_ENCODING);
        dataSource.setUsername(Configuration.Repository.MySQL.USER);
        dataSource.setPassword(Configuration.Repository.MySQL.PASSWORD);
        dataSource.setMaximumPoolSize(Configuration.Repository.MySQL.POOL_SIZE);
        dataSource.addDataSourceProperty("cachePrepStmts", true);
        dataSource.setLeakDetectionThreshold(10000);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "info;");
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                updateStructure(resultSet);
                updateInfo();
            } else {
                initStructure();
            }
        } catch (SQLSyntaxErrorException e) {
            initStructure();
        } catch (Exception e) {
            throw new RepositoryInitException(e);
        }
    }

    private void updateStructure(ResultSet resultSet) {
        try {
            int oldVersion = resultSet.getInt("version");

            if (oldVersion < version) {
                MyPetApi.getLogger().info("[MySQL] Updating database from v" + oldVersion + " to v" + version + ".");

                switch (oldVersion) {
                    case 1:
                        updateToV2();
                    case 2:
                        updateToV3();
                    case 3:
                        updateToV4();
                    case 4:
                        updateToV5();
                    case 5:
                        updateToV6();
                    case 6:
                    case 7:
                        updateToV8();
                    case 8:
                    case 9:
                        updateToV10();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initStructure() {
        try (Connection connection = dataSource.getConnection();
             Statement create = connection.createStatement()) {

            create.executeUpdate("CREATE TABLE " + Configuration.Repository.MySQL.PREFIX + "pets (" +
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

            create.executeUpdate("CREATE TABLE " + Configuration.Repository.MySQL.PREFIX + "players (" +
                    "internal_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "mojang_uuid VARCHAR(36) UNIQUE, " +
                    "name VARCHAR(16) UNIQUE, " +
                    "auto_respawn BOOLEAN, " +
                    "auto_respawn_min INTEGER , " +
                    "capture_mode BOOLEAN, " +
                    "health_bar INTEGER, " +
                    "pet_idle_volume FLOAT, " +
                    "extended_info BLOB, " +
                    "multi_world VARCHAR(2000), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY ( internal_uuid )" +
                    ")");

            create.executeUpdate("CREATE TABLE " + Configuration.Repository.MySQL.PREFIX + "info (" +
                    "version INTEGER UNIQUE, " +
                    "mypet_version VARCHAR(20), " +
                    "mypet_build VARCHAR(20), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            PreparedStatement insert = connection.prepareStatement("INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "info (version, mypet_version, mypet_build) VALUES (?,?,?);");
            insert.setInt(1, version);
            insert.setString(2, MyPetVersion.getVersion());
            insert.setString(3, MyPetVersion.getBuild());
            insert.executeUpdate();
            insert.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV2() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "info ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV3() {
        List<MyPetPlayer> players = getAllMyPetPlayers();

        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("UPDATE " + Configuration.Repository.MySQL.PREFIX + "players SET multi_world=NULL;");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players MODIFY multi_world VARCHAR(2000) DEFAULT \"\";");

            for (MyPetPlayer player : players) {
                updatePlayer(player);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV4() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD COLUMN pet_idle_volume FLOAT DEFAULT 1 AFTER health_bar;");
            update.executeUpdate("ALTER IGNORE TABLE " + Configuration.Repository.MySQL.PREFIX + "info ADD UNIQUE (version);");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV5() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets MODIFY COLUMN hunger DOUBLE NOT NULL;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV6() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players DROP COLUMN offline_uuid;");
            update.executeUpdate("ALTER IGNORE TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD UNIQUE (name);");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets ADD INDEX `owner_uuid` (`owner_uuid`);");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV8() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD COLUMN resource_pack BOOLEAN NULL DEFAULT NULL AFTER `pet_idle_volume`;");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets MODIFY name VARBINARY (1024)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV10() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players DROP COLUMN resource_pack;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup(final long timestamp, final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("DELETE FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE last_used<?;")) {
                    statement.setLong(1, timestamp);

                    int result = statement.executeUpdate();

                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), result);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), 0);
                    }
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @Override
    public void countMyPets(final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "pets;");
                     ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    callback.setValue(resultSet.getInt(1));
                    callback.runTask(MyPetApi.getPlugin());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @Override
    public void countMyPets(final MyPetType type, final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE type=?;")) {
                    statement.setString(1, type.name());
                    ResultSet resultSet = statement.executeQuery();
                    resultSet.next();
                    callback.setValue(resultSet.getInt(1));
                    callback.runTask(MyPetApi.getPlugin());
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    public void saveData() {
        updateInfo();
        savePets();
        savePlayers();
    }

    private void updateInfo() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement("UPDATE " + Configuration.Repository.MySQL.PREFIX + "info SET version=?, mypet_version=?, mypet_build=?;")) {
            update.setInt(1, version);
            update.setString(2, MyPetVersion.getVersion());
            update.setString(3, MyPetVersion.getBuild());
            update.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void savePets() {
        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE " + Configuration.Repository.MySQL.PREFIX + "pets SET " +
                         "owner_uuid=?, " +
                         "exp=?, " +
                         "health=?, " +
                         "respawn_time=?, " +
                         "name=?, " +
                         "type=?, " +
                         "last_used=?, " +
                         "hunger=?, " +
                         "world_group=?, " +
                         "wants_to_spawn=?, " +
                         "skilltree=?, " +
                         "skills=?, " +
                         "info=? " +
                         "WHERE uuid=?;")) {
                statement.setString(1, myPet.getOwner().getInternalUUID().toString());
                statement.setDouble(2, myPet.getExp());
                statement.setDouble(3, myPet.getHealth());
                statement.setInt(4, myPet.getRespawnTime());
                statement.setBinaryStream(5, new ByteArrayInputStream(myPet.getPetName().getBytes(StandardCharsets.UTF_8)));
                statement.setString(6, myPet.getPetType().name());
                statement.setLong(7, myPet.getLastUsed());
                statement.setDouble(8, myPet.getSaturation());
                statement.setString(9, myPet.getWorldGroup());
                statement.setBoolean(10, myPet.wantsToRespawn());
                statement.setString(11, myPet.getSkilltree() != null ? myPet.getSkilltree().getName() : null);
                statement.setBlob(12, new ByteArrayInputStream(TagStream.writeTag(myPet.getSkillInfo(), true)));
                statement.setBlob(13, new ByteArrayInputStream(TagStream.writeTag(myPet.getInfo(), true)));

                statement.setString(14, myPet.getUUID().toString());

                statement.executeUpdate();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE " + Configuration.Repository.MySQL.PREFIX + "players SET " +
                                 "mojang_uuid=?, " +
                                 "name=?, " +
                                 "auto_respawn=?, " +
                                 "auto_respawn_min=?, " +
                                 "capture_mode=?, " +
                                 "health_bar=?, " +
                                 "pet_idle_volume=?, " +
                                 "extended_info=?, " +
                                 "multi_world=? " +
                                 "WHERE internal_uuid=?;")) {
                statement.setString(1, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                statement.setString(2, player.getName());
                statement.setBoolean(3, player.hasAutoRespawnEnabled());
                statement.setInt(4, player.getAutoRespawnMin());
                statement.setBoolean(5, player.isCaptureHelperActive());
                statement.setBoolean(6, player.isHealthBarActive());
                statement.setFloat(7, player.getPetLivingSoundVolume());
                statement.setBlob(8, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));

                JsonObject multiWorldObject = new JsonObject();
                for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                }
                statement.setString(9, gson.toJson(multiWorldObject));
                statement.setString(10, player.getInternalUUID().toString());

                statement.executeUpdate();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    private List<StoredMyPet> resultSetToMyPet(MyPetPlayer owner, ResultSet resultSet) {
        List<StoredMyPet> pets = new ArrayList<>();
        try {
            while (resultSet.next()) {
                InactiveMyPet pet = new InactiveMyPet(owner);
                pet.setUUID(UUID.fromString(resultSet.getString("uuid")));
                pet.setWorldGroup(resultSet.getString("world_group"));
                pet.setExp(resultSet.getDouble("exp"));
                pet.setHealth(resultSet.getDouble("health"));
                pet.setRespawnTime(resultSet.getInt("respawn_time"));
                pet.setPetName(Util.toString(resultSet.getBinaryStream("name"), StandardCharsets.UTF_8));
                pet.setPetType(MyPetType.valueOf(resultSet.getString("type")));
                pet.setLastUsed(resultSet.getLong("last_used"));
                pet.setSaturation(resultSet.getDouble("hunger"));
                pet.wantsToRespawn = resultSet.getBoolean("wants_to_spawn");

                String skillTreeName = resultSet.getString("skilltree");
                if (skillTreeName != null) {
                    Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skillTreeName);
                    if (skilltree != null) {
                        pet.setSkilltree(skilltree);
                    }
                }

                try {
                    pet.setSkills(TagStream.readTag(resultSet.getBlob("skills").getBinaryStream(), true));
                } catch (ZipException exception) {
                    MyPetApi.getMyPetLogger().warning("Pet skills of player \"" + pet.getOwner().getName() + "\" (" + pet.getPetName() + ") could not be loaded!");
                }
                try {
                    pet.setInfo(TagStream.readTag(resultSet.getBlob("info").getBinaryStream(), true));
                } catch (ZipException exception) {
                    MyPetApi.getMyPetLogger().warning("Pet info of player \"" + pet.getOwner().getName() + "\" (" + pet.getPetName() + ") could not be loaded!");
                }

                List<RepositoryMyPetConverterService> converters = MyPetApi.getServiceManager().getServices(RepositoryMyPetConverterService.class);
                for (RepositoryMyPetConverterService converter : converters) {
                    converter.convert(pet);
                }

                pets.add(pet);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return pets;
    }

    @Override
    public List<StoredMyPet> getAllMyPets() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM pets;")) {
            List<MyPetPlayer> playerList = getAllMyPetPlayers();
            Map<UUID, MyPetPlayer> owners = new HashMap<>();

            for (MyPetPlayer player : playerList) {
                owners.put(player.getInternalUUID(), player);
            }

            List<StoredMyPet> pets = new ArrayList<>();
            while (resultSet.next()) {
                if (!owners.containsKey(UUID.fromString(resultSet.getString("owner_uuid")))) {
                    continue;
                }
                MyPetPlayer owner = owners.get(UUID.fromString(resultSet.getString("owner_uuid")));

                InactiveMyPet pet = new InactiveMyPet(owner);
                pet.setUUID(UUID.fromString(resultSet.getString("uuid")));
                pet.setWorldGroup(resultSet.getString("world_group"));
                pet.setExp(resultSet.getDouble("exp"));
                pet.setHealth(resultSet.getDouble("health"));
                pet.setRespawnTime(resultSet.getInt("respawn_time"));
                pet.setPetName(resultSet.getString("name"));
                pet.setPetType(MyPetType.valueOf(resultSet.getString("type")));
                pet.setLastUsed(resultSet.getLong("last_used"));
                pet.setSaturation(resultSet.getInt("hunger"));
                pet.wantsToRespawn = resultSet.getBoolean("wants_to_spawn");

                String skillTreeName = resultSet.getString("skilltree");
                if (skillTreeName != null) {
                    Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skillTreeName);
                    if (skilltree != null) {
                        pet.setSkilltree(skilltree);
                    }
                }

                pet.setSkills(TagStream.readTag(resultSet.getBlob("skills").getBinaryStream(), true));
                pet.setInfo(TagStream.readTag(resultSet.getBlob("info").getBinaryStream(), true));

                pets.add(pet);
            }

            return pets;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public void hasMyPets(final MyPetPlayer myPetPlayer, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE owner_uuid=?;")) {
                        statement.setString(1, myPetPlayer.getInternalUUID().toString());
                        ResultSet resultSet = statement.executeQuery();
                        resultSet.next();

                        callback.runTask(MyPetApi.getPlugin(), resultSet.getInt(1) > 0);
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    public void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<StoredMyPet>> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE owner_uuid=?;")) {
                        statement.setString(1, owner.getInternalUUID().toString());
                        ResultSet resultSet = statement.executeQuery();
                        List<StoredMyPet> pets = resultSetToMyPet(owner, resultSet);
                        callback.runTask(MyPetApi.getPlugin(), pets);
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    public void getMyPet(final UUID uuid, final RepositoryCallback<StoredMyPet> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE uuid=?;")) {
                        statement.setString(1, uuid.toString());

                        ResultSet resultSet = statement.executeQuery();

                        if (resultSet.first()) {
                            MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(UUID.fromString(resultSet.getString("owner_uuid")));
                            if (owner != null) {
                                resultSet.beforeFirst();
                                List<StoredMyPet> pets = resultSetToMyPet(owner, resultSet);
                                if (pets.size() > 0) {
                                    callback.runTask(MyPetApi.getPlugin(), pets.get(0));
                                }
                            }
                        }
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    public void removeMyPet(final UUID uuid, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("DELETE FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE uuid=?;")) {
                    statement.setString(1, uuid.toString());

                    int result = statement.executeUpdate();

                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), result > 0);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), false);
                    }
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @Override
    public void removeMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
        removeMyPet(storedMyPet.getUUID(), callback);
    }

    @Override
    public void addMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "pets (uuid, " +
                                     "owner_uuid, " +
                                     "exp, " +
                                     "health, " +
                                     "respawn_time, " +
                                     "name, " +
                                     "type, " +
                                     "last_used, " +
                                     "hunger, " +
                                     "world_group, " +
                                     "wants_to_spawn, " +
                                     "skilltree, " +
                                     "skills, " +
                                     "info) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                    statement.setString(1, storedMyPet.getUUID().toString());
                    statement.setString(2, storedMyPet.getOwner().getInternalUUID().toString());
                    statement.setDouble(3, storedMyPet.getExp());
                    statement.setDouble(4, storedMyPet.getHealth());
                    statement.setInt(5, storedMyPet.getRespawnTime());
                    statement.setString(6, storedMyPet.getPetName());
                    statement.setString(7, storedMyPet.getPetType().name());
                    statement.setLong(8, storedMyPet.getLastUsed());
                    statement.setDouble(9, storedMyPet.getSaturation());
                    statement.setString(10, storedMyPet.getWorldGroup());
                    statement.setBoolean(11, storedMyPet.wantsToRespawn());
                    statement.setString(12, storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);

                    try {
                        statement.setBlob(13, new ByteArrayInputStream(TagStream.writeTag(storedMyPet.getSkillInfo(), true)));
                        statement.setBlob(14, new ByteArrayInputStream(TagStream.writeTag(storedMyPet.getInfo(), true)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    boolean result = statement.executeUpdate() > 0;

                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), result);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    public boolean addMyPets(List<StoredMyPet> pets) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "pets (uuid, " +
                             "owner_uuid, " +
                             "exp, " +
                             "health, " +
                             "respawn_time, " +
                             "name, " +
                             "type, " +
                             "last_used, " +
                             "hunger, " +
                             "world_group, " +
                             "wants_to_spawn, " +
                             "skilltree, " +
                             "skills, " +
                             "info) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {


            int i = 0;
            for (StoredMyPet storedMyPet : pets) {
                statement.setString(1, storedMyPet.getUUID().toString());
                statement.setString(2, storedMyPet.getOwner().getInternalUUID().toString());
                statement.setDouble(3, storedMyPet.getExp());
                statement.setDouble(4, storedMyPet.getHealth());
                statement.setInt(5, storedMyPet.getRespawnTime());
                statement.setString(6, storedMyPet.getPetName());
                statement.setString(7, storedMyPet.getPetType().name());
                statement.setLong(8, storedMyPet.getLastUsed());
                statement.setDouble(9, storedMyPet.getSaturation());
                statement.setString(10, storedMyPet.getWorldGroup());
                statement.setBoolean(11, storedMyPet.wantsToRespawn());
                statement.setString(12, storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);

                try {
                    statement.setBlob(13, new ByteArrayInputStream(TagStream.writeTag(storedMyPet.getSkillInfo(), true)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    statement.setBlob(14, new ByteArrayInputStream(TagStream.writeTag(storedMyPet.getInfo(), true)));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                statement.addBatch();

                if (++i % 500 == 0 && i != pets.size()) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void updateMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("UPDATE " + Configuration.Repository.MySQL.PREFIX + "pets SET " +
                             "owner_uuid=?, " +
                             "exp=?, " +
                             "health=?, " +
                             "respawn_time=?, " +
                             "name=?, " +
                             "type=?, " +
                             "last_used=?, " +
                             "hunger=?, " +
                             "world_group=?, " +
                             "wants_to_spawn=?, " +
                             "skilltree=?, " +
                             "skills=?, " +
                             "info=? " +
                             "WHERE uuid=?;")) {
                    statement.setString(1, storedMyPet.getOwner().getInternalUUID().toString());
                    statement.setDouble(2, storedMyPet.getExp());
                    statement.setDouble(3, storedMyPet.getHealth());
                    statement.setInt(4, storedMyPet.getRespawnTime());
                    statement.setBinaryStream(5, new ByteArrayInputStream(storedMyPet.getPetName().getBytes(StandardCharsets.UTF_8)));
                    statement.setString(6, storedMyPet.getPetType().name());
                    statement.setLong(7, storedMyPet.getLastUsed());
                    statement.setDouble(8, storedMyPet.getSaturation());
                    statement.setString(9, storedMyPet.getWorldGroup());
                    statement.setBoolean(10, storedMyPet.wantsToRespawn());
                    statement.setString(11, storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);
                    statement.setBlob(12, new ByteArrayInputStream(TagStream.writeTag(storedMyPet.getSkillInfo(), true)));
                    statement.setBlob(13, new ByteArrayInputStream(TagStream.writeTag(storedMyPet.getInfo(), true)));

                    statement.setString(14, storedMyPet.getUUID().toString());

                    int result = statement.executeUpdate();

                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), result > 0);
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    // Players ---------------------------------------------------------------------------------------------------------

    private MyPetPlayer resultSetToMyPetPlayer(ResultSet resultSet) {
        try {
            if (resultSet.next()) {
                MyPetPlayerImpl petPlayer;

                UUID internalUUID = UUID.fromString(resultSet.getString("internal_uuid"));
                String playerName = resultSet.getString("name");

                UUID mojangUUID = resultSet.getString("mojang_uuid") != null ? UUID.fromString(resultSet.getString("mojang_uuid")) : null;
                if (mojangUUID != null) {
                    petPlayer = new MyPetPlayerImpl(internalUUID, mojangUUID);
                    petPlayer.setLastKnownName(playerName);
                } else if (playerName != null) {
                    petPlayer = new MyPetPlayerImpl(internalUUID, playerName);
                } else {
                    MyPetApi.getLogger().warning("Player with no UUID or name found!");
                    return null;
                }

                petPlayer.setAutoRespawnEnabled(resultSet.getBoolean("auto_respawn"));
                petPlayer.setAutoRespawnMin(resultSet.getInt("auto_respawn_min"));
                petPlayer.setCaptureHelperActive(resultSet.getBoolean("capture_mode"));
                petPlayer.setHealthBarActive(resultSet.getBoolean("health_bar"));
                petPlayer.setPetLivingSoundVolume(resultSet.getFloat("pet_idle_volume"));
                try {
                    petPlayer.setExtendedInfo(TagStream.readTag(resultSet.getBlob("extended_info").getBinaryStream(), true));
                } catch (ZipException exception) {
                    MyPetApi.getMyPetLogger().warning("Extended info of player \"" + playerName + "\" (" + mojangUUID + ") could not be loaded!");
                }

                ResultSetMetaData metaData = resultSet.getMetaData();
                int column = resultSet.findColumn("multi_world");

                switch (metaData.getColumnTypeName(column)) {
                    case "BLOB":
                        try {
                            TagCompound worldGroups = TagStream.readTag(resultSet.getBlob(column).getBinaryStream(), true);
                            for (String worldGroupName : worldGroups.getCompoundData().keySet()) {
                                String petUUID = worldGroups.getAs(worldGroupName, TagString.class).getStringData();
                                petPlayer.setMyPetForWorldGroup(worldGroupName, UUID.fromString(petUUID));
                            }
                        } catch (ZipException exception) {
                            MyPetApi.getMyPetLogger().warning("Multiworld info of player \"" + playerName + "\" (" + mojangUUID + ") could not be loaded!");
                        }
                        break;
                    case "VARCHAR":
                        try {
                            JsonObject jsonObject = gson.fromJson(resultSet.getString(column), JsonObject.class);
                            for (String uuid : jsonObject.keySet()) {
                                String petUUID = jsonObject.get(uuid).getAsString();
                                petPlayer.setMyPetForWorldGroup(uuid, UUID.fromString(petUUID));
                            }
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        }
                }
                return petPlayer;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<MyPetPlayer> getAllMyPetPlayers() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "players;")) {

            List<MyPetPlayer> players = new ArrayList<>();
            MyPetPlayer player;
            while (true) {
                player = resultSetToMyPetPlayer(resultSet);
                if (player == null) {
                    break;
                }
                players.add(player);
            }
            return players;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public void isMyPetPlayer(final Player player, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT COUNT(internal_uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE mojang_uuid=? OR name=?;")) {
                        statement.setString(1, player.getUniqueId().toString());
                        statement.setString(2, player.getName());
                        ResultSet resultSet = statement.executeQuery();
                        resultSet.next();

                        callback.runTask(MyPetApi.getPlugin(), resultSet.getInt(1) > 0);
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    public void getMyPetPlayer(final UUID uuid, final RepositoryCallback<MyPetPlayer> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE internal_uuid=?;")) {
                        statement.setString(1, uuid.toString());
                        ResultSet resultSet = statement.executeQuery();
                        MyPetPlayer player = resultSetToMyPetPlayer(resultSet);
                        if (player != null) {
                            callback.runTask(MyPetApi.getPlugin(), player);
                        }
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    public void getMyPetPlayer(final Player player, final RepositoryCallback<MyPetPlayer> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE mojang_uuid=? OR name=?;")) {
                        statement.setString(1, player.getUniqueId().toString());
                        statement.setString(2, player.getName());
                        ResultSet resultSet = statement.executeQuery();

                        MyPetPlayer player = resultSetToMyPetPlayer(resultSet);
                        if (player != null) {
                            callback.runTask(MyPetApi.getPlugin(), player);
                        }
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    public void updateMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean result = updatePlayer(player);

                if (callback != null) {
                    callback.runTask(MyPetApi.getPlugin(), result);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @SuppressWarnings("unchecked")
    public boolean updatePlayer(final MyPetPlayer player) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE " + Configuration.Repository.MySQL.PREFIX + "players SET " +
                             "mojang_uuid=?, " +
                             "name=?, " +
                             "auto_respawn=?, " +
                             "auto_respawn_min=?, " +
                             "capture_mode=?, " +
                             "health_bar=?, " +
                             "pet_idle_volume=?, " +
                             "extended_info=?, " +
                             "multi_world=? " +
                             "WHERE internal_uuid=?;")) {
            statement.setString(1, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
            statement.setString(2, player.getName());
            statement.setBoolean(3, player.hasAutoRespawnEnabled());
            statement.setInt(4, player.getAutoRespawnMin());
            statement.setBoolean(5, player.isCaptureHelperActive());
            statement.setBoolean(6, player.isHealthBarActive());
            statement.setFloat(7, player.getPetLivingSoundVolume());
            statement.setBlob(8, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));

            JsonObject multiWorldObject = new JsonObject();
            for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
            }
            statement.setString(9, gson.toJson(multiWorldObject));
            statement.setString(10, player.getInternalUUID().toString());

            int result = statement.executeUpdate();

            return result > 0;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void addMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "players (" +
                                     "internal_uuid, " +
                                     "mojang_uuid, " +
                                     "name, " +
                                     "auto_respawn, " +
                                     "auto_respawn_min, " +
                                     "capture_mode, " +
                                     "health_bar, " +
                                     "pet_idle_volume, " +
                                     "extended_info, " +
                                     "multi_world) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                    statement.setString(1, player.getInternalUUID().toString());
                    statement.setString(2, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                    statement.setString(3, player.getName());
                    statement.setBoolean(4, player.hasAutoRespawnEnabled());
                    statement.setInt(5, player.getAutoRespawnMin());
                    statement.setBoolean(6, player.isCaptureHelperActive());
                    statement.setBoolean(7, player.isHealthBarActive());
                    statement.setFloat(8, player.getPetLivingSoundVolume());
                    try {
                        statement.setBlob(9, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    JsonObject multiWorldObject = new JsonObject();
                    for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                        //noinspection unchecked
                        multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                    }
                    statement.setString(10, gson.toJson(multiWorldObject));


                    boolean result = statement.executeUpdate() > 0;

                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), result);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @SuppressWarnings("unchecked")
    public boolean addMyPetPlayers(List<MyPetPlayer> players) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "players (" +
                             "internal_uuid, " +
                             "mojang_uuid, " +
                             "name, " +
                             "auto_respawn, " +
                             "auto_respawn_min, " +
                             "capture_mode, " +
                             "health_bar, " +
                             "pet_idle_volume, " +
                             "extended_info, " +
                             "multi_world) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            int i = 0;
            HashSet<String> playerNames = new HashSet<>();

            for (MyPetPlayer player : players) {
                String playerName = player.getName();
                if (playerNames.contains(playerName)) {
                    MyPetApi.getLogger().info("Found duplicate Player: " + player.toString());
                    continue;
                }
                playerNames.add(playerName);

                statement.setString(1, player.getInternalUUID().toString());
                statement.setString(2, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                statement.setString(3, playerName);
                statement.setBoolean(4, player.hasAutoRespawnEnabled());
                statement.setInt(5, player.getAutoRespawnMin());
                statement.setBoolean(6, player.isCaptureHelperActive());
                statement.setBoolean(7, player.isHealthBarActive());
                statement.setFloat(8, player.getPetLivingSoundVolume());
                statement.setBlob(9, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));

                JsonObject multiWorldObject = new JsonObject();
                for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                }
                statement.setString(10, gson.toJson(multiWorldObject));

                statement.addBatch();
                if (++i % 500 == 0 && i != players.size()) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("DELETE FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE internal_uuid=?;")) {
                    statement.setString(1, player.getInternalUUID().toString());

                    int result = statement.executeUpdate();

                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), result > 0);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.runTask(MyPetApi.getPlugin(), false);
                    }
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }
}