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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.services.CreakingService;
import de.Keyle.MyPet.util.CompatUtil;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyPetPlayerImpl implements MyPetPlayer {

    protected String lastLanguage = "en_US";
    protected final UUID mojangUUID;

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

    private volatile ContributorCheck.ContributorRank rank = ContributorCheck.ContributorRank.None;
    private boolean contributorChecked = false;

    public MyPetPlayerImpl(UUID mojangUUID) {
        this.mojangUUID = mojangUUID;
    }

    public String getName() {
        Player player = getPlayer();
        if (player != null) {
            return player.getName();
        }
        return Bukkit.getOfflinePlayer(mojangUUID).getName();
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

    public UUID getUniqueId() {
        return mojangUUID;
    }

    public String getLanguage() {
        if (isOnline()) {
            lastLanguage = Locale.getPlayerLanguage(getPlayer());
        }
        return lastLanguage;
    }

    public boolean isMyPetAdmin() {
        return isOnline() && Permissions.has(getPlayer(), "MyPet.admin");
    }

    public boolean hasMyPet() {
        return MyPetApi.getMyPetManager().hasActiveMyPet(this);
    }

    public MyPet getMyPet() {
        return MyPetApi.getMyPetManager().getMyPet(this);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(getUniqueId());
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

    public ContributorCheck.ContributorRank getContributorRank() {
        return rank;
    }

    public void checkForContribution() {
        if (!contributorChecked) {
            contributorChecked = true;
            Bukkit.getServer().getAsyncScheduler().runDelayed(MyPetApi.getPlugin(), t -> rank = ContributorCheck.getContributorRank(MyPetPlayerImpl.this), 3L, TimeUnit.SECONDS);
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
        uuidBuilder.putString("Mojang-UUID", mojangUUID.toString());

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
            // Use cached status: the pet entity may be in a different Folia region, so touching
            // it (including health checks inside getStatus()) from the player's tick is unsafe.
            PetState cachedStatus = myPet.getCachedStatus();
            if (cachedStatus == PetState.Here) {
                Optional<Location> petLocOpt = myPet.getLocation();
                if (petLocOpt.isPresent()) {
                    Location petLoc = petLocOpt.get();
                    boolean tooFar = petLoc.getWorld() != p.getLocation().getWorld()
                            || petLoc.distance(p.getLocation()) > 40;
                    if (tooFar) {
                        Mob petMob = myPet.getBukkitEntity();
                        if (petMob != null) {
                            petMob.getScheduler().run(MyPetApi.getPlugin(), t -> {
                                myPet.removePet(Configuration.Misc.RECALL_PET_AFTER_DESPAWN);
                                if (!p.isGliding()) {
                                    myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Despawn", myPet.getOwner(), myPet.getDisplayName()));
                                }
                            }, null);
                        }
                    } else if (!Configuration.Misc.DISABLE_ALL_ACTIONBAR_MESSAGES && showHealthBar) {
                        // Dispatch to the pet's scheduler to safely read health; sendActionBar is thread-safe.
                        Mob petMob = myPet.getBukkitEntity();
                        if (petMob != null) {
                            petMob.getScheduler().run(MyPetApi.getPlugin(), t -> {
                                Component msg = buildPetHealthActionBar(myPet, myPet.getHealth(), myPet.getMaxHealth());
                                p.sendActionBar(msg);
                            }, null);
                        }
                    }
                }
            } else if (cachedStatus == PetState.Dead) {
                myPet.tickRespawnTimer();
            } else if (cachedStatus == PetState.Despawned) {
                if (myPet.wantsToRespawn() && !p.isFlying()) {
                    boolean velocity = p.getVelocity().getY() >= 0;
                    boolean fall = p.getFallDistance() == 0;

                    if (velocity || fall || p.isOnGround()) {
                        boolean spawn = true;

                        if (velocity) {
                            spawn = !p.isInsideVehicle();
                            if (spawn) {
                                spawn = !p.isGliding();
                            }
                        }
                        if (spawn && fall) {
                            spawn = switch (p.getWorld().getBlockAt(p.getLocation().subtract(0, 0.5, 0)).getType().name()) {
                                case "AIR", "CAVE_AIR", "VOID_AIR", "WATER", "LAVA" -> false;
                                default -> true;
                            };
                        }

                        if (spawn && myPet.createEntity() == MyPet.SpawnFlags.Success) {
                            p.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", p, myPet.getDisplayName()));
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
                if (entity instanceof LivingEntity && !(entity instanceof Player) && !(PetEntityMarker.isMarked(entity))) {
                    // Skip Creakings here - they're handled separately with particles on their heart block
                    if ("CREAKING".equals(entity.getType().name())) {
                        continue;
                    }
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
                            p.spawnParticle(Particle.ITEM, l, 20, 0, 0, 0, 0.02f, new ItemStack(Material.LIME_DYE));
                        } else {
                            p.spawnParticle(Particle.ITEM, l, 20, 0, 0, 0, 0.02f, new ItemStack(Material.RED_DYE));
                        }
                        if (count++ > 20) {
                            break;
                        }
                    }
                }
            }

            // Show particles above Creaking Heart blocks for heart-linked Creakings
            if (CompatUtil.minecraftVersionEqualsOrAbove("1.21.4")) {
                // Search in a wider radius since Creakings can wander far from their heart
                List<Entity> nearbyEntities = p.getNearbyEntities(32, 32, 32);
                Set<Location> shownHeartLocations = new HashSet<>();
                for (Entity entity : nearbyEntities) {
                    if (!"CREAKING".equals(entity.getType().name())) {
                        continue;
                    }
                    if (!(entity instanceof LivingEntity)) {
                        continue;
                    }

                    Location homePos = getCreakingHome(entity);
                    if (homePos == null || homePos.getWorld() == null) {
                        continue; // Not a heart-linked Creaking (transient) or invalid location
                    }

                    // Only show particles for hearts within 16 blocks of the player (same world)
                    Location blockLoc = homePos.getBlock().getLocation();
                    if (!blockLoc.getWorld().equals(p.getWorld())) {
                        continue; // Different world
                    }
                    if (blockLoc.distanceSquared(p.getLocation()) > 256) { // 16^2 = 256
                        continue;
                    }

                    // Avoid showing duplicate particles for the same heart block
                    if (shownHeartLocations.contains(blockLoc)) {
                        continue;
                    }
                    shownHeartLocations.add(blockLoc);

                    // Check if player can leash Creaking
                    if (!Permissions.has(this, "MyPet.leash.Creaking")) {
                        continue;
                    }

                    boolean canLeash = true;
                    for (LeashHook hook : MyPetApi.getPluginHookManager().getHooks(LeashHook.class)) {
                        if (!hook.canLeash(p, entity)) {
                            canLeash = false;
                            break;
                        }
                    }
                    if (!canLeash) {
                        continue;
                    }

                    // Show particles above the Creaking Heart block (centered, 1 block above)
                    Location particleLoc = blockLoc.clone().add(0.5, 1.5, 0.5);
                    if (checkTamable((LivingEntity) entity, p)) {
                        p.spawnParticle(Particle.ITEM, particleLoc, 20, 0, 0, 0, 0.02f, new ItemStack(Material.LIME_DYE));
                    } else {
                        p.spawnParticle(Particle.ITEM, particleLoc, 20, 0, 0, 0, 0.02f, new ItemStack(Material.RED_DYE));
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

    /**
     * Gets the home location of a Creaking entity using the version-specific CreakingService.
     */
    private static Location getCreakingHome(Entity entity) {
        return MyPetApi.getServiceManager()
                .getService(CreakingService.class)
                .map(service -> service.getCreakingHome(entity))
                .orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Player player) {
            return getUniqueId().equals(player.getUniqueId());
        } else if (obj instanceof OfflinePlayer offlinePlayer) {
            return getUniqueId().equals(offlinePlayer.getUniqueId());
        } else if (obj instanceof AnimalTamer animalTamer) {
            return getUniqueId().equals(animalTamer.getUniqueId());
        } else if (obj instanceof MyPetPlayerImpl) {
            return this == obj;
        }
        return obj instanceof Player p && p.getUniqueId().equals(getUniqueId());
    }

    private static Component buildPetHealthActionBar(MyPet myPet, double health, double maxHealth) {
        if (myPet == null) {
            return Component.empty();
        }
        double deltaHealth = maxHealth - health;

        NamedTextColor healthColor = NamedTextColor.RED;
        if (health > maxHealth / 3 * 2) {
            healthColor = NamedTextColor.GREEN;
        } else if (health > maxHealth / 3) {
            healthColor = NamedTextColor.YELLOW;
        }
        Component parsed = myPet.getDisplayName()
                .append(MyPetApi.getPlugin().getMiniMessage().deserialize("<reset>: "));
        if (health > 0) {
            parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                    "<healthcolor><health><white>/<maxhealth> ",
                    Placeholder.styling("healthcolor", healthColor),
                    Placeholder.unparsed("health", String.format("%1.2f", health)),
                    Placeholder.unparsed("maxhealth", String.format("%1.2f", maxHealth))));
            if (!myPet.getOwner().isHealthBarActive()) {
                parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                        "(<deltahealthcolor><deltahealth><reset>)",
                        Placeholder.parsed("deltahealthcolor", deltaHealth < 0 ? "<green>+" : "<red>-"),
                        Placeholder.unparsed("deltahealth", String.format("%1.2f", deltaHealth))));
            }
        } else {
            parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                    "<dead>",
                    Placeholder.unparsed("dead", Locale.getString("Name.Dead", myPet.getOwner()))));
        }
        return parsed;
    }

    @Override
    public String toString() {
        return "MyPetPlayer{name=" + getName() + ", uuid=" + mojangUUID + "}";
    }
}
