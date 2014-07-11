/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util.player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.support.Permissions;
import de.Keyle.MyPet.util.support.arenas.*;
import de.keyle.knbt.*;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class MyPetPlayer implements IScheduler, NBTStorage {
    public final static Set<UUID> onlinePlayerUUIDList = new HashSet<UUID>();
    protected final static Map<UUID, MyPetPlayer> uuidToOwner = new HashMap<UUID, MyPetPlayer>();
    protected final static Map<UUID, UUID> uuidToInternalUUID = new HashMap<UUID, UUID>();

    protected String lastKnownPlayerName;
    protected String lastLanguage = "en_US";
    protected UUID mojangUUID = null;
    protected UUID offlineUUID = null;
    protected final UUID internalUUID;

    protected boolean captureHelperMode = false;
    protected boolean autoRespawn = false;
    protected int autoRespawnMin = 1;

    protected BiMap<String, UUID> petWorldUUID = HashBiMap.create();
    protected BiMap<UUID, String> petUUIDWorld = petWorldUUID.inverse();
    protected TagCompound extendedInfo = new TagCompound();

    protected MyPetPlayer() {
        this(UUID.randomUUID());
    }

    protected MyPetPlayer(UUID internalUUID) {
        this.internalUUID = internalUUID;
        uuidToOwner.put(internalUUID, this);
    }

    public String getName() {
        return lastKnownPlayerName;
    }

    public boolean hasCustomData() {
        if (autoRespawn || autoRespawnMin != 1) {
            return true;
        } else if (captureHelperMode) {
            return true;
        } else if (extendedInfo.getCompoundData().size() > 0) {
            return true;
        } else if (petWorldUUID.size() > 0) {
            return true;
        }
        return false;
    }

    // Custom Data -----------------------------------------------------------------

    public void setAutoRespawnEnabled(boolean flag) {
        autoRespawn = flag;
    }

    public boolean hasAutoRespawnEnabled() {
        return autoRespawn;
    }

    public void setAutoRespawnMin(int value) {
        autoRespawnMin = value;
    }

    public int getAutoRespawnMin() {
        return autoRespawnMin;
    }

    public boolean isCaptureHelperActive() {
        return captureHelperMode;
    }

    public void setCaptureHelperActive(boolean captureHelperMode) {
        this.captureHelperMode = captureHelperMode;
    }

    public void setMyPetForWorldGroup(String worldGroup, UUID myPetUUID) {
        if (worldGroup == null || worldGroup.equals("")) {
            return;
        }
        if (myPetUUID == null) {
            petWorldUUID.remove(worldGroup);
        } else {
            try {
                petWorldUUID.put(worldGroup, myPetUUID);
            } catch (IllegalArgumentException e) {
                DebugLogger.warning("There are two pets registered for one worldgroup or vice versa!");
            }
        }
    }

    public UUID getMyPetForWorldGroup(String worldGroup) {
        return petWorldUUID.get(worldGroup);
    }

    public String getWorldGroupForMyPet(UUID petUUID) {
        return petUUIDWorld.get(petUUID);
    }

    public boolean hasMyPetInWorldGroup(String worldGroup) {
        return petWorldUUID.containsKey(worldGroup);
    }

    public boolean hasInactiveMyPetInWorldGroup(String worldGroup) {
        for (InactiveMyPet inactiveMyPet : getInactiveMyPets()) {
            if (inactiveMyPet.getWorldGroup().equals(worldGroup)) {
                return true;
            }
        }
        return false;
    }

    public void setExtendedInfo(TagCompound compound) {
        if (extendedInfo.getCompoundData().size() == 0) {
            extendedInfo = compound;
        }
    }

    public void addExtendedInfo(String key, TagBase tag) {
        extendedInfo.getCompoundData().put(key, tag);
    }

    public TagBase getExtendedInfo(String key) {
        if (extendedInfo.getCompoundData().containsKey(key)) {
            return extendedInfo.getCompoundData().get(key);
        }
        return null;
    }

    public TagCompound getExtendedInfo() {
        return extendedInfo;
    }

    // -----------------------------------------------------------------------------

    public abstract boolean isOnline();

    public boolean isInExternalGames() {
        if (MobArena.isInMobArena(this) ||
                Minigames.isInMinigame(this) ||
                BattleArena.isInBattleArena(this) ||
                PvPArena.isInPvPArena(this) ||
                MyHungerGames.isInHungerGames(this) ||
                UltimateSurvivalGames.isInSurvivalGames(this) ||
                SurvivalGames.isInSurvivalGames(this)) {
            return true;
        }
        return false;
    }

    public UUID getPlayerUUID() {
        if (BukkitUtil.isInOnlineMode()) {
            return mojangUUID;
        } else {
            return offlineUUID;
        }
    }

    public UUID getInternalUUID() {
        return internalUUID;
    }

    public UUID getOfflineUUID() {
        if (offlineUUID == null && lastKnownPlayerName != null) {
            return Util.getOfflinePlayerUUID(lastKnownPlayerName);
        }
        return offlineUUID;
    }

    public UUID getMojangUUID() {
        return mojangUUID;
    }

    public String getLanguage() {
        if (isOnline()) {
            lastLanguage = BukkitUtil.getPlayerLanguage(getPlayer());
        }
        return lastLanguage;
    }

    public boolean isMyPetAdmin() {
        return isOnline() && Permissions.has(getPlayer(), "MyPet.admin", false);
    }

    public boolean hasMyPet() {
        return MyPetList.hasMyPet(this);
    }

    public MyPet getMyPet() {
        return MyPetList.getMyPet(this);
    }

    public boolean hasInactiveMyPets() {
        return MyPetList.hasInactiveMyPets(this);
    }

    public InactiveMyPet getInactiveMyPet(UUID petUUID) {
        for (InactiveMyPet inactiveMyPet : MyPetList.getInactiveMyPets(this)) {
            if (inactiveMyPet.getUUID().equals(petUUID)) {
                return inactiveMyPet;
            }
        }
        return null;
    }

    public List<InactiveMyPet> getInactiveMyPets() {
        return MyPetList.getInactiveMyPets(this);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(getPlayerUUID());
    }

    public EntityPlayer getEntityPlayer() {
        Player p = getPlayer();
        if (p != null) {
            return ((CraftPlayer) p).getHandle();
        }
        return null;
    }

    public static UUID getInternalUUID(Player player) {
        return uuidToInternalUUID.get(player.getUniqueId());
    }

    public static UUID getInternalUUID(UUID playerUUID) {
        return uuidToInternalUUID.get(playerUUID);
    }

    public static MyPetPlayer getMyPetPlayer(UUID internalUUID) { //ToDo just for internal use
        return uuidToOwner.get(internalUUID);
    }

    public static MyPetPlayer getOrCreateMyPetPlayer(Player player) {
        UUID internalUUID = getInternalUUID(player);
        MyPetPlayer petPlayer;
        if (internalUUID == null) {
            if (BukkitUtil.isInOnlineMode()) {
                petPlayer = new OnlineMyPetPlayer(player.getUniqueId());
            } else {
                petPlayer = new OfflineMyPetPlayer(player.getName());
            }
        } else {
            petPlayer = MyPetPlayer.getMyPetPlayer(internalUUID);
        }
        return petPlayer;
    }

    public static MyPetPlayer getMyPetPlayer(String name) {
        UUID playerUUID;
        if (BukkitUtil.isInOnlineMode()) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) {
                playerUUID = p.getUniqueId();
            } else {
                playerUUID = Util.getOfflinePlayerUUID(name);
            }
        } else {
            playerUUID = Util.getOfflinePlayerUUID(name);
        }
        UUID internalUUID = getInternalUUID(playerUUID);
        if (internalUUID == null) {
            return null;
        }
        return uuidToOwner.get(internalUUID);
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
        UUID playerUUID;
        if (BukkitUtil.isInOnlineMode()) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) {
                playerUUID = p.getUniqueId();
            } else {
                playerUUID = Util.getOfflinePlayerUUID(name);
            }
        } else {
            playerUUID = Util.getOfflinePlayerUUID(name);
        }
        return getInternalUUID(playerUUID) != null;
    }

    public static boolean isMyPetPlayer(Player player) {
        return getInternalUUID(player.getUniqueId()) != null;
    }

    public static MyPetPlayer[] getMyPetPlayers() {
        MyPetPlayer[] playerArray;
        int playerCounter = 0;
        playerArray = new MyPetPlayer[uuidToOwner.size()];
        for (MyPetPlayer player : uuidToOwner.values()) {
            playerArray[playerCounter++] = player;
        }
        return playerArray;
    }

    public static boolean checkRemovePlayer(MyPetPlayer myPetPlayer) {
        if (!myPetPlayer.isOnline() && !myPetPlayer.hasCustomData() && myPetPlayer.getMyPet() == null && myPetPlayer.getInactiveMyPets().size() == 0) {
            if (BukkitUtil.isInOnlineMode()) {
                uuidToInternalUUID.remove(myPetPlayer.getMojangUUID());
            } else {
                uuidToInternalUUID.remove(myPetPlayer.getOfflineUUID());
            }
            uuidToOwner.remove(myPetPlayer.getPlayerUUID());
            return true;
        }
        return false;
    }

    @Override
    public TagCompound save() {
        TagCompound playerNBT = new TagCompound();

        playerNBT.getCompoundData().put("Name", new TagString(getName()));
        playerNBT.getCompoundData().put("AutoRespawn", new TagByte(hasAutoRespawnEnabled()));
        playerNBT.getCompoundData().put("AutoRespawnMin", new TagInt(getAutoRespawnMin()));
        playerNBT.getCompoundData().put("ExtendedInfo", getExtendedInfo());
        playerNBT.getCompoundData().put("CaptureMode", new TagByte(isCaptureHelperActive()));

        TagCompound playerUUIDTag = new TagCompound();
        if (mojangUUID != null) {
            playerUUIDTag.getCompoundData().put("Mojang-UUID", new TagString(mojangUUID.toString()));
        }
        if (offlineUUID != null) {
            playerUUIDTag.getCompoundData().put("Offline-UUID", new TagString(offlineUUID.toString()));
        }
        playerUUIDTag.getCompoundData().put("Internal-UUID", new TagString(internalUUID.toString()));
        playerNBT.getCompoundData().put("UUID", playerUUIDTag);

        TagCompound multiWorldCompound = new TagCompound();
        for (String worldGroupName : petWorldUUID.keySet()) {
            multiWorldCompound.getCompoundData().put(worldGroupName, new TagString(petWorldUUID.get(worldGroupName).toString()));
        }
        playerNBT.getCompoundData().put("MultiWorld", multiWorldCompound);

        return playerNBT;
    }

    @Override
    public void load(TagCompound myplayerNBT) {
        // ToDo remove --------------------------
        if (myplayerNBT.getCompoundData().containsKey("Offline-UUID")) {
            offlineUUID = UUID.fromString(myplayerNBT.getAs("Offline-UUID", TagString.class).getStringData());
        }
        if (myplayerNBT.getCompoundData().containsKey("Mojang-UUID")) {
            mojangUUID = UUID.fromString(myplayerNBT.getAs("Mojang-UUID", TagString.class).getStringData());
        }
        // --------------------------------------
        if (myplayerNBT.containsKeyAs("UUID", TagCompound.class)) {
            TagCompound uuidTag = myplayerNBT.getAs("UUID", TagCompound.class);

            if (uuidTag.getCompoundData().containsKey("Offline-UUID")) {
                offlineUUID = UUID.fromString(uuidTag.getAs("Offline-UUID", TagString.class).getStringData());
            }
            if (uuidTag.getCompoundData().containsKey("Mojang-UUID")) {
                mojangUUID = UUID.fromString(uuidTag.getAs("Mojang-UUID", TagString.class).getStringData());
            }
        }
        if (myplayerNBT.getCompoundData().containsKey("AutoRespawn")) {
            setAutoRespawnEnabled(myplayerNBT.getAs("AutoRespawn", TagByte.class).getBooleanData());
        }
        if (myplayerNBT.getCompoundData().containsKey("AutoRespawnMin")) {
            setAutoRespawnMin(myplayerNBT.getAs("AutoRespawnMin", TagInt.class).getIntData());
        }
        if (myplayerNBT.containsKeyAs("CaptureMode", TagString.class)) {
            if (!myplayerNBT.getAs("CaptureMode", TagString.class).getStringData().equals("Deactivated")) {
                setCaptureHelperActive(true);
            }
        } else if (myplayerNBT.containsKeyAs("CaptureMode", TagByte.class)) {
            setCaptureHelperActive(myplayerNBT.getAs("CaptureMode", TagByte.class).getBooleanData());
        }
        if (myplayerNBT.getCompoundData().containsKey("LastActiveMyPetUUID")) {
            String lastActive = myplayerNBT.getAs("LastActiveMyPetUUID", TagString.class).getStringData();
            if (!lastActive.equalsIgnoreCase("")) {
                UUID lastActiveUUID = UUID.fromString(lastActive);
                World newWorld = Bukkit.getServer().getWorlds().get(0);
                WorldGroup lastActiveGroup = WorldGroup.getGroupByWorld(newWorld.getName());
                this.setMyPetForWorldGroup(lastActiveGroup.getName(), lastActiveUUID);
            }
        }
        if (myplayerNBT.getCompoundData().containsKey("ExtendedInfo")) {
            setExtendedInfo(myplayerNBT.getAs("ExtendedInfo", TagCompound.class));
        }
        if (myplayerNBT.getCompoundData().containsKey("MultiWorld")) {
            TagCompound worldGroups = myplayerNBT.getAs("MultiWorld", TagCompound.class);
            for (String worldGroupName : worldGroups.getCompoundData().keySet()) {
                String petUUID = worldGroups.getAs(worldGroupName, TagString.class).getStringData();
                setMyPetForWorldGroup(worldGroupName, UUID.fromString(petUUID));
            }
        }
    }

    public void schedule() {
        if (!isOnline()) {
            return;
        }
        if (hasMyPet()) {
            MyPet myPet = getMyPet();
            if (myPet.getStatus() == PetState.Here) {
                if (myPet.getLocation().getWorld() != this.getPlayer().getLocation().getWorld() || myPet.getLocation().distance(this.getPlayer().getLocation()) > 40) {
                    myPet.removePet(true);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            if (BukkitUtil.isInOnlineMode()) {
                return getPlayerUUID().equals(player.getUniqueId());
            } else {
                return getName().equals(player.getName());
            }
        } else if (obj instanceof OfflinePlayer) {
            OfflinePlayer offlinePlayer = (OfflinePlayer) obj;
            if (BukkitUtil.isInOnlineMode()) {
                return getPlayerUUID().equals(offlinePlayer.getUniqueId());
            } else {
                return offlinePlayer.getName().equals(getName());
            }
        } else if (obj instanceof EntityHuman) {
            EntityHuman entityHuman = (EntityHuman) obj;
            if (BukkitUtil.isInOnlineMode()) {
                return getPlayerUUID().equals(entityHuman.getUniqueID());
            } else {
                return entityHuman.getName().equals(getName());
            }
        } else if (obj instanceof AnimalTamer) {
            AnimalTamer animalTamer = (AnimalTamer) obj;
            return animalTamer.getName().equals(getName());
        } else if (obj instanceof MyPetPlayer) {
            return this == obj;
        }
        return false;
    }

    @Override
    public String toString() {
        return "MyPetPlayer{name=" + getName() + ", internal-uuid=" + internalUUID + ", mojang-uuid=" + mojangUUID + ", offline-uuid=" + offlineUUID + "}";
    }
}