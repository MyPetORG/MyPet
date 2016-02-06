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

package de.Keyle.MyPet.api.player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.MyPetEntity;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.hooks.PluginHookManager;
import de.Keyle.MyPet.util.hooks.PvPChecker;
import de.Keyle.MyPet.util.hooks.arenas.*;
import de.Keyle.MyPet.util.locale.Translation;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.keyle.knbt.*;
import net.citizensnpcs.api.CitizensAPI;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public abstract class MyPetPlayer implements Scheduler, NBTStorage {
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
            } catch (IllegalArgumentException e) {
                DebugLogger.warning("There are two pets registered for one worldgroup or vice versa!");
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
        return MyPetList.hasActiveMyPet(this);
    }

    public MyPet getMyPet() {
        return MyPetList.getMyPet(this);
    }

    public void hasInactiveMyPets(RepositoryCallback<Boolean> callback) {
        MyPetList.hasInactiveMyPets(this, callback);
    }

    public void getInactiveMyPet(UUID petUUID, RepositoryCallback<InactiveMyPet> callback) {
        MyPetPlugin.getPlugin().getRepository().getMyPet(petUUID, callback);
    }

    public void getInactiveMyPets(RepositoryCallback<List<InactiveMyPet>> callback) {
        MyPetList.getInactiveMyPets(this, callback);
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

    public DonateCheck.DonationRank getDonationRank() {
        return rank;
    }

    public void checkForDonation() {
        if (!donationChecked) {
            donationChecked = true;
            Bukkit.getScheduler().runTaskLaterAsynchronously(MyPetPlugin.getPlugin(), new Runnable() {
                public void run() {
                    rank = DonateCheck.getDonationRank(MyPetPlayer.this);
                }
            }, 60L);
        }
    }

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
            if (myPet.getStatus() == PetState.Here) {
                if (myPet.getLocation().getWorld() != this.getPlayer().getLocation().getWorld() || myPet.getLocation().distance(this.getPlayer().getLocation()) > 40) {
                    myPet.removePet(true);
                    myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Despawn", myPet.getOwner()), myPet.getPetName()));
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
                    BukkitUtil.sendMessageActionBar(getPlayer(), msg);
                }
            }
        }

        boolean citizensUsable = PluginHookManager.isPluginUsable("Citizens");

        if (isCaptureHelperActive()) {
            Player p = getPlayer();
            List<Entity> entities = p.getNearbyEntities(10, 10, 10);

            for (Entity entity : entities) {
                if (entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof MyPetEntity)) {
                    if (MyPetType.isLeashableEntityType(entity.getType())) {
                        if (citizensUsable) {
                            try {
                                if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                                    continue;
                                }
                            } catch (Error | Exception ignored) {
                            }
                        }
                        if (!PvPChecker.canHurt(p, entity)) {
                            continue;
                        }
                        if (!Permissions.has(this, "MyPet.user.leash." + MyPetType.getMyPetTypeByEntityType(entity.getType()).getTypeName())) {
                            continue;
                        }
                        Location l = entity.getLocation();
                        l.add(0, ((LivingEntity) entity).getEyeHeight(true) + 1, 0);
                        if (CaptureHelper.checkTamable((LivingEntity) entity)) {
                            BukkitUtil.playParticleEffect(p, l, EnumParticle.ITEM_CRACK, 0, 0, 0, 0.02f, 20, 16, 351, 10);
                        } else {
                            BukkitUtil.playParticleEffect(p, l, EnumParticle.ITEM_CRACK, 0, 0, 0, 0.02f, 20, 16, 351, +1);
                        }
                    }
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