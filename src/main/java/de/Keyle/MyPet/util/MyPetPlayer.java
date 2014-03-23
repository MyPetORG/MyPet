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

package de.Keyle.MyPet.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.support.Permissions;
import de.Keyle.MyPet.util.support.arenas.*;
import de.keyle.knbt.*;
import net.minecraft.server.v1_7_R2.EntityHuman;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;

import java.util.*;

public class MyPetPlayer implements IScheduler, NBTStorage {
    public final static Set<String> onlinePlayerList = new HashSet<String>();
    private static List<MyPetPlayer> playerList = new ArrayList<MyPetPlayer>();

    private String playerName;
    private String lastLanguage = "en_US";
    private UUID mojangUUID = null;

    private boolean captureHelperMode = false;
    private boolean autoRespawn = false;
    private int autoRespawnMin = 1;

    private BiMap<String, UUID> petWorldUUID = HashBiMap.create();
    private BiMap<UUID, String> petUUIDWorld = petWorldUUID.inverse();
    private TagCompound extendedInfo = new TagCompound();

    private MyPetPlayer(String playerName) {
        this.playerName = playerName;
    }

    public String getName() {
        return playerName;
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

    public boolean isOnline() {
        return onlinePlayerList.contains(playerName);
    }

    public boolean isInExternalGames() {
        if (MobArena.isInMobArena(this) ||
                Minigames.isInMinigame(this) ||
                BattleArena.isInBattleArena(this) ||
                PvPArena.isInPvPArena(this) ||
                MyHungerGames.isInHungerGames(this) ||
                SurvivalGames.isInSurvivalGames(this)) {
            return true;
        }
        return false;
    }

    public UUID getMojangUUID() {
        if (mojangUUID == null && isOnline()) {
            mojangUUID = getPlayer().getUniqueId();
        }
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
        return MyPetList.hasMyPet(playerName);
    }

    public MyPet getMyPet() {
        return MyPetList.getMyPet(playerName);
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
        return Bukkit.getServer().getPlayerExact(playerName);
    }

    public EntityPlayer getEntityPlayer() {
        Player p = getPlayer();
        if (p != null) {
            return ((CraftPlayer) p).getHandle();
        }
        return null;
    }

    public static MyPetPlayer getMyPetPlayer(String name) {
        for (MyPetPlayer myPetPlayer : playerList) {
            if (myPetPlayer.getName().equals(name)) {
                return myPetPlayer;
            }
        }
        MyPetPlayer myPetPlayer = new MyPetPlayer(name);
        playerList.add(myPetPlayer);
        return myPetPlayer;
    }

    public static MyPetPlayer getMyPetPlayer(Player player) {
        return MyPetPlayer.getMyPetPlayer(player.getName());
    }

    public static boolean isMyPetPlayer(String name) {
        for (MyPetPlayer myPetPlayer : playerList) {
            if (myPetPlayer.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMyPetPlayer(Player player) {
        for (MyPetPlayer myPetPlayer : playerList) {
            if (myPetPlayer.equals(player)) {
                return true;
            }
        }
        return false;
    }

    public static MyPetPlayer[] getMyPetPlayers() {
        MyPetPlayer[] playerArray = new MyPetPlayer[playerList.size()];
        int playerCounter = 0;
        for (MyPetPlayer player : playerList) {
            playerArray[playerCounter++] = player;
        }
        return playerArray;
    }

    public static boolean checkRemovePlayer(MyPetPlayer myPetPlayer) {
        if (!myPetPlayer.isOnline() && !myPetPlayer.hasCustomData() && myPetPlayer.getMyPet() == null && myPetPlayer.getInactiveMyPets().size() == 0) {
            playerList.remove(myPetPlayer);
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
        playerNBT.getCompoundData().put("AutoRespawnMin2", new TagInt(getAutoRespawnMin()));
        playerNBT.getCompoundData().put("ExtendedInfo", getExtendedInfo());
        playerNBT.getCompoundData().put("CaptureMode", new TagByte(isCaptureHelperActive()));

        if (getMojangUUID() != null) {
            playerNBT.getCompoundData().put("UUID", new TagString(getMojangUUID().toString()));
        }
        TagCompound multiWorldCompound = new TagCompound();
        for (String worldGroupName : petWorldUUID.keySet()) {
            multiWorldCompound.getCompoundData().put(worldGroupName, new TagString(petWorldUUID.get(worldGroupName).toString()));
        }
        playerNBT.getCompoundData().put("MultiWorld", multiWorldCompound);

        return playerNBT;
    }

    @Override
    public void load(TagCompound myplayerNBT) {
        if (myplayerNBT.getCompoundData().containsKey("UUID")) {
            mojangUUID = UUID.fromString(myplayerNBT.getAs("UUID", TagString.class).getStringData());
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
        if (mojangUUID == null) {
            mojangUUID = getPlayer().getUniqueId();
        }
        if (hasMyPet()) {
            MyPet myPet = getMyPet();
            if (myPet.getStatus() == PetState.Here) {
                if (myPet.getLocation().getWorld() != this.getPlayer().getLocation().getWorld() || myPet.getLocation().distance(this.getPlayer().getLocation()) > 40) {
                    myPet.removePet(true);
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Despawn", getLanguage()), myPet.getPetName()));
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
            return playerName.equals(player.getName());
        } else if (obj instanceof OfflinePlayer) {
            return ((OfflinePlayer) obj).getName().equals(playerName);
        } else if (obj instanceof EntityHuman) {
            EntityHuman entityHuman = (EntityHuman) obj;
            return playerName.equals(entityHuman.getName());
        } else if (obj instanceof AnimalTamer) {
            return ((AnimalTamer) obj).getName().equals(playerName);
        } else if (obj instanceof MyPetPlayer) {
            return this == obj;
        }
        return false;
    }

    @Override
    public String toString() {
        return "MyPetPlayer{name=" + playerName + "}";
    }
}