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

package de.Keyle.MyPet.util.player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MyPetPlayerImpl implements MyPetPlayer {

    protected final UUID internalUUID;
    protected String lastKnownPlayerName;
    protected String lastLanguage = "en_US";
    protected UUID mojangUUID = null;
    protected boolean onlineMode = false;

    protected boolean captureHelperMode = false;
    protected int captureHelperTimer = 90;
    protected boolean autoRespawn = false;
    protected boolean showHealthBar = false;
    protected int autoRespawnMin = 1;
    protected float petLivingSoundVolume = 1f;

    protected BiMap<String, UUID> petWorldUUID = HashBiMap.create();
    protected BiMap<UUID, String> petUUIDWorld = petWorldUUID.inverse();
    protected CompoundBinaryTag extendedInfo = CompoundBinaryTag.empty();
    Map<Component, Long> sentMessages = new HashMap<>();

    private volatile DonateCheck.DonationRank rank = DonateCheck.DonationRank.None;
    private boolean donationChecked = false;

    public MyPetPlayerImpl(UUID internalUUID, String playerName) {
        this.internalUUID = internalUUID;
        this.lastKnownPlayerName = playerName;
    }

    public MyPetPlayerImpl(UUID internalUUID, UUID mojangUUID) {
        this.internalUUID = internalUUID;
        this.mojangUUID = mojangUUID;
    }

    public MyPetPlayerImpl(UUID internalUUID, UUID mojangUUID, String playerName) {
        this.internalUUID = internalUUID;
        this.mojangUUID = mojangUUID;
        this.lastKnownPlayerName = playerName;
    }

    public void setOnlineMode(boolean mode) {
        onlineMode = mode;
    }

    public void setLastKnownName(String name) {
        if (name != null) {
            this.lastKnownPlayerName = name;
        }
    }

    public String getName() {
        return lastKnownPlayerName;
    }

    public boolean hasCustomData() {
        if (autoRespawn || autoRespawnMin != 1) {
            return true;
        } else if (captureHelperMode) {
            return true;
        } else if (!extendedInfo.keySet().isEmpty()) {
            return true;
        } else if (!petWorldUUID.isEmpty()) {
            return true;
        } else if (showHealthBar) {
            return true;
        } else return petLivingSoundVolume < 1f;
    }

    // Custom Data -----------------------------------------------------------------

    public void setAutoRespawnEnabled(boolean flag) {
        autoRespawn = flag;
    }

    public boolean hasAutoRespawnEnabled() {
        return autoRespawn;
    }

    public int getAutoRespawnMin() {
        return autoRespawnMin;
    }

    public void setAutoRespawnMin(int value) {
        autoRespawnMin = value;
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
        if (captureHelperMode) {
            captureHelperTimer = 90;
        }
    }

    public void setMyPetForWorldGroup(String worldGroup, UUID myPetUUID) {
        if (worldGroup == null || worldGroup.isEmpty()) {
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

    public void setMyPetForWorldGroup(WorldGroup worldGroup, UUID myPetUUID) {
        if (worldGroup == null) {
            return;
        }
        if (myPetUUID == null) {
            petWorldUUID.remove(worldGroup.getName());
        } else {
            try {
                petWorldUUID.put(worldGroup.getName(), myPetUUID);
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

    public void addExtendedInfo(String key, BinaryTag tag) {
        extendedInfo = extendedInfo.put(key, tag);
    }

    public Optional<BinaryTag> getExtendedInfo(String key) {
        if (extendedInfo.keySet().contains(key)) {
            return Optional.ofNullable(extendedInfo.get(key));
        }
        return Optional.empty();
    }

    public CompoundBinaryTag getExtendedInfo() {
        return extendedInfo;
    }

    public void setExtendedInfo(CompoundBinaryTag compound) {
        if (extendedInfo.keySet().isEmpty()) {
            extendedInfo = compound;
        }
    }

    // -----------------------------------------------------------------------------

    public boolean isOnline() {
        Player p = getPlayer();
        return p != null && p.isOnline();
    }

    public UUID getPlayerUUID() {
        if (onlineMode) {
            return mojangUUID;
        } else {
            return Util.getOfflinePlayerUUID(getName());
        }
    }

    public UUID getInternalUUID() {
        return internalUUID;
    }

    public UUID getOfflineUUID() {
        return Util.getOfflinePlayerUUID(getName());
    }

    public UUID getMojangUUID() {
        return mojangUUID;
    }

    public void setMojangUUID(UUID uuid) {
        if (uuid != null) {
            this.mojangUUID = uuid;
        }
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

    public void sendMessage(Component message) {
        if (isOnline()) {
            getPlayer().sendMessage(message);
        }
    }

    public boolean sendMessage(Component message, int cooldown) {
        long currentTime = System.currentTimeMillis();
        if (sentMessages.containsKey(message)) {
            if (currentTime >= sentMessages.get(message)) {
                this.sentMessages.put(message, currentTime + cooldown);
                this.sendMessage(message);
                return true;
            }
        } else {
            this.sentMessages.put(message, currentTime + cooldown);
            this.sendMessage(message);
            return true;
        }
        return false;
    }

    public void sendActionBar(Component message) {
        if (isOnline()) {
            getPlayer().sendActionBar(message);
        }
    }

    public DonateCheck.DonationRank getDonationRank() {
        return rank;
    }

    public void checkForDonation() {
        if (!donationChecked) {
            donationChecked = true;
            Bukkit.getScheduler().runTaskLaterAsynchronously(MyPetApi.getPlugin(), () -> rank = DonateCheck.getDonationRank(MyPetPlayerImpl.this), 60L);
        }
    }

    @Override
    public CompoundBinaryTag save() {
        CompoundBinaryTag settingsTag = CompoundBinaryTag.builder()
                .putBoolean("AutoRespawn", hasAutoRespawnEnabled())
                .putInt("AutoRespawnMin", getAutoRespawnMin())
                .putBoolean("CaptureMode", isCaptureHelperActive())
                .putBoolean("HealthBar", isHealthBarActive())
                .putFloat("PetLivingSoundVolume", getPetLivingSoundVolume())
                .build();

        CompoundBinaryTag.Builder uuidBuilder = CompoundBinaryTag.builder();
        if (mojangUUID != null) {
            uuidBuilder.putString("Mojang-UUID", mojangUUID.toString());
        }
        uuidBuilder.putString("Name", getName());
        uuidBuilder.putString("Internal-UUID", internalUUID.toString());

        CompoundBinaryTag.Builder multiWorldBuilder = CompoundBinaryTag.builder();
        for (String worldGroupName : petWorldUUID.keySet()) {
            multiWorldBuilder.putString(worldGroupName, petWorldUUID.get(worldGroupName).toString());
        }

        return CompoundBinaryTag.builder()
                .put("Settings", settingsTag)
                .put("ExtendedInfo", getExtendedInfo())
                .put("UUID", uuidBuilder.build())
                .put("MultiWorld", multiWorldBuilder.build())
                .build();
    }

    @Override
    public void load(CompoundBinaryTag myplayerNBT) {
        if (myplayerNBT.keySet().contains("UUID")) {
            CompoundBinaryTag uuidTag = myplayerNBT.getCompound("UUID");

            if (uuidTag.keySet().contains("Mojang-UUID")) {
                mojangUUID = UUID.fromString(uuidTag.getString("Mojang-UUID"));
            }
            if (uuidTag.keySet().contains("Name") && lastKnownPlayerName == null) {
                lastKnownPlayerName = uuidTag.getString("Name");
            }
        }
        if (myplayerNBT.keySet().contains("Settings")) {
            CompoundBinaryTag settingsTag = myplayerNBT.getCompound("Settings");

            if (settingsTag.keySet().contains("AutoRespawn")) {
                setAutoRespawnEnabled(settingsTag.getBoolean("AutoRespawn"));
            }
            if (settingsTag.keySet().contains("AutoRespawnMin")) {
                setAutoRespawnMin(settingsTag.getInt("AutoRespawnMin"));
            }
            if (settingsTag.keySet().contains("CaptureMode")) {
                setCaptureHelperActive(settingsTag.getBoolean("CaptureMode"));
            }
            if (settingsTag.keySet().contains("HealthBar")) {
                setHealthBarActive(settingsTag.getBoolean("HealthBar"));
            }
            if (settingsTag.keySet().contains("PetLivingSoundVolume")) {
                setPetLivingSoundVolume(settingsTag.getFloat("PetLivingSoundVolume"));
            }
        } else {
            // Legacy fallback for old data format
            if (myplayerNBT.keySet().contains("Name") && lastKnownPlayerName == null) {
                lastKnownPlayerName = myplayerNBT.getString("Name");
            }
            if (myplayerNBT.keySet().contains("AutoRespawn")) {
                setAutoRespawnEnabled(myplayerNBT.getBoolean("AutoRespawn"));
            }
            if (myplayerNBT.keySet().contains("AutoRespawnMin")) {
                setAutoRespawnMin(myplayerNBT.getInt("AutoRespawnMin"));
            }
            if (myplayerNBT.keySet().contains("CaptureMode")) {
                // Legacy: CaptureMode could be string "Deactivated" or boolean
                BinaryTag captureModeTag = myplayerNBT.get("CaptureMode");
                if (captureModeTag instanceof net.kyori.adventure.nbt.StringBinaryTag stringTag) {
                    if (!stringTag.value().equals("Deactivated")) {
                        setCaptureHelperActive(true);
                    }
                } else {
                    setCaptureHelperActive(myplayerNBT.getBoolean("CaptureMode"));
                }
            }
            if (myplayerNBT.keySet().contains("HealthBar")) {
                setHealthBarActive(myplayerNBT.getBoolean("HealthBar"));
            }
            if (myplayerNBT.keySet().contains("PetLivingSoundVolume")) {
                setPetLivingSoundVolume(myplayerNBT.getFloat("PetLivingSoundVolume"));
            }
        }
        if (myplayerNBT.keySet().contains("ExtendedInfo")) {
            setExtendedInfo(myplayerNBT.getCompound("ExtendedInfo"));
        }
        if (myplayerNBT.keySet().contains("MultiWorld")) {
            CompoundBinaryTag worldGroups = myplayerNBT.getCompound("MultiWorld");
            for (String worldGroupName : worldGroups.keySet()) {
                String petUUID = worldGroups.getString(worldGroupName);
                setMyPetForWorldGroup(worldGroupName, UUID.fromString(petUUID));
            }
        }
    }

    public void schedule() {
        if (!isOnline()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        sentMessages.keySet().removeIf(message -> currentTime >= sentMessages.get(message));
        if (hasMyPet()) {
            MyPet myPet = getMyPet();
            Player p = this.getPlayer();
            if (myPet.getStatus() == PetState.Here) {
                if (myPet.getLocation().get().getWorld() != p.getLocation().getWorld() || MyPetApi.getPlatformHelper().distance(myPet.getLocation().get(), p.getLocation()) > 40) {
                    myPet.removePet(Configuration.Misc.RECALL_PET_AFTER_DESPAWN);
                    if (!MyPetApi.getCompatUtil().getMinecraftVersion().startsWith("1.8")) {
                        if (!p.isGliding()) {
                            myPet.getOwner().sendMessage(Util.formatTranslation("Message.Spawn.Despawn", myPet.getOwner(), myPet.getPetName()));
                        }
                    } else {
                        myPet.getOwner().sendMessage(Util.formatTranslation("Message.Spawn.Despawn", myPet.getOwner(), myPet.getPetName()));
                    }
                }

                if (!Configuration.Misc.DISABLE_ALL_ACTIONBAR_MESSAGES && showHealthBar) {
                    Component msg = MyPetApi.getPlatformHelper().buildPetHealthActionBar(myPet, myPet.getHealth(), myPet.getMaxHealth());
                    getPlayer().sendActionBar(msg);
                }
            } else if (myPet.getStatus() == PetState.Despawned) {
                if (myPet.wantsToRespawn() && !p.isFlying()) {
                    boolean velocity = p.getVelocity().getY() >= 0;
                    boolean fall = p.getFallDistance() == 0;

                    if (velocity || fall || p.isOnGround()) {
                        boolean spawn = true;

                        if (velocity) {
                            spawn = !p.isInsideVehicle();
                            if (spawn && MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                                spawn = !p.isGliding();
                            }
                        }
                        if (spawn && fall) {
                            spawn = switch (p.getWorld().getBlockAt(p.getLocation().subtract(0, 0.5, 0)).getType().name()) {
                                case "AIR", "CAVE_AIR", "VOID_AIR", "WATER", "STATIONARY_WATER", "LAVA",
                                     "STATIONARY_LAVA" -> false;
                                default -> spawn;
                            };
                        }

                        if (spawn && myPet.createEntity() == MyPet.SpawnFlags.Success) {
                            p.sendMessage(Util.formatTranslation("Message.Command.Call.Success", p, myPet.getPetName()));
                        }
                    }
                }
            }
        }

        if (isCaptureHelperActive()) {
            if (captureHelperTimer-- <= 0) {
                setCaptureHelperActive(false);
            }

            Player p = getPlayer();
            List<Entity> entities = p.getNearbyEntities(7, 7, 7);
            int count = 0;

            entityLoop:
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof MyPetBukkitEntity)) {
                    if (MyPetApi.getMyPetInfo().isLeashableEntityType(entity.getType())) {
                        for (LeashHook hook : MyPetApi.getPluginHookManager().getHooks(LeashHook.class)) {
                            if (!hook.canLeash(p, entity)) {
                                continue entityLoop;
                            }
                        }
                        if (!MyPetApi.getHookHelper().canHurt(p, entity)) {
                            continue;
                        }
                        if (!Permissions.has(this, "MyPet.leash." + MyPetType.byEntityTypeName(entity.getType().name()))) {
                            continue;
                        }
                        Location l = entity.getLocation();
                        l.add(0, ((LivingEntity) entity).getEyeHeight(true) + 1, 0);
                        if (checkTamable((LivingEntity) entity, p)) {
                            p.spawnParticle(Particle.ITEM_CRACK, l, 20, 0, 0, 0, 0.02f, new ItemStack(Material.LIME_DYE));
                        } else {
                            p.spawnParticle(Particle.ITEM_CRACK, l, 20, 0, 0, 0, 0.02f, new ItemStack(Material.RED_DYE));
                        }
                        if (count++ > 20) {
                            break;
                        }
                    }
                }
            }
        }
    }

    protected boolean checkTamable(LivingEntity leashTarget, Player p) {
        for (Settings flagSettings : MyPetApi.getMyPetInfo().getLeashFlagSettings(MyPetType.byEntityTypeName(leashTarget.getType().name()))) {
            String flagName = flagSettings.getName();
            LeashFlag flag = MyPetApi.getLeashFlagManager().getLeashFlag(flagName);
            if (flag != null && (flag.ignoredByHelper() || !flag.check(p, leashTarget, 0, flagSettings))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Player player) {
            return getPlayerUUID().equals(player.getUniqueId()) || Util.stringsEqual(getName(), player.getName(), false);
        } else if (obj instanceof OfflinePlayer offlinePlayer) {
            return Objects.equals(getPlayer().getUniqueId(), offlinePlayer.getUniqueId()) || Util.stringsEqual(offlinePlayer.getName(), getName(), false);
        } else if (obj instanceof AnimalTamer animalTamer) {
            return Util.stringsEqual(animalTamer.getName(), getName(), false);
        } else if (obj instanceof MyPetPlayerImpl) {
            return this == obj;
        }
        return MyPetApi.getPlatformHelper().comparePlayerWithEntity(this, obj);
    }

    @Override
    public String toString() {
        return "MyPetPlayer{name=" + getName() + ", internal-uuid=" + internalUUID + ", mojang-uuid=" + mojangUUID + "}";
    }
}