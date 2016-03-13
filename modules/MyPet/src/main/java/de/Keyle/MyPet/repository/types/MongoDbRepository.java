/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.UUIDFetcher;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.repository.RepositoryInitException;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.player.OfflineMyPetPlayer;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import de.keyle.knbt.TagStream;
import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.*;

public class MongoDbRepository implements Repository {
    private MongoClient mongo;
    private MongoDatabase db;
    private int version = 1;
    // https://search.maven.org/remotecontent?filepath=org/mongodb/mongo-java-driver/3.2.1/mongo-java-driver-3.2.1.jar

    @Override
    public void disable() {
        saveData();

        if (this.mongo != null) {
            this.mongo.close();
        }
    }

    @Override
    public void save() {
        saveData();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws RepositoryInitException {
        connect();

        if (!collectionExists(Configuration.Repository.MongoDB.PREFIX + "info")) {
            initStructure();
        } else {

            MongoCollection infoCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "info");
            Document info = (Document) infoCollection.find().first();

            updateStructure(info.getInteger("version"));
        }

        updateInfo();
    }

    @SuppressWarnings("unchecked")
    private void initStructure() {
        db.createCollection(Configuration.Repository.MongoDB.PREFIX + "info");
        db.createCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
        db.createCollection(Configuration.Repository.MongoDB.PREFIX + "players");

        Document info = new Document();

        updateInfoDocument(info);

        MongoCollection infoCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "info");
        infoCollection.insertOne(info);
    }

    private void updateStructure(int oldVersion) {
        if (oldVersion < version) {
            MyPetApi.getLogger().info("Updating database from version " + oldVersion + " to version " + version + ".");

            switch (oldVersion) {
                case 1:
                    //updateToV2();
            }
        }
    }

    public boolean collectionExists(final String collectionName) {
        for (final String name : db.listCollectionNames()) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    private void connect() throws RepositoryInitException {
        try {
            MongoClientOptions.Builder o = MongoClientOptions.builder().connectTimeout(3000);
            if (Configuration.Repository.MongoDB.USER.equals("")) {
                this.mongo = new MongoClient(new ServerAddress(Configuration.Repository.MongoDB.HOST, Configuration.Repository.MongoDB.PORT), o.build());
            } else {
                MongoCredential credentials = MongoCredential.createCredential(Configuration.Repository.MongoDB.USER, Configuration.Repository.MongoDB.DATABASE, Configuration.Repository.MongoDB.PASSWORD.toCharArray());
                this.mongo = new MongoClient(new ServerAddress(Configuration.Repository.MongoDB.HOST, Configuration.Repository.MongoDB.PORT), Lists.newArrayList(credentials), o.build());
            }

            this.mongo.getAddress();

            this.db = this.mongo.getDatabase(Configuration.Repository.MongoDB.DATABASE);
        } catch (Exception e) {
            throw new RepositoryInitException(e);
        }
    }

    @Override
    public void cleanup(final long timestamp, final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                long result = petCollection.deleteMany(new Document("last_used", new Document("$lt", timestamp))).getDeletedCount();
                if (callback != null) {
                    callback.runTask((int) result);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @Override
    public void countMyPets(final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (callback != null) {
                    MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                    long result = petCollection.count();
                    callback.runTask((int) result);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @Override
    public void countMyPets(final MyPetType type, final RepositoryCallback<Integer> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (callback != null) {
                    MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                    long result = petCollection.count(new Document("type", type.name()));
                    callback.runTask((int) result);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    public void saveData() {
        updateInfo();
        savePets();
        savePlayers();
    }

    @SuppressWarnings("unchecked")
    private void updateInfo() {
        MongoCollection infoCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "info");
        Document info = (Document) infoCollection.find().first();
        updateInfoDocument(info);
        updateInfoDocument(info);
        infoCollection.replaceOne(new Document("_id", info.getObjectId("_id")), info);
    }

    private void updateInfoDocument(Document info) {
        info.append("version", version);
        info.append("mypet_version", MyPetVersion.getVersion());
        info.append("mypet_build", MyPetVersion.getBuild());
        info.append("last_update", new Date());
    }

    private void savePets() {
        for (StoredMyPet storedMyPet : MyPetApi.getMyPetList().getAllActiveMyPets()) {
            updateMyPet(storedMyPet);
        }
    }

    @SuppressWarnings("unchecked")
    private void savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerList().getMyPetPlayers()) {
            updatePlayer(player);
        }
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    private StoredMyPet documentToMyPet(MyPetPlayer owner, Document document) {
        try {
            InactiveMyPet pet = new InactiveMyPet(owner);
            pet.setUUID(UUID.fromString(document.getString("uuid")));
            pet.setExp(document.getDouble("exp"));
            pet.setHealth(document.getDouble("health"));
            pet.setRespawnTime(document.getInteger("respawn_time"));
            pet.setPetName(document.getString("name"));
            pet.setPetType(MyPetType.valueOf(document.getString("type")));
            pet.setLastUsed(document.getLong("last_used"));
            pet.setHungerValue(document.getDouble("hunger"));
            pet.setWorldGroup(document.getString("world_group"));
            pet.wantsToRespawn = document.getBoolean("wants_to_spawn");

            String skillTreeName = document.getString("skilltree");
            if (skillTreeName != null) {
                if (SkillTreeMobType.byPetType(pet.getPetType()) != null) {
                    SkillTreeMobType mobType = SkillTreeMobType.byPetType(pet.getPetType());

                    if (mobType.hasSkillTree(skillTreeName)) {
                        pet.setSkilltree(mobType.getSkillTree(skillTreeName));
                    }
                }
            }

            pet.setSkills(TagStream.readTag(((Binary) document.get("skills")).getData(), true));
            pet.setInfo(TagStream.readTag(((Binary) document.get("info")).getData(), true));

            return pet;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StoredMyPet> getAllMyPets() {

        List<MyPetPlayer> playerList = getAllMyPetPlayers();
        final Map<UUID, MyPetPlayer> owners = new HashMap<>();

        for (MyPetPlayer player : playerList) {
            owners.put(player.getInternalUUID(), player);
        }

        MongoCollection petCollection = this.db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");

        final List<StoredMyPet> myPetList = new ArrayList<>();

        petCollection.find().forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                UUID ownerUUID = UUID.fromString(document.getString("owner_uuid"));
                if (owners.containsKey(ownerUUID)) {
                    StoredMyPet storedMyPet = documentToMyPet(owners.get(ownerUUID), document);
                    if (storedMyPet != null) {
                        myPetList.add(storedMyPet);
                    }
                }
            }
        });


        return myPetList;
    }

    @Override
    public void hasMyPets(final MyPetPlayer myPetPlayer, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                        long result = petCollection.count(new Document("owner_uuid", myPetPlayer.getInternalUUID()));
                        callback.runTask(result > 0);
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<StoredMyPet>> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        final List<StoredMyPet> pets = new ArrayList<>();
                        MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                        FindIterable petDocuments = petCollection.find(new Document("owner_uuid", owner.getInternalUUID()));
                        petDocuments.forEach(new Block<Document>() {
                            @Override
                            public void apply(final Document document) {
                                StoredMyPet storedMyPet = documentToMyPet(owner, document);
                                if (storedMyPet != null) {
                                    pets.add(storedMyPet);
                                }
                            }
                        });
                        callback.runTask(pets);
                    }
                }
            }.runTaskAsynchronously(MyPetApi.getPlugin());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getMyPet(final UUID uuid, final RepositoryCallback<StoredMyPet> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                        Document petDocument = (Document) petCollection.find(new Document("uuid", uuid.toString())).first();
                        if (petDocument != null) {
                            MyPetPlayer owner = MyPetApi.getPlayerList().getMyPetPlayer(UUID.fromString(petDocument.getString("owner_uuid")));
                            StoredMyPet pet = documentToMyPet(owner, petDocument);

                            if (pet != null) {
                                callback.runTask(pet);
                            }
                        }
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
                MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
                boolean result = petCollection.deleteOne(new Document("uuid", uuid.toString())).getDeletedCount() > 0;
                if (callback != null) {
                    callback.runTask(result);
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
                addMyPet(storedMyPet);

                if (callback != null) {
                    callback.runTask(true);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @SuppressWarnings("unchecked")
    public void addMyPet(StoredMyPet storedMyPet) {
        MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");

        Document petDocument = new Document();
        petDocument.append("uuid", storedMyPet.getUUID().toString());
        petDocument.append("owner_uuid", storedMyPet.getOwner().getInternalUUID().toString());
        petDocument.append("exp", storedMyPet.getExp());
        petDocument.append("health", storedMyPet.getHealth());
        petDocument.append("respawn_time", storedMyPet.getRespawnTime());
        petDocument.append("name", storedMyPet.getPetName());
        petDocument.append("type", storedMyPet.getPetType().name());
        petDocument.append("last_used", storedMyPet.getLastUsed());
        petDocument.append("hunger", storedMyPet.getHungerValue());
        petDocument.append("world_group", storedMyPet.getWorldGroup());
        petDocument.append("wants_to_spawn", storedMyPet.wantsToRespawn());
        petDocument.append("skilltree", storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);

        try {
            petDocument.append("skills", TagStream.writeTag(storedMyPet.getSkillInfo(), true));
            petDocument.append("info", TagStream.writeTag(storedMyPet.getInfo(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        petCollection.insertOne(petDocument);
    }

    @Override
    public void updateMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean result = updateMyPet(storedMyPet);

                if (callback != null) {
                    callback.runTask(result);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @SuppressWarnings("unchecked")
    private boolean updateMyPet(StoredMyPet storedMyPet) {
        MongoCollection petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
        Document filter = new Document("uuid", storedMyPet.getUUID().toString());
        Document petDocument = (Document) petCollection.find(filter).first();

        if (petDocument == null) {
            return false;
        }

        petDocument.append("uuid", storedMyPet.getUUID().toString());
        petDocument.append("owner_uuid", storedMyPet.getOwner().getInternalUUID().toString());
        petDocument.append("exp", storedMyPet.getExp());
        petDocument.append("health", storedMyPet.getHealth());
        petDocument.append("respawn_time", storedMyPet.getRespawnTime());
        petDocument.append("name", storedMyPet.getPetName());
        petDocument.append("type", storedMyPet.getPetType().name());
        petDocument.append("last_used", storedMyPet.getLastUsed());
        petDocument.append("hunger", storedMyPet.getHungerValue());
        petDocument.append("world_group", storedMyPet.getWorldGroup());
        petDocument.append("wants_to_spawn", storedMyPet.wantsToRespawn());
        petDocument.append("skilltree", storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);

        try {
            petDocument.append("skills", TagStream.writeTag(storedMyPet.getSkillInfo(), true));
            petDocument.append("info", TagStream.writeTag(storedMyPet.getInfo(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        petCollection.replaceOne(filter, petDocument);

        return true;
    }

    // Players ---------------------------------------------------------------------------------------------------------

    private MyPetPlayer documentToPlayer(Document document) {
        try {

            MyPetPlayer petPlayer = null;

            UUID internalUUID = UUID.fromString(document.getString("internal_uuid"));
            String playerName = document.getString("name");

            if (MyPetApi.getPlugin().isInOnlineMode()) {
                UUID mojangUUID = document.getString("mojang_uuid") != null ? UUID.fromString(document.getString("mojang_uuid")) : null;
                if (mojangUUID != null) {
                    petPlayer = new OnlineMyPetPlayer(internalUUID, mojangUUID);
                    if (playerName != null) {
                        ((OnlineMyPetPlayer) petPlayer).setLastKnownName(playerName);
                    }
                } else if (playerName != null) {
                    Map<String, UUID> fetchedUUIDs = UUIDFetcher.call(playerName);
                    if (!fetchedUUIDs.containsKey(playerName)) {
                        MyPetApi.getLogger().warning(ChatColor.RED + "Can't get UUID for \"" + playerName + "\"! Pets may not be loaded for this player!");
                        return null;
                    } else {
                        petPlayer = new OnlineMyPetPlayer(internalUUID, fetchedUUIDs.get(playerName));
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
                petPlayer.setAutoRespawnEnabled(document.getBoolean("auto_respawn"));
                petPlayer.setAutoRespawnMin(document.getInteger("auto_respawn_min"));
                petPlayer.setCaptureHelperActive(document.getBoolean("capture_mode"));
                petPlayer.setHealthBarActive(document.getBoolean("health_bar"));
                petPlayer.setPetLivingSoundVolume(document.getDouble("pet_idle_volume").floatValue());
                petPlayer.setExtendedInfo(TagStream.readTag(((Binary) document.get("extended_info")).getData(), true));

                Document jsonObject = (Document) document.get("multi_world");
                for (Object o : jsonObject.keySet()) {
                    String petUUID = jsonObject.get(o.toString()).toString();
                    petPlayer.setMyPetForWorldGroup(o.toString(), UUID.fromString(petUUID));
                }

            }
            return petPlayer;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MyPetPlayer> getAllMyPetPlayers() {
        MongoCollection playerCollection = this.db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");

        final List<MyPetPlayer> playerList = new ArrayList<>();
        playerCollection.find().forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                MyPetPlayer player = documentToPlayer(document);
                if (player != null) {
                    playerList.add(player);
                }
            }
        });
        return playerList;
    }

    @Override
    public void isMyPetPlayer(final Player player, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        String uuidType = MyPetApi.getPlugin().isInOnlineMode() ? "mojang" : "offline";

                        MongoCollection playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
                        long result = playerCollection.count(new Document(uuidType + "_uuid", player.getUniqueId().toString()));
                        callback.runTask(result > 0);
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
                    if (callback != null) {
                        MongoCollection playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
                        Document playerDocument = (Document) playerCollection.find(new Document("internal_uuid", uuid.toString())).first();
                        if (playerDocument != null) {
                            MyPetPlayer player = documentToPlayer(playerDocument);
                            if (player != null) {
                                callback.runTask(player);
                            }
                        }
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
                    if (callback != null) {
                        String uuidType = MyPetApi.getPlugin().isInOnlineMode() ? "mojang" : "offline";
                        MongoCollection playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
                        Document playerDocument = (Document) playerCollection.find(new Document(uuidType + "_uuid", player.getUniqueId().toString())).first();
                        if (playerDocument != null) {
                            MyPetPlayer player = documentToPlayer(playerDocument);
                            if (player != null) {
                                callback.runTask(player);
                            }
                        }
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
        MongoCollection playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        Document filter = new Document("internal_uuid", player.getInternalUUID().toString());
        Document playerDocument = (Document) playerCollection.find(filter).first();
        if (playerDocument != null) {
            setPlayerData(player, playerDocument);
            return playerCollection.replaceOne(filter, playerDocument).getModifiedCount() > 0;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void setPlayerData(MyPetPlayer player, Document playerDocument) {
        playerDocument.append("internal_uuid", player.getInternalUUID().toString());
        playerDocument.append("mojang_uuid", player.getMojangUUID().toString());
        playerDocument.append("offline_uuid", player.getOfflineUUID().toString());
        playerDocument.append("name", player.getName());
        playerDocument.append("auto_respawn", player.hasAutoRespawnEnabled());
        playerDocument.append("auto_respawn_min", player.getAutoRespawnMin());
        playerDocument.append("capture_mode", player.isCaptureHelperActive());
        playerDocument.append("health_bar", player.isHealthBarActive());
        playerDocument.append("pet_idle_volume", player.getPetLivingSoundVolume());

        try {
            playerDocument.append("extended_info", TagStream.writeTag(player.getExtendedInfo(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Document multiWorldDocument = new Document();
        for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
            multiWorldDocument.append(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
        }

        playerDocument.append("multi_world", multiWorldDocument);
    }


    @Override
    @SuppressWarnings("unchecked")
    public void addMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                addMyPetPlayer(player);

                if (callback != null) {
                    callback.runTask(MyPetApi.getPlugin(), true);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    @SuppressWarnings("unchecked")
    public boolean addMyPetPlayer(MyPetPlayer player) {
        Document playerDocument = new Document();
        setPlayerData(player, playerDocument);

        MongoCollection playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        playerCollection.insertOne(playerDocument);
        return true;
    }

    @Override
    public void removeMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                MongoCollection playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
                boolean result = playerCollection.deleteOne(new Document("internal_uuid", player.getInternalUUID().toString())).getDeletedCount() > 0;
                if (callback != null) {
                    callback.runTask(result);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
    }
}