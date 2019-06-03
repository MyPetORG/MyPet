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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetVersion;
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
import de.keyle.knbt.TagStream;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class SqLiteRepository implements Repository {

    protected Gson gson = new Gson();
    private Connection connection;
    private int version = 1;

    @Override
    public void disable() {
        saveData();
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save() {
        saveData();
    }

    @Override
    public void init() throws RepositoryInitException {
        try {
            File dbFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "pets.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());


            PreparedStatement statement = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='info';");
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                resultSet.close();
                statement = connection.prepareStatement("SELECT * FROM info;");
                resultSet = statement.executeQuery();
                updateStructure(resultSet);
                updateInfo();
            } else {
                initStructure();
            }
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RepositoryInitException(e);
        }
    }

    private void updateStructure(ResultSet resultSet) {
        try {
            int oldVersion = resultSet.getInt("version");

            if (oldVersion < version) {
                MyPetApi.getLogger().info("[SQLite] Updating database from v" + oldVersion + " to v" + version + ".");

                switch (oldVersion) {
                    case 1:
                        //updateToV2();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initStructure() {
        try {
            Statement create = connection.createStatement();

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
                    "internal_uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "mojang_uuid VARCHAR(36) UNIQUE, " +
                    "name VARCHAR(16) UNIQUE, " +
                    "auto_respawn BOOLEAN, " +
                    "auto_respawn_min INTEGER , " +
                    "capture_mode BOOLEAN, " +
                    "health_bar INTEGER, " +
                    "pet_idle_volume FLOAT, " +
                    "extended_info BLOB, " +
                    "multi_world VARCHAR(2000), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            createTimestampTrigger("players", "last_update", "internal_uuid");

            create.executeUpdate("CREATE TABLE info (" +
                    "version INTEGER UNIQUE, " +
                    "mypet_version VARCHAR(20), " +
                    "mypet_build VARCHAR(20), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            createTimestampTrigger("info", "last_update", "version");


            PreparedStatement insert = connection.prepareStatement("INSERT INTO info (version, mypet_version, mypet_build) VALUES (?,?,?);");
            insert.setInt(1, version);
            insert.setString(2, MyPetVersion.getVersion());
            insert.setString(3, MyPetVersion.getBuild());
            insert.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTimestampTrigger(String table, String column, String id) {
        try {
            Statement create = connection.createStatement();
            create.execute("CREATE TRIGGER [update_time_trigger_" + table + "] " +
                    "AFTER UPDATE ON " + table + " FOR EACH ROW " +
                    "WHEN NEW." + column + " < OLD." + column + " " +
                    "BEGIN " +
                    "  UPDATE " + table +
                    "    SET " + column + "=CURRENT_TIMESTAMP " +
                    "    WHERE NEW." + id + "=OLD." + id + ";" +
                    "END;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup(final long timestamp, final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM pets WHERE last_used<?;");
                    statement.setLong(1, timestamp);

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("DELETE pet: " + result);

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
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM pets;");
                    ResultSet resultSet = statement.executeQuery();
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
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM pets WHERE type=?;");
                    statement.setString(1, type.name());
                    ResultSet resultSet = statement.executeQuery();
                    resultSet.next();
                    callback.setValue(resultSet.getInt(1));
                    callback.runTask(MyPetApi.getPlugin());
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
        try {
            PreparedStatement update = connection.prepareStatement("UPDATE info SET version=?, mypet_version=?, mypet_build=?;");
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
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE pets SET " +
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
                        "WHERE uuid=?;");
                statement.setString(1, myPet.getOwner().getInternalUUID().toString());
                statement.setDouble(2, myPet.getExp());
                statement.setDouble(3, myPet.getHealth());
                statement.setInt(4, myPet.getRespawnTime());
                statement.setBytes(5, myPet.getPetName().getBytes(StandardCharsets.UTF_8));
                statement.setString(6, myPet.getPetType().name());
                statement.setLong(7, myPet.getLastUsed());
                statement.setDouble(8, myPet.getSaturation());
                statement.setString(9, myPet.getWorldGroup());
                statement.setBoolean(10, myPet.wantsToRespawn());
                statement.setString(11, myPet.getSkilltree() != null ? myPet.getSkilltree().getName() : null);
                statement.setBytes(12, TagStream.writeTag(myPet.getSkillInfo(), true));
                statement.setBytes(13, TagStream.writeTag(myPet.getInfo(), true));

                statement.setString(14, myPet.getUUID().toString());

                int result = statement.executeUpdate();

                //MyPetLogger.write("UPDATE pet: " + result);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE players SET " +
                                "mojang_uuid=?, " +
                                "name=?, " +
                                "auto_respawn=?, " +
                                "auto_respawn_min=?, " +
                                "capture_mode=?, " +
                                "health_bar=?, " +
                                "pet_idle_volume=?, " +
                                "extended_info=?, " +
                                "multi_world=? " +
                                "WHERE internal_uuid=?;");
                statement.setString(1, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                statement.setString(2, player.getName());
                statement.setBoolean(3, player.hasAutoRespawnEnabled());
                statement.setInt(4, player.getAutoRespawnMin());
                statement.setBoolean(5, player.isCaptureHelperActive());
                statement.setBoolean(6, player.isHealthBarActive());
                statement.setFloat(7, player.getPetLivingSoundVolume());
                statement.setBytes(8, TagStream.writeTag(player.getExtendedInfo(), true));

                JsonObject multiWorldObject = new JsonObject();
                for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                }
                statement.setString(9, gson.toJson(multiWorldObject));
                statement.setString(10, player.getInternalUUID().toString());

                int result = statement.executeUpdate();

                //MyPetLogger.write("UPDATE player: " + result);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    private List<StoredMyPet> resultSetToMyPet(MyPetPlayer owner, ResultSet resultSet, boolean next) {
        List<StoredMyPet> pets = new ArrayList<>();
        try {
            while (!next || resultSet.next()) {
                next = true;
                InactiveMyPet pet = new InactiveMyPet(owner);
                pet.setUUID(UUID.fromString(resultSet.getString("uuid")));
                pet.setWorldGroup(resultSet.getString("world_group"));
                pet.setExp(resultSet.getDouble("exp"));
                pet.setHealth(resultSet.getDouble("health"));
                pet.setRespawnTime(resultSet.getInt("respawn_time"));
                pet.setPetName(new String(resultSet.getBytes("name"), StandardCharsets.UTF_8));
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

                pet.setSkills(TagStream.readTag(resultSet.getBytes("skills"), true));
                pet.setInfo(TagStream.readTag(resultSet.getBytes("info"), true));

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
        try {
            List<MyPetPlayer> playerList = getAllMyPetPlayers();
            Map<UUID, MyPetPlayer> owners = new HashMap<>();

            for (MyPetPlayer player : playerList) {
                owners.put(player.getInternalUUID(), player);
            }

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM pets;");
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

                pet.setSkills(TagStream.readTag(resultSet.getBytes("skills"), true));
                pet.setInfo(TagStream.readTag(resultSet.getBytes("info"), true));

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
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM pets WHERE owner_uuid=?;");
                        statement.setString(1, myPetPlayer.getInternalUUID().toString());
                        ResultSet resultSet = statement.executeQuery();
                        resultSet.next();
                        //MyPetLogger.write("HAS pet: " + (resultSet.getInt(1) > 0));

                        callback.runTask(MyPetApi.getPlugin(), resultSet.getInt(1) > 0);
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
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM pets WHERE owner_uuid=?;");
                        statement.setString(1, owner.getInternalUUID().toString());
                        ResultSet resultSet = statement.executeQuery();
                        List<StoredMyPet> pets = resultSetToMyPet(owner, resultSet, true);
                        //MyPetLogger.write("LOAD pets: " + pets);
                        callback.runTask(MyPetApi.getPlugin(), pets);
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
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM pets WHERE uuid=?;");
                        statement.setString(1, uuid.toString());

                        ResultSet resultSet = statement.executeQuery();

                        if (resultSet.next()) {
                            MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(UUID.fromString(resultSet.getString("owner_uuid")));
                            if (owner != null) {
                                List<StoredMyPet> pets = resultSetToMyPet(owner, resultSet, false);
                                if (pets.size() > 0) {
                                    //MyPetLogger.write("LOAD pet: " + pets.get(0));
                                    callback.runTask(MyPetApi.getPlugin(), pets.get(0));
                                }
                            }
                        }
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
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM pets WHERE uuid=?;");
                    statement.setString(1, uuid.toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("DELETE pet: " + result);

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
                try {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO pets (uuid, " +
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
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
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
                        statement.setBytes(13, TagStream.writeTag(storedMyPet.getSkillInfo(), true));
                        statement.setBytes(14, TagStream.writeTag(storedMyPet.getInfo(), true));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    boolean result = statement.executeUpdate() > 0;
                    //MyPetLogger.write("INSERT pet: " + result);

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
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO pets (uuid, " +
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
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");


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
                    statement.setBytes(13, TagStream.writeTag(storedMyPet.getSkillInfo(), true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    statement.setBytes(14, TagStream.writeTag(storedMyPet.getInfo(), true));
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
                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE pets SET " +
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
                            "WHERE uuid=?;");
                    statement.setString(1, storedMyPet.getOwner().getInternalUUID().toString());
                    statement.setDouble(2, storedMyPet.getExp());
                    statement.setDouble(3, storedMyPet.getHealth());
                    statement.setInt(4, storedMyPet.getRespawnTime());
                    statement.setBytes(5, storedMyPet.getPetName().getBytes(StandardCharsets.UTF_8));
                    statement.setString(6, storedMyPet.getPetType().name());
                    statement.setLong(7, storedMyPet.getLastUsed());
                    statement.setDouble(8, storedMyPet.getSaturation());
                    statement.setString(9, storedMyPet.getWorldGroup());
                    statement.setBoolean(10, storedMyPet.wantsToRespawn());
                    statement.setString(11, storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);
                    statement.setBytes(12, TagStream.writeTag(storedMyPet.getSkillInfo(), true));
                    statement.setBytes(13, TagStream.writeTag(storedMyPet.getInfo(), true));

                    statement.setString(14, storedMyPet.getUUID().toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("UPDATE pet: " + result);

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
                petPlayer.setExtendedInfo(TagStream.readTag(resultSet.getBytes("extended_info"), true));

                try {
                    JsonObject jsonObject = gson.fromJson(resultSet.getString("multi_world"), JsonObject.class);

                    for (String uuid : jsonObject.keySet()) {
                        String petUUID = jsonObject.get(uuid).getAsString();
                        petPlayer.setMyPetForWorldGroup(uuid, UUID.fromString(petUUID));
                    }
                } catch (JsonParseException e) {
                    e.printStackTrace();
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
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM players;");

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
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(internal_uuid) FROM players WHERE mojang_uuid=? OR name=?;");
                        statement.setString(1, player.getUniqueId().toString());
                        statement.setString(2, player.getName());
                        ResultSet resultSet = statement.executeQuery();
                        resultSet.next();

                        //MyPetLogger.write("IS player: " + (resultSet.getInt(1) > 0));

                        callback.runTask(MyPetApi.getPlugin(), resultSet.getInt(1) > 0);
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
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE internal_uuid=?;");
                        statement.setString(1, uuid.toString());
                        ResultSet resultSet = statement.executeQuery();
                        MyPetPlayer player = resultSetToMyPetPlayer(resultSet);
                        if (player != null) {
                            //MyPetLogger.write("LOAD player: " + player);
                            callback.runTask(MyPetApi.getPlugin(), player);
                        }
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
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE mojang_uuid=? OR name=?;");
                        statement.setString(1, player.getUniqueId().toString());
                        statement.setString(2, player.getName());
                        ResultSet resultSet = statement.executeQuery();

                        MyPetPlayer player = resultSetToMyPetPlayer(resultSet);
                        if (player != null) {
                            //MyPetLogger.write("LOAD player: " + player);
                            callback.runTask(MyPetApi.getPlugin(), player);
                        }
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
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE players SET " +
                            "mojang_uuid=?, " +
                            "name=?, " +
                            "auto_respawn=?, " +
                            "auto_respawn_min=?, " +
                            "capture_mode=?, " +
                            "health_bar=?, " +
                            "pet_idle_volume=?, " +
                            "extended_info=?, " +
                            "multi_world=? " +
                            "WHERE internal_uuid=?;");
            statement.setString(1, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
            statement.setString(2, player.getName());
            statement.setBoolean(3, player.hasAutoRespawnEnabled());
            statement.setInt(4, player.getAutoRespawnMin());
            statement.setBoolean(5, player.isCaptureHelperActive());
            statement.setBoolean(6, player.isHealthBarActive());
            statement.setFloat(7, player.getPetLivingSoundVolume());
            statement.setBytes(8, TagStream.writeTag(player.getExtendedInfo(), true));

            JsonObject multiWorldObject = new JsonObject();
            for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
            }
            statement.setString(9, gson.toJson(multiWorldObject));
            statement.setString(10, player.getInternalUUID().toString());

            int result = statement.executeUpdate();

            //MyPetLogger.write("UPDATE player: " + result);

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
                try {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO players (" +
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
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
                    statement.setString(1, player.getInternalUUID().toString());
                    statement.setString(2, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                    statement.setString(3, player.getName());
                    statement.setBoolean(4, player.hasAutoRespawnEnabled());
                    statement.setInt(5, player.getAutoRespawnMin());
                    statement.setBoolean(6, player.isCaptureHelperActive());
                    statement.setBoolean(7, player.isHealthBarActive());
                    statement.setFloat(8, player.getPetLivingSoundVolume());
                    try {
                        statement.setBytes(9, TagStream.writeTag(player.getExtendedInfo(), true));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    JsonObject multiWorldObject = new JsonObject();
                    for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                        multiWorldObject.addProperty(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
                    }
                    statement.setString(10, gson.toJson(multiWorldObject));


                    boolean result = statement.executeUpdate() > 0;
                    //MyPetLogger.write("INSERT player: " + result);

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
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO players (" +
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
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

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
                statement.setBytes(9, TagStream.writeTag(player.getExtendedInfo(), true));

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
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM players WHERE internal_uuid=?;");
                    statement.setString(1, player.getInternalUUID().toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("DELETE player: " + result);

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