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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.util.translation.PetDefaultNameResolver;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.skill.PetExperience;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.api.event.PetCreateEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.types.LeashEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import de.Keyle.MyPet.api.util.hooks.types.PetModelSourceHook;
import de.Keyle.MyPet.entity.model.PetModelService;
import de.Keyle.MyPet.entity.types.ModelPet;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

public class EntityListener implements Listener {

    Map<UUID, ItemStack> usedItems = new ConcurrentHashMap<>();
    Set<UUID> justLeashed = ConcurrentHashMap.newKeySet();

    @EventHandler(ignoreCancelled = true)
    public void on(CreatureSpawnEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getLocation().getWorld()).isDisabled()) {
            return;
        }
        if (!MyPetGlobal.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.get().isEmpty()) {
            // PDC instead of server metadata: the metadata store never evicts
            // entries for despawned entities, so it grew per spawn forever.
            event.getEntity().getPersistentDataContainer().set(
                    SPAWN_REASON_KEY, PersistentDataType.STRING, event.getSpawnReason().name());
        }
    }

    private static final NamespacedKey SPAWN_REASON_KEY = new NamespacedKey("mypet", "spawn_reason");

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerInteractEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetGlobal.Misc.ALLOW_RANGED_LEASHING.get()) {
            // Snapshot the held item for any right-click: the leash item is
            // admin-configurable to any material and custom-plugin items can
            // launch projectiles, so a material whitelist would silently break
            // ranged leashing for those. The ProjectileLaunchEvent consumer
            // only tags genuine non-arrow player projectiles.
            if (event.useItemInHand() != Event.Result.DENY && event.getItem() != null) {
                UUID playerId = event.getPlayer().getUniqueId();
                usedItems.put(playerId, event.getItem().clone());
                // Retired callback also removes — without it the entry leaks
                // when the player disconnects the same tick.
                event.getPlayer().getScheduler().runDelayed(MyPetApi.getPlugin(),
                        t -> usedItems.remove(playerId), () -> usedItems.remove(playerId), 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityShootBowEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetGlobal.Misc.ALLOW_RANGED_LEASHING.get()) {
            if (event.getEntity() instanceof Player player) {
                if (event.getProjectile() instanceof Arrow projectile) {
                    PlayerInventory inventory = player.getInventory();

                    if (event.getBow() != null) {
                        projectile.setMetadata("MyPetLeashItem", new FixedMetadataValue(MyPetApi.getPlugin(), event.getBow().clone()));
                    }

                    ItemStack arrow = null;
                    arrow = switch (inventory.getItemInOffHand().getType()) {
                        case ARROW, TIPPED_ARROW, SPECTRAL_ARROW -> inventory.getItemInOffHand();
                        default -> arrow;
                    };
                    arrow = switch (inventory.getItemInMainHand().getType()) {
                        case ARROW, TIPPED_ARROW, SPECTRAL_ARROW -> inventory.getItemInMainHand();
                        default -> arrow;
                    };
                    if (arrow == null) {
                        int firstArrow = -1;
                        int normalArrow = inventory.first(Material.ARROW);
                        if (normalArrow != -1) {
                            arrow = inventory.getItem(inventory.first(Material.ARROW));
                            firstArrow = normalArrow;
                        }
                        int tippedFirst = inventory.first(Material.TIPPED_ARROW);
                        if (tippedFirst != -1 && firstArrow > tippedFirst) {
                            arrow = inventory.getItem(inventory.first(Material.TIPPED_ARROW));
                            firstArrow = tippedFirst;
                        }
                        int spectralFirst = inventory.first(Material.SPECTRAL_ARROW);
                        if (spectralFirst != -1 && firstArrow > spectralFirst) {
                            arrow = inventory.getItem(inventory.first(Material.SPECTRAL_ARROW));
                        }
                    }
                    if (arrow != null) {
                        projectile.setMetadata("MyPetLeashItemArrow", new FixedMetadataValue(MyPetApi.getPlugin(), arrow.clone()));
                    }

                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(ProjectileLaunchEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player player && !(projectile instanceof Arrow)) {
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                return;
            }
            // hasPet(): a leash throw is only tagged when the thrower has NOTHING out,
            // so this is a presence check, not a per-pet one.
            if (!MyPetApi.getPlayerManager().isMyPetPlayer(player) || !MyPetApi.getPlayerManager().getMyPetPlayer(player).hasPet()) {
                ItemStack leashItem = usedItems.get(player.getUniqueId());
                if (leashItem != null) {
                    projectile.setMetadata("MyPetLeashItem", new FixedMetadataValue(MyPetApi.getPlugin(), leashItem));
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final EntityDamageByEntityEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (!event.getEntity().isDead() && !(PetEntityMarker.isMarked(event.getEntity()))) {
            if (MyPetApi.getPetInfo().isLeashableEntityType(event.getEntity().getType())) {
                ItemStack leashItem = null;
                ItemStack leashItemArrow = null;
                Player player;
                if (MyPetGlobal.Misc.ALLOW_RANGED_LEASHING.get() && event.getDamager() instanceof Projectile projectile) {
                    if (!(projectile.getShooter() instanceof Player)) {
                        return;
                    }
                    player = (Player) projectile.getShooter();

                    List<MetadataValue> metaList;
                    if (projectile.hasMetadata("MyPetLeashItem")) {
                        metaList = projectile.getMetadata("MyPetLeashItem");
                        for (MetadataValue meta : metaList) {
                            if (meta.getOwningPlugin().getName().equals("MyPet")) {
                                leashItem = (ItemStack) meta.value();
                                break;
                            }
                        }
                        if (leashItem == null) {
                            return;
                        }
                        projectile.removeMetadata("MyPetLeashItem", MyPetApi.getPlugin());
                    }
                    if (projectile.hasMetadata("MyPetLeashItemArrow")) {
                        metaList = projectile.getMetadata("MyPetLeashItemArrow");
                        for (MetadataValue meta : metaList) {
                            if (meta.getOwningPlugin().getName().equals("MyPet")) {
                                leashItemArrow = (ItemStack) meta.value();
                                break;
                            }
                        }
                        if (leashItemArrow == null) {
                            return;
                        }
                        projectile.removeMetadata("MyPetLeashItemArrow", MyPetApi.getPlugin());
                    }
                } else if (event.getDamager() instanceof Player) {
                    player = (Player) event.getDamager();
                    leashItem = player.getEquipment().getItemInMainHand();
                } else {
                    return;
                }

                if (!getPetManager().hasActivePet(player) && !justLeashed.contains(player.getUniqueId())) {
                    LivingEntity leashTarget = (LivingEntity) event.getEntity();

                    PetType petType = resolveSourcePetType(leashTarget);
                    if (petType == null) {
                        petType = PetType.byEntityTypeName(leashTarget.getType().name());
                    }
                    ConfigItem neededLeashItem = MyPetApi.getPetInfo().getLeashItem(petType);

                    // Item compare first: it rejects most hits (normal combat
                    // with a non-leash item) far cheaper than a permission query.
                    boolean usedArrow = false;
                    if (!neededLeashItem.compare(leashItem)) {
                        if (leashItemArrow == null || !neededLeashItem.compare(leashItemArrow)) {
                            return;
                        } else {
                            usedArrow = true;
                        }
                    }
                    if (!Permissions.has(player, "MyPet.leash." + petType.name())) {
                        return;
                    }
                    for (LeashHook hook : MyPetApi.getServiceManager().getServices(LeashHook.class)) {
                        if (!hook.canLeash(player, leashTarget)) {
                            return;
                        }
                    }

                    boolean willBeLeashed = true;

                    for (Settings flagSettings : MyPetApi.getPetInfo().getLeashFlagSettings(petType)) {
                        String flagName = flagSettings.getName();
                        LeashFlag flag = MyPetApi.getLeashFlagManager().getLeashFlag(flagName);
                        if (flag == null) {
                            MyPetApi.getLogger().warning("\"" + flagName + "\" is not a valid leash requirement!");
                            continue;
                        }
                        MyPetPlayer myPetPlayer = null;
                        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                            myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
                        }
                        if (!flag.check(player, leashTarget, event.getDamage(), flagSettings)) {
                            willBeLeashed = false;
                            if (myPetPlayer != null) {
                                if (myPetPlayer.isCaptureHelperActive()) {
                                    Component message = flag.getMissingMessage(player, leashTarget, event.getDamage(), flagSettings);
                                    if (message != null) {
                                        myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(false).append(message), 10000);
                                    }
                                }
                            }
                        } else {
                            if (myPetPlayer != null) {
                                if (myPetPlayer.isCaptureHelperActive()) {
                                    Component message = flag.getMissingMessage(player, leashTarget, event.getDamage(), flagSettings);
                                    if (message != null) {
                                        myPetPlayer.sendMessage(LeashFlag.getComponentPrefix(true).append(message), 10000);
                                    }
                                }
                            }
                        }
                    }

                    if (willBeLeashed) {
                        event.setCancelled(true);

                        final MyPetPlayer owner;
                        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                            owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
                        } else {
                            owner = MyPetApi.getPlayerManager().registerMyPetPlayer(player);
                        }

                        WorldGroup worldGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
                        // Snapshot the wild mob's visual state into the pet's info tag for DB persistence.
                        // The mob itself is kept in-place (not destroyed) — its visual state is already correct.
                        CompoundBinaryTag snapshot = PetEntitySnapshot.capture((Mob) leashTarget);

                        final PersistedPet inactivePet = PersistedPet.builder(owner)
                                .petType(petType)
                                .petName(PetDefaultNameResolver.resolve(petType, owner))
                                .worldGroup(worldGroup.getName())
                                .info(snapshot)
                                .build();
                        inactivePet.getOwner().setPetForWorldGroup(worldGroup, inactivePet.getUUID());

                        // Store reference to the original mob so the activation callback can
                        // convert it in-place rather than destroying + re-spawning.
                        final Mob capturedMob = (Mob) leashTarget;

                        boolean remove = false; // Keep the original mob — converted in-place below
                        for (LeashEntityHook hook : MyPetApi.getServiceManager().getServices(LeashEntityHook.class)) {
                            if (!hook.prepare(leashTarget)) {
                                remove = false;
                            }
                        }

                        if (!usedArrow) {
                            if (MyPetGlobal.Misc.CONSUME_LEASH_ITEM.get() && player.getGameMode() != GameMode.CREATIVE && leashItem != null) {
                                if (leashItem.getAmount() > 1) {
                                    leashItem.setAmount(leashItem.getAmount() - 1);
                                } else {
                                    player.getEquipment().setItemInMainHand(null);
                                }
                            }
                        }

                        PetCreateEvent createEvent = new PetCreateEvent(inactivePet, PetCreateEvent.Source.LEASH);
                        Bukkit.getServer().getPluginManager().callEvent(createEvent);

                        PetSaveEvent saveEvent = new PetSaveEvent(inactivePet);
                        Bukkit.getServer().getPluginManager().callEvent(saveEvent);

                        justLeashed.add(player.getUniqueId());
                        MyPetPlugin.getInstance().getRepository().addPet(inactivePet).thenAccept(value -> {
                            player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                                owner.sendMessage(Locale.getComponent("Message.Leash.Add", owner));

                                Optional<Pet> activePet = getPetManager().activatePet(inactivePet);
                                activePet.ifPresent(pet -> {
                                    // In-place conversion: the original wild mob is still alive.
                                    // Wire it into the activated Pet without destroying + re-spawning.
                                    new VanillaMobSpawner().convertInPlace(pet, capturedMob);
                                    // Use updateStatus (direct field write) instead of setStatus —
                                    // setStatus(Here) from Despawned would call createEntity() and
                                    // spawn a DUPLICATE entity via VanillaMobSpawner.spawn().
                                    ((PetImpl) pet).updateStatus(Pet.PetState.Here);
                                    // Runs last: setEquipment's live-entity write path is only
                                    // active once the pet is Here, so the domain model and the
                                    // mob cannot diverge.
                                    applyTameEquipmentPolicy(pet, capturedMob);
                                });
                                if (owner.isCaptureHelperActive()) {
                                    owner.setCaptureHelperActive(false);
                                    owner.sendMessage(Locale.getFormattedComponent("Message.Command.CaptureHelper.Mode", owner, Locale.getComponent("Name.Disabled", owner)));
                                }
                                justLeashed.remove(player.getUniqueId());
                            }, null);
                        }).exceptionally(err -> {
                            MyPetApi.getLogger().warning("Failed to save captured pet: " + err);
                            return null;
                        });
                    }
                }
            }
        }
    }

    /**
     * Applies {@code MyPet.Pets.<Type>.RetainEquipmentOnTame} to a mob that was just
     * converted in place by a leash tame.
     * <p>
     * Taming never destroys the wild mob, so its armor and weapons are still on the
     * entity at this point — but MyPet's own equipment model knows nothing about them.
     * With the flag on (the default) each allowed slot is imported through
     * {@link PetEquipment#setEquipment}, which leaves the gear exactly where it is and
     * additionally makes it real pet equipment, so it drops on death and release. With
     * the flag off the same slots are cleared on the mob and the items are dropped at
     * its feet instead, and nothing enters the domain model.
     * <p>
     * Slots outside {@link PetEquipment#getAllowedSlotNames()} are left untouched either
     * way, and vanilla per-slot drop chances are deliberately ignored: whatever is
     * visible on the mob is what taming acts on.
     * <p>
     * This lives here rather than in {@code VanillaMobSpawner.convertInPlace} on purpose —
     * source-driven adoptions (MythicMobs and friends) carry gear authored by the source
     * plugin and must keep it regardless of the flag.
     */
    private static void applyTameEquipmentPolicy(Pet pet, Mob mob) {
        if (!(pet instanceof PetEquipment equipmentPet)) {
            return;
        }
        EntityEquipment mobEquipment = mob.getEquipment();
        if (mobEquipment == null) {
            return;
        }
        boolean retain = equipmentPet.retainEquipmentOnTame();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!equipmentPet.canUseSlot(slot)) {
                continue;
            }
            ItemStack item = mobEquipment.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (retain) {
                equipmentPet.setEquipment(slot, item);
            } else {
                ItemStack dropped = item.clone();
                mobEquipment.setItem(slot, null);
                mob.getWorld().dropItem(mob.getLocation(), dropped);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitor(final EntityDamageByEntityEvent event) {
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        Entity target = event.getEntity();
        if (WorldGroup.getGroupByWorld(target.getWorld()).isDisabled()) {
            return;
        }

        if (target instanceof LivingEntity) {
            Entity source = event.getDamager();

            // Damage-weighted XP attribution is recorded by PetXpAttributionListener.

            if (source instanceof Projectile) {
                ProjectileSource projectileSource = ((Projectile) source).getShooter();
                if (projectileSource instanceof Entity) {
                    source = (Entity) projectileSource;
                }
            }

            if (source instanceof Player player) {
                if (event.getDamage() == 0) {
                    return;
                } else if (PetEntityMarker.isMarked(target)) {
                    if (MyPetApi.getPetInfo().getLeashItem(getPetManager().getPetFromEntity(target).getPetType()).compare(player.getInventory().getItemInMainHand())) {
                        return;
                    }
                }
                if (source != target) {
                    if (target instanceof Tameable && source.equals(((Tameable) target).getOwner())) {
                        return;
                    }
                    // Every Pet the owner has out retaliates, not just the primary one.
                    for (Pet pet : getPetManager().getPets(player)) {
                        if (pet.getStatus() == PetState.Here) {
                            Mob entity = pet.getBukkitEntity();
                            if (entity != null && target != entity) {
                                if (pet.getDamage() > 0 || pet.getRangedDamage() > 0) {
                                    pet.setTarget((LivingEntity) target, TargetPriority.OwnerHurts);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(final EntityDeathEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        LivingEntity deadEntity = event.getEntity();
        if (PetEntityMarker.isMarked(deadEntity)) {
            return;
        }
        if (WorldGroup.getGroupByWorld(deadEntity.getWorld()).isDisabled()) {
            return;
        }
        if (MyPetGlobal.LevelSystem.Experience.DISABLED_WORLDS.get().contains(deadEntity.getWorld().getName())) {
            return;
        }
        if (!MyPetGlobal.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.get().isEmpty()) {
            String spawnReason = deadEntity.getPersistentDataContainer()
                    .get(SPAWN_REASON_KEY, PersistentDataType.STRING);
            if (spawnReason != null
                    && MyPetGlobal.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.get().contains(spawnReason)) {
                return;
            }
        }
        if (MyPetGlobal.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION.get()) {
            Map<UUID, Double> damagePercentMap = PetExperience.getDamageToEntityPercent(deadEntity);
            for (UUID entityUUID : damagePercentMap.keySet()) {
                Entity entity = Bukkit.getEntity(entityUUID);
                if (PetEntityMarker.isMarked(entity)) {
                    Pet pet = getPetManager().getPetFromEntity(entity);
                    if (MyPetGlobal.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE.get() && pet.getSkilltree() == null) {
                        if (!pet.autoAssignSkilltree()) {
                            continue;
                        }
                    }
                    if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                        double randomExp = MonsterExperience.getMonsterExperience(deadEntity).getRandomExp();
                        pet.getExperience().addExp(damagePercentMap.get(entity.getUniqueId()) * randomExp, true);
                    }
                } else if (entity instanceof Player owner) {
                    // Credit every Pet the owner has out. `continue` now skips the Pet
                    // that cannot be levelled rather than abandoning the whole damager.
                    for (Pet pet : getPetManager().getPets(owner)) {
                        if (MyPetGlobal.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE.get() && pet.getSkilltree() == null) {
                            if (!pet.autoAssignSkilltree()) {
                                continue;
                            }
                        }
                        if (pet.isPassive() || MyPetGlobal.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP.get()) {
                            if (pet.getStatus() == PetState.Here) {
                                if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                    int percentage = (int) (MyPetGlobal.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER.get() * damagePercentMap.get(entity.getUniqueId()));
                                    pet.getExperience().addExp(deadEntity, percentage, true);
                                }
                            }
                        }
                    }
                } else if (entity instanceof Tameable tameable) {
                    if (tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner() instanceof Player owner) {
                        for (Pet pet : getPetManager().getPets(owner)) {
                            if (MyPetGlobal.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE.get() && pet.getSkilltree() == null) {
                                continue;
                            }
                            if (pet.isPassive() || MyPetGlobal.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP.get()) {
                                if (pet.getStatus() == PetState.Here) {
                                    if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                        int percentage = (int) (MyPetGlobal.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER.get() * damagePercentMap.get(entity.getUniqueId()));
                                        pet.getExperience().addExp(deadEntity, percentage, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            PetExperience.clearDamageMap(deadEntity);
        } else if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent edbee) {

            Entity damager = edbee.getDamager();
            if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Entity) {
                damager = (Entity) ((Projectile) damager).getShooter();
            }
            if (PetEntityMarker.isMarked(damager)) {
                Pet pet = getPetManager().getPetFromEntity(damager);
                if (pet.getSkilltree() == null && MyPetGlobal.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE.get()) {
                    if (!pet.autoAssignSkilltree()) {
                        return;
                    }
                }
                pet.getExperience().addExp(edbee.getEntity(), true);
            } else if (damager instanceof Player owner) {
                for (Pet pet : getPetManager().getPets(owner)) {
                    if (MyPetGlobal.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE.get() && pet.getSkilltree() == null) {
                        if (!pet.autoAssignSkilltree()) {
                            continue;
                        }
                    }
                    if (pet.isPassive() || MyPetGlobal.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP.get()) {
                        if (pet.getStatus() == PetState.Here) {
                            if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                pet.getExperience().addExp(deadEntity, MyPetGlobal.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER.get(), true);
                            }
                        }
                    }
                }
            } else if (damager instanceof Tameable tameable) {
                if (tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner() instanceof Player owner) {
                    for (Pet pet : getPetManager().getPets(owner)) {
                        if (MyPetGlobal.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE.get() && pet.getSkilltree() == null) {
                            continue;
                        }
                        if (pet.isPassive() || MyPetGlobal.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP.get()) {
                            if (pet.getStatus() == PetState.Here) {
                                if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                    pet.getExperience().addExp(deadEntity, MyPetGlobal.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER.get(), true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(final EntityTargetEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (PetEntityMarker.isMarked(event.getEntity())) {
            Pet pet = getPetManager().getPetFromEntity(event.getEntity());
            if (pet.getSkills().isActive(Behavior.class)) {
                Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
                if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
                    event.setCancelled(true);
                } else if (event.getTarget() instanceof Player && event.getTarget().getName().equals(pet.getOwner().getName())) {
                    event.setCancelled(true);
                } else if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                    if (event.getTarget() instanceof Player) {
                        event.setCancelled(true);
                    } else if (event.getTarget() instanceof Tameable && ((Tameable) event.getTarget()).isTamed()) {
                        event.setCancelled(true);
                    } else if (PetEntityMarker.isMarked(event.getTarget())) {
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getEntity() instanceof Tameable tameable) {
            if (PetEntityMarker.isMarked(event.getTarget())) {
                Pet pet = getPetManager().getPetFromEntity(event.getTarget());
                if (pet.getOwner().equals(tameable.getOwner())) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getEntity() instanceof IronGolem) {
            if (PetEntityMarker.isMarked(event.getTarget())) {
                if (event.getReason() == TargetReason.RANDOM_TARGET) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private static PetType resolveSourcePetType(LivingEntity target) {
        for (PetModelSourceHook src : MyPetApi.getServiceManager().getServices(PetModelSourceHook.class)) {
            Optional<String> id = src.sourceIdOf(target);
            if (id.isEmpty()) {
                continue;
            }
            String raw = id.get();
            // 1. A source-driven type named after the id (methods 6-9: MythicMob name / IA id / model id).
            String normalized = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
            for (String candidate : new String[]{raw, normalized, raw.replace(':', '_')}) {
                PetType t = PetType.byNameOrNull(candidate);
                if (t != null && t.getPetClass() == ModelPet.class) {
                    return t;
                }
            }
            // 2. A custom creature whose Model.Provider+Id match this detecting hook + id (rendered
            //    creature, /meg summon mob, or a wild MythicMob whose internal name is the Model.Id).
            //    Matching the provider (the hook that detected it) keeps a same-id model on another
            //    provider from cross-resolving.
            PetType byModel = PetModelService.typeForModel(src.getServiceName(), raw);
            if (byModel != null) {
                return byModel;
            }
        }
        return null;
    }
}