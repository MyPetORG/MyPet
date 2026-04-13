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
import de.Keyle.MyPet.api.repository.RepositoryInitException;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.NbtUtil;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MySqlRepository implements Repository {

    protected Gson gson = new Gson();
    private final Map<UUID, StoredMyPet> petsToBeSaved = new ConcurrentHashMap<>();
    private final Map<UUID, MyPetPlayer> playersToBeSaved = new ConcurrentHashMap<>();
    private HikariDataSource dataSource;
    private int version = 10;

    private ExecutorService executor;

    private void backupCorruptedData(StoredMyPet pet, String fieldName, byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            Path corruptedDir = MyPetApi.getPlugin().getDataFolder().toPath().resolve("corrupted");
            Files.createDirectories(corruptedDir);
            String safePetName = pet.getPetName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = pet.getOwner().getUniqueId() + "_" + safePetName + "_" + fieldName + ".dat";
            Path backupFile = corruptedDir.resolve(filename);
            Files.write(backupFile, data);
            MyPetApi.getLogger().info("Corrupted data backed up to: " + backupFile);
        } catch (IOException e) {
            MyPetApi.getLogger().warning("Failed to backup corrupted data for pet " + pet.getUUID() + ": " + e.getMessage());
        }
    }

    @Override
    public void disable() {
        saveData();
        if (executor != null) {
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

        this.executor = Executors.newFixedThreadPool(
                Math.max(1, Configuration.Repository.MySQL.POOL_SIZE),
                r -> {
                    Thread t = new Thread(r, "MyPet-MySQL");
                    t.setDaemon(true);
                    return t;
                });

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
            ErrorUtil.reportError("MySQL database operation failed", e);
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

            create.executeUpdate("CREATE TABLE " + Configuration.Repository.MySQL.PREFIX + "info (" +
                    "version INTEGER UNIQUE, " +
                    "mypet_version VARCHAR(20), " +
                    "mypet_build VARCHAR(20), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "info (version, mypet_version, mypet_build) VALUES (?,?,?);")) {
                insert.setInt(1, version);
                insert.setString(2, MyPetVersion.getVersion());
                insert.setString(3, MyPetVersion.getBuild());
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void updateToV2() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "info ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
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
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void updateToV4() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD COLUMN pet_idle_volume FLOAT DEFAULT 1 AFTER health_bar;");
            update.executeUpdate("ALTER IGNORE TABLE " + Configuration.Repository.MySQL.PREFIX + "info ADD UNIQUE (version);");
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void updateToV5() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets MODIFY COLUMN hunger DOUBLE NOT NULL;");
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void updateToV6() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players DROP COLUMN offline_uuid;");
            update.executeUpdate("ALTER IGNORE TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD UNIQUE (name);");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets ADD INDEX `owner_uuid` (`owner_uuid`);");
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void updateToV8() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players ADD COLUMN resource_pack BOOLEAN NULL DEFAULT NULL AFTER `pet_idle_volume`;");
            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "pets MODIFY name VARBINARY (1024)");
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void updateToV10() {
        try (Connection connection = dataSource.getConnection();
             Statement update = connection.createStatement()) {

            update.executeUpdate("ALTER TABLE " + Configuration.Repository.MySQL.PREFIX + "players DROP COLUMN resource_pack;");
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    @Override
    public CompletableFuture<Integer> cleanup(final long timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE last_used<?;")) {
                statement.setLong(1, timestamp);
                return statement.executeUpdate();
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                return 0;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countMyPets() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "pets;");
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countMyPets(final MyPetType type) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE type=?;")) {
                statement.setString(1, type.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
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
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
    }

    private void savePets() {
        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            savePet(myPet);
        }
        for (StoredMyPet myPet : petsToBeSaved.values()) {
            savePet(myPet);
        }
    }

    public boolean savePet(StoredMyPet myPet) {
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
            statement.setString(1, myPet.getOwner().getUniqueId().toString());
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
            statement.setBlob(12, new ByteArrayInputStream(NbtUtil.writeCompressed(myPet.getSkillInfo())));
            statement.setBlob(13, new ByteArrayInputStream(NbtUtil.writeCompressed(myPet.getInfo())));

            statement.setString(14, myPet.getUUID().toString());

            statement.executeUpdate();
        } catch (SQLException | IOException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
            savePlayer(player);
        }
        for (MyPetPlayer player : playersToBeSaved.values()) {
            savePlayer(player);
        }
    }

    private void savePlayer(MyPetPlayer player) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE " + Configuration.Repository.MySQL.PREFIX + "players SET " +
                             "auto_respawn=?, " +
                             "auto_respawn_min=?, " +
                             "capture_mode=?, " +
                             "health_bar=?, " +
                             "pet_idle_volume=?, " +
                             "extended_info=?, " +
                             "multi_world=? " +
                             "WHERE uuid=?;")) {
            statement.setBoolean(1, player.hasAutoRespawnEnabled());
            statement.setInt(2, player.getAutoRespawnMin());
            statement.setBoolean(3, player.isCaptureHelperActive());
            statement.setBoolean(4, player.isHealthBarActive());
            statement.setFloat(5, player.getPetLivingSoundVolume());
            statement.setBlob(6, new ByteArrayInputStream(NbtUtil.writeCompressed(player.getExtendedInfo())));

            JsonObject multiWorldObject = new JsonObject();
            for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
            }
            statement.setString(7, gson.toJson(multiWorldObject));
            statement.setString(8, player.getUniqueId().toString());

            statement.executeUpdate();
        } catch (SQLException | IOException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
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
                MyPetType type = MyPetType.byNameOrNull(resultSet.getString("type"));
                if (type == null) continue;
                pet.setPetType(type);
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

                byte[] skillsData = resultSet.getBlob("skills").getBytes(1, (int) resultSet.getBlob("skills").length());
                try {
                    pet.setSkills(NbtUtil.readCompressed(skillsData));
                } catch (IOException e) {
                    MyPetApi.getLogger().warning("Failed to load skills for " + pet.getOwner().getName() + "'s Pet " + pet.getPetName() + " - the data was likely corrupted.");
                    backupCorruptedData(pet, "skills", skillsData);
                    pet.setSkills(CompoundBinaryTag.empty());
                }

                byte[] infoData = resultSet.getBlob("info").getBytes(1, (int) resultSet.getBlob("info").length());
                try {
                    pet.setInfo(NbtUtil.readCompressed(infoData));
                } catch (IOException e) {
                    MyPetApi.getLogger().warning("Failed to load info for " + pet.getOwner().getName() + "'s Pet " + pet.getPetName() + " - the data was likely corrupted.");
                    backupCorruptedData(pet, "info", infoData);
                    pet.setInfo(CompoundBinaryTag.empty());
                }

                pets.add(pet);
            }
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
        return pets;
    }

    @Override
    public List<StoredMyPet> getAllMyPets() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "pets;")) {
            List<MyPetPlayer> playerList = getAllMyPetPlayers();
            Map<UUID, MyPetPlayer> owners = new HashMap<>();

            for (MyPetPlayer player : playerList) {
                owners.put(player.getUniqueId(), player);
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
                MyPetType type = MyPetType.byNameOrNull(resultSet.getString("type"));
                if (type == null) continue;
                pet.setPetType(type);
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

                byte[] skillsData = resultSet.getBlob("skills").getBytes(1, (int) resultSet.getBlob("skills").length());
                try {
                    pet.setSkills(NbtUtil.readCompressed(skillsData));
                } catch (IOException e) {
                    MyPetApi.getLogger().warning("Failed to load skills for " + pet.getOwner().getName() + "'s Pet " + pet.getPetName() + " - the data was likely corrupted.");
                    backupCorruptedData(pet, "skills", skillsData);
                    pet.setSkills(CompoundBinaryTag.empty());
                }

                byte[] infoData = resultSet.getBlob("info").getBytes(1, (int) resultSet.getBlob("info").length());
                try {
                    pet.setInfo(NbtUtil.readCompressed(infoData));
                } catch (IOException e) {
                    MyPetApi.getLogger().warning("Failed to load info for " + pet.getOwner().getName() + "'s Pet " + pet.getPetName() + " - the data was likely corrupted.");
                    backupCorruptedData(pet, "info", infoData);
                    pet.setInfo(CompoundBinaryTag.empty());
                }

                pets.add(pet);
            }

            return pets;
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
        return new ArrayList<>();
    }

    @Override
    public CompletableFuture<Boolean> hasMyPets(final MyPetPlayer myPetPlayer) {
        if (myPetPlayer == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE owner_uuid=?;")) {
                statement.setString(1, myPetPlayer.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1) > 0;
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<StoredMyPet>> getMyPets(final MyPetPlayer owner) {
        if (owner == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE owner_uuid=?;")) {
                statement.setString(1, owner.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSetToMyPet(owner, resultSet);
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<StoredMyPet> getMyPet(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!MyPetApi.getPlugin().isEnabled()) {
                return null;
            }
            StoredMyPet pending = petsToBeSaved.get(uuid);
            if (pending != null) {
                return pending;
            }
            StoredMyPet result = null;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE uuid=?;",
                         ResultSet.TYPE_SCROLL_SENSITIVE,
                         ResultSet.CONCUR_UPDATABLE)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.first()) {
                        MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(UUID.fromString(resultSet.getString("owner_uuid")));
                        if (owner != null) {
                            resultSet.beforeFirst();
                            List<StoredMyPet> pets = resultSetToMyPet(owner, resultSet);
                            if (!pets.isEmpty()) {
                                result = pets.get(0);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> removeMyPet(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM " + Configuration.Repository.MySQL.PREFIX + "pets WHERE uuid=?;")) {
                statement.setString(1, uuid.toString());
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                return false;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> removeMyPet(final StoredMyPet storedMyPet) {
        return removeMyPet(storedMyPet.getUUID());
    }

    @Override
    public CompletableFuture<Boolean> addMyPet(final StoredMyPet storedMyPet) {
        return CompletableFuture.supplyAsync(() -> {
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
                statement.setString(2, storedMyPet.getOwner().getUniqueId().toString());
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
                    statement.setBlob(13, new ByteArrayInputStream(NbtUtil.writeCompressed(storedMyPet.getSkillInfo())));
                    statement.setBlob(14, new ByteArrayInputStream(NbtUtil.writeCompressed(storedMyPet.getInfo())));
                } catch (IOException e) {
                    ErrorUtil.reportError("MySQL database operation failed", e);
                }

                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                return false;
            }
        }, executor);
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
                statement.setString(2, storedMyPet.getOwner().getUniqueId().toString());
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
                    statement.setBlob(13, new ByteArrayInputStream(NbtUtil.writeCompressed(storedMyPet.getSkillInfo())));
                } catch (IOException e) {
                    ErrorUtil.reportError("MySQL database operation failed", e);
                }
                try {
                    statement.setBlob(14, new ByteArrayInputStream(NbtUtil.writeCompressed(storedMyPet.getInfo())));
                } catch (IOException e) {
                    ErrorUtil.reportError("MySQL database operation failed", e);
                }

                statement.addBatch();

                if (++i % 500 == 0 && i != pets.size()) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            return true;
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> updateMyPet(final StoredMyPet storedMyPet) {
        petsToBeSaved.put(storedMyPet.getUUID(), storedMyPet);
        return CompletableFuture.supplyAsync(() -> {
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
                statement.setString(1, storedMyPet.getOwner().getUniqueId().toString());
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
                statement.setBlob(12, new ByteArrayInputStream(NbtUtil.writeCompressed(storedMyPet.getSkillInfo())));
                statement.setBlob(13, new ByteArrayInputStream(NbtUtil.writeCompressed(storedMyPet.getInfo())));

                statement.setString(14, storedMyPet.getUUID().toString());

                int result = statement.executeUpdate();

                if (result > 0) {
                    petsToBeSaved.remove(storedMyPet.getUUID());
                }
                return result > 0;
            } catch (SQLException | IOException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    // Players ---------------------------------------------------------------------------------------------------------

    private MyPetPlayer resultSetToMyPetPlayer(ResultSet resultSet) {
        try {
            if (resultSet.next()) {
                UUID mojangUUID = UUID.fromString(resultSet.getString("uuid"));
                MyPetPlayerImpl petPlayer = new MyPetPlayerImpl(mojangUUID);

                petPlayer.setAutoRespawnEnabled(resultSet.getBoolean("auto_respawn"));
                petPlayer.setAutoRespawnMin(resultSet.getInt("auto_respawn_min"));
                petPlayer.setCaptureHelperActive(resultSet.getBoolean("capture_mode"));
                petPlayer.setHealthBarActive(resultSet.getBoolean("health_bar"));
                petPlayer.setPetLivingSoundVolume(resultSet.getFloat("pet_idle_volume"));
                try {
                    petPlayer.setExtendedInfo(NbtUtil.readCompressed(resultSet.getBlob("extended_info").getBinaryStream()));
                } catch (IOException exception) {
                    MyPetApi.getLogger().warning("Extended info of player (" + mojangUUID + ") could not be loaded!");
                }

                ResultSetMetaData metaData = resultSet.getMetaData();
                int column = resultSet.findColumn("multi_world");

                switch (metaData.getColumnTypeName(column)) {
                    case "BLOB":
                        try {
                            CompoundBinaryTag worldGroups = NbtUtil.readCompressed(resultSet.getBlob(column).getBinaryStream());
                            for (String worldGroupName : worldGroups.keySet()) {
                                String petUUID = worldGroups.getString(worldGroupName);
                                petPlayer.setMyPetForWorldGroup(worldGroupName, UUID.fromString(petUUID));
                            }
                        } catch (IOException e) {
                            MyPetApi.getLogger().warning("Multiworld info of player (" + mojangUUID + ") could not be loaded!");
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
                            ErrorUtil.reportError("MySQL database operation failed", e);
                        }
                }
                return petPlayer;
            }
        } catch (SQLException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
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
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
        return new ArrayList<>();
    }

    @Override
    public CompletableFuture<Boolean> isMyPetPlayer(final Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1) > 0;
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<MyPetPlayer> getMyPetPlayer(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE uuid=?;")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSetToMyPetPlayer(resultSet);
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<MyPetPlayer> getMyPetPlayer(final Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSetToMyPetPlayer(resultSet);
                }
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> updateMyPetPlayer(final MyPetPlayer player) {
        playersToBeSaved.put(player.getUniqueId(), player);
        return CompletableFuture.supplyAsync(() -> {
            boolean result = updatePlayer(player);
            if (result) {
                playersToBeSaved.remove(player.getUniqueId());
            }
            return result;
        }, executor);
    }

    @SuppressWarnings("unchecked")
    public boolean updatePlayer(final MyPetPlayer player) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE " + Configuration.Repository.MySQL.PREFIX + "players SET " +
                             "auto_respawn=?, " +
                             "auto_respawn_min=?, " +
                             "capture_mode=?, " +
                             "health_bar=?, " +
                             "pet_idle_volume=?, " +
                             "extended_info=?, " +
                             "multi_world=? " +
                             "WHERE uuid=?;")) {
            statement.setBoolean(1, player.hasAutoRespawnEnabled());
            statement.setInt(2, player.getAutoRespawnMin());
            statement.setBoolean(3, player.isCaptureHelperActive());
            statement.setBoolean(4, player.isHealthBarActive());
            statement.setFloat(5, player.getPetLivingSoundVolume());
            statement.setBlob(6, new ByteArrayInputStream(NbtUtil.writeCompressed(player.getExtendedInfo())));

            JsonObject multiWorldObject = new JsonObject();
            for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
            }
            statement.setString(7, gson.toJson(multiWorldObject));
            statement.setString(8, player.getUniqueId().toString());

            int result = statement.executeUpdate();

            return result > 0;
        } catch (SQLException | IOException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
        return false;
    }


    @Override
    public CompletableFuture<Boolean> addMyPetPlayer(final MyPetPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "players (" +
                                 "uuid, " +
                                 "auto_respawn, " +
                                 "auto_respawn_min, " +
                                 "capture_mode, " +
                                 "health_bar, " +
                                 "pet_idle_volume, " +
                                 "extended_info, " +
                                 "multi_world) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setBoolean(2, player.hasAutoRespawnEnabled());
                statement.setInt(3, player.getAutoRespawnMin());
                statement.setBoolean(4, player.isCaptureHelperActive());
                statement.setBoolean(5, player.isHealthBarActive());
                statement.setFloat(6, player.getPetLivingSoundVolume());
                try {
                    statement.setBlob(7, new ByteArrayInputStream(NbtUtil.writeCompressed(player.getExtendedInfo())));
                } catch (IOException e) {
                    ErrorUtil.reportError("MySQL database operation failed", e);
                }

                JsonObject multiWorldObject = new JsonObject();
                for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                    //noinspection unchecked
                    multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                }
                statement.setString(8, gson.toJson(multiWorldObject));

                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                return false;
            }
        }, executor);
    }

    @SuppressWarnings("unchecked")
    public boolean addMyPetPlayers(List<MyPetPlayer> players) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + Configuration.Repository.MySQL.PREFIX + "players (" +
                             "uuid, " +
                             "auto_respawn, " +
                             "auto_respawn_min, " +
                             "capture_mode, " +
                             "health_bar, " +
                             "pet_idle_volume, " +
                             "extended_info, " +
                             "multi_world) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {

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

                statement.setString(1, mojangUUID.toString());
                statement.setBoolean(2, player.hasAutoRespawnEnabled());
                statement.setInt(3, player.getAutoRespawnMin());
                statement.setBoolean(4, player.isCaptureHelperActive());
                statement.setBoolean(5, player.isHealthBarActive());
                statement.setFloat(6, player.getPetLivingSoundVolume());
                statement.setBlob(7, new ByteArrayInputStream(NbtUtil.writeCompressed(player.getExtendedInfo())));

                JsonObject multiWorldObject = new JsonObject();
                for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                }
                statement.setString(8, gson.toJson(multiWorldObject));

                statement.addBatch();
                if (++i % 500 == 0 && i != players.size()) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            return true;
        } catch (SQLException | IOException e) {
            ErrorUtil.reportError("MySQL database operation failed", e);
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> removeMyPetPlayer(final MyPetPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM " + Configuration.Repository.MySQL.PREFIX + "players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString());
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                ErrorUtil.reportError("MySQL database operation failed", e);
                return false;
            }
        }, executor);
    }
}
