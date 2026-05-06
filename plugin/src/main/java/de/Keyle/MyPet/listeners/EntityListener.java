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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.skill.PetExperience;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.types.PetEnderman;
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
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

public class EntityListener implements Listener {

    Map<UUID, ItemStack> usedItems = new HashMap<>();
    Set<UUID> justLeashed = new HashSet<>();

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
        if (!Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.isEmpty()) {
            event.getEntity().setMetadata("SpawnReason", new FixedMetadataValue(MyPetApi.getPlugin(), event.getSpawnReason().name()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerInteractEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (Configuration.Misc.ALLOW_RANGED_LEASHING) {
            if (event.useItemInHand() != Event.Result.DENY && event.getItem() != null) {
                usedItems.put(event.getPlayer().getUniqueId(), event.getItem().clone());
                event.getPlayer().getScheduler().runDelayed(MyPetApi.getPlugin(), t -> usedItems.remove(event.getPlayer().getUniqueId()), null, 1L);
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
        if (Configuration.Misc.ALLOW_RANGED_LEASHING) {
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
            if (!MyPetApi.getPlayerManager().isMyPetPlayer(player) || !MyPetApi.getPlayerManager().getMyPetPlayer(player).hasPet()) {
                ItemStack leashItem = usedItems.get(player.getUniqueId());
                if (leashItem != null) {
                    projectile.setMetadata("MyPetLeashItem", new FixedMetadataValue(MyPetApi.getPlugin(), leashItem));
                }
            }
        }
    }

    // Belt-and-suspenders for Wither pets: WitherAutonomousAttackSuppressor clears
    // the three head targets every tick so WitherBoss#customServerAiStep's fire loop
    // finds nothing to shoot at, but there is a narrow intra-tick race on scan ticks
    // (the side-head scan runs inside the same tick as the fire loop, after our
    // target-clear runs). Any skull that does slip through is silently cancelled
    // here so it never lands a hit. Skulls fired via PetRangedAttackGoal always tag
    // PROJECTILE_DAMAGE_KEY; an untagged skull from a marked pet shooter is
    // necessarily from the autonomous code path.
    @EventHandler
    public void onPetAutonomousWitherSkull(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof WitherSkull skull)) return;
        if (!(skull.getShooter() instanceof LivingEntity shooter)) return;
        if (!PetEntityMarker.isMarked(shooter)) return;
        if (skull.getPersistentDataContainer().has(PetRangedAttackGoal.PROJECTILE_DAMAGE_KEY, PersistentDataType.FLOAT)) {
            return;
        }
        event.setCancelled(true);
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
                if (Configuration.Misc.ALLOW_RANGED_LEASHING && event.getDamager() instanceof Projectile projectile) {
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

                    PetType petType = PetType.byEntityTypeName(leashTarget.getType().name());
                    ConfigItem neededLeashItem = MyPetApi.getPetInfo().getLeashItem(petType);

                    if (!Permissions.has(player, "MyPet.leash." + petType.name())) {
                        return;
                    }
                    boolean usedArrow = false;
                    if (!neededLeashItem.compare(leashItem)) {
                        if (leashItemArrow == null || !neededLeashItem.compare(leashItemArrow)) {
                            return;
                        } else {
                            usedArrow = true;
                        }
                    }
                    for (LeashHook hook : MyPetApi.getPluginHookManager().getHooks(LeashHook.class)) {
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
                                .petName(Locale.getString("Name." + petType.name(), owner))
                                .worldGroup(worldGroup.getName())
                                .info(snapshot)
                                .build();
                        inactivePet.getOwner().setPetForWorldGroup(worldGroup, inactivePet.getUUID());

                        // Store reference to the original mob so the activation callback can
                        // convert it in-place rather than destroying + re-spawning.
                        final Mob capturedMob = (Mob) leashTarget;

                        boolean remove = false; // Keep the original mob — converted in-place below
                        for (LeashEntityHook hook : MyPetApi.getPluginHookManager().getHooks(LeashEntityHook.class)) {
                            if (!hook.prepare(leashTarget)) {
                                remove = false;
                            }
                        }

                        if (!usedArrow) {
                            if (Configuration.Misc.CONSUME_LEASH_ITEM && player.getGameMode() != GameMode.CREATIVE && leashItem != null) {
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

            if (Configuration.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION && !(target instanceof Player) && !(PetEntityMarker.isMarked(target))) {
                LivingEntity livingSource = null;
                if (source instanceof Projectile projectile) {
                    if (projectile.getShooter() instanceof LivingEntity) {
                        livingSource = (LivingEntity) projectile.getShooter();
                    }
                } else if (source instanceof LivingEntity) {
                    livingSource = (LivingEntity) source;
                }
                if (livingSource != null) {
                    PetExperience.addDamageToEntity(livingSource, (LivingEntity) target, event.getDamage());
                }
            }

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
                    if (getPetManager().hasActivePet(player)) {
                        Pet pet = getPetManager().getPet(player);
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityResult(final EntityDamageByEntityEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        Entity damagedEntity = event.getEntity();
        // -- fix unwanted screaming of Endermen --
        if (damagedEntity instanceof Enderman enderman && PetEntityMarker.isMarked(damagedEntity)) {
            Pet pet = getPetManager().getPetFromEntity(damagedEntity);
            if (pet instanceof PetEnderman petEnderman) {
                enderman.setScreaming(petEnderman.isPermaScreaming());
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
        if (Configuration.LevelSystem.Experience.DISABLED_WORLDS.contains(deadEntity.getWorld().getName())) {
            return;
        }
        if (!Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.isEmpty() && event.getEntity().hasMetadata("SpawnReason")) {
            for (MetadataValue value : event.getEntity().getMetadata("SpawnReason")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    if (Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.contains(value.asString())) {
                        return;
                    }
                    break;
                }
            }
            event.getEntity().removeMetadata("SpawnReason", MyPetApi.getPlugin());
        }
        if (Configuration.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION) {
            Map<UUID, Double> damagePercentMap = PetExperience.getDamageToEntityPercent(deadEntity);
            for (UUID entityUUID : damagePercentMap.keySet()) {
                Entity entity = Bukkit.getEntity(entityUUID);
                if (PetEntityMarker.isMarked(entity)) {
                    Pet pet = getPetManager().getPetFromEntity(entity);
                    if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && pet.getSkilltree() == null) {
                        if (!pet.autoAssignSkilltree()) {
                            continue;
                        }
                    }
                    if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                        double randomExp = MonsterExperience.getMonsterExperience(deadEntity).getRandomExp();
                        pet.getExperience().addExp(damagePercentMap.get(entity.getUniqueId()) * randomExp, true);
                    }
                } else if (entity instanceof Player owner) {
                    if (getPetManager().hasActivePet(owner)) {
                        Pet pet = getPetManager().getPet(owner);
                        if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && pet.getSkilltree() == null) {
                            if (!pet.autoAssignSkilltree()) {
                                continue;
                            }
                        }
                        if (pet.isPassive() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                            if (pet.getStatus() == PetState.Here) {
                                if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                    int percentage = (int) (Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER * damagePercentMap.get(entity.getUniqueId()));
                                    pet.getExperience().addExp(deadEntity, percentage, true);
                                }
                            }
                        }
                    }
                } else if (entity instanceof Tameable tameable) {
                    if (tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner() instanceof Player owner) {
                        if (getPetManager().hasActivePet(owner)) {
                            Pet pet = getPetManager().getPet(owner);
                            if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && pet.getSkilltree() == null) {
                                continue;
                            }
                            if (pet.isPassive() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                                if (pet.getStatus() == PetState.Here) {
                                    if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                        int percentage = (int) (Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER * damagePercentMap.get(entity.getUniqueId()));
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
                if (pet.getSkilltree() == null && Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE) {
                    if (!pet.autoAssignSkilltree()) {
                        return;
                    }
                }
                pet.getExperience().addExp(edbee.getEntity(), true);
            } else if (damager instanceof Player owner) {
                if (getPetManager().hasActivePet(owner)) {
                    Pet pet = getPetManager().getPet(owner);
                    if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && pet.getSkilltree() == null) {
                        if (!pet.autoAssignSkilltree()) {
                            return;
                        }
                    }
                    if (pet.isPassive() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                        if (pet.getStatus() == PetState.Here) {
                            if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                pet.getExperience().addExp(deadEntity, Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER, true);
                            }
                        }
                    }
                }
            } else if (damager instanceof Tameable tameable) {
                if (tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner() instanceof Player owner) {
                    if (getPetManager().hasActivePet(owner)) {
                        Pet pet = getPetManager().getPet(owner);
                        if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && pet.getSkilltree() == null) {
                            return;
                        }
                        if (pet.isPassive() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                            if (pet.getStatus() == PetState.Here) {
                                if (pet.getSkilltree() == null || pet.getSkilltree().getMaxLevel() <= 1 || pet.getExperience().getLevel() < pet.getSkilltree().getMaxLevel()) {
                                    pet.getExperience().addExp(deadEntity, Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER, true);
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
}