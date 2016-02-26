/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.UUIDFetcher;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.configuration.ConfigurationNBT;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.Backup;
import de.Keyle.MyPet.util.player.OfflineMyPetPlayer;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import de.keyle.knbt.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class NbtRepository implements Repository, Scheduler {
    protected Map<UUID, TagCompound> petTags = new HashMap<>();
    protected Map<UUID, TagCompound> playerTags = new HashMap<>();
    protected Multimap<UUID, UUID> petPlayerMultiMap = HashMultimap.create();

    private File NBTPetFile;
    private int autoSaveTimer = 0;
    private Backup backupManager;

    @Override
    public void disable() {
        cleanPlayers();
        saveData(false);
        petTags.clear();
        playerTags.clear();
        petPlayerMultiMap.clear();
    }

    @Override
    public void save() {
        saveData(true);
    }

    @Override
    public void init() {
        NBTPetFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");

        if (Configuration.Repository.NBT.MAKE_BACKUPS) {
            new File(MyPetApi.getPlugin().getDataFolder().getAbsolutePath() + File.separator + "backups" + File.separator).mkdirs();
            backupManager = new Backup(NBTPetFile, new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "backups" + File.separator));
        }

        loadData(NBTPetFile);
    }

    public void saveData(boolean async) {
        autoSaveTimer = Configuration.Repository.NBT.AUTOSAVE_TIME;

        TagCompound fileTag = new TagCompound();

        fileTag.getCompoundData().put("Version", new TagString(MyPetVersion.getVersion()));
        fileTag.getCompoundData().put("Build", new TagInt(Integer.parseInt(MyPetVersion.getBuild())));
        fileTag.getCompoundData().put("OnlineMode", new TagByte(MyPetApi.getPlugin().isInOnlineMode()));
        fileTag.getCompoundData().put("Pets", savePets());
        fileTag.getCompoundData().put("Players", savePlayers());

        if (async) {
            try {
                final byte[] data = TagStream.writeTag(fileTag, false);
                Bukkit.getScheduler().runTaskAsynchronously(MyPetApi.getPlugin(), new Runnable() {
                    public void run() {
                        try {
                            OutputStream os = new GZIPOutputStream(new FileOutputStream(NBTPetFile));
                            os.write(data);
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ConfigurationNBT.save(NBTPetFile, fileTag);
        }
    }

    private void loadData(File f) {
        ConfigurationNBT nbtConfiguration = new ConfigurationNBT(f);
        if (!nbtConfiguration.load()) {
            return;
        }

        if (nbtConfiguration.getNBTCompound().containsKeyAs("Players", TagList.class)) {
            int playerCount = loadPlayers(nbtConfiguration.getNBTCompound().getAs("Players", TagList.class));
            MyPetApi.getLogger().info("[NBT] " + ChatColor.YELLOW + playerCount + ChatColor.RESET + " PetPlayer(s) loaded");
        }

        int petCount = loadPets(nbtConfiguration.getNBTCompound().getAs("Pets", TagList.class));
        MyPetApi.getLogger().info("[NBT] " + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) loaded");
    }

    public Backup getBackupManager() {
        return backupManager;
    }

    @Override
    public void schedule() {
        if (Configuration.Repository.NBT.AUTOSAVE_TIME > 0 && autoSaveTimer-- <= 0) {
            saveData(true);
            autoSaveTimer = Configuration.Repository.NBT.AUTOSAVE_TIME;
        }
    }

    @Override
    public void cleanup(long timestamp, final RepositoryCallback<Integer> callback) {
        List<TagCompound> deletionList = new ArrayList<>();
        for (TagCompound petTag : petTags.values()) {
            if (petTag.containsKey("LastUsed")) {
                if (petTag.getAs("LastUsed", TagLong.class).getLongData() < timestamp) {
                    deletionList.add(petTag);
                }
            } else {
                deletionList.add(petTag);
            }
        }

        int deletedPetCount = 0;
        if (deletionList.size() > 0) {
            if (Configuration.Repository.NBT.MAKE_BACKUPS) {
                backupManager.createAsyncBackup();
            }

            TagLoop:
            for (TagCompound petTag : deletionList) {
                UUID petUUID = getPetUUID(petTag);

                for (ActiveMyPet pet : MyPetApi.getMyPetList().getAllActiveMyPets()) {
                    if (pet.getUUID().equals(petUUID)) {
                        continue TagLoop;
                    }
                }

                if (petTag.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                    UUID ownerUUID = UUID.fromString(petTag.getAs("Internal-Owner-UUID", TagString.class).getStringData());

                    petPlayerMultiMap.remove(ownerUUID, petUUID);
                }
                petTags.remove(petUUID);
                deletedPetCount++;
            }

            cleanPlayers();

            if (deletedPetCount > 0) {
                save();
            }
        }
        if (callback != null) {
            callback.run(deletedPetCount);
        }
    }

    @Override
    public void countMyPets(final RepositoryCallback<Integer> callback) {
        callback.run(petTags.size());
    }

    @Override
    public void countMyPets(MyPetType type, final RepositoryCallback<Integer> callback) {
        int counter = 0;
        for (TagCompound petTag : petTags.values()) {
            if (petTag.getAs("Type", TagString.class).equals(type.name())) {
                counter++;
            }
        }
        callback.run(counter);
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    @Override
    public List<MyPet> getAllMyPets() {
        List<MyPetPlayer> playerList = getAllMyPetPlayers();
        Map<UUID, MyPetPlayer> owners = new HashMap<>();

        for (MyPetPlayer player : playerList) {
            owners.put(player.getInternalUUID(), player);
        }

        List<MyPet> pets = new ArrayList<>();
        for (UUID petUUID : petTags.keySet()) {
            TagCompound petTag = petTags.get(petUUID);
            if (petTag.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                UUID ownerUUID = UUID.fromString(petTag.getAs("Internal-Owner-UUID", TagString.class).getStringData());

                if (owners.containsKey(ownerUUID)) {
                    InactiveMyPet myPet = new InactiveMyPet(owners.get(ownerUUID));
                    myPet.load(petTag);
                    pets.add(myPet);
                }
            }
        }

        return pets;
    }

    @Override
    public void hasMyPets(final MyPetPlayer myPetPlayer, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            callback.run(hasMyPets(myPetPlayer));
        }
    }

    public boolean hasMyPets(MyPetPlayer myPetPlayer) {
        return hasMyPets(myPetPlayer.getInternalUUID());
    }

    public boolean hasMyPets(UUID playerUUID) {
        return petPlayerMultiMap.containsKey(playerUUID) && petPlayerMultiMap.get(playerUUID).size() > 0;
    }

    @Override
    public void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<MyPet>> callback) {
        if (callback != null) {
            List<MyPet> petList = new ArrayList<>();

            for (UUID petUUID : petPlayerMultiMap.get(owner.getInternalUUID())) {
                if (petTags.containsKey(petUUID)) {
                    InactiveMyPet myPet = new InactiveMyPet(owner);
                    myPet.load(petTags.get(petUUID));
                    petList.add(myPet);
                }
            }
            callback.run(petList);
        }
    }

    @Override
    public void getMyPet(final UUID uuid, final RepositoryCallback<MyPet> callback) {
        if (callback != null) {
            if (petTags.containsKey(uuid)) {
                TagCompound petTag = petTags.get(uuid);
                UUID ownerUUID;
                if (petTag.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                    ownerUUID = UUID.fromString(petTag.getAs("Internal-Owner-UUID", TagString.class).getStringData());
                } else {
                    return;
                }
                if (!playerTags.containsKey(ownerUUID)) {
                    return;
                }
                MyPetPlayer owner = MyPetApi.getPlayerList().getMyPetPlayer(ownerUUID);
                if (owner != null) {
                    InactiveMyPet myPet = new InactiveMyPet(owner);
                    myPet.load(petTag);
                    callback.run(myPet);
                }
            }
        }
    }

    @Override
    public void removeMyPet(final UUID uuid, final RepositoryCallback<Boolean> callback) {
        TagCompound petTag = petTags.remove(uuid);
        if (petTag != null) {
            if (petTag.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                UUID ownerUUID = UUID.fromString(petTag.getAs("Internal-Owner-UUID", TagString.class).getStringData());

                petPlayerMultiMap.remove(ownerUUID, uuid);
            }

            if (Configuration.Repository.NBT.SAVE_ON_PET_REMOVE) {
                saveData(true);
            }
        }
        if (callback != null) {
            callback.run(petTag != null);
        }
    }

    @Override
    public void removeMyPet(final MyPet inactiveMyPet, final RepositoryCallback<Boolean> callback) {
        UUID ownerUUID = inactiveMyPet.getOwner().getInternalUUID();
        if (petPlayerMultiMap.containsKey(ownerUUID)) {
            petPlayerMultiMap.remove(ownerUUID, inactiveMyPet.getUUID());
        }
        removeMyPet(inactiveMyPet.getUUID(), callback);
    }

    @Override
    public void addMyPet(final MyPet inactiveMyPet, final RepositoryCallback<Boolean> callback) {
        if (!petTags.containsKey(inactiveMyPet.getUUID())) {
            petTags.put(inactiveMyPet.getUUID(), savePet(inactiveMyPet));
            petPlayerMultiMap.put(inactiveMyPet.getOwner().getInternalUUID(), inactiveMyPet.getUUID());
            if (Configuration.Repository.NBT.SAVE_ON_PET_ADD) {
                saveData(true);
            }
            if (callback != null) {
                callback.run(true);
            }
            return;
        }
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void updateMyPet(final MyPet myPet, final RepositoryCallback<Boolean> callback) {
        if (petTags.containsKey(myPet.getUUID())) {
            petTags.put(myPet.getUUID(), savePet(myPet));

            if (Configuration.Repository.NBT.SAVE_ON_PET_UPDATE) {
                saveData(true);
            }

            if (callback != null) {
                callback.run(true);
            }
            return;
        }
        if (callback != null) {
            callback.run(false);
        }
    }

    private int loadPets(TagList petList) {
        int petCount = 0;
        boolean oldPets = false;
        for (int i = 0; i < petList.getReadOnlyList().size(); i++) {
            TagCompound petTag = petList.getTagAs(i, TagCompound.class);
            UUID ownerUUID;

            if (petTag.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                ownerUUID = UUID.fromString(petTag.getAs("Internal-Owner-UUID", TagString.class).getStringData());
            } else {
                oldPets = true;
                continue;
            }
            if (!playerTags.containsKey(ownerUUID)) {
                MyPetApi.getLogger().warning("Owner for a pet (" + petTag.getAs("Name", TagString.class) + " not found, pet loading skipped.");
                continue;
            }

            UUID petUUID = getPetUUID(petTag);
            petTags.put(petUUID, petTag);
            petPlayerMultiMap.put(ownerUUID, petUUID);

            petCount++;
        }
        if (oldPets) {
            MyPetApi.getLogger().warning("Old MyPets can not be loaded! Please use a previous version to upgrade first.");
        }
        return petCount;
    }

    private TagList savePets() {
        for (MyPet myPet : MyPetApi.getMyPetList().getAllActiveMyPets()) {
            petTags.put(myPet.getUUID(), savePet(myPet));
        }
        return new TagList(Lists.newArrayList(petTags.values()));
    }

    public TagCompound savePet(MyPet myPet) {
        TagCompound petNBT = new TagCompound();

        petNBT.getCompoundData().put("UUID", new TagString(myPet.getUUID().toString()));
        petNBT.getCompoundData().put("Type", new TagString(myPet.getPetType().name()));
        petNBT.getCompoundData().put("Health", new TagDouble(myPet.getHealth()));
        petNBT.getCompoundData().put("Respawntime", new TagInt(myPet.getRespawnTime()));
        petNBT.getCompoundData().put("Hunger", new TagDouble(myPet.getHungerValue()));
        petNBT.getCompoundData().put("Name", new TagString(myPet.getPetName()));
        petNBT.getCompoundData().put("WorldGroup", new TagString(myPet.getWorldGroup()));
        petNBT.getCompoundData().put("Exp", new TagDouble(myPet.getExp()));
        petNBT.getCompoundData().put("LastUsed", new TagLong(myPet.getLastUsed()));
        petNBT.getCompoundData().put("Info", myPet.getInfo());
        petNBT.getCompoundData().put("Internal-Owner-UUID", new TagString(myPet.getOwner().getInternalUUID().toString()));
        petNBT.getCompoundData().put("Wants-To-Respawn", new TagByte(myPet.wantsToRespawn()));
        if (myPet.getSkilltree() != null) {
            petNBT.getCompoundData().put("Skilltree", new TagString(myPet.getSkilltree().getName()));
        }
        petNBT.getCompoundData().put("Skills", myPet.getSkillInfo());

        return petNBT;
    }

    public UUID getPetUUID(TagCompound petTag) {
        return UUID.fromString(petTag.getAs("UUID", TagString.class).getStringData());
    }

    // Players ---------------------------------------------------------------------------------------------------------

    @Override
    public List<MyPetPlayer> getAllMyPetPlayers() {
        List<MyPetPlayer> playerList = new ArrayList<>();

        for (TagCompound playerTag : playerTags.values()) {
            MyPetPlayer player = createMyPetPlayer(playerTag);
            if (player != null) {
                playerList.add(player);
            }
        }

        return playerList;
    }

    @Override
    public void isMyPetPlayer(final Player player, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            for (TagCompound playerTag : playerTags.values()) {
                UUID playerUUID = getPlayerUUID(playerTag);
                if (playerUUID != null) {
                    if (playerUUID.equals(player.getUniqueId())) {
                        callback.run(true);
                        return;
                    }
                }
            }
            callback.run(false);
        }
    }

    public void getMyPetPlayer(final UUID uuid, final RepositoryCallback<MyPetPlayer> callback) {
        if (playerTags.containsKey(uuid)) {
            if (callback != null) {
                MyPetPlayer myPetPlayer = createMyPetPlayer(playerTags.get(uuid));
                callback.run(myPetPlayer);
            }
        }
    }

    @Override
    public void getMyPetPlayer(final Player player, final RepositoryCallback<MyPetPlayer> callback) {
        if (callback != null) {
            for (TagCompound playerTag : playerTags.values()) {
                UUID playerUUID = getPlayerUUID(playerTag);
                if (playerUUID != null) {
                    if (playerUUID.equals(player.getUniqueId())) {
                        MyPetPlayer myPetPlayer = createMyPetPlayer(playerTag);
                        callback.run(myPetPlayer);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void updateMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        if (playerTags.containsKey(player.getInternalUUID())) {
            playerTags.put(player.getInternalUUID(), player.save());

            if (Configuration.Repository.NBT.SAVE_ON_PLAYER_UPDATE) {
                saveData(true);
            }

            if (callback != null) {
                callback.run(true);
            }
            return;
        }
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void addMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        if (!playerTags.containsKey(player.getInternalUUID())) {
            playerTags.put(player.getInternalUUID(), player.save());

            if (Configuration.Repository.NBT.SAVE_ON_PLAYER_ADD) {
                saveData(true);
            }
            if (callback != null) {
                callback.run(true);
            }
            return;
        }
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void removeMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        boolean result = playerTags.remove(player.getInternalUUID()) != null;

        // remove all remaining pets
        for (UUID petUUID : petPlayerMultiMap.get(player.getInternalUUID())) {
            petTags.remove(petUUID);
        }
        petPlayerMultiMap.removeAll(player.getInternalUUID());

        if (result && Configuration.Repository.NBT.SAVE_ON_PLAYER_REMOVE) {
            saveData(true);
        }

        if (callback != null) {
            callback.run(result);
        }
    }

    private TagList savePlayers() {
        for (MyPetPlayer player : MyPetApi.getPlayerList().getMyPetPlayers()) {
            playerTags.put(player.getInternalUUID(), player.save());
        }
        return new TagList(Lists.newArrayList(playerTags.values()));
    }

    private int loadPlayers(TagList playerList) {
        int playerCount = 0;
        if (MyPetApi.getPlugin().isInOnlineMode()) {
            List<String> unknownPlayers = new ArrayList<>();
            for (int i = 0; i < playerList.getReadOnlyList().size(); i++) {
                TagCompound playerTag = playerList.getTagAs(i, TagCompound.class);
                if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                    TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
                    if (!uuidTag.containsKeyAs("Mojang-UUID", TagString.class)) {
                        if (playerTag.containsKeyAs("Name", TagString.class)) {
                            String playerName = playerTag.getAs("Name", TagString.class).getStringData();
                            unknownPlayers.add(playerName);
                        }
                    }
                }
            }
            UUIDFetcher.call(unknownPlayers);
        }

        for (int i = 0; i < playerList.getReadOnlyList().size(); i++) {
            TagCompound playerTag = playerList.getTagAs(i, TagCompound.class);
            UUID internalUUID = getInternalUUID(playerTag);
            if (internalUUID != null) {
                playerTags.put(internalUUID, playerTag);
                playerCount++;
            }
        }
        return playerCount;
    }

    private UUID getInternalUUID(TagCompound playerTag) {
        if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
            TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
            if (uuidTag.getCompoundData().containsKey("Internal-UUID")) {
                return UUID.fromString(uuidTag.getAs("Internal-UUID", TagString.class).getStringData());
            }
        }
        return null;
    }

    private UUID getPlayerUUID(TagCompound playerTag) {
        if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
            TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);

            if (MyPetApi.getPlugin().isInOnlineMode()) {
                if (uuidTag.getCompoundData().containsKey("Mojang-UUID")) {
                    return UUID.fromString(uuidTag.getAs("Mojang-UUID", TagString.class).getStringData());
                }
            } else {
                if (uuidTag.getCompoundData().containsKey("Offline-UUID")) {
                    return UUID.fromString(uuidTag.getAs("Offline-UUID", TagString.class).getStringData());
                }
            }
        }
        return null;
    }

    private void cleanPlayers() {
        Iterator<UUID> iterator = playerTags.keySet().iterator();
        while (iterator.hasNext()) {
            UUID playerUUID = iterator.next();
            if (hasMyPets(playerUUID)) {
                continue;
            }

            TagCompound playerTag = playerTags.get(playerUUID);
            MyPetPlayer player = new de.Keyle.MyPet.util.player.MyPetPlayer() {
            };
            player.load(playerTag);

            if (!player.hasCustomData()) {
                iterator.remove();
            }
        }
    }

    public static MyPetPlayer createMyPetPlayer(TagCompound playerTag) {
        MyPetPlayer petPlayer = null;
        if (MyPetApi.getPlugin().isInOnlineMode()) {
            UUID mojangUUID = null;
            UUID internalUUID = null;
            if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
                if (uuidTag.getCompoundData().containsKey("Internal-UUID")) {
                    internalUUID = UUID.fromString(uuidTag.getAs("Internal-UUID", TagString.class).getStringData());
                }
                if (uuidTag.getCompoundData().containsKey("Mojang-UUID")) {
                    mojangUUID = UUID.fromString(uuidTag.getAs("Mojang-UUID", TagString.class).getStringData());
                }
            } else if (playerTag.getCompoundData().containsKey("Mojang-UUID")) {
                mojangUUID = UUID.fromString(playerTag.getAs("Mojang-UUID", TagString.class).getStringData());
            }
            if (internalUUID == null) {
                internalUUID = UUID.randomUUID();
            }
            if (mojangUUID != null) {
                petPlayer = new OnlineMyPetPlayer(internalUUID, mojangUUID);
                if (playerTag.containsKeyAs("Name", TagString.class)) {
                    String playerName = playerTag.getAs("Name", TagString.class).getStringData();
                    ((OnlineMyPetPlayer) petPlayer).setLastKnownName(playerName);
                }
            } else if (playerTag.containsKeyAs("Name", TagString.class)) {
                String playerName = playerTag.getAs("Name", TagString.class).getStringData();
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
            UUID internalUUID = null;
            String playerName = null;
            if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
                if (uuidTag.getCompoundData().containsKey("Internal-UUID")) {
                    internalUUID = UUID.fromString(uuidTag.getAs("Internal-UUID", TagString.class).getStringData());
                }
            }
            if (playerTag.containsKeyAs("Name", TagString.class)) {
                playerName = playerTag.getAs("Name", TagString.class).getStringData();
            }
            if (playerName == null) {
                return null;
            }
            if (internalUUID == null) {
                internalUUID = UUID.randomUUID();
            }
            petPlayer = new OfflineMyPetPlayer(internalUUID, playerName);
        }
        if (petPlayer != null) {
            petPlayer.load(playerTag);
        }
        return petPlayer;
    }
}