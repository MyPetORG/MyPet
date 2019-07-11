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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.configuration.ConfigurationNBT;
import de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class NbtRepository implements Repository {
    protected Map<UUID, TagCompound> petTags = new HashMap<>();
    protected Map<UUID, TagCompound> playerTags = new HashMap<>();
    protected Multimap<UUID, UUID> petPlayerMultiMap = HashMultimap.create();

    @Override
    public void disable() {
        petTags.clear();
        playerTags.clear();
        petPlayerMultiMap.clear();
    }

    @Override
    public void save() {
    }

    @Override
    public void cleanup(long timestamp, final RepositoryCallback<Integer> callback) {
    }

    @Override
    public void init() {
        File NBTPetFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");

        loadData(NBTPetFile);
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
    public List<StoredMyPet> getAllMyPets() {
        List<MyPetPlayer> playerList = getAllMyPetPlayers();
        Map<UUID, MyPetPlayer> owners = new HashMap<>();

        for (MyPetPlayer player : playerList) {
            owners.put(player.getInternalUUID(), player);
        }

        List<StoredMyPet> pets = new ArrayList<>();
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
    public void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<StoredMyPet>> callback) {
        if (callback != null) {
            List<StoredMyPet> petList = new ArrayList<>();

            for (UUID petUUID : petPlayerMultiMap.get(owner.getInternalUUID())) {
                if (petTags.containsKey(petUUID)) {
                    InactiveMyPet myPet = new InactiveMyPet(owner);
                    myPet.load(petTags.get(petUUID));

                    List<RepositoryMyPetConverterService> converters = MyPetApi.getServiceManager().getServices(RepositoryMyPetConverterService.class);
                    for (RepositoryMyPetConverterService converter : converters) {
                        converter.convert(myPet);
                    }

                    petList.add(myPet);
                }
            }
            callback.run(petList);
        }
    }

    @Override
    public void getMyPet(final UUID uuid, final RepositoryCallback<StoredMyPet> callback) {
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
                MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(ownerUUID);
                if (owner != null) {
                    InactiveMyPet myPet = new InactiveMyPet(owner);
                    myPet.load(petTag);

                    List<RepositoryMyPetConverterService> converters = MyPetApi.getServiceManager().getServices(RepositoryMyPetConverterService.class);
                    for (RepositoryMyPetConverterService converter : converters) {
                        converter.convert(myPet);
                    }

                    callback.run(myPet);
                }
            }
        }
    }

    @Override
    public void removeMyPet(final UUID uuid, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void removeMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
        removeMyPet(storedMyPet.getUUID(), callback);
    }

    @Override
    public void addMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void updateMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback) {
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
                if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                    TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);

                    if (uuidTag.getCompoundData().containsKey("Mojang-UUID")) {
                        if (UUID.fromString(uuidTag.getAs("Mojang-UUID", TagString.class).getStringData()).equals(player.getUniqueId())) {
                            callback.run(true);
                            return;
                        }
                    }
                    if (uuidTag.getCompoundData().containsKey("Name")) {
                        if (Util.getOfflinePlayerUUID(uuidTag.getAs("Name", TagString.class).getStringData()).equals(player.getUniqueId())) {
                            callback.run(true);
                            return;
                        }
                    }
                    if (playerTag.getCompoundData().containsKey("Name")) {
                        if (Util.getOfflinePlayerUUID(playerTag.getAs("Name", TagString.class).getStringData()).equals(player.getUniqueId())) {
                            callback.run(true);
                            return;
                        }
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
                if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                    TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);

                    if (uuidTag.getCompoundData().containsKey("Mojang-UUID")) {
                        if (UUID.fromString(uuidTag.getAs("Mojang-UUID", TagString.class).getStringData()).equals(player.getUniqueId())) {
                            MyPetPlayer myPetPlayer = createMyPetPlayer(playerTag);
                            callback.run(myPetPlayer);
                            return;
                        }
                    }
                    if (uuidTag.getCompoundData().containsKey("Name")) {
                        if (uuidTag.getAs("Name", TagString.class).getStringData().equals(player.getName())) {
                            MyPetPlayer myPetPlayer = createMyPetPlayer(playerTag);
                            callback.run(myPetPlayer);
                            return;
                        }
                    }
                    if (playerTag.getCompoundData().containsKey("Name")) {
                        if (playerTag.getAs("Name", TagString.class).getStringData().equals(player.getName())) {
                            MyPetPlayer myPetPlayer = createMyPetPlayer(playerTag);
                            callback.run(myPetPlayer);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void updateMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void addMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            callback.run(false);
        }
    }

    @Override
    public void removeMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback) {
        if (callback != null) {
            callback.run(false);
        }
    }

    private int loadPlayers(TagList playerList) {
        int playerCount = 0;

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

    public static MyPetPlayer createMyPetPlayer(TagCompound playerTag) {
        MyPetPlayerImpl petPlayer = null;
        UUID mojangUUID = null;
        UUID internalUUID = null;
        String playerName = null;
        if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
            TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
            if (uuidTag.getCompoundData().containsKey("Internal-UUID")) {
                internalUUID = UUID.fromString(uuidTag.getAs("Internal-UUID", TagString.class).getStringData());
            }
            if (uuidTag.getCompoundData().containsKey("Mojang-UUID")) {
                mojangUUID = UUID.fromString(uuidTag.getAs("Mojang-UUID", TagString.class).getStringData());
            }
            if (uuidTag.containsKeyAs("Name", TagString.class)) {
                playerName = uuidTag.getAs("Name", TagString.class).getStringData();
            }
        }
        if (playerTag.containsKeyAs("Name", TagString.class)) {
            playerName = playerTag.getAs("Name", TagString.class).getStringData();
        }
        if (internalUUID == null) {
            return null;
        }
        if (mojangUUID != null) {
            petPlayer = new MyPetPlayerImpl(internalUUID, mojangUUID);
            petPlayer.setLastKnownName(playerName);
        } else if (playerName != null) {
            petPlayer = new MyPetPlayerImpl(internalUUID, playerName);
            petPlayer.setLastKnownName(playerName);
        }
        if (petPlayer != null) {
            petPlayer.load(playerTag);
        }
        return petPlayer;
    }
}