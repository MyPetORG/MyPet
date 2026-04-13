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
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryInitException;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.NbtUtil;
import de.Keyle.MyPet.entity.InactiveMyPet;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MongoDbRepository implements Repository {

    private MongoClient mongo;
    private final Map<UUID, StoredMyPet> petsToBeSaved = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, MyPetPlayer> playersToBeSaved = new java.util.concurrent.ConcurrentHashMap<>();
    private MongoDatabase db;
    private int version = 4;

    // MongoDB Java driver is thread-safe per MongoClient.
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "MyPet-Mongo");
        t.setDaemon(true);
        return t;
    });

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

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (this.mongo != null) {
            this.mongo.close();
        }
    }

    @Override
    public void save() {
        saveData();
    }

    @Override
    public void init() throws RepositoryInitException {
        connect();

        if (!collectionExists(Configuration.Repository.MongoDB.PREFIX + "info")) {
            initStructure();
        } else {

            MongoCollection<Document> infoCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "info");
            Document info = infoCollection.find().first();

            updateStructure(info.getInteger("version"));
        }

        updateInfo();
    }

    private void initStructure() {
        db.createCollection(Configuration.Repository.MongoDB.PREFIX + "info");
        db.createCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
        db.createCollection(Configuration.Repository.MongoDB.PREFIX + "players");

        MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
        petCollection.createIndex(new BasicDBObject("uuid", 1));
        petCollection.createIndex(new BasicDBObject("owner_uuid", 1));
        MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        playerCollection.createIndex(new BasicDBObject("uuid", 1));

        Document info = new Document();

        updateInfoDocument(info);

        MongoCollection<Document> infoCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "info");
        infoCollection.insertOne(info);
    }

    private void updateStructure(int oldVersion) {
        if (oldVersion < version) {
            MyPetApi.getLogger().info("Updating database from version " + oldVersion + " to version " + version + ".");

            switch (oldVersion) {
                case 1:
                    updateToV2();
                case 2:
                    updateToV3();
                case 3:
                    updateToV4();
            }
        }
    }

    private void updateToV2() {
        MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
        petCollection.createIndex(new BasicDBObject("uuid", 1));
        petCollection.createIndex(new BasicDBObject("owner_uuid", 1));
        MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        playerCollection.createIndex(new BasicDBObject("uuid", 1));
    }

    private void updateToV3() {
        MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        playerCollection.dropIndex(new BasicDBObject("offline_uuid", 1));
        playerCollection.createIndex(new BasicDBObject("name", 1));
    }

    private void updateToV4() {
        MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        Document filter = new Document();
        Document data = new Document("$set", new Document("last_update", System.currentTimeMillis()));

        playerCollection.updateMany(filter, data);
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
            if (Configuration.Repository.MongoDB.USER.isEmpty()) {
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
    public CompletableFuture<Integer> cleanup(final long timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            return (int) petCollection.deleteMany(new Document("last_used", new Document("$lt", timestamp))).getDeletedCount();
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countMyPets() {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            return (int) petCollection.count();
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countMyPets(final MyPetType type) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            return (int) petCollection.count(new Document("type", type.name()));
        }, executor);
    }

    public void saveData() {
        updateInfo();
        savePets();
        savePlayers();
    }

    private void updateInfo() {
        MongoCollection<Document> infoCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "info");
        Document info = infoCollection.find().first();
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
        for (StoredMyPet storedMyPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            savePet(storedMyPet);
        }
        for (StoredMyPet myPet : petsToBeSaved.values()) {
            savePet(myPet);
        }
    }

    private void savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
            updatePlayer(player);
        }
        for (MyPetPlayer player : playersToBeSaved.values()) {
            updatePlayer(player);
        }
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    private StoredMyPet documentToMyPet(MyPetPlayer owner, Document document) {
        try {
            InactiveMyPet pet = new InactiveMyPet(owner);
            pet.setUUID(UUID.fromString(document.getString("uuid")));
            pet.setWorldGroup(document.getString("world_group"));
            pet.setExp(document.getDouble("exp"));
            pet.setHealth(document.getDouble("health"));
            pet.setRespawnTime(document.getInteger("respawn_time"));
            pet.setPetName(document.getString("name"));
            MyPetType type = MyPetType.byNameOrNull(document.getString("type"));
            if (type == null) return null;
            pet.setPetType(type);
            pet.setLastUsed(document.getLong("last_used"));
            pet.setSaturation(((Number) document.get("hunger")).doubleValue());
            pet.wantsToRespawn = document.getBoolean("wants_to_spawn");

            String skillTreeName = document.getString("skilltree");
            if (skillTreeName != null) {
                Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skillTreeName);
                if (skilltree != null) {
                    pet.setSkilltree(skilltree);
                }
            }

            byte[] skillsData = ((Binary) document.get("skills")).getData();
            try {
                pet.setSkills(NbtUtil.readCompressed(skillsData));
            } catch (IOException e) {
                MyPetApi.getLogger().warning("Failed to load skills for " + pet.getOwner().getName() + "'s Pet " + pet.getPetName() + " - the data was likely corrupted.");
                backupCorruptedData(pet, "skills", skillsData);
                pet.setSkills(CompoundBinaryTag.empty());
            }

            byte[] infoData = ((Binary) document.get("info")).getData();
            try {
                pet.setInfo(NbtUtil.readCompressed(infoData));
            } catch (IOException e) {
                MyPetApi.getLogger().warning("Failed to load info for " + pet.getOwner().getName() + "'s Pet " + pet.getPetName() + " - the data was likely corrupted.");
                backupCorruptedData(pet, "info", infoData);
                pet.setInfo(CompoundBinaryTag.empty());
            }

            return pet;
        } catch (Exception e) {
            ErrorUtil.reportError("MongoDB database operation failed", e);
        }

        return null;
    }

    @Override
    public List<StoredMyPet> getAllMyPets() {

        List<MyPetPlayer> playerList = getAllMyPetPlayers();
        final Map<UUID, MyPetPlayer> owners = new HashMap<>();

        for (MyPetPlayer player : playerList) {
            owners.put(player.getUniqueId(), player);
        }

        MongoCollection<Document> petCollection = this.db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");

        final List<StoredMyPet> myPetList = new ArrayList<>();

        petCollection.find().forEach((Block<Document>) document -> {
            UUID ownerUUID = UUID.fromString(document.getString("owner_uuid"));
            if (owners.containsKey(ownerUUID)) {
                StoredMyPet storedMyPet = documentToMyPet(owners.get(ownerUUID), document);
                if (storedMyPet != null) {
                    myPetList.add(storedMyPet);
                }
            }
        });


        return myPetList;
    }

    @Override
    public CompletableFuture<Boolean> hasMyPets(final MyPetPlayer myPetPlayer) {
        if (myPetPlayer == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            return petCollection.count(new Document("owner_uuid", myPetPlayer.getUniqueId().toString())) > 0;
        }, executor);
    }

    @Override
    public CompletableFuture<List<StoredMyPet>> getMyPets(final MyPetPlayer owner) {
        if (owner == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            final List<StoredMyPet> pets = new ArrayList<>();
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            FindIterable<Document> petDocuments = petCollection.find(new Document("owner_uuid", owner.getUniqueId().toString()));
            petDocuments.forEach((Block<Document>) document -> {
                StoredMyPet storedMyPet = documentToMyPet(owner, document);
                if (storedMyPet != null) {
                    pets.add(storedMyPet);
                }
            });
            return pets;
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
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            Document petDocument = petCollection.find(new Document("uuid", uuid.toString())).first();
            if (petDocument != null) {
                MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(UUID.fromString(petDocument.getString("owner_uuid")));
                result = documentToMyPet(owner, petDocument);
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> removeMyPet(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
            return petCollection.deleteOne(new Document("uuid", uuid.toString())).getDeletedCount() > 0;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> removeMyPet(final StoredMyPet storedMyPet) {
        return removeMyPet(storedMyPet.getUUID());
    }

    @Override
    public CompletableFuture<Boolean> addMyPet(final StoredMyPet storedMyPet) {
        return CompletableFuture.supplyAsync(() -> {
            insertMyPet(storedMyPet);
            return true;
        }, executor);
    }

    private void insertMyPet(StoredMyPet storedMyPet) {
        MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");

        Document petDocument = new Document();
        petDocument.append("uuid", storedMyPet.getUUID().toString());
        petDocument.append("owner_uuid", storedMyPet.getOwner().getUniqueId().toString());
        petDocument.append("exp", storedMyPet.getExp());
        petDocument.append("health", storedMyPet.getHealth());
        petDocument.append("respawn_time", storedMyPet.getRespawnTime());
        petDocument.append("name", storedMyPet.getPetName());
        petDocument.append("type", storedMyPet.getPetType().name());
        petDocument.append("last_used", storedMyPet.getLastUsed());
        petDocument.append("hunger", storedMyPet.getSaturation());
        petDocument.append("world_group", storedMyPet.getWorldGroup());
        petDocument.append("wants_to_spawn", storedMyPet.wantsToRespawn());
        petDocument.append("skilltree", storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);

        try {
            petDocument.append("skills", NbtUtil.writeCompressed(storedMyPet.getSkillInfo()));
            petDocument.append("info", NbtUtil.writeCompressed(storedMyPet.getInfo()));
        } catch (IOException e) {
            ErrorUtil.reportError("MongoDB database operation failed", e);
        }

        petCollection.insertOne(petDocument);
    }

    @Override
    public CompletableFuture<Boolean> updateMyPet(final StoredMyPet storedMyPet) {
        petsToBeSaved.put(storedMyPet.getUUID(), storedMyPet);
        return CompletableFuture.supplyAsync(() -> {
            boolean result = savePet(storedMyPet);
            if (result) {
                petsToBeSaved.remove(storedMyPet.getUUID());
            }
            return result;
        }, executor);
    }

    public boolean savePet(StoredMyPet storedMyPet) {
        MongoCollection<Document> petCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "pets");
        Document filter = new Document("uuid", storedMyPet.getUUID().toString());
        Document petDocument = petCollection.find(filter).first();

        if (petDocument == null) {
            return false;
        }

        petDocument.append("uuid", storedMyPet.getUUID().toString());
        petDocument.append("owner_uuid", storedMyPet.getOwner().getUniqueId().toString());
        petDocument.append("exp", storedMyPet.getExp());
        petDocument.append("health", storedMyPet.getHealth());
        petDocument.append("respawn_time", storedMyPet.getRespawnTime());
        petDocument.append("name", storedMyPet.getPetName());
        petDocument.append("type", storedMyPet.getPetType().name());
        petDocument.append("last_used", storedMyPet.getLastUsed());
        petDocument.append("hunger", storedMyPet.getSaturation());
        petDocument.append("world_group", storedMyPet.getWorldGroup());
        petDocument.append("wants_to_spawn", storedMyPet.wantsToRespawn());
        petDocument.append("skilltree", storedMyPet.getSkilltree() != null ? storedMyPet.getSkilltree().getName() : null);

        try {
            petDocument.append("skills", NbtUtil.writeCompressed(storedMyPet.getSkillInfo()));
            petDocument.append("info", NbtUtil.writeCompressed(storedMyPet.getInfo()));
        } catch (IOException e) {
            ErrorUtil.reportError("MongoDB database operation failed", e);
        }

        petCollection.replaceOne(filter, petDocument);

        return true;
    }

    // Players ---------------------------------------------------------------------------------------------------------

    private MyPetPlayer documentToPlayer(Document document) {
        try {
            // raw "get" fixes wrong data type
            UUID mojangUUID = document.get("uuid") != null ? UUID.fromString("" + document.get("uuid")) : null;
            if (mojangUUID == null) {
                MyPetApi.getLogger().warning("Player document with no uuid found. Skipping.");
                return null;
            }

            MyPetPlayerImpl petPlayer = new MyPetPlayerImpl(mojangUUID);

            try {
                petPlayer.setExtendedInfo(NbtUtil.readCompressed(((Binary) document.get("extended_info")).getData()));
            } catch (IOException e) {
                MyPetApi.getLogger().warning("Extended info of player (" + mojangUUID + ") could not be loaded!");
            }

            Document jsonObject = (Document) document.get("multi_world");
            for (Object o : jsonObject.keySet()) {
                String petUUID = jsonObject.get(o.toString()).toString();
                petPlayer.setMyPetForWorldGroup(o.toString(), UUID.fromString(petUUID));
            }

            if (document.containsKey("settings")) {
                document = (Document) document.get("settings");
            }

            petPlayer.setAutoRespawnEnabled(document.getBoolean("auto_respawn"));
            petPlayer.setAutoRespawnMin(document.getInteger("auto_respawn_min"));
            petPlayer.setCaptureHelperActive(document.getBoolean("capture_mode"));
            petPlayer.setHealthBarActive(document.getBoolean("health_bar"));
            petPlayer.setPetLivingSoundVolume(document.getDouble("pet_idle_volume").floatValue());

            return petPlayer;
        } catch (Exception e) {
            ErrorUtil.reportError("MongoDB database operation failed", e);
        }

        return null;
    }

    @Override
    public List<MyPetPlayer> getAllMyPetPlayers() {
        MongoCollection<Document> playerCollection = this.db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");

        final List<MyPetPlayer> playerList = new ArrayList<>();
        playerCollection.find().forEach((Block<Document>) document -> {
            MyPetPlayer player = documentToPlayer(document);
            if (player != null) {
                playerList.add(player);
            }
        });
        return playerList;
    }

    @Override
    public CompletableFuture<Boolean> isMyPetPlayer(final Player player) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
            return playerCollection.count(new BasicDBObject("uuid", player.getUniqueId().toString())) > 0;
        }, executor);
    }

    @Override
    public CompletableFuture<MyPetPlayer> getMyPetPlayer(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
            Document playerDocument = playerCollection.find(new Document("uuid", uuid.toString())).first();
            if (playerDocument != null) {
                return documentToPlayer(playerDocument);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<MyPetPlayer> getMyPetPlayer(final Player player) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
            Document playerDocument = playerCollection.find(new BasicDBObject("uuid", player.getUniqueId().toString())).first();
            if (playerDocument != null) {
                return documentToPlayer(playerDocument);
            }
            return null;
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

    public boolean updatePlayer(final MyPetPlayer player) {
        MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        Document filter = new Document("uuid", player.getUniqueId().toString());
        Document playerDocument = playerCollection.find(filter).first();
        if (playerDocument != null) {
            setPlayerData(player, playerDocument);
            return playerCollection.replaceOne(filter, playerDocument).getModifiedCount() > 0;
        }
        return false;
    }

    private void setPlayerData(MyPetPlayer player, Document playerDocument) {
        playerDocument.append("uuid", player.getUniqueId().toString());
        playerDocument.append("last_update", System.currentTimeMillis());

        Document settingsDocument = new Document();
        settingsDocument.append("auto_respawn", player.hasAutoRespawnEnabled());
        settingsDocument.append("auto_respawn_min", player.getAutoRespawnMin());
        settingsDocument.append("capture_mode", player.isCaptureHelperActive());
        settingsDocument.append("health_bar", player.isHealthBarActive());
        settingsDocument.append("pet_idle_volume", player.getPetLivingSoundVolume());

        playerDocument.append("settings", settingsDocument);

        try {
            playerDocument.append("extended_info", NbtUtil.writeCompressed(player.getExtendedInfo()));
        } catch (IOException e) {
            ErrorUtil.reportError("MongoDB database operation failed", e);
        }

        Document multiWorldDocument = new Document();
        for (String worldGroupName : player.getMyPetsForWorldGroups().keySet()) {
            multiWorldDocument.append(worldGroupName, player.getMyPetsForWorldGroups().get(worldGroupName).toString());
        }

        playerDocument.append("multi_world", multiWorldDocument);
    }


    @Override
    public CompletableFuture<Boolean> addMyPetPlayer(final MyPetPlayer player) {
        return CompletableFuture.supplyAsync(() -> insertMyPetPlayer(player), executor);
    }

    private boolean insertMyPetPlayer(MyPetPlayer player) {
        Document playerDocument = new Document();
        setPlayerData(player, playerDocument);

        MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
        playerCollection.insertOne(playerDocument);
        return true;
    }

    @Override
    public CompletableFuture<Boolean> removeMyPetPlayer(final MyPetPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> playerCollection = db.getCollection(Configuration.Repository.MongoDB.PREFIX + "players");
            return playerCollection.deleteOne(new Document("uuid", player.getUniqueId().toString())).getDeletedCount() > 0;
        }, executor);
    }
}
