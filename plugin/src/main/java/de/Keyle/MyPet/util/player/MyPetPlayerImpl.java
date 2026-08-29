/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.PetType;
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
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Creaking;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyPetPlayerImpl implements MyPetPlayer {

    protected String lastLanguage = "en_US";
    protected final UUID mojangUUID;

    protected boolean captureHelperMode = false;
    protected int captureHelperTimer = 90;
    private int creakingSweepCounter = 0;
    protected boolean autoRespawn = false;
    /**
     * Scheduler passes (one per second) the auto-recall in {@link #schedule()} must wait
     * before retrying a spawn that failed. createEntity() clears wantsToRespawn only on
     * success, so without this back-off a pet the owner has no room for was re-attempted --
     * and a PetCallEvent fired -- every single second for as long as the owner stood there.
     */
    private int respawnRetryDelay = 0;
    /** Seconds to wait after a failed auto-recall spawn before trying again. */
    private static final int FAILED_RESPAWN_RETRY_DELAY = 5;
    protected boolean showHealthBar = false;
    protected int autoRespawnMin = 1;
    protected volatile float petVolume = 1f;

    protected final ListMultimap<String, UUID> petWorldUUID = ArrayListMultimap.create();
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
        } else return petVolume < 1f;
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

    public float getPetVolume() {
        return petVolume;
    }

    public void setPetVolume(float volume) {
        petVolume = Math.min(Math.max(volume, 0), 1f);
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

    public void setPetForWorldGroup(String worldGroup, UUID petUUID) {
        if (worldGroup == null || worldGroup.isEmpty()) {
            return;
        }
        if (petUUID == null) {
            petWorldUUID.removeAll(worldGroup);
            return;
        }
        // Replace, don't append. The BiMap this used to be enforced two things that
        // ListMultimap does not, and both have to be restored by hand:
        //
        //  1. Key uniqueness -- BiMap.put replaced the group's binding, Multimap.put
        //     appends. /petswitch and friends rebind a group without clearing it, so
        //     appending would leave the stale pet at index 0, where getPetForWorldGroup
        //     reads it, and grow the persisted list on every switch.
        //  2. Value uniqueness -- BiMap rejected binding one pet to a second group
        //     (the old catch (IllegalArgumentException) swallowed exactly that).
        //     Without the unbind below, a pet moved between groups stays listed under
        //     both, and getWorldGroupForPet then answers with whichever key iterates
        //     first.
        //
        // Phase 2 needs a separate "bind an additional pet to this group" path; it must
        // relax (1) while keeping (2).
        unbindPetFromAllWorldGroups(petUUID);
        petWorldUUID.replaceValues(worldGroup, List.of(petUUID));
    }

    public void setPetForWorldGroup(WorldGroup worldGroup, UUID petUUID) {
        if (worldGroup == null) {
            return;
        }
        setPetForWorldGroup(worldGroup.getName(), petUUID);
    }

    /** Drops every binding for this pet, so it can only ever be bound to one world group. */
    private void unbindPetFromAllWorldGroups(UUID petUUID) {
        petWorldUUID.entries().removeIf(entry -> entry.getValue().equals(petUUID));
    }

    public UUID getPetForWorldGroup(String worldGroup) {
        List<UUID> pets = petWorldUUID.get(worldGroup);
        return pets.isEmpty() ? null : pets.get(0);
    }

    public UUID getPetForWorldGroup(WorldGroup worldGroup) {
        return getPetForWorldGroup(worldGroup.getName());
    }

    public List<UUID> getPetsForWorldGroup(String worldGroup) {
        // Snapshot, not a view — callers may rebind the group while iterating.
        return List.copyOf(petWorldUUID.get(worldGroup));
    }

    public List<UUID> getPetsForWorldGroup(WorldGroup worldGroup) {
        return getPetsForWorldGroup(worldGroup.getName());
    }

    public Multimap<String, UUID> getWorldGroupBindings() {
        // Snapshot, not a view: this is read on the repository's background executor
        // while the main thread can be rebinding groups.
        return ImmutableListMultimap.copyOf(petWorldUUID);
    }

    @Deprecated
    public BiMap<String, UUID> getPetsForWorldGroups() {
        // Lossy by construction -- a BiMap cannot hold two pets for one group, nor the
        // same pet under two groups. Retained only so addons compiled against the
        // pre-multi-pet API keep linking; take the first binding per group and skip any
        // pet already present, since BiMap.put would throw on a duplicate value.
        BiMap<String, UUID> primaryBindings = HashBiMap.create();
        for (String worldGroup : petWorldUUID.keySet()) {
            List<UUID> bound = petWorldUUID.get(worldGroup);
            if (!bound.isEmpty() && !primaryBindings.containsValue(bound.get(0))) {
                primaryBindings.put(worldGroup, bound.get(0));
            }
        }
        return primaryBindings;
    }

    public String getWorldGroupForPet(UUID petUUID) {
        for (Map.Entry<String, UUID> entry : petWorldUUID.entries()) {
            if (entry.getValue().equals(petUUID)) return entry.getKey();
        }
        return null;
    }

    public boolean hasPetInWorldGroup(String worldGroup) {
        return petWorldUUID.containsKey(worldGroup);
    }

    public boolean hasPetInWorldGroup(WorldGroup worldGroup) {
        return petWorldUUID.containsKey(worldGroup.getName());
    }

    public void addExtendedInfo(Plugin owner, String key, BinaryTag tag) {
        String namespace = owner.getName();
        CompoundBinaryTag bucket = extendedInfo.getCompound(namespace).put(key, tag);
        extendedInfo = extendedInfo.put(namespace, bucket);
    }

    public Optional<BinaryTag> getExtendedInfo(Plugin owner, String key) {
        CompoundBinaryTag bucket = extendedInfo.getCompound(owner.getName());
        return bucket.keySet().contains(key) ? Optional.ofNullable(bucket.get(key)) : Optional.empty();
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

    public boolean hasPet() {
        return MyPetApi.getPetManager().hasActivePet(this);
    }

    public Pet getPet() {
        return MyPetApi.getPetManager().getPet(this);
    }

    public List<Pet> getPets() {
        return MyPetApi.getPetManager().getPets(this);
    }

    public int getPetCount() {
        return MyPetApi.getPetManager().getPets(this).size();
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
                .putFloat("PetLivingSoundVolume", getPetVolume())
                .build();

        CompoundBinaryTag.Builder uuidBuilder = CompoundBinaryTag.builder();
        uuidBuilder.putString("Mojang-UUID", mojangUUID.toString());

        CompoundBinaryTag.Builder multiWorldBuilder = CompoundBinaryTag.builder();
        for (String worldGroupName : petWorldUUID.keySet()) {
            List<StringBinaryTag> petTags = new ArrayList<>();
            for (UUID petUUID : petWorldUUID.get(worldGroupName)) {
                petTags.add(StringBinaryTag.stringBinaryTag(petUUID.toString()));
            }
            multiWorldBuilder.put(worldGroupName, ListBinaryTag.from(petTags));
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
                setPetVolume(settingsTag.getFloat("PetLivingSoundVolume"));
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
                setPetVolume(myplayerNBT.getFloat("PetLivingSoundVolume"));
            }
        }
        if (myplayerNBT.keySet().contains("ExtendedInfo")) {
            setExtendedInfo(myplayerNBT.getCompound("ExtendedInfo"));
        }
        if (myplayerNBT.keySet().contains("MultiWorld")) {
            CompoundBinaryTag worldGroups = myplayerNBT.getCompound("MultiWorld");
            for (String worldGroupName : worldGroups.keySet()) {
                BinaryTag tag = worldGroups.get(worldGroupName);
                if (tag instanceof ListBinaryTag list) {
                    for (BinaryTag petUUID : list) {
                        setPetForWorldGroup(worldGroupName, UUID.fromString(((StringBinaryTag) petUUID).value()));
                    }
                } else if (tag instanceof StringBinaryTag single) {
                    // Legacy scalar form.
                    setPetForWorldGroup(worldGroupName, UUID.fromString(single.value()));
                }
            }
        }
    }

    public void schedule() {
        if (!isOnline()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        sentMessages.keySet().removeIf(message -> currentTime >= sentMessages.get(message));
        if (hasPet()) {
            Pet pet = getPet();
            Player p = this.getPlayer();
            // Use cached status: the pet entity may be in a different Folia region, so touching
            // it (including health checks inside getStatus()) from the player's tick is unsafe.
            PetState cachedStatus = pet.getCachedStatus();
            if (cachedStatus == PetState.Here) {
                Optional<Location> petLocOpt = pet.getLocation();
                if (petLocOpt.isPresent()) {
                    Location petLoc = petLocOpt.get();
                    boolean tooFar = petLoc.getWorld() != p.getLocation().getWorld()
                            || petLoc.distance(p.getLocation()) > 40;
                    if (tooFar) {
                        Mob petMob = pet.getBukkitEntity();
                        if (petMob != null) {
                            petMob.getScheduler().run(MyPetApi.getPlugin(), t -> {
                                pet.removePet(MyPetGlobal.Misc.RECALL_PET_AFTER_DESPAWN.get());
                                if (!p.isGliding()) {
                                    pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Despawn", pet.getOwner(), pet.getDisplayName()));
                                }
                            }, null);
                        }
                    } else if (!MyPetGlobal.Misc.DISABLE_ALL_ACTIONBAR_MESSAGES.get() && showHealthBar) {
                        // Dispatch to the pet's scheduler to safely read health; sendActionBar is thread-safe.
                        Mob petMob = pet.getBukkitEntity();
                        if (petMob != null) {
                            petMob.getScheduler().run(MyPetApi.getPlugin(), t -> {
                                Component msg = buildPetHealthActionBar(pet, pet.getHealth(), pet.getMaxHealth());
                                p.sendActionBar(msg);
                            }, null);
                        }
                    }
                }
            } else if (cachedStatus == PetState.Dead) {
                pet.tickRespawnTimer();
            } else if (cachedStatus == PetState.Despawned) {
                if (respawnRetryDelay > 0) {
                    respawnRetryDelay--;
                } else if (pet.wantsToRespawn() && !p.isFlying()) {
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

                        if (spawn) {
                            if (pet.createEntity() == Pet.SpawnFlags.Success) {
                                p.sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", p, pet.getDisplayName()));
                            } else {
                                // NoSpace / NotAllowed / ... : back off instead of hammering
                                // the same failing spawn (and PetCallEvent) once a second.
                                respawnRetryDelay = FAILED_RESPAWN_RETRY_DELAY;
                            }
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
                    if (MyPetApi.getPetInfo().isLeashableEntityType(entity.getType())) {
                        for (LeashHook hook : MyPetApi.getServiceManager().getServices(LeashHook.class)) {
                            if (!hook.canLeash(p, entity)) {
                                continue entityLoop;
                            }
                        }
                        if (!MyPetApi.getHookHelper().canHurt(p, entity)) {
                            continue;
                        }
                        if (!Permissions.has(this, "MyPet.leash." + PetType.byEntityTypeName(entity.getType().name()))) {
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

            // Show particles above Creaking Heart blocks for heart-linked Creakings.
            // Only every 3rd second — the wide-radius sweep is the expensive part of this task.
            if (CompatUtil.minecraftVersionEqualsOrAbove("1.21.4") && ++creakingSweepCounter % 3 == 0) {
                CreakingSweep.run(this, p);
            }
        }
    }

    protected boolean checkTamable(LivingEntity leashTarget, Player p) {
        for (Settings flagSettings : MyPetApi.getPetInfo().getLeashFlagSettings(PetType.byEntityTypeName(leashTarget.getType().name()))) {
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

    /**
     * Nested holder so the {@link Creaking} type reference lives in bytecode that is only
     * loaded when the 1.21.4+ runtime gate actually invokes it.
     */
    private static final class CreakingSweep {

        private static void run(MyPetPlayerImpl owner, Player p) {
            // Search in a wider radius since Creakings can wander far from their heart
            Set<Location> shownHeartLocations = new HashSet<>();
            for (Creaking entity : p.getWorld().getNearbyEntitiesByType(Creaking.class, p.getLocation(), 32)) {
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
                if (!Permissions.has(owner, "MyPet.leash.Creaking")) {
                    continue;
                }

                boolean canLeash = true;
                for (LeashHook hook : MyPetApi.getServiceManager().getServices(LeashHook.class)) {
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
                if (owner.checkTamable(entity, p)) {
                    p.spawnParticle(Particle.ITEM, particleLoc, 20, 0, 0, 0, 0.02f, new ItemStack(Material.LIME_DYE));
                } else {
                    p.spawnParticle(Particle.ITEM, particleLoc, 20, 0, 0, 0, 0.02f, new ItemStack(Material.RED_DYE));
                }
            }
        }
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

    private static Component buildPetHealthActionBar(Pet pet, double health, double maxHealth) {
        if (pet == null) {
            return Component.empty();
        }
        double deltaHealth = maxHealth - health;

        NamedTextColor healthColor = NamedTextColor.RED;
        if (health > maxHealth / 3 * 2) {
            healthColor = NamedTextColor.GREEN;
        } else if (health > maxHealth / 3) {
            healthColor = NamedTextColor.YELLOW;
        }
        Component parsed = pet.getDisplayName()
                .append(MyPetApi.getPlugin().getMiniMessage().deserialize("<reset>: "));
        if (health > 0) {
            parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                    "<healthcolor><health><white>/<maxhealth> ",
                    Placeholder.styling("healthcolor", healthColor),
                    Placeholder.unparsed("health", String.format("%1.2f", health)),
                    Placeholder.unparsed("maxhealth", String.format("%1.2f", maxHealth))));
            if (!pet.getOwner().isHealthBarActive()) {
                parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                        "(<deltahealthcolor><deltahealth><reset>)",
                        Placeholder.parsed("deltahealthcolor", deltaHealth < 0 ? "<green>+" : "<red>-"),
                        Placeholder.unparsed("deltahealth", String.format("%1.2f", deltaHealth))));
            }
        } else {
            parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                    "<dead>",
                    Placeholder.component("dead", Locale.getComponent("Name.Dead", pet.getOwner()))));
        }
        return parsed;
    }

    @Override
    public String toString() {
        return "MyPetPlayer{name=" + getName() + ", uuid=" + mojangUUID + "}";
    }
}
