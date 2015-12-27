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

package de.Keyle.MyPet.repository;

import com.google.common.collect.Sets;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.player.OfflineMyPetPlayer;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import de.Keyle.MyPet.util.player.UUIDFetcher;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerList {
    public final static Set<UUID> onlinePlayerUUIDList = Sets.newHashSet();
    protected final static Map<UUID, UUID> uuidToInternalUUID = new ConcurrentHashMap<>();
    protected final static Map<UUID, MyPetPlayer> onlinePlayers = new ConcurrentHashMap<>();

    public static UUID getInternalUUID(Player player) {
        return uuidToInternalUUID.get(player.getUniqueId());
    }

    public static UUID getInternalUUID(UUID playerUUID) {
        return uuidToInternalUUID.get(playerUUID);
    }

    public static MyPetPlayer getMyPetPlayer(UUID internalUUID) {
        if (internalUUID != null) {
            return onlinePlayers.get(internalUUID);
        }
        return null;
    }

    public static MyPetPlayer getMyPetPlayer(Player player) {
        UUID internalUUID = getInternalUUID(player);
        if (internalUUID == null) {
            return null;
        }
        return getMyPetPlayer(internalUUID);
    }

    public static MyPetPlayer getMyPetPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return getMyPetPlayer(player);
    }

    public static void setOnline(MyPetPlayer player) {
        onlinePlayers.put(player.getInternalUUID(), player);
        uuidToInternalUUID.put(player.getPlayerUUID(), player.getInternalUUID());
    }

    public static void setOffline(MyPetPlayer player) {
        onlinePlayers.remove(player.getInternalUUID());
        uuidToInternalUUID.remove(player.getPlayerUUID());
        MyPetPlugin.getPlugin().getRepository().updatePlayer(player, null);
    }

    public static MyPetPlayer createMyPetPlayer(Player player) {
        MyPetPlayer petPlayer = getMyPetPlayer(player);
        if (petPlayer == null) {
            if (BukkitUtil.isInOnlineMode()) {
                petPlayer = new OnlineMyPetPlayer(player.getUniqueId());
            } else {
                petPlayer = new OfflineMyPetPlayer(player.getName());
            }
        }
        return petPlayer;
    }

    public static MyPetPlayer createMyPetPlayer(TagCompound playerTag) {
        MyPetPlayer petPlayer = null;
        if (BukkitUtil.isInOnlineMode()) {
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
                    MyPetLogger.write(ChatColor.RED + "Can't get UUID for \"" + playerName + "\"! Pets may not be loaded for this player!");
                    return null;
                } else {
                    petPlayer = new OnlineMyPetPlayer(fetchedUUIDs.get(playerName));
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
            DebugLogger.info("   " + petPlayer);
        }
        return petPlayer;
    }

    public static boolean isMyPetPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return isMyPetPlayer(player);
    }

    public static boolean isMyPetPlayer(Player player) {
        return getInternalUUID(player.getUniqueId()) != null;
    }

    public static MyPetPlayer[] getMyPetPlayers() {
        MyPetPlayer[] playerArray;
        int playerCounter = 0;
        playerArray = new MyPetPlayer[onlinePlayers.size()];
        for (MyPetPlayer player : onlinePlayers.values()) {
            playerArray[playerCounter++] = player;
        }
        return playerArray;
    }

    public static MyPetPlayer registerMyPetPlayer(Player player) {
        MyPetPlayer myPetPlayer = createMyPetPlayer(player);
        MyPetPlugin.getPlugin().getRepository().addMyPetPlayer(myPetPlayer, null);
        PlayerList.setOnline(myPetPlayer);
        return myPetPlayer;
    }
}