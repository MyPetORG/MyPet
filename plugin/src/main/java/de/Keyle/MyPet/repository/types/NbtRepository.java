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
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class NbtRepository implements Repository {
    protected Map<UUID, CompoundBinaryTag> petTags = new HashMap<>();
    protected Map<UUID, CompoundBinaryTag> playerTags = new HashMap<>();
    protected Multimap<UUID, UUID> petPlayerMultiMap = HashMultimap.create();

    public static MyPetPlayer createMyPetPlayer(CompoundBinaryTag playerTag) {
        MyPetPlayerImpl petPlayer = null;
        UUID mojangUUID = null;
        UUID internalUUID = null;
        String playerName = null;
        if (playerTag.keySet().contains("UUID")) {
            CompoundBinaryTag uuidTag = playerTag.getCompound("UUID");
            if (uuidTag.keySet().contains("Internal-UUID")) {
                internalUUID = UUID.fromString(uuidTag.getString("Internal-UUID"));
            }
            if (uuidTag.keySet().contains("Mojang-UUID")) {
                mojangUUID = UUID.fromString(uuidTag.getString("Mojang-UUID"));
            }
            String name = uuidTag.getString("Name");
            if (!name.isEmpty()) {
                playerName = name;
            }
        }
        String tagName = playerTag.getString("Name");
        if (!tagName.isEmpty()) {
            playerName = tagName;
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

        CompoundBinaryTag root = nbtConfiguration.getNBTCompound();
        if (root.keySet().contains("Players")) {
            int playerCount = loadPlayers(root.getList("Players"));
            MyPetApi.getLogger().info("[NBT] " + ChatColor.YELLOW + playerCount + ChatColor.RESET + " PetPlayer(s) loaded");
        }

        int petCount = loadPets(root.getList("Pets"));
        MyPetApi.getLogger().info("[NBT] " + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) loaded");
    }

    @Override
    public void countMyPets(final RepositoryCallback<Integer> callback) {
        callback.run(petTags.size());
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    @Override
    public void countMyPets(MyPetType type, final RepositoryCallback<Integer> callback) {
        int counter = 0;
        for (CompoundBinaryTag petTag : petTags.values()) {
            if (petTag.getString("Type").equals(type.name())) {
                counter++;
            }
        }
        callback.run(counter);
    }

    @Override
    public List<StoredMyPet> getAllMyPets() {
        List<MyPetPlayer> playerList = getAllMyPetPlayers();
        Map<UUID, MyPetPlayer> owners = new HashMap<>();

        for (MyPetPlayer player : playerList) {
            owners.put(player.getInternalUUID(), player);
        }

        List<StoredMyPet> pets = new ArrayList<>();
        for (UUID petUUID : petTags.keySet()) {
            CompoundBinaryTag petTag = petTags.get(petUUID);
            String ownerUUIDStr = petTag.getString("Internal-Owner-UUID");
            if (!ownerUUIDStr.isEmpty()) {
                UUID ownerUUID = UUID.fromString(ownerUUIDStr);

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
        return petPlayerMultiMap.containsKey(playerUUID) && !petPlayerMultiMap.get(playerUUID).isEmpty();
    }

    @Override
    public void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<StoredMyPet>> callback) {
        if (callback != null) {
            List<StoredMyPet> petList = new ArrayList<>();

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
    public void getMyPet(final UUID uuid, final RepositoryCallback<StoredMyPet> callback) {
        if (callback != null) {
            if (petTags.containsKey(uuid)) {
                CompoundBinaryTag petTag = petTags.get(uuid);
                UUID ownerUUID;
                String ownerUUIDStr = petTag.getString("Internal-Owner-UUID");
                if (!ownerUUIDStr.isEmpty()) {
                    ownerUUID = UUID.fromString(ownerUUIDStr);
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

    @Override
    public boolean savePet(StoredMyPet storedMyPet) {
        return false;
    }

    private int loadPets(ListBinaryTag petList) {
        int petCount = 0;
        boolean oldPets = false;
        for (int i = 0; i < petList.size(); i++) {
            CompoundBinaryTag petTag = petList.getCompound(i);
            UUID ownerUUID;

            String ownerUUIDStr = petTag.getString("Internal-Owner-UUID");
            if (!ownerUUIDStr.isEmpty()) {
                ownerUUID = UUID.fromString(ownerUUIDStr);
            } else {
                oldPets = true;
                continue;
            }
            if (!playerTags.containsKey(ownerUUID)) {
                MyPetApi.getLogger().warning("Owner for a pet (" + petTag.getString("Name") + " not found, pet loading skipped.");
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

    // Players ---------------------------------------------------------------------------------------------------------

    public UUID getPetUUID(CompoundBinaryTag petTag) {
        return UUID.fromString(petTag.getString("UUID"));
    }

    @Override
    public List<MyPetPlayer> getAllMyPetPlayers() {
        List<MyPetPlayer> playerList = new ArrayList<>();

        for (CompoundBinaryTag playerTag : playerTags.values()) {
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
            for (CompoundBinaryTag playerTag : playerTags.values()) {
                if (playerTag.keySet().contains("UUID")) {
                    CompoundBinaryTag uuidTag = playerTag.getCompound("UUID");

                    String mojangUUID = uuidTag.getString("Mojang-UUID");
                    if (!mojangUUID.isEmpty()) {
                        if (UUID.fromString(mojangUUID).equals(player.getUniqueId())) {
                            callback.run(true);
                            return;
                        }
                    }
                    String name = uuidTag.getString("Name");
                    if (!name.isEmpty()) {
                        if (Util.getOfflinePlayerUUID(name).equals(player.getUniqueId())) {
                            callback.run(true);
                            return;
                        }
                    }
                    String tagName = playerTag.getString("Name");
                    if (!tagName.isEmpty()) {
                        if (Util.getOfflinePlayerUUID(tagName).equals(player.getUniqueId())) {
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
            for (CompoundBinaryTag playerTag : playerTags.values()) {
                if (playerTag.keySet().contains("UUID")) {
                    CompoundBinaryTag uuidTag = playerTag.getCompound("UUID");

                    String mojangUUID = uuidTag.getString("Mojang-UUID");
                    if (!mojangUUID.isEmpty()) {
                        if (UUID.fromString(mojangUUID).equals(player.getUniqueId())) {
                            MyPetPlayer myPetPlayer = createMyPetPlayer(playerTag);
                            callback.run(myPetPlayer);
                            return;
                        }
                    }
                    String name = uuidTag.getString("Name");
                    if (!name.isEmpty()) {
                        if (name.equals(player.getName())) {
                            MyPetPlayer myPetPlayer = createMyPetPlayer(playerTag);
                            callback.run(myPetPlayer);
                            return;
                        }
                    }
                    String tagName = playerTag.getString("Name");
                    if (!tagName.isEmpty()) {
                        if (tagName.equals(player.getName())) {
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

    private int loadPlayers(ListBinaryTag playerList) {
        int playerCount = 0;

        for (int i = 0; i < playerList.size(); i++) {
            CompoundBinaryTag playerTag = playerList.getCompound(i);
            UUID internalUUID = getInternalUUID(playerTag);
            if (internalUUID != null) {
                playerTags.put(internalUUID, playerTag);
                playerCount++;
            }
        }
        return playerCount;
    }

    private UUID getInternalUUID(CompoundBinaryTag playerTag) {
        if (playerTag.keySet().contains("UUID")) {
            CompoundBinaryTag uuidTag = playerTag.getCompound("UUID");
            String internalUUID = uuidTag.getString("Internal-UUID");
            if (!internalUUID.isEmpty()) {
                return UUID.fromString(internalUUID);
            }
        }
        return null;
    }
}