/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Control;
import de.Keyle.MyPet.skill.skills.implementation.Inventory;
import de.Keyle.MyPet.skill.skills.implementation.Ride;
import de.Keyle.MyPet.skill.skills.implementation.inventory.CustomInventory;
import de.Keyle.MyPet.skill.skills.implementation.ranged.MyPetProjectile;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.hooks.PvPChecker;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.HashSet;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final int[] ControllIgnoreBlocks = {6, 27, 28, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 90, 92, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 116, 117, 118, 119};

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) && Control.CONTROL_ITEM.compare(event.getPlayer().getItemInHand()) && MyPetList.hasMyPet(event.getPlayer())) {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.getStatus() == PetState.Here && myPet.getCraftPet().canMove()) {
                if (myPet.getSkills().isSkillActive(Control.class)) {
                    if (myPet.getSkills().isSkillActive(Behavior.class)) {
                        Behavior behavior = myPet.getSkills().getSkill(Behavior.class);
                        if (behavior.getBehavior() == BehaviorState.Aggressive || behavior.getBehavior() == BehaviorState.Farm) {
                            event.getPlayer().sendMessage(Util.formatText(Locales.getString("Message.Skill.Control.AggroFarm", event.getPlayer()), myPet.getPetName(), behavior.getBehavior().name()));
                            return;
                        }
                    }
                    if (myPet.getSkills().isSkillActive(Ride.class)) {
                        if (myPet.getCraftPet().getHandle().hasRider()) {
                            event.getPlayer().sendMessage(Util.formatText(Locales.getString("Message.Skill.Control.Ride", event.getPlayer()), myPet.getPetName()));
                            return;
                        }
                    }
                    if (!Permissions.hasExtended(event.getPlayer(), "MyPet.user.extended.Control")) {
                        myPet.sendMessageToOwner(Locales.getString("Message.No.CanUse", myPet.getOwner().getLanguage()));
                        return;
                    }
                    Block block = event.getPlayer().getTargetBlock((HashSet<Byte>) null, 100);
                    if (block != null && block.getType() != Material.AIR) {
                        for (int i : ControllIgnoreBlocks) {
                            if (block.getTypeId() == i) {
                                block = block.getRelative(BlockFace.DOWN);
                                break;
                            }
                        }
                        myPet.getSkills().getSkill(Control.class).setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        PlayerList.onlinePlayerUUIDList.add(event.getPlayer().getUniqueId());

        MyPetPlayer joinedPlayer = MyPetPlugin.getPlugin().getRepository().getMyPetPlayer(event.getPlayer());

        if (joinedPlayer != null) {
            PlayerList.setOnline(joinedPlayer);

            if (BukkitUtil.isInOnlineMode()) {
                if (joinedPlayer instanceof OnlineMyPetPlayer) {
                    ((OnlineMyPetPlayer) joinedPlayer).setLastKnownName(event.getPlayer().getName());
                }
            }

            WorldGroup joinGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());
            if (joinedPlayer.hasMyPet()) {
                MyPet myPet = joinedPlayer.getMyPet();
                if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                    MyPetList.deactivateMyPet(joinedPlayer);
                }
            }

            if (joinGroup != null && !joinedPlayer.hasMyPet() && joinedPlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                UUID groupMyPetUUID = joinedPlayer.getMyPetForWorldGroup(joinGroup.getName());
                for (InactiveMyPet inactiveMyPet : joinedPlayer.getInactiveMyPets()) {
                    if (inactiveMyPet.getUUID().equals(groupMyPetUUID)) {
                        MyPetList.activateMyPet(inactiveMyPet);
                        MyPet activeMyPet = joinedPlayer.getMyPet();
                        activeMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.MultiWorld.NowActivePet", joinedPlayer), activeMyPet.getPetName()));
                        break;
                    }
                }
                if (!joinedPlayer.hasMyPet() && joinedPlayer.getInactiveMyPets().size() > 0) {
                    joinedPlayer.getPlayer().sendMessage(Locales.getString("Message.MultiWorld.NoActivePetInThisWorld", joinedPlayer));
                    joinedPlayer.setMyPetForWorldGroup(joinGroup.getName(), null);
                }
            }
            if (joinedPlayer.hasMyPet()) {
                final MyPet myPet = joinedPlayer.getMyPet();
                final MyPetPlayer myPetPlayer = myPet.getOwner();
                if (myPet.wantToRespawn()) {
                    MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                        public void run() {
                            if (myPetPlayer.hasMyPet()) {
                                MyPet runMyPet = myPetPlayer.getMyPet();
                                switch (runMyPet.createPet()) {
                                    case Canceled:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                        break;
                                    case NoSpace:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                        break;
                                    case NotAllowed:
                                        runMyPet.sendMessageToOwner(Locales.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                        break;
                                    case Dead:
                                        runMyPet.sendMessageToOwner(Locales.getString("Message.Spawn.Respawn.In", myPet.getOwner()).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime()));
                                        break;
                                    case Flying:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                        break;
                                    case Success:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Call.Success", myPet.getOwner()), runMyPet.getPetName()));
                                        break;
                                }
                            }
                        }
                    }, 10L);
                } else {
                    myPet.setStatus(PetState.Despawned);
                }
            }
            //donate-delete-start
            joinedPlayer.checkForDonation();
            //donate-delete-end
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (((CraftEntity) event.getDamager()).getHandle() instanceof MyPetProjectile) {
                MyPetProjectile projectile = (MyPetProjectile) ((CraftEntity) event.getDamager()).getHandle();
                if (PlayerList.isMyPetPlayer(victim)) {
                    MyPetPlayer myPetPlayerDamagee = PlayerList.getMyPetPlayer(victim);
                    if (myPetPlayerDamagee.hasMyPet()) {
                        if (myPetPlayerDamagee.getMyPet() == projectile.getShooter().getMyPet()) {
                            event.setCancelled(true);
                        }
                    }
                }
                if (!PvPChecker.canHurt(projectile.getShooter().getOwner().getPlayer(), victim, true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        PlayerList.onlinePlayerUUIDList.remove(event.getPlayer().getUniqueId());
        if (PlayerList.isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = PlayerList.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet()) {
                MyPet myPet = player.getMyPet();
                if (myPet.getSkills().isSkillActive(Behavior.class)) {
                    Behavior behavior = myPet.getSkills().getSkill(Behavior.class);
                    if (behavior.getBehavior() != BehaviorState.Normal && behavior.getBehavior() != BehaviorState.Friendly) {
                        behavior.setBehavior(BehaviorState.Normal);
                    }
                }
                myPet.removePet(true);
                MyPetList.deactivateMyPet(player);

                if (Configuration.STORE_PETS_ON_PLAYER_QUIT) {
                    MyPetPlugin.getPlugin().getRepository().save();
                }
            }

            PlayerList.setOffline(player);
        }
    }

    @EventHandler
    public void onMyPetPlayerChangeWorld(final PlayerChangedWorldEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }
        if (PlayerList.isMyPetPlayer(event.getPlayer().getName())) {
            final MyPetPlayer myPetPlayer = PlayerList.getMyPetPlayer(event.getPlayer());

            WorldGroup fromGroup = WorldGroup.getGroupByWorld(event.getFrom().getName());
            WorldGroup toGroup = WorldGroup.getGroupByWorld(event.getPlayer().getWorld().getName());

            boolean callAfterSwap = false;
            if (myPetPlayer.hasMyPet()) {
                callAfterSwap = myPetPlayer.getMyPet().getStatus() == PetState.Here;
                myPetPlayer.getMyPet().removePet(callAfterSwap);
            }

            boolean hadMyPetInFromWorld = false;
            if (fromGroup != toGroup) {
                if (myPetPlayer.hasMyPet()) {
                    hadMyPetInFromWorld = true;
                    MyPetList.deactivateMyPet(myPetPlayer);
                }
                if (myPetPlayer.hasMyPetInWorldGroup(toGroup.getName())) {
                    UUID groupMyPetUUID = myPetPlayer.getMyPetForWorldGroup(toGroup.getName());
                    for (InactiveMyPet inactiveMyPet : myPetPlayer.getInactiveMyPets()) {
                        if (inactiveMyPet.getUUID().equals(groupMyPetUUID)) {
                            MyPet activeMyPet = MyPetList.activateMyPet(inactiveMyPet);
                            if (activeMyPet != null) {
                                if (myPetPlayer.getInactiveMyPets().size() > 0) {
                                    activeMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.MultiWorld.NowActivePet", myPetPlayer), activeMyPet.getPetName()));
                                }
                                break;
                            }
                        }
                    }
                    if (!myPetPlayer.hasMyPet()) {
                        myPetPlayer.setMyPetForWorldGroup(toGroup.getName(), null);
                    }
                }

            }
            if (hadMyPetInFromWorld && !myPetPlayer.hasMyPet()) {
                myPetPlayer.getPlayer().sendMessage(Locales.getString("Message.MultiWorld.NoActivePetInThisWorld", myPetPlayer));
            } else {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (callAfterSwap) {
                    MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                        public void run() {
                            if (myPetPlayer.hasMyPet()) {
                                MyPet runMyPet = myPetPlayer.getMyPet();
                                switch (runMyPet.createPet()) {
                                    case Canceled:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                        break;
                                    case NoSpace:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                        break;
                                    case NotAllowed:
                                        runMyPet.sendMessageToOwner(Locales.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                        break;
                                    case Dead:
                                        if (runMyPet != myPet) {
                                            runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Call.Dead", myPet.getOwner()), runMyPet.getPetName(), runMyPet.getRespawnTime()));
                                        }
                                        break;
                                    case Flying:
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                        break;
                                    case Success:
                                        if (runMyPet != myPet) {
                                            runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Call.Success", myPet.getOwner()), runMyPet.getPetName()));
                                        }
                                        break;
                                }
                            }
                        }
                    }, 25L);
                }
            }
        }
    }

    @EventHandler
    public void onMyPetPlayerTeleport(final PlayerTeleportEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }
        if (PlayerList.isMyPetPlayer(event.getPlayer().getName())) {
            final MyPetPlayer myPetPlayer = PlayerList.getMyPetPlayer(event.getPlayer());
            if (myPetPlayer.hasMyPet()) {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here) {
                    if (myPet.getLocation().getWorld() != event.getTo().getWorld() || myPet.getLocation().distance(event.getTo()) > 10) {
                        myPet.removePet(false);
                        MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                            public void run() {
                                if (myPetPlayer.hasMyPet()) {
                                    MyPet runMyPet = myPetPlayer.getMyPet();
                                    switch (runMyPet.createPet()) {
                                        case Canceled:
                                            runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                            break;
                                        case NoSpace:
                                            runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                            break;
                                        case Flying:
                                            runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                            break;
                                        case NotAllowed:
                                            runMyPet.sendMessageToOwner(Locales.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                            break;
                                    }
                                }
                            }
                        }, 20L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (PlayerList.isMyPetPlayer(event.getEntity())) {
            MyPetPlayer myPetPlayer = PlayerList.getMyPetPlayer(event.getEntity());
            if (myPetPlayer.hasMyPet()) {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here && Inventory.DROP_WHEN_OWNER_DIES) {
                    if (myPet.getSkills().isSkillActive(Inventory.class)) {
                        CustomInventory inv = myPet.getSkills().getSkill(Inventory.class).inv;
                        inv.dropContentAt(myPet.getLocation());
                    }
                }
                myPet.removePet(true);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        if (PlayerList.isMyPetPlayer(event.getPlayer())) {
            final MyPetPlayer respawnedMyPetPlayer = PlayerList.getMyPetPlayer(event.getPlayer());
            final MyPet myPet = respawnedMyPetPlayer.getMyPet();

            if (respawnedMyPetPlayer.hasMyPet() && myPet.wantToRespawn()) {
                MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                    public void run() {
                        if (respawnedMyPetPlayer.hasMyPet()) {
                            MyPet runMyPet = respawnedMyPetPlayer.getMyPet();
                            switch (runMyPet.createPet()) {
                                case Canceled:
                                    runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Prevent", runMyPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NoSpace:
                                    runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.NoSpace", runMyPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NotAllowed:
                                    runMyPet.sendMessageToOwner(Locales.getString("Message.No.AllowedHere", runMyPet.getOwner()).replace("%petname%", runMyPet.getPetName()));
                                    break;
                                case Dead:
                                    if (runMyPet != myPet) {
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Call.Dead", runMyPet.getOwner()), runMyPet.getPetName(), runMyPet.getRespawnTime()));
                                    }
                                    break;
                                case Flying:
                                    runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                    break;
                                case Success:
                                    if (runMyPet != myPet) {
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Call.Success", runMyPet.getOwner()), runMyPet.getPetName()));
                                    }
                                    break;
                            }
                        }
                    }
                }, 25L);
            }
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && event.getEntity() instanceof Player) {
            if (event.getEntity().getVehicle() instanceof CraftMyPet) {
                event.setCancelled(true);
            }
        }
    }
}