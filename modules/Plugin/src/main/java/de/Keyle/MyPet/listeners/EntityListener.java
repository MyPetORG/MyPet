/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSettings;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.entity.types.MyEnderman;
import de.Keyle.MyPet.api.event.*;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.OnDamageByEntitySkill;
import de.Keyle.MyPet.api.skill.OnHitSkill;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skills.Damage;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.chat.FancyMessage;
import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.hooks.types.EconomyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerLeashEntityHook;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EntityConverterService;
import de.Keyle.MyPet.commands.CommandInfo;
import de.Keyle.MyPet.commands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMyPet(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void on(CreatureSpawnEvent event) {
        if (Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.size() > 0) {
            event.getEntity().setMetadata("SpawnReason", new FixedMetadataValue(MyPetApi.getPlugin(), event.getSpawnReason().name()));
        }
        if (event.getEntity() instanceof Zombie) {
            MyPetApi.getPlatformHelper().addZombieTargetGoal((Zombie) event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMyPet(EntityPortalEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMyPet(EntityInteractEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            if (event.getBlock().getType() == Material.SOIL) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMyPet(EntityCombustByEntityEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            if (event.getCombuster() instanceof Player || (event.getCombuster() instanceof Projectile && ((Projectile) event.getCombuster()).getShooter() instanceof Player)) {
                Player damager;
                if (event.getCombuster() instanceof Projectile) {
                    damager = (Player) ((Projectile) event.getCombuster()).getShooter();
                } else {
                    damager = (Player) event.getCombuster();
                }

                MyPet myPet = ((MyPetBukkitEntity) event.getEntity()).getMyPet();

                if (myPet.getOwner().equals(damager) && !Configuration.Misc.OWNER_CAN_ATTACK_PET) {
                    event.setCancelled(true);
                } else if (!myPet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, myPet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMyPet(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            MyPetBukkitEntity craftMyPet = (MyPetBukkitEntity) event.getEntity();
            MyPet myPet = craftMyPet.getMyPet();
            if (event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player)) {
                Player damager;
                if (event.getDamager() instanceof Projectile) {
                    damager = (Player) ((Projectile) event.getDamager()).getShooter();
                } else {
                    damager = (Player) event.getDamager();
                }
                ItemStack leashItem;
                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                    leashItem = damager.getEquipment().getItemInMainHand();
                } else {
                    leashItem = damager.getItemInHand();
                }
                if (MyPetApi.getMyPetInfo().getLeashItem(myPet.getPetType()).compare(leashItem)) {
                    boolean infoShown = false;
                    if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, damager, myPet)) {
                        damager.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, damager, myPet) && myPet.getOwner().getPlayer() != damager) {
                        damager.sendMessage("   " + Translation.getString("Name.Owner", damager) + ": " + myPet.getOwner().getName());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, damager, myPet)) {
                        String msg;
                        if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2) {
                            msg = "" + ChatColor.GREEN;
                        } else if (myPet.getHealth() > myPet.getMaxHealth() / 3) {
                            msg = "" + ChatColor.YELLOW;
                        } else {
                            msg = "" + ChatColor.RED;
                        }
                        msg += String.format("%1.2f", myPet.getHealth()) + ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                        damager.sendMessage("   " + Translation.getString("Name.HP", damager) + ": " + msg);
                        infoShown = true;
                    }
                    if (myPet.getStatus() == PetState.Dead && CommandInfo.canSee(PetInfoDisplay.RespawnTime.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Translation.getString("Name.Respawntime", damager) + ": " + myPet.getRespawnTime());
                        infoShown = true;
                    }
                    if (!myPet.isPassiv() && CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, damager, myPet)) {
                        double damage = (myPet.getSkills().isActive(Damage.class) ? myPet.getSkills().get(Damage.class).getDamage().getValue().doubleValue() : 0);
                        damager.sendMessage("   " + Translation.getString("Name.Damage", damager) + ": " + String.format("%1.2f", damage));
                        infoShown = true;
                    }
                    if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, damager, myPet)) {
                        double damage = myPet.getRangedDamage();
                        damager.sendMessage("   " + Translation.getString("Name.RangedDamage", damager) + ": " + String.format("%1.2f", damage));
                        infoShown = true;
                    }
                    if (myPet.getSkills().has(Behavior.class) && CommandInfo.canSee(PetInfoDisplay.Behavior.adminOnly, damager, myPet)) {
                        Behavior behavior = myPet.getSkills().get(Behavior.class);
                        damager.sendMessage("   " + Translation.getString("Name.Skill.Behavior", damager) + ": " + Translation.getString("Name." + behavior.getBehavior().name(), damager));
                        infoShown = true;
                    }
                    if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Translation.getString("Name.Hunger", damager) + ": " + Math.round(myPet.getSaturation()));

                        FancyMessage m = new FancyMessage("   " + Translation.getString("Name.Food", damager) + ": ");
                        boolean comma = false;
                        for (ConfigItem material : MyPetApi.getMyPetInfo().getFood(myPet.getPetType())) {
                            ItemStack is = material.getItem();
                            if (is == null) {
                                continue;
                            }
                            if (comma) {
                                m.then(", ");
                            }
                            if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                                m.then(is.getItemMeta().getDisplayName());
                            } else {
                                try {
                                    m.thenTranslate(MyPetApi.getPlatformHelper().getVanillaName(is) + ".name");
                                } catch (Exception e) {
                                    MyPetApi.getLogger().warning("A food item for \"" + myPet.getPetType().name() + "\" caused an error. If you think this is a bug please report it to the MyPet developer.");
                                    MyPetApi.getLogger().warning(is.toString());
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                            m.color(ChatColor.GOLD);
                            ItemTooltip it = new ItemTooltip();
                            it.setMaterial(is.getType());
                            if (is.hasItemMeta()) {
                                if (is.getItemMeta().hasDisplayName()) {
                                    it.setTitle(is.getItemMeta().getDisplayName());
                                }
                                if (is.getItemMeta().hasLore()) {
                                    it.setLore(is.getItemMeta().getLore().toArray(new String[is.getItemMeta().getLore().size()]));
                                }
                            }
                            m.itemTooltip(it);
                            comma = true;
                        }
                        MyPetApi.getPlatformHelper().sendMessageRaw(damager, m.toJSONString());

                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, damager, myPet) && myPet.getSkilltree() != null) {
                        damager.sendMessage("   " + Translation.getString("Name.Skilltree", damager) + ": " + myPet.getSkilltree().getName());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, damager, myPet)) {
                        int lvl = myPet.getExperience().getLevel();
                        damager.sendMessage("   " + Translation.getString("Name.Level", damager) + ": " + lvl);
                        infoShown = true;
                    }
                    int maxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : Configuration.LevelSystem.Experience.LEVEL_CAP;
                    if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, damager, myPet) && myPet.getExperience().getLevel() < maxLevel) {
                        double exp = myPet.getExperience().getCurrentExp();
                        double reqEXP = myPet.getExperience().getRequiredExp();
                        damager.sendMessage("   " + Translation.getString("Name.Exp", damager) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                        infoShown = true;
                    }
                    if (myPet.getOwner().getDonationRank() != DonateCheck.DonationRank.None) {
                        infoShown = true;
                        String donationMessage = "" + ChatColor.GOLD;
                        donationMessage += myPet.getOwner().getDonationRank().getDefaultIcon();
                        donationMessage += " " + Translation.getString("Name.Title." + myPet.getOwner().getDonationRank().name(), damager) + " ";
                        donationMessage += myPet.getOwner().getDonationRank().getDefaultIcon();
                        damager.sendMessage("   " + donationMessage);
                    }

                    if (!infoShown) {
                        damager.sendMessage(Translation.getString("Message.No.NothingToSeeHere", myPet.getOwner().getLanguage()));
                    }

                    event.setCancelled(true);
                } else if (myPet.getOwner().equals(damager) && (!Configuration.Misc.OWNER_CAN_ATTACK_PET)) {
                    event.setCancelled(true);
                } else if (!myPet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, myPet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
            if (event.getDamager() instanceof CraftMyPetProjectile) {
                EntityMyPetProjectile projectile = ((CraftMyPetProjectile) event.getDamager()).getMyPetProjectile();

                if (myPet == projectile.getShooter().getMyPet()) {
                    event.setCancelled(true);
                }
                if (!MyPetApi.getHookHelper().canHurt(projectile.getShooter().getOwner().getPlayer(), myPet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
            if (!event.isCancelled() && event.getDamager() instanceof LivingEntity) {
                LivingEntity damager = (LivingEntity) event.getDamager();
                if (damager instanceof Player) {
                    if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), (Player) damager, true)) {
                        return;
                    }
                }

                if (!isSkillActive) {
                    for (Skill skill : myPet.getSkills().all()) {
                        if (skill instanceof OnDamageByEntitySkill) {
                            OnDamageByEntitySkill damageByEntitySkill = (OnDamageByEntitySkill) skill;
                            if (damageByEntitySkill.trigger()) {
                                isSkillActive = true;
                                damageByEntitySkill.apply(damager, event);
                                isSkillActive = false;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerInteractEvent event) {
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
        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player && !(projectile instanceof Arrow)) {
            Player player = (Player) projectile.getShooter();
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
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
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
                            if (meta.getOwningPlugin() == MyPetApi.getPlugin()) {
                                leashItem = (ItemStack) meta.value();
                                break;
                            }
                        }
                        if (leashItem == null) {
                            return;
                        }
                    }
                    if (projectile.hasMetadata("")) {
                        metaList = projectile.getMetadata("MyPetLeashItemArrow");
                        for (MetadataValue meta : metaList) {
                            if (meta.getOwningPlugin() == MyPetApi.getPlugin()) {
                                leashItemArrow = (ItemStack) meta.value();
                                break;
                            }
                        }
                        if (leashItemArrow == null) {
                            return;
                        }
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

                if (!MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                    LivingEntity leashTarget = (LivingEntity) event.getEntity();

                    MyPetType petType = MyPetType.byEntityTypeName(leashTarget.getType().name());
                    ConfigItem neededLeashItem = MyPetApi.getMyPetInfo().getLeashItem(petType);

                    if (!Permissions.hasLegacy(player, "MyPet.leash.", petType.name())) {
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
                    for (PlayerLeashEntityHook hook : MyPetApi.getPluginHookManager().getHooks(PlayerLeashEntityHook.class)) {
                        if (!hook.canLeash(player, leashTarget)) {
                            return;
                        }
                    }

                    boolean willBeLeashed = true;

                    for (LeashFlagSettings flagSettings : MyPetApi.getMyPetInfo().getLeashFlagSettings(petType)) {
                        String flagName = flagSettings.getFlagName();
                        LeashFlag flag = MyPetApi.getLeashFlagManager().getLeashFlag(flagName);
                        if (!flag.check(player, leashTarget, event.getDamage(), flagSettings)) {
                            willBeLeashed = false;
                        }
                        if (!willBeLeashed) {
                            break;
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
                        inactiveMyPet.setPetName(Translation.getString("Name." + petType.name(), inactiveMyPet.getOwner().getLanguage()));

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

                        leashTarget.remove();

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

                        MyPetApi.getPlugin().getRepository().addMyPet(inactiveMyPet, new RepositoryCallback<Boolean>() {
                            @Override
                            public void callback(Boolean value) {
                                owner.sendMessage(Translation.getString("Message.Leash.Add", owner));

                                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(inactiveMyPet);
                                if (myPet.isPresent()) {
                                    myPet.get().createEntity();
                                }
                                if (owner.isCaptureHelperActive()) {
                                    owner.setCaptureHelperActive(false);
                                    owner.sendMessage(Util.formatText(Translation.getString("Message.Command.CaptureHelper.Mode", owner), Translation.getString("Name.Disabled", owner)));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPet(final EntityDamageEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            MyPetBukkitEntity bukkitEntity = (MyPetBukkitEntity) event.getEntity();

            if (event.getCause() == DamageCause.SUFFOCATION) {
                final MyPet myPet = bukkitEntity.getMyPet();
                final MyPetPlayer myPetPlayer = myPet.getOwner();

                myPet.removePet();
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Despawn", myPetPlayer.getLanguage()), myPet.getPetName()));

                MyPetApi.getPlugin().getServer().getScheduler().runTaskLater(MyPetApi.getPlugin(), new Runnable() {
                    public void run() {
                        if (myPetPlayer.hasMyPet()) {
                            MyPet runMyPet = myPetPlayer.getMyPet();
                            switch (runMyPet.createEntity()) {
                                case Canceled:
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NoSpace:
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NotAllowed:
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", myPet.getOwner()), myPet.getPetName()));
                                    break;
                                case Flying:
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                    break;
                                case Success:
                                    if (runMyPet != myPet) {
                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", myPet.getOwner().getLanguage()), runMyPet.getPetName()));
                                    }
                                    break;
                            }
                        }
                    }
                }, 10L);
            }
        }
    }

    boolean isSkillActive = false;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitor(final EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();

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
                if (source != target && MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                    MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
                    if (myPet.getStatus() == PetState.Here && target != myPet.getEntity().get()) {
                        if (myPet.getDamage() > 0 || myPet.getRangedDamage() > 0) {
                            myPet.getEntity().get().setTarget((LivingEntity) target, TargetPriority.OwnerHurts);
                        }
                    }
                }
            } else if (source instanceof MyPetBukkitEntity) {
                MyPet myPet = ((MyPetBukkitEntity) source).getMyPet();

                if (myPet.getStatus() != PetState.Here) {
                    return;
                }

                // fix influence of other plugins for this event and throw damage event
                if (event.getDamager() instanceof Projectile) {
                    MyPetDamageEvent petDamageEvent = new MyPetDamageEvent(myPet, target, event.getOriginalDamage(EntityDamageEvent.DamageModifier.BASE));
                    Bukkit.getPluginManager().callEvent(petDamageEvent);
                    event.setDamage(petDamageEvent.getDamage());
                } else {
                    MyPetDamageEvent petDamageEvent = new MyPetDamageEvent(myPet, target, event.getOriginalDamage(EntityDamageEvent.DamageModifier.BASE));
                    Bukkit.getPluginManager().callEvent(petDamageEvent);
                    event.setDamage(petDamageEvent.getDamage());
                }

                if (!isSkillActive) {
                    for (Skill skill : myPet.getSkills().all()) {
                        if (skill instanceof OnHitSkill) {
                            OnHitSkill onHitSkill = (OnHitSkill) skill;
                            if (onHitSkill.trigger()) {
                                MyPetOnHitSkillEvent skillEvent = new MyPetOnHitSkillEvent(myPet, onHitSkill, (LivingEntity) target);
                                Bukkit.getPluginManager().callEvent(skillEvent);
                                if (!skillEvent.isCancelled()) {
                                    isSkillActive = true;
                                    onHitSkill.apply((LivingEntity) target);
                                    isSkillActive = false;
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
        Entity damagedEntity = event.getEntity();
        // --  fix unwanted screaming of Endermen --
        if (damagedEntity instanceof MyPetBukkitEntity && ((MyPetBukkitEntity) damagedEntity).getPetType() == MyPetType.Enderman) {
            ((MyEnderman) ((MyPetBukkitEntity) damagedEntity).getMyPet()).setScreaming(true);
            ((MyEnderman) ((MyPetBukkitEntity) damagedEntity).getMyPet()).setScreaming(false);
        }
    }

    @EventHandler
    public void onMyPet(final EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof MyPetBukkitEntity) {
            MyPet myPet = ((MyPetBukkitEntity) deadEntity).getMyPet();
            if (myPet == null || myPet.getHealth() > 0) // check health for death events where the pet isn't really dead (/killall)
            {
                return;
            }

            final MyPetPlayer owner = myPet.getOwner();

            if (Configuration.Misc.RELEASE_PETS_ON_DEATH && !owner.isMyPetAdmin()) {
                MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.Death);
                Bukkit.getServer().getPluginManager().callEvent(removeEvent);

                if (myPet.getSkills().isActive(Backpack.class)) {
                    CustomInventory inv = myPet.getSkills().get(Backpack.class).getInventory();
                    inv.dropContentAt(myPet.getLocation().get());
                }
                if (myPet instanceof MyPetEquipment) {
                    ((MyPetEquipment) myPet).dropEquipment();
                }


                myPet.removePet();
                owner.setMyPetForWorldGroup(WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()), null);

                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Release.Dead", owner), myPet.getPetName()));

                MyPetApi.getMyPetManager().deactivateMyPet(owner, false);
                MyPetApi.getRepository().removeMyPet(myPet.getUUID(), null);

                return;
            }

            myPet.setRespawnTime((Configuration.Respawn.TIME_FIXED + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(myPet.getPetType())) + (myPet.getExperience().getLevel() * (Configuration.Respawn.TIME_FACTOR + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(myPet.getPetType()))));
            myPet.setStatus(PetState.Dead);

            if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();

                if (e.getDamager() instanceof Player) {
                    myPet.setRespawnTime((Configuration.Respawn.TIME_PLAYER_FIXED + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(myPet.getPetType())) + (myPet.getExperience().getLevel() * (Configuration.Respawn.TIME_PLAYER_FACTOR + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(myPet.getPetType()))));
                } else if (e.getDamager() instanceof MyPetBukkitEntity) {
                    MyPet killerMyPet = ((MyPetBukkitEntity) e.getDamager()).getMyPet();
                    if (myPet.getSkills().isActive(Behavior.class) && killerMyPet.getSkills().isActive(Behavior.class)) {
                        Behavior killerBehaviorSkill = killerMyPet.getSkills().get(Behavior.class);
                        Behavior deadBehaviorSkill = myPet.getSkills().get(Behavior.class);
                        if (deadBehaviorSkill.getBehavior() == BehaviorMode.Duel && killerBehaviorSkill.getBehavior() == BehaviorMode.Duel) {
                            MyPetMinecraftEntity myPetEntity = ((MyPetBukkitEntity) deadEntity).getHandle();

                            if (e.getDamager().equals(myPetEntity.getTarget())) {
                                myPet.setRespawnTime(10);
                                killerMyPet.setHealth(killerMyPet.getMaxHealth());
                            }
                        }
                    }
                }
            }
            event.setDroppedExp(0);

            if (Configuration.LevelSystem.Experience.LOSS_FIXED > 0 || Configuration.LevelSystem.Experience.LOSS_PERCENT > 0) {
                double lostExpirience = Configuration.LevelSystem.Experience.LOSS_FIXED;
                lostExpirience += myPet.getExperience().getRequiredExp() * Configuration.LevelSystem.Experience.LOSS_PERCENT / 100;
                if (lostExpirience > myPet.getExperience().getCurrentExp()) {
                    lostExpirience = myPet.getExperience().getCurrentExp();
                }
                if (myPet.getSkilltree() != null) {
                    int requiredLevel = myPet.getSkilltree().getRequiredLevel();
                    if (requiredLevel > 1) {
                        double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                        lostExpirience = myPet.getExp() - lostExpirience < minExp ? myPet.getExp() - minExp : lostExpirience;
                    }
                }
                if (Configuration.LevelSystem.Experience.DROP_LOST_EXP) {
                    event.setDroppedExp((int) (lostExpirience + 0.5));
                }
                if (Configuration.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE) {
                    myPet.getExperience().removeExp(lostExpirience);
                } else {
                    myPet.getExperience().removeCurrentExp(lostExpirience);
                }
            }
            if (myPet.getSkills().isActive(Backpack.class)) {
                BackpackImpl inventorySkill = myPet.getSkills().get(BackpackImpl.class);
                inventorySkill.closeInventory();
                if (inventorySkill.getDropOnDeath().getValue() && !owner.isMyPetAdmin()) {
                    inventorySkill.getInventory().dropContentAt(myPet.getLocation().get());
                }
            }
            sendDeathMessage(event);
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", owner.getPlayer()), myPet.getPetName(), myPet.getRespawnTime()));

            if (MyPetApi.getPluginHookManager().isHookActive(EconomyHook.class) && owner.hasAutoRespawnEnabled() && myPet.getRespawnTime() >= owner.getAutoRespawnMin() && Permissions.hasLegacy(owner.getPlayer(), "MyPet.command.respawn")) {
                double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
                if (MyPetApi.getHookHelper().getEconomy().canPay(owner, costs)) {
                    MyPetApi.getHookHelper().getEconomy().pay(owner, costs);
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Paid", owner.getPlayer()), myPet.getPetName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                    myPet.setRespawnTime(1);
                } else {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.NoMoney", owner.getPlayer()), myPet.getPetName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void on(final EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof MyPetBukkitEntity) {
            return;
        }
        if (Configuration.Hooks.SkillAPI.DISABLE_VANILLA_EXP) {
            return;
        }
        if (Configuration.LevelSystem.Experience.DISABLED_WORLDS.contains(deadEntity.getWorld().getName())) {
            return;
        }
        if (Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.size() > 0 && event.getEntity().hasMetadata("SpawnReason")) {
            for (MetadataValue value : event.getEntity().getMetadata("SpawnReason")) {
                if (value.getOwningPlugin().getName().equals(MyPetApi.getPlugin().getName())) {
                    if (Configuration.LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.contains(value.asString())) {
                        return;
                    }
                    break;
                }
            }
        }
        if (Configuration.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION) {
            Map<Entity, Double> damagePercentMap = MyPetExperience.getDamageToEntityPercent(deadEntity);
            for (Entity entity : damagePercentMap.keySet()) {
                if (entity instanceof MyPetBukkitEntity) {
                    MyPet myPet = ((MyPetBukkitEntity) entity).getMyPet();
                    if (Configuration.Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkilltree() == null) {
                        if (!myPet.autoAssignSkilltree()) {
                            continue;
                        }
                    }
                    if (myPet.getSkilltree() == null || myPet.getSkilltree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkilltree().getMaxLevel()) {
                        double randomExp = MonsterExperience.getMonsterExperience(deadEntity.getType()).getRandomExp();
                        myPet.getExperience().addExp(damagePercentMap.get(entity) * randomExp);
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
                                    int percentage = (int) (Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER * damagePercentMap.get(entity));
                                    myPet.getExperience().addExp(deadEntity.getType(), percentage);
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
                                        int percentage = (int) (Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER * damagePercentMap.get(entity));
                                        myPet.getExperience().addExp(deadEntity.getType(), percentage);
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                myPet.getExperience().addExp(edbee.getEntity().getType());
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
                                myPet.getExperience().addExp(deadEntity.getType(), Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER);
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
                                    myPet.getExperience().addExp(deadEntity.getType(), Configuration.LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER);
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

    private void sendDeathMessage(final EntityDeathEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            MyPet myPet = ((MyPetBukkitEntity) event.getEntity()).getMyPet();
            String killer;
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER) {
                    if (e.getDamager() == myPet.getOwner().getPlayer()) {
                        killer = Translation.getString("Name.You", myPet.getOwner().getLanguage());
                    } else {
                        killer = e.getDamager().getName();
                    }
                } else if (e.getDamager().getType() == EntityType.WOLF) {
                    Wolf w = (Wolf) e.getDamager();
                    killer = Translation.getString("Name.Wolf", myPet.getOwner().getLanguage());
                    if (w.isTamed()) {
                        killer += " (" + w.getOwner().getName() + ')';
                    }
                } else if (e.getDamager() instanceof MyPetBukkitEntity) {
                    MyPetBukkitEntity craftMyPet = (MyPetBukkitEntity) e.getDamager();
                    killer = craftMyPet.getMyPet().getPetName() + " (" + craftMyPet.getOwner().getName() + ')';
                } else if (e.getDamager() instanceof Projectile) {
                    Projectile projectile = (Projectile) e.getDamager();
                    killer = Translation.getString("Name." + Util.capitalizeName(projectile.getType().name()), myPet.getOwner().getLanguage()) + " (";
                    if (projectile.getShooter() instanceof Player) {
                        if (projectile.getShooter() == myPet.getOwner().getPlayer()) {
                            killer += Translation.getString("Name.You", myPet.getOwner().getLanguage());
                        } else {
                            killer += ((Player) projectile.getShooter()).getName();
                        }
                    } else {
                        if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                            killer += Translation.getString("Name." + Util.capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner().getLanguage());
                        } else if (e.getDamager().getType().getName() != null) {
                            killer += Translation.getString("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner().getLanguage());
                        } else {
                            killer += Translation.getString("Name.Unknow", myPet.getOwner().getLanguage());
                        }
                    }
                    killer += ")";
                } else {
                    if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                        killer = Translation.getString("Name." + Util.capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner().getLanguage());
                    } else {
                        if (e.getDamager().getType().getName() != null) {
                            killer = Translation.getString("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner().getLanguage());
                        } else {
                            killer = Translation.getString("Name.Unknow", myPet.getOwner().getLanguage());
                        }
                    }
                }
            } else {
                if (event.getEntity().getLastDamageCause() != null) {
                    killer = Translation.getString("Name." + Util.capitalizeName(event.getEntity().getLastDamageCause().getCause().name()), myPet.getOwner().getLanguage());
                } else {
                    killer = Translation.getString("Name.Unknow", myPet.getOwner().getLanguage());
                }
            }
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.DeathMessage", myPet.getOwner().getLanguage()), myPet.getPetName(), killer));
        }
    }
}