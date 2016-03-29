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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetPlayerJoinEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skills.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.api.skill.skills.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.skill.skills.Inventory;
import de.Keyle.MyPet.skill.skills.Ride;
import de.Keyle.MyPet.util.hooks.PvPChecker;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import org.bukkit.Bukkit;
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

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) && Configuration.Skilltree.Skill.CONTROL_ITEM.compare(event.getPlayer().getItemInHand()) && MyPetApi.getMyPetManager().hasActiveMyPet(event.getPlayer())) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(event.getPlayer());
            if (myPet.getStatus() == MyPet.PetState.Here && myPet.getEntity().get().canMove()) {
                if (myPet.getSkills().isSkillActive(Control.class)) {
                    if (myPet.getSkills().isSkillActive(Behavior.class)) {
                        Behavior behavior = myPet.getSkills().getSkill(Behavior.class).get();
                        if (behavior.getBehavior() == BehaviorState.Aggressive || behavior.getBehavior() == BehaviorState.Farm) {
                            event.getPlayer().sendMessage(Util.formatText(Translation.getString("Message.Skill.Control.AggroFarm", event.getPlayer()), myPet.getPetName(), behavior.getBehavior().name()));
                            return;
                        }
                    }
                    if (myPet.getSkills().isSkillActive(Ride.class)) {
                        if (myPet.getEntity().get().getHandle().hasRider()) {
                            event.getPlayer().sendMessage(Util.formatText(Translation.getString("Message.Skill.Control.Ride", event.getPlayer()), myPet.getPetName()));
                            return;
                        }
                    }
                    if (!Permissions.hasExtendedLegacy(event.getPlayer(), "MyPet.extended.control")) {
                        myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner().getLanguage()));
                        return;
                    }
                    Block block = event.getPlayer().getTargetBlock((HashSet<Byte>) null, 100);
                    if (block != null && block.getType() != Material.AIR) {
                        if (!block.getType().isSolid()) {
                            block = block.getRelative(BlockFace.DOWN);
                        }
                        myPet.getSkills().getSkill(Control.class).get().setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(PlayerGameModeChangeEvent event) {
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
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            if (event.getRightClicked() instanceof MyPetBukkitEntity) {
                if (((MyPetBukkitEntity) event.getRightClicked()).getOwner().equals(event.getPlayer())) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        long delay = MyPetApi.getRepository() instanceof NbtRepository ? 1L : 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                MyPetApi.getRepository().getMyPetPlayer(event.getPlayer(), new RepositoryCallback<MyPetPlayer>() {
                    @Override
                    public void callback(final MyPetPlayer joinedPlayer) {
                        MyPetApi.getPlayerManager().setOnline(joinedPlayer);

                        if (MyPetApi.getPlugin().isInOnlineMode()) {
                            if (joinedPlayer instanceof OnlineMyPetPlayer) {
                                ((OnlineMyPetPlayer) joinedPlayer).setLastKnownName(event.getPlayer().getName());
                            }
                        }

                        final WorldGroup joinGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());
                        if (joinedPlayer.hasMyPet()) {
                            MyPet myPet = joinedPlayer.getMyPet();
                            if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                                MyPetApi.getMyPetManager().deactivateMyPet(joinedPlayer, true);
                            }
                        }

                        if (joinGroup != null && !joinedPlayer.hasMyPet() && joinedPlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                            final UUID petUUID = joinedPlayer.getMyPetForWorldGroup(joinGroup.getName());
                            MyPetApi.getRepository().getMyPet(petUUID, new RepositoryCallback<StoredMyPet>() {
                                @Override
                                public void callback(StoredMyPet storedMyPet) {
                                    MyPetApi.getMyPetManager().activateMyPet(storedMyPet);

                                    if (joinedPlayer.hasMyPet()) {
                                        final MyPet myPet = joinedPlayer.getMyPet();
                                        final MyPetPlayer myPetPlayer = myPet.getOwner();
                                        if (myPet.wantsToRespawn()) {
                                            if (myPetPlayer.hasMyPet()) {
                                                MyPet runMyPet = myPetPlayer.getMyPet();
                                                switch (runMyPet.createEntity()) {
                                                    case Canceled:
                                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                    case NotAllowed:
                                                        runMyPet.getOwner().sendMessage(Translation.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                                        break;
                                                    case Dead:
                                                        runMyPet.getOwner().sendMessage(Translation.getString("Message.Spawn.Respawn.In", myPet.getOwner()).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime()));
                                                        break;
                                                    case Flying:
                                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                                        break;
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                        }
                        //donate-delete-start
                        joinedPlayer.checkForDonation();
                        //donate-delete-end

                        Bukkit.getServer().getPluginManager().callEvent(new MyPetPlayerJoinEvent(joinedPlayer));
                    }
                });
            }
        }.runTaskLater(MyPetApi.getPlugin(), delay);
    }

    @EventHandler
    public void onPlayerDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (event.getDamager() instanceof CraftMyPetProjectile) {
                CraftMyPetProjectile projectile = (CraftMyPetProjectile) event.getDamager();
                if (MyPetApi.getPlayerManager().isMyPetPlayer(victim)) {
                    MyPetPlayer myPetPlayerDamagee = MyPetApi.getPlayerManager().getMyPetPlayer(victim);
                    if (myPetPlayerDamagee.hasMyPet()) {
                        if (myPetPlayerDamagee.getMyPet() == projectile.getMyPetProjectile().getShooter().getMyPet()) {
                            event.setCancelled(true);
                        }
                    }
                }
                if (!PvPChecker.canHurt(projectile.getMyPetProjectile().getShooter().getOwner().getPlayer(), victim, true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet()) {
                MyPet myPet = player.getMyPet();
                myPet.removePet();
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
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());

            final WorldGroup fromGroup = WorldGroup.getGroupByWorld(event.getFrom().getName());
            final WorldGroup toGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());


            final MyPet myPet = myPetPlayer.hasMyPet() ? myPetPlayer.getMyPet() : null;
            final BukkitRunnable callPet = new BukkitRunnable() {
                public void run() {
                    if (myPetPlayer.isOnline() && myPetPlayer.hasMyPet()) {
                        MyPet runMyPet = myPetPlayer.getMyPet();
                        switch (runMyPet.createEntity()) {
                            case Canceled:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", runMyPet.getOwner()), runMyPet.getPetName()));
                                break;
                            case NoSpace:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", runMyPet.getOwner()), runMyPet.getPetName()));
                                break;
                            case NotAllowed:
                                runMyPet.getOwner().sendMessage(Translation.getString("Message.No.AllowedHere", runMyPet.getOwner()).replace("%petname%", runMyPet.getPetName()));
                                break;
                            case Dead:
                                if (runMyPet != myPet) {
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", runMyPet.getOwner()), runMyPet.getPetName(), runMyPet.getRespawnTime()));
                                }
                                break;
                            case Flying:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", runMyPet.getOwner()), runMyPet.getPetName()));
                                break;
                            case Success:
                                if (runMyPet != myPet) {
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", runMyPet.getOwner()), runMyPet.getPetName()));
                                }
                                break;
                        }
                    }
                }
            };

            if (fromGroup != toGroup) {
                final boolean hadMyPetInFromWorld = MyPetApi.getMyPetManager().deactivateMyPet(myPetPlayer, true);
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
                                myPetPlayer.setMyPetForWorldGroup(toGroup.getName(), null);
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
    public void onMyPetPlayerTeleport(final PlayerTeleportEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer().getName())) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            if (myPetPlayer.hasMyPet()) {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == MyPet.PetState.Here) {
                    if (myPet.getLocation().get().getWorld() != event.getTo().getWorld() || myPet.getLocation().get().distance(event.getTo()) > 10) {
                        final boolean sameWorld = myPet.getLocation().get().getWorld() == event.getTo().getWorld();
                        myPet.removePet();
                        new BukkitRunnable() {
                            public void run() {
                                if (myPetPlayer.isOnline() && myPetPlayer.hasMyPet()) {
                                    MyPet runMyPet = myPetPlayer.getMyPet();
                                    switch (runMyPet.createEntity()) {
                                        case Canceled:
                                            runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                            break;
                                        case NoSpace:
                                            if (sameWorld) {
                                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                            }
                                            break;
                                        case Flying:
                                            if (sameWorld) {
                                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                            }
                                            break;
                                        case NotAllowed:
                                            runMyPet.getOwner().sendMessage(Translation.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
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
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getEntity())) {
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(event.getEntity());
            if (myPetPlayer.hasMyPet()) {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == MyPet.PetState.Here && Configuration.Skilltree.Skill.Inventory.DROP_WHEN_OWNER_DIES) {
                    if (myPet.getSkills().isSkillActive(Inventory.class)) {
                        CustomInventory inv = myPet.getSkills().getSkill(Inventory.class).get().getInventory();
                        inv.dropContentAt(myPet.getLocation().get());
                    }
                }
                myPet.removePet();
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
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
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", runMyPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NoSpace:
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", runMyPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NotAllowed:
                                    runMyPet.getOwner().sendMessage(Translation.getString("Message.No.AllowedHere", runMyPet.getOwner()).replace("%petname%", runMyPet.getPetName()));
                                    break;
                                case Dead:
                                    if (runMyPet != myPet) {
                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", runMyPet.getOwner()), runMyPet.getPetName(), runMyPet.getRespawnTime()));
                                    }
                                    break;
                                case Flying:
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                    break;
                                case Success:
                                    if (runMyPet != myPet) {
                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", runMyPet.getOwner()), runMyPet.getPetName()));
                                    }
                                    break;
                            }
                        }
                    }
                }.runTaskLater(MyPetApi.getPlugin(), 25L);
            }
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && event.getEntity() instanceof Player) {
            if (event.getEntity().getVehicle() instanceof MyPetBukkitEntity) {
                event.setCancelled(true);
            }
        }
    }
}