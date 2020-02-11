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
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.event.MyPetPlayerJoinEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.repository.types.SqLiteRepository;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import de.Keyle.MyPet.skill.skills.ShieldImpl;
import de.Keyle.MyPet.util.Updater;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    @EventHandler
    public void on(PlayerInteractEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) && Configuration.Skilltree.Skill.CONTROL_ITEM.compare(event.getPlayer().getItemInHand()) && MyPetApi.getMyPetManager().hasActiveMyPet(event.getPlayer())) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(event.getPlayer());
            if (myPet.getStatus() == MyPet.PetState.Here && myPet.getEntity().isPresent() && myPet.getEntity().get().canMove()) {
                if (myPet.getSkills().isActive(ControlImpl.class)) {
                    if (myPet.getSkills().isActive(Behavior.class)) {
                        Behavior behavior = myPet.getSkills().get(Behavior.class);
                        if (behavior.getBehavior() == BehaviorMode.Aggressive || behavior.getBehavior() == BehaviorMode.Farm) {
                            event.getPlayer().sendMessage(Util.formatText(Translation.getString("Message.Skill.Control.AggroFarm", event.getPlayer()), myPet.getPetName(), behavior.getBehavior().name()));
                            return;
                        }
                    }
                    if (myPet.getSkills().isActive(Ride.class)) {
                        if (myPet.getEntity().get().getHandle().hasRider()) {
                            event.getPlayer().sendMessage(Util.formatText(Translation.getString("Message.Skill.Control.Ride", event.getPlayer()), myPet.getPetName()));
                            return;
                        }
                    }
                    if (!Permissions.hasExtended(event.getPlayer(), "MyPet.extended.control")) {
                        myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner()), 10000);
                        return;
                    }
                    Block block = event.getPlayer().getTargetBlock(null, 100);
                    if (block != null && block.getType() != Material.AIR) {
                        if (!block.getType().isSolid()) {
                            block = block.getRelative(BlockFace.DOWN);
                        }
                        myPet.getSkills().get(ControlImpl.class).setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(PlayerGameModeChangeEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (event.getNewGameMode().name().equals("SPECTATOR")) {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                if (myPetPlayerDamagee.hasMyPet()) {
                    myPetPlayerDamagee.getMyPet().removePet();
                }
            }
        } else {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                if (myPetPlayerDamagee.hasMyPet()) {
                    MyPet myPet = myPetPlayerDamagee.getMyPet();
                    if (myPet.wantsToRespawn()) {
                        switch (myPet.createEntity()) {
                            case Success:
                                myPetPlayerDamagee.sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", myPetPlayerDamagee), myPet.getPetName()));
                                break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            if (event.getRightClicked() instanceof MyPetBukkitEntity) {
                if (((MyPetBukkitEntity) event.getRightClicked()).getOwner().equals(event.getPlayer())) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        long delay = MyPetApi.getRepository() instanceof SqLiteRepository ? 1L : Configuration.Repository.EXTERNAL_LOAD_DELAY;

        new BukkitRunnable() {
            @Override
            public void run() {
                MyPetApi.getRepository().getMyPetPlayer(event.getPlayer(), new RepositoryCallback<MyPetPlayer>() {
                    @Override
                    public void callback(final MyPetPlayer p) {
                        final MyPetPlayerImpl joinedPlayer = (MyPetPlayerImpl) p;

                        joinedPlayer.setLastKnownName(event.getPlayer().getName());
                        if (!event.getPlayer().getUniqueId().equals(joinedPlayer.getOfflineUUID())) {
                            if (joinedPlayer.getMojangUUID() == null) {
                                joinedPlayer.setMojangUUID(event.getPlayer().getUniqueId());
                            }
                            joinedPlayer.setOnlineMode(true);
                        }

                        MyPetApi.getPlayerManager().setOnline(joinedPlayer);

                        final WorldGroup joinGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());
                        if (joinedPlayer.hasMyPet()) {
                            MyPet myPet = joinedPlayer.getMyPet();
                            if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                                MyPetApi.getMyPetManager().deactivateMyPet(joinedPlayer, true);
                            }
                        }

                        if (!joinedPlayer.hasMyPet() && joinedPlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                            final UUID petUUID = joinedPlayer.getMyPetForWorldGroup(joinGroup.getName());
                            MyPetApi.getRepository().getMyPet(petUUID, new RepositoryCallback<StoredMyPet>() {
                                @Override
                                public void callback(StoredMyPet storedMyPet) {
                                    MyPetApi.getMyPetManager().activateMyPet(storedMyPet);

                                    if (joinedPlayer.hasMyPet()) {
                                        final MyPet myPet = joinedPlayer.getMyPet();
                                        if (myPet.wantsToRespawn()) {
                                            switch (myPet.createEntity()) {
                                                case Canceled:
                                                    joinedPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", joinedPlayer), myPet.getPetName()));
                                                    break;
                                                case NotAllowed:
                                                    joinedPlayer.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", joinedPlayer), myPet.getPetName()));
                                                    break;
                                                case Dead:
                                                    if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                        joinedPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", joinedPlayer), myPet.getPetName()));
                                                    } else {
                                                        joinedPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", joinedPlayer), myPet.getPetName(), myPet.getRespawnTime()));
                                                    }
                                                    break;
                                                case Flying:
                                                    joinedPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", joinedPlayer), myPet.getPetName()));
                                                    break;
                                                case NoSpace:
                                                    joinedPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", joinedPlayer), myPet.getPetName()));
                                                    break;
                                            }
                                        }
                                    }
                                }
                            });
                        }
                        joinedPlayer.checkForDonation();

                        Bukkit.getServer().getPluginManager().callEvent(new MyPetPlayerJoinEvent(joinedPlayer));
                    }
                });
            }
        }.runTaskLater(MyPetApi.getPlugin(), delay);

        if (Configuration.Update.SHOW_OP && event.getPlayer().isOp() && Updater.isUpdateAvailable()) {
            event.getPlayer().sendMessage(Translation.getString("Message.Update.Available", event.getPlayer()) + " " + Updater.getLatest());
            event.getPlayer().sendMessage(ChatColor.DARK_GREEN + "    https://mypet-plugin.de/download");

        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (WorldGroup.getGroupByWorld(victim.getWorld()).isDisabled()) {
                return;
            }
            if (event.getDamager() instanceof CraftMyPetProjectile) {
                CraftMyPetProjectile projectile = (CraftMyPetProjectile) event.getDamager();
                if (MyPetApi.getPlayerManager().isMyPetPlayer(victim)) {
                    MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(victim);
                    if (myPetPlayerDamagee.hasMyPet()) {
                        if (projectile != null && projectile.getMyPetProjectile().getShooter() != null) {
                            if (myPetPlayerDamagee.getMyPet() == projectile.getMyPetProjectile().getShooter().getMyPet()) {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
                if (projectile.getMyPetProjectile().getShooter() != null) {
                    if (!MyPetApi.getHookHelper().canHurt(projectile.getShootingMyPet().getOwner().getPlayer(), victim, true)) {
                        event.setCancelled(true);
                    }
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (WorldGroup.getGroupByWorld(victim.getWorld()).isDisabled()) {
                return;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(victim)) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                        victim.isInsideVehicle() &&
                        victim.getVehicle() instanceof MyPetBukkitEntity) {
                    event.setCancelled(true);
                    return;
                }
                MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(victim);
                if (myPetPlayerDamagee.hasMyPet()) {
                    MyPet myPet = myPetPlayerDamagee.getMyPet();
                    if (myPet.getSkills().has(ShieldImpl.class)) {
                        ShieldImpl shield = myPet.getSkills().get(ShieldImpl.class);
                        if (shield.trigger()) {
                            shield.apply(event);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerQuitEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet()) {
                MyPet myPet = player.getMyPet();

                if (myPet.getStatus() == MyPet.PetState.Here) {
                    myPet.removePet(true);
                }
                MyPetApi.getMyPetManager().deactivateMyPet(player, true);
            }

            MyPetApi.getPlayerManager().setOffline(player);
        }
    }

    @EventHandler
    public void onMyPetPlayerChangeWorld(final PlayerChangedWorldEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }

        final WorldGroup toGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());

        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());

            final WorldGroup fromGroup = WorldGroup.getGroupByWorld(event.getFrom().getName());

            final MyPet myPet = myPetPlayer.hasMyPet() ? myPetPlayer.getMyPet() : null;
            final BukkitRunnable callPet = new BukkitRunnable() {
                public void run() {
                    if (myPetPlayer.isOnline() && myPetPlayer.hasMyPet()) {
                        MyPet runMyPet = myPetPlayer.getMyPet();
                        switch (runMyPet.createEntity()) {
                            case Canceled:
                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPetPlayer), runMyPet.getPetName()));
                                break;
                            case NoSpace:
                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPetPlayer), runMyPet.getPetName()));
                                break;
                            case NotAllowed:
                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", myPetPlayer), runMyPet.getPetName()));
                                break;
                            case Dead:
                                if (runMyPet != myPet) {
                                    if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                        myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", runMyPet.getOwner()), runMyPet.getPetName()));
                                    } else {
                                        myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead.Respawn", runMyPet.getOwner()), runMyPet.getPetName(), runMyPet.getRespawnTime()));
                                    }
                                }
                                break;
                            case Flying:
                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPetPlayer), runMyPet.getPetName()));
                                break;
                            case Success:
                                if (runMyPet != myPet) {
                                    myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", myPetPlayer), runMyPet.getPetName()));
                                }
                                break;
                        }
                    }
                }
            };

            if (fromGroup != toGroup) {
                final boolean hadMyPetInFromWorld = MyPetApi.getMyPetManager().deactivateMyPet(myPetPlayer, true);

                if (toGroup.isDisabled()) {
                    return;
                }

                if (myPetPlayer.hasMyPetInWorldGroup(toGroup)) {
                    final UUID groupMyPetUUID = myPetPlayer.getMyPetForWorldGroup(toGroup);
                    MyPetApi.getRepository().getMyPets(myPetPlayer, new RepositoryCallback<List<StoredMyPet>>() {
                        @Override
                        public void callback(List<StoredMyPet> pets) {
                            for (StoredMyPet myPet : pets) {
                                if (myPet.getUUID().equals(groupMyPetUUID)) {
                                    MyPetApi.getMyPetManager().activateMyPet(myPet);
                                    break;
                                }
                            }
                            if (myPetPlayer.hasMyPet()) {
                                if (myPetPlayer.getMyPet().wantsToRespawn()) {
                                    callPet.runTaskLater(MyPetApi.getPlugin(), 20L);
                                }
                            } else {
                                myPetPlayer.setMyPetForWorldGroup(toGroup, null);
                            }
                        }
                    });
                } else if (hadMyPetInFromWorld) {
                    myPetPlayer.getPlayer().sendMessage(Translation.getString("Message.MultiWorld.NoActivePetInThisWorld", myPetPlayer));
                }
            } else if (myPet != null) {
                if (myPet.wantsToRespawn()) {
                    callPet.runTaskLater(MyPetApi.getPlugin(), 20L);
                }
            }
        }
    }

    @EventHandler
    public void onMyPet(PlayerTeleportEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isInsideVehicle() && player.getVehicle() instanceof MyPetBukkitEntity) {
            if (player.getLocation().getWorld() != event.getTo().getWorld() || MyPetApi.getPlatformHelper().distance(event.getFrom(), event.getTo()) > 10) {
                if (Configuration.Skilltree.Skill.Ride.PREVENT_TELEPORTATION) {
                    event.setCancelled(true);
                    player.sendMessage(Translation.getString("Message.Skill.Ride.NoTeleport", player));
                    return;
                }
                player.getVehicle().eject();
            }
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            if (myPetPlayer.hasMyPet()) {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == MyPet.PetState.Here) {
                    if (event.getFrom().getWorld() != event.getTo().getWorld() || MyPetApi.getPlatformHelper().distance(event.getFrom(), event.getTo()) > 10) {
                        final boolean sameWorld = event.getFrom().getWorld() == event.getTo().getWorld();
                        myPet.removePet();
                        new BukkitRunnable() {
                            public void run() {
                                if (myPetPlayer.isOnline() && myPetPlayer.hasMyPet()) {
                                    MyPet runMyPet = myPetPlayer.getMyPet();
                                    switch (runMyPet.createEntity()) {
                                        case Canceled:
                                            myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPetPlayer), runMyPet.getPetName()));
                                            break;
                                        case NoSpace:
                                            if (sameWorld) {
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPetPlayer), runMyPet.getPetName()));
                                            }
                                            break;
                                        case Flying:
                                            if (sameWorld) {
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPetPlayer), runMyPet.getPetName()));
                                            }
                                            break;
                                        case NotAllowed:
                                            myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", myPetPlayer), runMyPet.getPetName()));
                                            break;
                                    }
                                }
                            }
                        }.runTaskLater(MyPetApi.getPlugin(), 20L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (!MyPetApi.getPlatformHelper().compareBlockPositions(event.getFrom(), event.getTo())) {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                    if (!MyPetApi.getHookHelper().isPetAllowed(player)) {
                        player.getMyPet().removePet(true);
                        player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerDeathEvent event) {
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getEntity())) {
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getEntity());
            if (myPetPlayer.hasMyPet()) {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == MyPet.PetState.Here && Configuration.Skilltree.Skill.Backpack.DROP_WHEN_OWNER_DIES) {
                    if (myPet.getSkills().isActive(BackpackImpl.class)) {
                        CustomInventory inv = myPet.getSkills().get(BackpackImpl.class).getInventory();
                        inv.dropContentAt(myPet.getLocation().get());
                    }
                }
                myPet.removePet();
            }
        }
    }

    @EventHandler
    public void on(final PlayerRespawnEvent event) {
        if (WorldGroup.getGroupByWorld(event.getPlayer().getWorld()).isDisabled()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            final MyPetPlayer respawnedMyPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            final MyPet myPet = respawnedMyPetPlayer.getMyPet();

            if (respawnedMyPetPlayer.hasMyPet() && myPet.wantsToRespawn()) {
                new BukkitRunnable() {
                    public void run() {
                        if (respawnedMyPetPlayer.hasMyPet()) {
                            MyPet runMyPet = respawnedMyPetPlayer.getMyPet();
                            switch (runMyPet.createEntity()) {
                                case Canceled:
                                    respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", respawnedMyPetPlayer), runMyPet.getPetName()));
                                    break;
                                case NoSpace:
                                    respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", respawnedMyPetPlayer), runMyPet.getPetName()));
                                    break;
                                case NotAllowed:
                                    respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", respawnedMyPetPlayer), runMyPet.getPetName()));
                                    break;
                                case Dead:
                                    if (runMyPet != myPet) {
                                        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                            respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", respawnedMyPetPlayer), myPet.getPetName()));
                                        } else {
                                            respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead.Respawn", respawnedMyPetPlayer), runMyPet.getPetName(), runMyPet.getRespawnTime()));
                                        }
                                    }
                                    break;
                                case Flying:
                                    respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", respawnedMyPetPlayer), runMyPet.getPetName()));
                                    break;
                                case Success:
                                    if (runMyPet != myPet) {
                                        respawnedMyPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", respawnedMyPetPlayer), runMyPet.getPetName()));
                                    }
                                    break;
                            }
                        }
                    }
                }.runTaskLater(MyPetApi.getPlugin(), 25L);
            }
        }
    }
}
