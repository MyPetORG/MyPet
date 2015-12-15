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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.repository.Repository;
import de.Keyle.MyPet.util.Backup;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.MyPetVersion;
import de.Keyle.MyPet.util.configuration.ConfigurationNBT;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.player.UUIDFetcher;
import de.keyle.knbt.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class NbtRepository implements Repository, IScheduler {
    public static final ArrayListMultimap<MyPetPlayer, InactiveMyPet> myPets = ArrayListMultimap.create();
    protected final static Map<UUID, MyPetPlayer> players = Maps.newHashMap();

    private File NBTPetFile;
    private int autoSaveTimer = 0;
    private Backup backupManager;

    @Override
    public void disable() {
        myPets.clear();
    }

    @Override
    public void save() {
        saveData(false);
    }

    @Override
    public void init() {
        NBTPetFile = new File(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");

        if (Backup.MAKE_BACKUPS) {
            backupManager = new Backup(NBTPetFile, new File(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "backups" + File.separator));
        }


        loadData(NBTPetFile);
    }

    @Override
    public int countMyPets() {
        return myPets.values().size();
    }

    @Override
    public int countMyPets(MyPetType type) {
        int counter = 0;
        for (InactiveMyPet inactiveMyPet : getAllMyPets()) {
            if (inactiveMyPet.getPetType() == type) {
                counter++;
            }
        }
        return counter;
    }

    public void saveData(boolean async) {
        autoSaveTimer = Configuration.AUTOSAVE_TIME;
        final ConfigurationNBT nbtConfiguration = new ConfigurationNBT(NBTPetFile);

        nbtConfiguration.getNBTCompound().getCompoundData().put("Version", new TagString(MyPetVersion.getVersion()));
        nbtConfiguration.getNBTCompound().getCompoundData().put("Build", new TagInt(Integer.parseInt(MyPetVersion.getBuild())));
        nbtConfiguration.getNBTCompound().getCompoundData().put("OnlineMode", new TagByte(BukkitUtil.isInOnlineMode()));
        nbtConfiguration.getNBTCompound().getCompoundData().put("Pets", savePets());
        nbtConfiguration.getNBTCompound().getCompoundData().put("Players", savePlayers());
        nbtConfiguration.getNBTCompound().getCompoundData().put("PluginStorage", MyPetPlugin.getPlugin().getPluginStorage().save());
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                public void run() {
                    nbtConfiguration.save();
                }
            });
        } else {
            nbtConfiguration.save();
        }
    }

    private void loadData(File f) {
        ConfigurationNBT nbtConfiguration = new ConfigurationNBT(f);
        if (!nbtConfiguration.load()) {
            return;
        }

        if (nbtConfiguration.getNBTCompound().containsKeyAs("CleanShutdown", TagByte.class)) {
            DebugLogger.info("Clean shutdown: " + nbtConfiguration.getNBTCompound().getAs("CleanShutdown", TagByte.class).getBooleanData());
        }

        DebugLogger.info("Loading plugin storage ------------------" + nbtConfiguration.getNBTCompound().containsKeyAs("PluginStorage", TagCompound.class));
        if (nbtConfiguration.getNBTCompound().containsKeyAs("PluginStorage", TagCompound.class)) {
            TagCompound storageTag = nbtConfiguration.getNBTCompound().getAs("PluginStorage", TagCompound.class);
            for (String plugin : storageTag.getCompoundData().keySet()) {
                DebugLogger.info("  " + plugin);
            }
            DebugLogger.info(" Storage for " + storageTag.getCompoundData().keySet().size() + " MyPet-plugin(s) loaded");

        }
        DebugLogger.info("-----------------------------------------");

        DebugLogger.info("Loading players -------------------------");
        if (nbtConfiguration.getNBTCompound().containsKeyAs("Players", TagList.class)) {
            DebugLogger.info(loadPlayers(nbtConfiguration.getNBTCompound().getAs("Players", TagList.class)) + " PetPlayer(s) loaded");
        }
        DebugLogger.info("-----------------------------------------");

        DebugLogger.info("Loading Pets: -----------------------------");
        int petCount = loadPets(nbtConfiguration.getNBTCompound().getAs("Pets", TagList.class));
        MyPetLogger.write("" + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) loaded");
        DebugLogger.info("-----------------------------------------");
    }

    public Backup getBackupManager() {
        return backupManager;
    }

    @Override
    public void schedule() {
        if (Configuration.AUTOSAVE_TIME > 0 && autoSaveTimer-- <= 0) {
            saveData(true);
            autoSaveTimer = Configuration.AUTOSAVE_TIME;
        }
    }

    // Pets ------------------------------------------------------------------------------------------------------------

    @Override
    public Collection<InactiveMyPet> getAllMyPets() {
        return myPets.values();
    }

    @Override
    public boolean hasMyPets(MyPetPlayer myPetPlayer) {
        return myPets.containsKey(myPetPlayer);
    }

    @Override
    public List<InactiveMyPet> getMyPets(MyPetPlayer owner) {
        return myPets.get(owner);
    }

    @Override
    public InactiveMyPet getMyPet(UUID uuid) {
        for (InactiveMyPet pet : myPets.values()) {
            if (uuid.equals(pet.getUUID())) {
                return pet;
            }
        }
        return null;
    }

    @Override
    public void removeMyPet(UUID uuid) {
        for (InactiveMyPet pet : myPets.values()) {
            if (uuid.equals(pet.getUUID())) {
                myPets.remove(pet.getOwner(), pet);
            }
        }
    }

    @Override
    public void removeMyPet(InactiveMyPet inactiveMyPet) {
        myPets.remove(inactiveMyPet.getOwner(), inactiveMyPet);
    }

    @Override
    public void addMyPet(InactiveMyPet inactiveMyPet) {
        if (!myPets.containsEntry(inactiveMyPet.getOwner(), inactiveMyPet)) {
            myPets.put(inactiveMyPet.getOwner(), inactiveMyPet);
        }
    }

    @Override
    public boolean updateMyPet(MyPet myPet) {
        List<InactiveMyPet> pets = getMyPets(myPet.getOwner());
        for (InactiveMyPet pet : pets) {
            if (myPet.getUUID().equals(pet.getUUID())) {
                myPets.put(myPet.getOwner(), MyPetList.getInactiveMyPetFromMyPet(myPet));
                myPets.remove(myPet.getOwner(), pet);
                return true;
            }
        }
        return false;
    }

    private int loadPets(TagList petList) {
        int petCount = 0;
        boolean oldPets = false;
        for (int i = 0; i < petList.getReadOnlyList().size(); i++) {
            TagCompound myPetNBT = petList.getTagAs(i, TagCompound.class);
            MyPetPlayer petPlayer;
            if (myPetNBT.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                UUID ownerUUID = UUID.fromString(myPetNBT.getAs("Internal-Owner-UUID", TagString.class).getStringData());
                petPlayer = getMyPetPlayer(ownerUUID);
            } else {
                oldPets = true;
                continue;
            }
            if (petPlayer == null) {
                MyPetLogger.write("Owner for a pet (" + myPetNBT.getAs("Name", TagString.class) + " not found, pet loading skipped.");
                continue;
            }
            InactiveMyPet inactiveMyPet = new InactiveMyPet(petPlayer);
            inactiveMyPet.load(myPetNBT);

            MyPetList.addInactiveMyPet(inactiveMyPet);

            DebugLogger.info("   " + inactiveMyPet.toString());

            petCount++;
        }
        if (oldPets) {
            MyPetLogger.write("Old MyPets can not be loaded! Please use a previous version to upgrade first.");
        }
        return petCount;
    }

    private TagList savePets() {
        List<TagCompound> petList = new ArrayList<TagCompound>();

        for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
            updateMyPet(myPet);
        }
        for (InactiveMyPet inactiveMyPet : MyPetList.getAllInactiveMyPets()) {
            try {
                TagCompound petNBT = inactiveMyPet.save();
                petList.add(petNBT);
            } catch (Exception e) {
                DebugLogger.printThrowable(e);
            }
        }
        return new TagList(petList);
    }


    // Players ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean isMyPetPlayer(Player player) {
        for (MyPetPlayer p : players.values()) {
            if (p.getPlayerUUID().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public MyPetPlayer getMyPetPlayer(UUID uuid) {
        return players.get(uuid);
    }

    @Override
    public MyPetPlayer getMyPetPlayer(Player player) {
        for (MyPetPlayer p : players.values()) {
            if (p.getPlayerUUID().equals(player.getUniqueId())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void updatePlayer(MyPetPlayer player) {
        // we work with live data so no update required
    }

    @Override
    public void addMyPetPlayer(MyPetPlayer player) {
        players.put(player.getInternalUUID(), player);
    }

    @Override
    public void removeMyPetPlayer(MyPetPlayer player) {
        players.remove(player.getInternalUUID());
    }

    private TagList savePlayers() {
        List<TagCompound> playerList = Lists.newArrayList();
        for (MyPetPlayer myPetPlayer : players.values()) {
            if (myPetPlayer.hasMyPet() || myPetPlayer.hasInactiveMyPets() || myPetPlayer.hasCustomData()) {
                try {
                    playerList.add(myPetPlayer.save());
                } catch (Exception e) {
                    DebugLogger.printThrowable(e);
                }
            }
        }
        return new TagList(playerList);
    }

    private int loadPlayers(TagList playerList) {
        int playerCount = 0;
        if (BukkitUtil.isInOnlineMode()) {
            List<String> unknownPlayers = new ArrayList<String>();
            for (int i = 0; i < playerList.getReadOnlyList().size(); i++) {
                TagCompound playerTag = playerList.getTagAs(i, TagCompound.class);
                if (playerTag.containsKeyAs("Name", TagString.class)) {
                    if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                        TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
                        if (!uuidTag.containsKeyAs("Mojang-UUID", TagString.class)) {
                            String playerName = playerTag.getAs("Name", TagString.class).getStringData();
                            unknownPlayers.add(playerName);
                        }
                    } else if (!playerTag.getCompoundData().containsKey("Mojang-UUID")) {
                        String playerName = playerTag.getAs("Name", TagString.class).getStringData();
                        unknownPlayers.add(playerName);
                    }
                }
            }
            UUIDFetcher.call(unknownPlayers);
        }

        for (int i = 0; i < playerList.getReadOnlyList().size(); i++) {
            TagCompound playerTag = playerList.getTagAs(i, TagCompound.class);
            MyPetPlayer player = PlayerList.createMyPetPlayer(playerTag);
            if (player != null) {
                players.put(player.getInternalUUID(), player);
                playerCount++;
            }
        }
        return playerCount;
    }
}