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

package de.Keyle.MyPet.util.player;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class MyPetPlayer implements de.Keyle.MyPet.api.player.MyPetPlayer {
    protected String lastKnownPlayerName;
    protected String lastLanguage = "en_US";
    protected UUID mojangUUID = null;
    protected UUID offlineUUID = null;
    protected final UUID internalUUID;

    protected boolean captureHelperMode = false;
    protected boolean autoRespawn = false;
    protected boolean showHealthBar = false;
    protected int autoRespawnMin = 1;
    protected float petLivingSoundVolume = 1f;

    protected BiMap<String, UUID> petWorldUUID = HashBiMap.create();
    protected BiMap<UUID, String> petUUIDWorld = petWorldUUID.inverse();
    protected TagCompound extendedInfo = new TagCompound();

    private volatile DonateCheck.DonationRank rank = DonateCheck.DonationRank.None;
    private boolean donationChecked = false;

    protected MyPetPlayer() {
        this(UUID.randomUUID());
    }

    protected MyPetPlayer(UUID internalUUID) {
        this.internalUUID = internalUUID;
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
        } else if (showHealthBar) {
            return true;
        } else if (petLivingSoundVolume < 1f) {
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

    public float getPetLivingSoundVolume() {
        return petLivingSoundVolume;
    }

    public void setPetLivingSoundVolume(float volume) {
        petLivingSoundVolume = Math.min(Math.max(volume, 0), 1f);
    }

    public boolean isHealthBarActive() {
        return showHealthBar;
    }

    public void setHealthBarActive(boolean showHealthBar) {
        this.showHealthBar = showHealthBar;
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
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public UUID getMyPetForWorldGroup(String worldGroup) {
        return petWorldUUID.get(worldGroup);
    }

    public UUID getMyPetForWorldGroup(WorldGroup worldGroup) {
        return petWorldUUID.get(worldGroup.getName());
    }

    public BiMap<String, UUID> getMyPetsForWorldGroups() {
        return petWorldUUID;
    }

    public String getWorldGroupForMyPet(UUID petUUID) {
        return petUUIDWorld.get(petUUID);
    }

    public boolean hasMyPetInWorldGroup(String worldGroup) {
        return petWorldUUID.containsKey(worldGroup);
    }

    public boolean hasMyPetInWorldGroup(WorldGroup worldGroup) {
        return petWorldUUID.containsKey(worldGroup.getName());
    }

    public void setExtendedInfo(TagCompound compound) {
        if (extendedInfo.getCompoundData().size() == 0) {
            extendedInfo = compound;
        }
    }

    public void addExtendedInfo(String key, TagBase tag) {
        extendedInfo.getCompoundData().put(key, tag);
    }

    public Optional<TagBase> getExtendedInfo(String key) {
        if (extendedInfo.getCompoundData().containsKey(key)) {
            return Optional.fromNullable(extendedInfo.getCompoundData().get(key));
        }
        return Optional.absent();
    }

    public TagCompound getExtendedInfo() {
        return extendedInfo;
    }

    // -----------------------------------------------------------------------------

    public boolean isOnline() {
        Player p = getPlayer();
        return p != null && p.isOnline();
    }

    public UUID getPlayerUUID() {
        if (MyPetApi.getPlugin().isInOnlineMode()) {
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
            lastLanguage = MyPetApi.getPlatformHelper().getPlayerLanguage(getPlayer());
        }
        return lastLanguage;
    }

    public boolean isMyPetAdmin() {
        return isOnline() && Permissions.has(getPlayer(), "MyPet.admin", false);
    }

    public boolean hasMyPet() {
        return MyPetApi.getMyPetManager().hasActiveMyPet(this);
    }

    public MyPet getMyPet() {
        return MyPetApi.getMyPetManager().getMyPet(this);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(getPlayerUUID());
    }

    public void sendMessage(String message) {
        if (isOnline()) {
            getPlayer().sendMessage(message);
        }
    }

    public DonateCheck.DonationRank getDonationRank() {
        return rank;
    }

    //donate-delete-start
    public void checkForDonation() {
        if (!donationChecked) {
            donationChecked = true;
            Bukkit.getScheduler().runTaskLaterAsynchronously(MyPetApi.getPlugin(), new Runnable() {
                public void run() {
                    rank = DonateCheck.getDonationRank(MyPetPlayer.this);
                }
            }, 60L);
        }
    }
    //donate-delete-end

    @Override
    public TagCompound save() {
        TagCompound playerNBT = new TagCompound();

        playerNBT.getCompoundData().put("Name", new TagString(getName()));
        playerNBT.getCompoundData().put("AutoRespawn", new TagByte(hasAutoRespawnEnabled()));
        playerNBT.getCompoundData().put("AutoRespawnMin", new TagInt(getAutoRespawnMin()));
        playerNBT.getCompoundData().put("ExtendedInfo", getExtendedInfo());
        playerNBT.getCompoundData().put("CaptureMode", new TagByte(isCaptureHelperActive()));
        playerNBT.getCompoundData().put("HealthBar", new TagByte(isHealthBarActive()));
        playerNBT.getCompoundData().put("PetLivingSoundVolume", new TagFloat(getPetLivingSoundVolume()));

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
        if (myplayerNBT.getCompoundData().containsKey("HealthBar")) {
            setHealthBarActive(myplayerNBT.getAs("HealthBar", TagByte.class).getBooleanData());
        }
        if (myplayerNBT.getCompoundData().containsKey("PetLivingSoundVolume")) {
            setPetLivingSoundVolume(myplayerNBT.getAs("PetLivingSoundVolume", TagFloat.class).getFloatData());
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
            if (myPet.getStatus() == de.Keyle.MyPet.entity.MyPet.PetState.Here) {
                if (myPet.getLocation().get().getWorld() != this.getPlayer().getLocation().getWorld() || myPet.getLocation().get().distance(this.getPlayer().getLocation()) > 40) {
                    myPet.removePet();
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Despawn", myPet.getOwner()), myPet.getPetName()));
                }

                if (showHealthBar) {
                    String msg = myPet.getPetName() + ChatColor.RESET + ": ";
                    if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2) {
                        msg += org.bukkit.ChatColor.GREEN;
                    } else if (myPet.getHealth() > myPet.getMaxHealth() / 3) {
                        msg += org.bukkit.ChatColor.YELLOW;
                    } else {
                        msg += org.bukkit.ChatColor.RED;
                    }
                    msg += String.format("%1.2f", myPet.getHealth()) + org.bukkit.ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                    MyPetApi.getPlatformHelper().sendMessageActionBar(getPlayer(), msg);
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
            if (MyPetApi.getPlugin().isInOnlineMode()) {
                return getPlayerUUID().equals(player.getUniqueId());
            } else {
                return getName().equals(player.getName());
            }
        } else if (obj instanceof OfflinePlayer) {
            OfflinePlayer offlinePlayer = (OfflinePlayer) obj;
            if (MyPetApi.getPlugin().isInOnlineMode()) {
                return getPlayerUUID().equals(offlinePlayer.getUniqueId());
            } else {
                return offlinePlayer.getName().equals(getName());
            }
        } else if (obj instanceof AnimalTamer) {
            AnimalTamer animalTamer = (AnimalTamer) obj;
            return animalTamer.getName().equals(getName());
        } else if (obj instanceof MyPetPlayer) {
            return this == obj;
        }
        return MyPetApi.getPlatformHelper().comparePlayerWithEntity(this, obj);
    }

    @Override
    public String toString() {
        return "MyPetPlayer{name=" + getName() + ", internal-uuid=" + internalUUID + ", mojang-uuid=" + mojangUUID + ", offline-uuid=" + offlineUUID + "}";
    }
}