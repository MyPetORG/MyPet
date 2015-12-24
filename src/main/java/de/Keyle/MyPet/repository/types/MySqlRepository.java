/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2015 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.repository.types;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.repository.RepositoryCallback;
import de.Keyle.MyPet.repository.RepositoryInitException;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.MyPetVersion;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.player.OfflineMyPetPlayer;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import de.Keyle.MyPet.util.player.UUIDFetcher;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagStream;
import de.keyle.knbt.TagString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class MySqlRepository implements Repository {
    private Connection connection;

    public static String USER = "root";
    public static String PASSWORD = "";
    public static String DATABASE = "mypet";
    public static String HOST = "localhost";
    public static int PORT = 3306;

    private int version = 2;


    @Override
    public void disable() {
        saveData();

        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void save() {
        saveData();
    }

    @Override
    public void init() throws RepositoryInitException {
        connect();

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM info;");
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                updateStructure(resultSet);
            } else {
                initStructure();
            }
        } catch (MySQLSyntaxErrorException e) {
            initStructure();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //loadData(NBTPetFile);
    }

    private void updateStructure(ResultSet resultSet) {
        try {
            int oldVersion = resultSet.getInt("version");

            if (oldVersion < version) {
                DebugLogger.info("Updating database from version " + oldVersion + " to version " + version + ".");

                switch (oldVersion) {
                    case 1:
                        updateToV2();
                    case 2:
                }

                updateInfo();
            } else {
                DebugLogger.info("No database update required.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initStructure() {
        try {
            Statement create = connection.createStatement();

            create.executeUpdate("CREATE TABLE pets (" +
                    "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "exp DOUBLE, " +
                    "health DOUBLE, " +
                    "respawn_time INTEGER, " +
                    "name VARCHAR(64), " +
                    "type VARCHAR(20), " +
                    "last_used BIGINT, " +
                    "hunger INTEGER, " +
                    "world_group VARCHAR(255), " +
                    "wants_to_spawn BOOLEAN, " +
                    "skilltree VARCHAR(255), " +
                    "skills BLOB, " +
                    "info BLOB, " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY ( uuid )" +
                    ")");

            create.executeUpdate("CREATE TABLE players (" +
                    "internal_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "mojang_uuid VARCHAR(36) UNIQUE, " +
                    "offline_uuid VARCHAR(36) UNIQUE, " +
                    "name VARCHAR(16), " +
                    "auto_respawn BOOLEAN, " +
                    "auto_respawn_min INTEGER , " +
                    "capture_mode BOOLEAN, " +
                    "health_bar INTEGER , " +
                    "extended_info BLOB, " +
                    "multi_world BLOB, " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY ( internal_uuid )" +
                    ")");

            create.executeUpdate("CREATE TABLE info (" +
                    "version INTEGER, " +
                    "mypet_version VARCHAR(20), " +
                    "mypet_build VARCHAR(20), " +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            PreparedStatement insert = connection.prepareStatement("INSERT INTO info (version, mypet_version, mypet_build) VALUES (?,?,?);");
            insert.setInt(1, version);
            insert.setString(2, MyPetVersion.getVersion());
            insert.setString(3, MyPetVersion.getBuild());
            insert.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateToV2() {
        try {
            Statement update = connection.createStatement();

            update.executeUpdate("ALTER TABLE pets ADD last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            update.executeUpdate("ALTER TABLE players ADD last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            update.executeUpdate("ALTER TABLE info ADD last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void connect() throws RepositoryInitException {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE, USER, PASSWORD);
        } catch (SQLException e) {
            MyPetLogger.write("Could not connect Connection to MySQL Database.");
            throw new RepositoryInitException(e);
        }
    }

    private void checkConnection() {
        try {
            if (this.connection == null || !this.connection.isClosed()) {
                connect();
            }
        } catch (SQLException | RepositoryInitException e) {
            try {
                connect();
            } catch (RepositoryInitException e1) {
                MyPetLogger.write("MySQL connection could not be created!");
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void countMyPets(final RepositoryCallback<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM pets;");
                    ResultSet resultSet = statement.executeQuery();
                    resultSet.next();
                    callback.setValue(resultSet.getInt(1));
                    Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void countMyPets(final MyPetType type, final RepositoryCallback<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM pets WHERE type=?;");
                    statement.setString(1, type.getTypeName());
                    ResultSet resultSet = statement.executeQuery();
                    resultSet.next();
                    callback.setValue(resultSet.getInt(1));
                    Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
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
        checkConnection();

        for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE pets SET exp=? , health=?, respawn_time=?, name=?, last_used=?, hunger=?, world_group=?, wants_to_spawn=?, skilltree=?, skills=?, info=? WHERE uuid=?;");
                statement.setDouble(1, myPet.getExp());
                statement.setDouble(2, myPet.getHealth());
                statement.setInt(3, myPet.getRespawnTime());
                statement.setString(4, myPet.getPetName());
                statement.setLong(5, myPet.getLastUsed());
                statement.setInt(6, myPet.getHungerValue());
                statement.setString(7, myPet.getWorldGroup());
                statement.setBoolean(8, myPet.wantToRespawn());
                statement.setString(9, myPet.getSkillTree() != null ? myPet.getSkillTree().getName() : null);

                TagCompound skillsNBT = new TagCompound();
                Collection<ISkillInstance> skillList = myPet.getSkills().getSkills();
                if (skillList.size() > 0) {
                    for (ISkillInstance skill : skillList) {
                        if (skill instanceof ISkillStorage) {
                            ISkillStorage storageSkill = (ISkillStorage) skill;
                            TagCompound s = storageSkill.save();
                            if (s != null) {
                                skillsNBT.getCompoundData().put(skill.getName(), s);
                            }
                        }
                    }
                }

                statement.setBlob(10, new ByteArrayInputStream(TagStream.writeTag(skillsNBT, true)));
                statement.setBlob(11, new ByteArrayInputStream(TagStream.writeTag(myPet.writeExtendedInfo(), true)));

                statement.setString(12, myPet.getUUID().toString());

                int result = statement.executeUpdate();

                //MyPetLogger.write("UPDATE pet: " + result);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void savePlayers() {
        for (MyPetPlayer player : PlayerList.getMyPetPlayers()) {
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE players " +
                                "SET mojang_uuid= ?, offline_uuid= ?, name= ?, auto_respawn= ?, auto_respawn_min=?, capture_mode=?, health_bar=?, extended_info=?, multi_world=? " +
                                "WHERE internal_uuid=?;");
                statement.setString(1, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                statement.setString(2, player.getOfflineUUID() != null ? player.getOfflineUUID().toString() : null);
                statement.setString(3, player.getName());
                statement.setBoolean(4, player.hasAutoRespawnEnabled());
                statement.setInt(5, player.getAutoRespawnMin());
                statement.setBoolean(6, player.isCaptureHelperActive());
                statement.setBoolean(7, player.isHealthBarActive());
                statement.setBlob(8, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));

                TagCompound multiWorldCompound = new TagCompound();
                for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                    multiWorldCompound.getCompoundData().put(worldGroupName, new TagString(player.getMyPetsForWorldGroups().get(worldGroupName).toString()));
                }
                statement.setBlob(9, new ByteArrayInputStream(TagStream.writeTag(multiWorldCompound, true)));
                statement.setString(10, player.getInternalUUID().toString());

                int result = statement.executeUpdate();

                //MyPetLogger.write("UPDATE player: " + result);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    private List<InactiveMyPet> resultSetToMyPet(MyPetPlayer owner, ResultSet resultSet) {
        List<InactiveMyPet> pets = new ArrayList<>();
        try {
            while (resultSet.next()) {
                InactiveMyPet pet = new InactiveMyPet(owner);
                pet.setUUID(UUID.fromString(resultSet.getString("uuid")));
                pet.setExp(resultSet.getDouble("exp"));
                pet.setHealth(resultSet.getDouble("health"));
                pet.setRespawnTime(resultSet.getInt("respawn_time"));
                pet.setPetName(resultSet.getString("name"));
                pet.setPetType(MyPetType.valueOf(resultSet.getString("type")));
                pet.setLastUsed(resultSet.getLong("last_used"));
                pet.setHungerValue(resultSet.getInt("hunger"));
                pet.setWorldGroup(resultSet.getString("world_group"));
                pet.wantsToRespawn = resultSet.getBoolean("wants_to_spawn");

                String skillTreeName = resultSet.getString("skilltree");
                if (skillTreeName != null) {
                    if (SkillTreeMobType.getMobTypeByPetType(pet.getPetType()) != null) {
                        SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByPetType(pet.getPetType());

                        if (mobType.hasSkillTree(skillTreeName)) {
                            pet.setSkillTree(mobType.getSkillTree(skillTreeName));
                        }
                    }
                }

                pet.setSkills(TagStream.readTag(resultSet.getBlob("skills").getBinaryStream(), true));
                pet.setInfo(TagStream.readTag(resultSet.getBlob("info").getBinaryStream(), true));

                pets.add(pet);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return pets;
    }

    @Override
    public List<InactiveMyPet> getAllMyPets(Map<UUID, MyPetPlayer> owners) {
        try {
            checkConnection();

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM pets;");
            List<InactiveMyPet> pets = new ArrayList<>();
            while (resultSet.next()) {
                if (!owners.containsKey(UUID.fromString(resultSet.getString("owner_uuid")))) {
                    continue;
                }
                MyPetPlayer owner = owners.get(UUID.fromString(resultSet.getString("owner_uuid")));

                InactiveMyPet pet = new InactiveMyPet(owner);
                pet.setUUID(UUID.fromString(resultSet.getString("uuid")));
                pet.setExp(resultSet.getDouble("exp"));
                pet.setHealth(resultSet.getDouble("health"));
                pet.setRespawnTime(resultSet.getInt("respawn_time"));
                pet.setPetName(resultSet.getString("name"));
                pet.setPetType(MyPetType.valueOf(resultSet.getString("type")));
                pet.setLastUsed(resultSet.getLong("last_used"));
                pet.setHungerValue(resultSet.getInt("hunger"));
                pet.setWorldGroup(resultSet.getString("world_group"));
                pet.wantsToRespawn = resultSet.getBoolean("wants_to_spawn");

                String skillTreeName = resultSet.getString("skilltree");
                if (skillTreeName != null) {
                    if (SkillTreeMobType.getMobTypeByPetType(pet.getPetType()) != null) {
                        SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByPetType(pet.getPetType());

                        if (mobType.hasSkillTree(skillTreeName)) {
                            pet.setSkillTree(mobType.getSkillTree(skillTreeName));
                        }
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
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    checkConnection();

                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(uuid) FROM pets WHERE owner_uuid=?;");
                        statement.setString(1, myPetPlayer.getInternalUUID().toString());
                        ResultSet resultSet = statement.executeQuery();
                        resultSet.next();
                        //MyPetLogger.write("HAS pet: " + (resultSet.getInt(1) > 0));

                        callback.setValue(resultSet.getInt(1) > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<InactiveMyPet>> callback) {
        if (callback != null) {
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    checkConnection();

                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM pets WHERE owner_uuid=?;");
                        statement.setString(1, owner.getInternalUUID().toString());
                        ResultSet resultSet = statement.executeQuery();
                        List<InactiveMyPet> pets = resultSetToMyPet(owner, resultSet);
                        //MyPetLogger.write("LOAD pets: " + pets);
                        callback.setValue(pets);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void getMyPet(final UUID uuid, final RepositoryCallback<InactiveMyPet> callback) {
        if (callback != null) {
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    checkConnection();

                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM pets WHERE uuid=?;");
                        statement.setString(1, uuid.toString());

                        ResultSet resultSet = statement.executeQuery();

                        if (resultSet.first()) {
                            MyPetPlayer owner = PlayerList.getMyPetPlayer(UUID.fromString(resultSet.getString("owner_uuid")));
                            if (owner != null) {
                                resultSet.beforeFirst();
                                List<InactiveMyPet> pets = resultSetToMyPet(owner, resultSet);
                                if (pets.size() > 0) {
                                    //MyPetLogger.write("LOAD pet: " + pets.get(0));
                                    callback.setValue(pets.get(0));
                                    Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void removeMyPet(final UUID uuid, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM pets WHERE uuid=?;");
                    statement.setString(1, uuid.toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("DELETE pet: " + result);

                    if (callback != null) {
                        callback.setValue(result > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.setValue(false);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                }
            }
        });
    }

    @Override
    public void removeMyPet(final InactiveMyPet inactiveMyPet, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM pets WHERE internal_uuid=?;");
                    statement.setString(1, inactiveMyPet.getUUID().toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("DELETE pet: " + result);

                    if (callback != null) {
                        callback.setValue(result > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.setValue(false);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                }
            }
        });
    }

    @Override
    public void addMyPet(final InactiveMyPet inactiveMyPet, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                boolean result = addMyPet(inactiveMyPet);
                //MyPetLogger.write("INSERT pet: " + result);

                if (callback != null) {
                    callback.setValue(result);
                    Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                }
            }
        });
    }

    public boolean addMyPet(InactiveMyPet myPet) {
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
            statement.setString(1, myPet.getUUID().toString());
            statement.setString(2, myPet.getOwner().getInternalUUID().toString());
            statement.setDouble(3, myPet.getExp());
            statement.setDouble(4, myPet.getHealth());
            statement.setInt(5, myPet.getRespawnTime());
            statement.setString(6, myPet.getPetName());
            statement.setString(7, myPet.getPetType().getTypeName());
            statement.setLong(8, myPet.getLastUsed());
            statement.setInt(9, myPet.getHungerValue());
            statement.setString(10, myPet.getWorldGroup());
            statement.setBoolean(11, myPet.wantsToRespawn);
            statement.setString(12, myPet.getSkillTree() != null ? myPet.getSkillTree().getName() : null);

            statement.setBlob(13, new ByteArrayInputStream(TagStream.writeTag(myPet.getSkills(), true)));
            statement.setBlob(14, new ByteArrayInputStream(TagStream.writeTag(myPet.getInfo(), true)));

            return statement.executeUpdate() > 0;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void updateMyPet(final MyPet myPet, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE pets SET exp=? , health=?, respawn_time=?, name=?, last_used=?, hunger=?, world_group=?, wants_to_spawn=?, skilltree=?, skills=?, info=? WHERE uuid=?;");
                    statement.setDouble(1, myPet.getExp());
                    statement.setDouble(2, myPet.getHealth());
                    statement.setInt(3, myPet.getRespawnTime());
                    statement.setString(4, myPet.getPetName());
                    statement.setLong(5, myPet.getLastUsed());
                    statement.setInt(6, myPet.getHungerValue());
                    statement.setString(7, myPet.getWorldGroup());
                    statement.setBoolean(8, myPet.wantToRespawn());
                    statement.setString(9, myPet.getSkillTree() != null ? myPet.getSkillTree().getName() : null);

                    TagCompound skillsNBT = new TagCompound();
                    Collection<ISkillInstance> skillList = myPet.getSkills().getSkills();
                    if (skillList.size() > 0) {
                        for (ISkillInstance skill : skillList) {
                            if (skill instanceof ISkillStorage) {
                                ISkillStorage storageSkill = (ISkillStorage) skill;
                                TagCompound s = storageSkill.save();
                                if (s != null) {
                                    skillsNBT.getCompoundData().put(skill.getName(), s);
                                }
                            }
                        }
                    }

                    statement.setBlob(10, new ByteArrayInputStream(TagStream.writeTag(skillsNBT, true)));
                    statement.setBlob(11, new ByteArrayInputStream(TagStream.writeTag(myPet.writeExtendedInfo(), true)));

                    statement.setString(12, myPet.getUUID().toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("UPDATE pet: " + result);

                    if (callback != null) {
                        callback.setValue(result > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /*
    private TagList savePets() {
        List<TagCompound> petList = new ArrayList<>();

        for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
            List<InactiveMyPet> pets = myPets.get(myPet.getOwner());
            for (InactiveMyPet pet : pets) {
                if (myPet.getUUID().equals(pet.getUUID())) {
                    myPets.put(myPet.getOwner(), MyPetList.getInactiveMyPetFromMyPet(myPet));
                    myPets.remove(myPet.getOwner(), pet);
                    break;
                }
            }
        }
        for (InactiveMyPet inactiveMyPet : myPets.values()) {
            try {
                TagCompound petNBT = inactiveMyPet.save();
                petList.add(petNBT);
            } catch (Exception e) {
                DebugLogger.printThrowable(e);
            }
        }
        return new TagList(petList);
    }
    */

    // Players ---------------------------------------------------------------------------------------------------------

    private MyPetPlayer resultSetToMyPetPlayer(ResultSet resultSet) {
        try {
            if (resultSet.next()) {

                MyPetPlayer petPlayer = null;

                UUID internalUUID = UUID.fromString(resultSet.getString("internal_uuid"));
                String playerName = resultSet.getString("name");

                if (BukkitUtil.isInOnlineMode()) {
                    UUID mojangUUID = resultSet.getString("mojang_uuid") != null ? UUID.fromString(resultSet.getString("mojang_uuid")) : null;
                    if (mojangUUID != null) {
                        petPlayer = new OnlineMyPetPlayer(internalUUID, mojangUUID);
                        if (playerName != null) {
                            ((OnlineMyPetPlayer) petPlayer).setLastKnownName(playerName);
                        }
                    } else if (playerName != null) {
                        Map<String, UUID> fetchedUUIDs = UUIDFetcher.call(playerName);
                        if (!fetchedUUIDs.containsKey(playerName)) {
                            MyPetLogger.write(ChatColor.RED + "Can't get UUID for \"" + playerName + "\"! Pets may not be loaded for this player!");
                            return null;
                        } else {
                            petPlayer = new OnlineMyPetPlayer(fetchedUUIDs.get(playerName));
                            ((OnlineMyPetPlayer) petPlayer).setLastKnownName(playerName);
                        }
                    }
                } else {
                    if (playerName == null) {
                        return null;
                    }
                    petPlayer = new OfflineMyPetPlayer(internalUUID, playerName);
                }
                if (petPlayer != null) {

                    petPlayer.setAutoRespawnEnabled(resultSet.getBoolean("auto_respawn"));
                    petPlayer.setAutoRespawnMin(resultSet.getInt("auto_respawn_min"));
                    petPlayer.setCaptureHelperActive(resultSet.getBoolean("capture_mode"));
                    petPlayer.setHealthBarActive(resultSet.getBoolean("health_bar"));
                    petPlayer.setExtendedInfo(TagStream.readTag(resultSet.getBlob("extended_info").getBinaryStream(), true));

                    TagCompound worldGroups = TagStream.readTag(resultSet.getBlob("multi_world").getBinaryStream(), true);
                    for (String worldGroupName : worldGroups.getCompoundData().keySet()) {
                        String petUUID = worldGroups.getAs(worldGroupName, TagString.class).getStringData();
                        petPlayer.setMyPetForWorldGroup(worldGroupName, UUID.fromString(petUUID));
                    }

                    DebugLogger.info("   " + petPlayer);
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
        checkConnection();

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
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    checkConnection();

                    String uuidType = BukkitUtil.isInOnlineMode() ? "mojang" : "offline";

                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(internal_uuid) FROM players WHERE " + uuidType + "_uuid=?;");
                        statement.setString(1, player.getUniqueId().toString());
                        ResultSet resultSet = statement.executeQuery();
                        resultSet.next();

                        //MyPetLogger.write("IS player: " + (resultSet.getInt(1) > 0));

                        callback.setValue(resultSet.getInt(1) > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void getMyPetPlayer(final UUID uuid, final RepositoryCallback<MyPetPlayer> callback) {
        if (callback != null) {
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    checkConnection();

                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE internal_uuid=?;");
                        statement.setString(1, uuid.toString());
                        ResultSet resultSet = statement.executeQuery();
                        MyPetPlayer player = resultSetToMyPetPlayer(resultSet);
                        if (player != null) {
                            //MyPetLogger.write("LOAD player: " + player);
                            callback.setValue(player);
                            Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void getMyPetPlayer(final Player player, final RepositoryCallback<MyPetPlayer> callback) {
        if (callback != null) {
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    checkConnection();

                    String uuidType = BukkitUtil.isInOnlineMode() ? "mojang" : "offline";
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE " + uuidType + "_uuid=?;");
                        statement.setString(1, player.getUniqueId().toString());
                        ResultSet resultSet = statement.executeQuery();

                        MyPetPlayer player = resultSetToMyPetPlayer(resultSet);
                        if (player != null) {
                            //MyPetLogger.write("LOAD player: " + player);
                            callback.setValue(player);
                            Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void updatePlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE players " +
                                    "SET mojang_uuid= ?, offline_uuid= ?, name= ?, auto_respawn= ?, auto_respawn_min=?, capture_mode=?, health_bar=?, extended_info=?, multi_world=? " +
                                    "WHERE internal_uuid=?;");
                    statement.setString(1, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
                    statement.setString(2, player.getOfflineUUID() != null ? player.getOfflineUUID().toString() : null);
                    statement.setString(3, player.getName());
                    statement.setBoolean(4, player.hasAutoRespawnEnabled());
                    statement.setInt(5, player.getAutoRespawnMin());
                    statement.setBoolean(6, player.isCaptureHelperActive());
                    statement.setBoolean(7, player.isHealthBarActive());
                    statement.setBlob(8, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));

                    TagCompound multiWorldCompound = new TagCompound();
                    for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                        multiWorldCompound.getCompoundData().put(worldGroupName, new TagString(player.getMyPetsForWorldGroups().get(worldGroupName).toString()));
                    }
                    statement.setBlob(9, new ByteArrayInputStream(TagStream.writeTag(multiWorldCompound, true)));
                    statement.setString(10, player.getInternalUUID().toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("UPDATE player: " + result);

                    if (callback != null) {
                        callback.setValue(result > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void addMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                boolean result = addMyPetPlayer(player);
                //MyPetLogger.write("INSERT player: " + result);

                if (callback != null) {
                    callback.setValue(result);
                    Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                }
            }
        });
    }

    public boolean addMyPetPlayer(MyPetPlayer player) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO players (" +
                            "internal_uuid, " +
                            "mojang_uuid, " +
                            "offline_uuid, " +
                            "name, " +
                            "auto_respawn, " +
                            "auto_respawn_min, " +
                            "capture_mode, " +
                            "health_bar, " +
                            "extended_info, " +
                            "multi_world) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            statement.setString(1, player.getInternalUUID().toString());
            statement.setString(2, player.getMojangUUID() != null ? player.getMojangUUID().toString() : null);
            statement.setString(3, player.getOfflineUUID() != null ? player.getOfflineUUID().toString() : null);
            statement.setString(4, player.getName());
            statement.setBoolean(5, player.hasAutoRespawnEnabled());
            statement.setInt(6, player.getAutoRespawnMin());
            statement.setBoolean(7, player.isCaptureHelperActive());
            statement.setBoolean(8, player.isHealthBarActive());
            statement.setBlob(9, new ByteArrayInputStream(TagStream.writeTag(player.getExtendedInfo(), true)));

            TagCompound multiWorldCompound = new TagCompound();
            for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
                multiWorldCompound.getCompoundData().put(worldGroupName, new TagString(player.getMyPetsForWorldGroups().get(worldGroupName).toString()));
            }
            statement.setBlob(10, new ByteArrayInputStream(TagStream.writeTag(multiWorldCompound, true)));

            return statement.executeUpdate() > 0;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
            @Override
            public void run() {
                checkConnection();

                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM players WHERE internal_uuid=?;");
                    statement.setString(1, player.getInternalUUID().toString());

                    int result = statement.executeUpdate();

                    //MyPetLogger.write("DELETE player: " + result);

                    if (callback != null) {
                        callback.setValue(result > 0);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.setValue(false);
                        Bukkit.getScheduler().runTask(MyPetPlugin.getPlugin(), callback);
                    }
                }
            }
        });
    }
}