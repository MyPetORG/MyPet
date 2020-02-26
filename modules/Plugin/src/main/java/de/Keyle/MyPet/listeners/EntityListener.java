/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.types.MyEnderman;
import de.Keyle.MyPet.api.event.MyPetCreateEvent;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.types.LeashEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EntityConverterService;
import de.Keyle.MyPet.entity.InactiveMyPet;
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
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

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
        if (Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.size() > 0) {
            event.getEntity().setMetadata("SpawnReason", new FixedMetadataValue(MyPetApi.getPlugin(), event.getSpawnReason().name()));
        }
        if (event.getEntity() instanceof Zombie) {
            MyPetApi.getPlatformHelper().addZombieTargetGoal((Zombie) event.getEntity());
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
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        usedItems.remove(event.getPlayer().getUniqueId());
                    }
                }.runTaskLater(MyPetApi.getPlugin(), 0);
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
            if (event.getEntity() instanceof Player) {
                if (event.getProjectile() instanceof Arrow) {
                    Player player = (Player) event.getEntity();
                    Arrow projectile = (Arrow) event.getProjectile();
                    PlayerInventory inventory = player.getInventory();

                    if (event.getBow() != null) {
                        projectile.setMetadata("MyPetLeashItem", new FixedMetadataValue(MyPetApi.getPlugin(), event.getBow().clone()));
                    }

                    ItemStack arrow = null;
                    if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                        switch (inventory.getItemInOffHand().getType()) {
                            case ARROW:
                            case TIPPED_ARROW:
                            case SPECTRAL_ARROW:
                                arrow = inventory.getItemInOffHand();
                        }
                        switch (inventory.getItemInMainHand().getType()) {
                            case ARROW:
                            case TIPPED_ARROW:
                            case SPECTRAL_ARROW:
                                arrow = inventory.getItemInMainHand();
                        }
                    }
                    if (arrow == null) {
                        int firstArrow = -1;
                        int normalArrow = inventory.first(Material.ARROW);
                        if (normalArrow != -1) {
                            arrow = inventory.getItem(inventory.first(Material.ARROW));
                            firstArrow = normalArrow;
                        }
                        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                            int tippedFirst = inventory.first(Material.TIPPED_ARROW);
                            if (tippedFirst != -1 && firstArrow > tippedFirst) {
                                arrow = inventory.getItem(inventory.first(Material.TIPPED_ARROW));
                                firstArrow = tippedFirst;
                            }
                        }
                        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                            int spectralFirst = inventory.first(Material.SPECTRAL_ARROW);
                            if (spectralFirst != -1 && firstArrow > spectralFirst) {
                                arrow = inventory.getItem(inventory.first(Material.SPECTRAL_ARROW));
                            }
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
        if (projectile.getShooter() instanceof Player && !(projectile instanceof Arrow)) {
            Player player = (Player) projectile.getShooter();
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                return;
            }
            if (!MyPetApi.getPlayerManager().isMyPetPlayer(player) || !MyPetApi.getPlayerManager().getMyPetPlayer(player).hasMyPet()) {
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
        if (!event.getEntity().isDead() && !(event.getEntity() instanceof MyPetBukkitEntity)) {
            if (MyPetApi.getMyPetInfo().isLeashableEntityType(event.getEntity().getType())) {
                ItemStack leashItem = null;
                ItemStack leashItemArrow = null;
                Player player;
                if (Configuration.Misc.ALLOW_RANGED_LEASHING && event.getDamager() instanceof Projectile) {
                    Projectile projectile = (Projectile) event.getDamager();
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
                    if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                        leashItem = player.getEquipment().getItemInMainHand();
                    } else {
                        leashItem = player.getItemInHand();
                    }
                } else {
                    return;
                }

                if (!MyPetApi.getMyPetManager().hasActiveMyPet(player) && !justLeashed.contains(player.getUniqueId())) {
                    LivingEntity leashTarget = (LivingEntity) event.getEntity();

                    MyPetType petType = MyPetType.byEntityTypeName(leashTarget.getType().name());
                    ConfigItem neededLeashItem = MyPetApi.getMyPetInfo().getLeashItem(petType);

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

                    for (Settings flagSettings : MyPetApi.getMyPetInfo().getLeashFlagSettings(petType)) {
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
                                    String message = flag.getMissingMessage(player, leashTarget, event.getDamage(), flagSettings);
                                    if (message != null) {
                                        myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(false) + message, 10000);
                                    }
                                }
                            }
                        } else {
                            if (myPetPlayer != null) {
                                if (myPetPlayer.isCaptureHelperActive()) {
                                    String message = flag.getMissingMessage(player, leashTarget, event.getDamage(), flagSettings);
                                    if (message != null) {
                                        myPetPlayer.sendMessage(LeashFlag.getMessagePrefix(true) + message, 10000);
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

                        final InactiveMyPet inactiveMyPet = new InactiveMyPet(owner);
                        inactiveMyPet.setPetType(petType);
                        inactiveMyPet.setPetName(Translation.getString("Name." + petType.name(), inactiveMyPet.getOwner()));

                        WorldGroup worldGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
                        inactiveMyPet.setWorldGroup(worldGroup.getName());
                        inactiveMyPet.getOwner().setMyPetForWorldGroup(worldGroup, inactiveMyPet.getUUID());

                        /*
                        if(leashTarget.getCustomName() != null)
                        {
                            inactiveMyPet.setPetName(leashTarget.getCustomName());
                        }
                        */

                        Optional<EntityConverterService> converter = MyPetApi.getServiceManager().getService(EntityConverterService.class);
                        converter.ifPresent(service -> inactiveMyPet.setInfo(service.convertEntity(leashTarget)));

                        boolean remove = true;
                        for (LeashEntityHook hook : MyPetApi.getPluginHookManager().getHooks(LeashEntityHook.class)) {
                            if (!hook.prepare(leashTarget)) {
                                remove = false;
                            }
                        }
                        if (remove) {
                            leashTarget.remove();
                        }

                        if (!usedArrow) {
                            if (Configuration.Misc.CONSUME_LEASH_ITEM && player.getGameMode() != GameMode.CREATIVE && leashItem != null) {
                                if (leashItem.getAmount() > 1) {
                                    leashItem.setAmount(leashItem.getAmount() - 1);
                                } else {
                                    if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                                        player.getEquipment().setItemInMainHand(null);
                                    } else {
                                        player.setItemInHand(null);
                                    }
                                }
                            }
                        }

                        MyPetCreateEvent createEvent = new MyPetCreateEvent(inactiveMyPet, MyPetCreateEvent.Source.Leash);
                        Bukkit.getServer().getPluginManager().callEvent(createEvent);

                        MyPetSaveEvent saveEvent = new MyPetSaveEvent(inactiveMyPet);
                        Bukkit.getServer().getPluginManager().callEvent(saveEvent);

                        justLeashed.add(player.getUniqueId());
                        MyPetApi.getPlugin().getRepository().addMyPet(inactiveMyPet, new RepositoryCallback<Boolean>() {
                            @Override
                            public void callback(Boolean value) {
                                owner.sendMessage(Translation.getString("Message.Leash.Add", owner));

                                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(inactiveMyPet);
                                myPet.ifPresent(MyPet::createEntity);
                                if (owner.isCaptureHelperActive()) {
                                    owner.setCaptureHelperActive(false);
                                    owner.sendMessage(Util.formatText(Translation.getString("Message.Command.CaptureHelper.Mode", owner), Translation.getString("Name.Disabled", owner)));
                                }
                                justLeashed.remove(player.getUniqueId());
                            }
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

            if (Configuration.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION && !(target instanceof Player) && !(target instanceof MyPetBukkitEntity)) {
                LivingEntity livingSource = null;
                if (source instanceof Projectile) {
                    Projectile projectile = (Projectile) source;
                    if (projectile.getShooter() instanceof LivingEntity) {
                        livingSource = (LivingEntity) projectile.getShooter();
                    }
                } else if (source instanceof LivingEntity) {
                    livingSource = (LivingEntity) source;
                }
                if (livingSource != null) {
                    MyPetExperience.addDamageToEntity(livingSource, (LivingEntity) target, event.getDamage());
                }
            }

            if (source instanceof Projectile) {
                ProjectileSource projectileSource = ((Projectile) source).getShooter();
                if (projectileSource instanceof Entity) {
                    source = (Entity) projectileSource;
                }
            }

            if (source instanceof Player) {
                Player player = (Player) source;
                if (event.getDamage() == 0) {
                    return;
                } else if (target instanceof MyPetBukkitEntity) {
                    if (MyPetApi.getMyPetInfo().getLeashItem(((MyPetBukkitEntity) target).getPetType()).compare(player.getItemInHand())) {
                        return;
                    }
                }
                if (source != target) {
                    if (target instanceof Tameable && source.equals(((Tameable) target).getOwner())) {
                        return;
                    }
                    if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
                        if (myPet.getStatus() == PetState.Here) {
                            myPet.getEntity().ifPresent(entity -> {
                                if (target != entity) {
                                    if (myPet.getDamage() > 0 || myPet.getRangedDamage() > 0) {
                                        entity.setTarget((LivingEntity) target, TargetPriority.OwnerHurts);
                                    }
                                }
                            });
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
        // --  fix unwanted screaming of Endermen --
        if (damagedEntity instanceof MyPetBukkitEntity && ((MyPetBukkitEntity) damagedEntity).getPetType() == MyPetType.Enderman) {
            ((MyEnderman) ((MyPetBukkitEntity) damagedEntity).getMyPet()).setScreaming(true);
            ((MyEnderman) ((MyPetBukkitEntity) damagedEntity).getMyPet()).setScreaming(false);
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
        if (deadEntity instanceof MyPetBukkitEntity) {
            return;
        }
        if (WorldGroup.getGroupByWorld(deadEntity.getWorld()).isDisabled()) {
            return;
        }
        if (Configuration.LevelSystem.Experience.DISABLED_WORLDS.contains(deadEntity.getWorld().getName())) {
            return;
        }
        if (Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.size() > 0 && event.getEntity().hasMetadata("SpawnReason")) {
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
            Map<UUID, Double> damagePercentMap = MyPetExperience.getDamageToEntityPercent(deadEntity);
            for (UUID entityUUID : damagePercentMap.keySet()) {
                Entity entity = MyPetApi.getPlatformHelper().getEntityByUUID(entityUUID);
                if (entity instanceof MyPetBukkitEntity) {
                    MyPet myPet = ((MyPetBukkitEntity) entity).getMyPet();
                    if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkilltree() == null) {
                        if (!myPet.autoAssignSkilltree()) {
                            continue;
                        }
                    }
                    if (myPet.getSkilltree() == null || myPet.getSkilltree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkilltree().getMaxLevel()) {
                        double randomExp = MonsterExperience.getMonsterExperience(deadEntity).getRandomExp();
                        myPet.getExperience().addExp(damagePercentMap.get(entity.getUniqueId()) * randomExp, true);
                    }
                } else if (entity instanceof Player) {
                    Player owner = (Player) entity;
                    if (MyPetApi.getMyPetManager().hasActiveMyPet(owner)) {
                        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(owner);
                        if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkilltree() == null) {
                            if (!myPet.autoAssignSkilltree()) {
                                continue;
                            }
                        }
                        if (myPet.isPassiv() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                            if (myPet.getStatus() == PetState.Here) {
                                if (myPet.getSkilltree() == null || myPet.getSkilltree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkilltree().getMaxLevel()) {
                                    int percentage = (int) (Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER * damagePercentMap.get(entity.getUniqueId()));
                                    myPet.getExperience().addExp(deadEntity, percentage, true);
                                }
                            }
                        }
                    }
                } else if (entity instanceof Tameable) {
                    Tameable tameable = (Tameable) entity;
                    if (tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner() instanceof Player) {
                        Player owner = (Player) tameable.getOwner();
                        if (MyPetApi.getMyPetManager().hasActiveMyPet(owner)) {
                            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(owner);
                            if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkilltree() == null) {
                                continue;
                            }
                            if (myPet.isPassiv() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                                if (myPet.getStatus() == PetState.Here) {
                                    if (myPet.getSkilltree() == null || myPet.getSkilltree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkilltree().getMaxLevel()) {
                                        int percentage = (int) (Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER * damagePercentMap.get(entity.getUniqueId()));
                                        myPet.getExperience().addExp(deadEntity, percentage, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            deadEntity.removeMetadata("MyPetDamageCount", MyPetApi.getPlugin());
        } else if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();

            Entity damager = edbee.getDamager();
            if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Entity) {
                damager = (Entity) ((Projectile) damager).getShooter();
            }
            if (damager instanceof MyPetBukkitEntity) {
                MyPet myPet = ((MyPetBukkitEntity) damager).getMyPet();
                if (myPet.getSkilltree() == null && Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE) {
                    if (!myPet.autoAssignSkilltree()) {
                        return;
                    }
                }
                myPet.getExperience().addExp(edbee.getEntity(), true);
            } else if (damager instanceof Player) {
                Player owner = (Player) damager;
                if (MyPetApi.getMyPetManager().hasActiveMyPet(owner)) {
                    MyPet myPet = MyPetApi.getMyPetManager().getMyPet(owner);
                    if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkilltree() == null) {
                        if (!myPet.autoAssignSkilltree()) {
                            return;
                        }
                    }
                    if (myPet.isPassiv() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                        if (myPet.getStatus() == PetState.Here) {
                            if (myPet.getSkilltree() == null || myPet.getSkilltree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkilltree().getMaxLevel()) {
                                myPet.getExperience().addExp(deadEntity, Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER, true);
                            }
                        }
                    }
                }
            } else if (damager instanceof Tameable) {
                Tameable tameable = (Tameable) damager;
                if (tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner() instanceof Player) {
                    Player owner = (Player) tameable.getOwner();
                    if (MyPetApi.getMyPetManager().hasActiveMyPet(owner)) {
                        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(owner);
                        if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkilltree() == null) {
                            return;
                        }
                        if (myPet.isPassiv() || Configuration.LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP) {
                            if (myPet.getStatus() == PetState.Here) {
                                if (myPet.getSkilltree() == null || myPet.getSkilltree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkilltree().getMaxLevel()) {
                                    myPet.getExperience().addExp(deadEntity, Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER, true);
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
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            MyPet myPet = ((MyPetBukkitEntity) event.getEntity()).getMyPet();
            if (myPet.getSkills().isActive(Behavior.class)) {
                Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
                if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
                    event.setCancelled(true);
                } else if (event.getTarget() instanceof Player && event.getTarget().getName().equals(myPet.getOwner().getName())) {
                    event.setCancelled(true);
                } else if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                    if (event.getTarget() instanceof Player) {
                        event.setCancelled(true);
                    } else if (event.getTarget() instanceof Tameable && ((Tameable) event.getTarget()).isTamed()) {
                        event.setCancelled(true);
                    } else if (event.getTarget() instanceof MyPetBukkitEntity) {
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getEntity() instanceof Tameable) {
            if (event.getTarget() instanceof MyPetBukkitEntity) {
                Tameable tameable = ((Tameable) event.getEntity());
                MyPet myPet = ((MyPetBukkitEntity) event.getTarget()).getMyPet();
                if (myPet.getOwner().equals(tameable.getOwner())) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getEntity() instanceof IronGolem) {
            if (event.getTarget() instanceof MyPetBukkitEntity) {
                if (event.getReason() == TargetReason.RANDOM_TARGET) {
                    event.setCancelled(true);
                }
            }
        }
    }
}