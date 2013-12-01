/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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
import net.minecraft.server.v1_7_R1.EntityHuman;
import net.minecraft.server.v1_7_R1.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.spout.nbt.*;

import java.util.*;

public class MyPetPlayer implements IScheduler, NBTStorage {
    public final static Set<String> onlinePlayerList = new HashSet<String>();
    private static List<MyPetPlayer> playerList = new ArrayList<MyPetPlayer>();

    private String playerName;
    private String lastLanguage = "en_US";

    private boolean captureHelperMode = false;
    private boolean autoRespawn = false;
    private int autoRespawnMin = 1;

    private BiMap<String, UUID> petWorldUUID = HashBiMap.create();
    private BiMap<UUID, String> petUUIDWorld = petWorldUUID.inverse();
    private CompoundTag extendedInfo = new CompoundTag("ExtendedInfo", new CompoundMap());

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
        } else if (extendedInfo.getValue().size() > 0) {
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

    public void setExtendedInfo(CompoundTag compound) {
        if (extendedInfo.getValue().size() == 0) {
            extendedInfo = compound;
        }
    }

    public void addExtendedInfo(String key, Tag<?> tag) {
        extendedInfo.getValue().put(key, tag);
    }

    public Tag<?> getExtendedInfo(String key) {
        if (extendedInfo.getValue().containsKey(key)) {
            return extendedInfo.getValue().get(key);
        }
        return null;
    }

    public CompoundTag getExtendedInfo() {
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
    public CompoundTag save() {
        CompoundTag playerNBT = new CompoundTag(getName(), new CompoundMap());

        playerNBT.getValue().put("Name", new StringTag("Name", getName()));
        playerNBT.getValue().put("AutoRespawn", new ByteTag("AutoRespawn", hasAutoRespawnEnabled()));
        playerNBT.getValue().put("AutoRespawnMin", new IntTag("AutoRespawnMin", getAutoRespawnMin()));
        playerNBT.getValue().put("AutoRespawnMin2", new IntTag("AutoRespawnMin2", getAutoRespawnMin()));
        playerNBT.getValue().put("ExtendedInfo", getExtendedInfo());
        playerNBT.getValue().put("CaptureMode", new ByteTag("CaptureMode", isCaptureHelperActive()));

        CompoundTag multiWorldCompound = new CompoundTag("MultiWorld", new CompoundMap());
        for (String worldGroupName : petWorldUUID.keySet()) {
            multiWorldCompound.getValue().put(worldGroupName, new StringTag(worldGroupName, petWorldUUID.get(worldGroupName).toString()));
        }
        playerNBT.getValue().put("MultiWorld", multiWorldCompound);

        return playerNBT;
    }

    @Override
    public void load(CompoundTag myplayerNBT) {
        if (myplayerNBT.getValue().containsKey("AutoRespawn")) {
            setAutoRespawnEnabled(((ByteTag) myplayerNBT.getValue().get("AutoRespawn")).getBooleanValue());
        }
        if (myplayerNBT.getValue().containsKey("AutoRespawnMin")) {
            setAutoRespawnMin(((IntTag) myplayerNBT.getValue().get("AutoRespawnMin")).getValue());
        }
        if (myplayerNBT.getValue().containsKey("CaptureMode")) {
            if (myplayerNBT.getValue().get("CaptureMode").getType() == TagType.TAG_STRING) {
                if (!((StringTag) myplayerNBT.getValue().get("CaptureMode")).getValue().equals("Deactivated")) {
                    setCaptureHelperActive(true);
                }
            } else if (myplayerNBT.getValue().get("CaptureMode").getType() == TagType.TAG_BYTE) {
                setCaptureHelperActive(((ByteTag) myplayerNBT.getValue().get("CaptureMode")).getBooleanValue());
            }
        }
        if (myplayerNBT.getValue().containsKey("LastActiveMyPetUUID")) {
            String lastActive = ((StringTag) myplayerNBT.getValue().get("LastActiveMyPetUUID")).getValue();
            if (!lastActive.equalsIgnoreCase("")) {
                UUID lastActiveUUID = UUID.fromString(lastActive);
                World newWorld = Bukkit.getServer().getWorlds().get(0);
                WorldGroup lastActiveGroup = WorldGroup.getGroupByWorld(newWorld.getName());
                this.setMyPetForWorldGroup(lastActiveGroup.getName(), lastActiveUUID);
            }
        }
        if (myplayerNBT.getValue().containsKey("ExtendedInfo")) {
            setExtendedInfo((CompoundTag) myplayerNBT.getValue().get("ExtendedInfo"));
        }
        if (myplayerNBT.getValue().containsKey("MultiWorld")) {
            CompoundMap map = ((CompoundTag) myplayerNBT.getValue().get("MultiWorld")).getValue();
            for (String worldGroupName : map.keySet()) {
                String petUUID = ((StringTag) map.get(worldGroupName)).getValue();
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