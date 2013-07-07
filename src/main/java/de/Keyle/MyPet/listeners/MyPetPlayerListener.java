/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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
import de.Keyle.MyPet.api.event.MyPetSpoutEvent;
import de.Keyle.MyPet.api.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Control;
import de.Keyle.MyPet.skill.skills.implementation.Inventory;
import de.Keyle.MyPet.skill.skills.implementation.inventory.MyPetCustomInventory;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

import static org.bukkit.Bukkit.getPluginManager;

public class MyPetPlayerListener implements Listener
{
    private final int[] ControllIgnoreBlocks = {6, 27, 28, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 90, 92, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 116, 117, 118, 119};

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if ((event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && event.getPlayer().getItemInHand().getTypeId() == Control.CONTROL_ITEM && MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.getStatus() == PetState.Here && myPet.getCraftPet().canMove())
            {
                if (myPet.getSkills().isSkillActive("Control"))
                {
                    if (myPet.getSkills().isSkillActive("Behavior"))
                    {
                        Behavior behavior = (Behavior) myPet.getSkills().getSkill("Behavior");
                        if (behavior.getBehavior() == BehaviorState.Aggressive || behavior.getBehavior() == BehaviorState.Farm)
                        {
                            event.getPlayer().sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.Skill.Control.AggroFarm", event.getPlayer()).replace("%petname%", myPet.getPetName()).replace("%mode%", "" + behavior.getBehavior().name())));
                            return;
                        }
                    }
                    if (myPet.getSkills().isSkillActive("Ride"))
                    {
                        if (myPet.getCraftPet().getHandle().hasRider())
                        {
                            return;
                        }
                    }
                    if (!MyPetPermissions.hasExtended(event.getPlayer(), "MyPet.user.extended.Control"))
                    {
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.CantUse", myPet.getOwner().getLanguage())));
                        return;
                    }
                    Block block = event.getPlayer().getTargetBlock(null, 100);
                    if (block != null && block.getType() != Material.AIR)
                    {
                        for (int i : ControllIgnoreBlocks)
                        {
                            if (block.getTypeId() == i)
                            {
                                block = block.getRelative(BlockFace.DOWN);
                                break;
                            }
                        }
                        ((Control) myPet.getSkills().getSkill("Control")).setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetPlayerJoin(final PlayerJoinEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getPlayer()))
        {
            MyPetPlayer joinedPlayer = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            MyPetWorldGroup joinGroup = MyPetWorldGroup.getGroup(event.getPlayer().getWorld().getName());
            if (joinGroup != null && !joinedPlayer.hasMyPet() && joinedPlayer.hasMyPetInWorldGroup(joinGroup.getName()))
            {
                UUID groupMyPetUUID = joinedPlayer.getMyPetForWorldGroup(joinGroup.getName());
                for (InactiveMyPet inactiveMyPet : joinedPlayer.getInactiveMyPets())
                {
                    if (inactiveMyPet.getUUID().equals(groupMyPetUUID))
                    {
                        MyPetList.setMyPetActive(inactiveMyPet);
                        MyPet activeMyPet = joinedPlayer.getMyPet();
                        activeMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NowActivePet", joinedPlayer).replace("%petname%", activeMyPet.getPetName())));
                        break;
                    }
                }
                if (!joinedPlayer.hasMyPet())
                {
                    joinedPlayer.getPlayer().sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NoActivePetInThisWorld", joinedPlayer)));
                    joinedPlayer.setMyPetForWorldGroup(joinGroup.getName(), null);
                }
            }
            if (joinedPlayer.hasMyPet())
            {
                MyPet myPet = joinedPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Dead)
                {
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnIn", myPet.getOwner().getLanguage()).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime())));
                }
                else if (myPet.wantToRespawn() && myPet.getLocation().getWorld() == event.getPlayer().getLocation().getWorld() && myPet.getLocation().distance(event.getPlayer().getLocation()) < 75)
                {
                    switch (myPet.createPet())
                    {
                        case Success:
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.Call", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
                            if (MyPetConfiguration.ENABLE_EVENTS)
                            {
                                getPluginManager().callEvent(new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.Call));
                            }
                            break;
                        case Canceled:
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnPrevent", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
                            break;
                        case NoSpace:
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnNoSpace", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
                            break;
                        case NotAllowed:
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NotAllowedHere", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
                            break;
                    }
                }
                else
                {
                    myPet.setStatus(PetState.Despawned);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.getSkills().isSkillActive("Behavior"))
            {
                Behavior behavior = (Behavior) myPet.getSkills().getSkill("Behavior");
                if (behavior.getBehavior() != BehaviorState.Normal && behavior.getBehavior() != BehaviorState.Friendly)
                {
                    behavior.setBehavior(BehaviorState.Normal);
                }
            }
            if (MyPetConfiguration.STORE_PETS_ON_PLAYER_QUIT)
            {
                MyPetPlugin.getPlugin().savePets(false);
            }
            myPet.removePet(true);
        }
    }

    @EventHandler
    public void onMyPetPlayerChangeWorld(final PlayerChangedWorldEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getPlayer().getName()))
        {
            final MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(event.getPlayer());

            MyPetWorldGroup fromGroup = MyPetWorldGroup.getGroup(event.getFrom().getName());
            MyPetWorldGroup toGroup = MyPetWorldGroup.getGroup(event.getPlayer().getWorld().getName());

            boolean callAfterSwap = myPetPlayer.hasMyPet() && myPetPlayer.getMyPet().getStatus() == PetState.Here;

            if (fromGroup != toGroup)
            {
                if (myPetPlayer.hasMyPet())
                {
                    MyPetList.setMyPetInactive(myPetPlayer);
                }
                if (myPetPlayer.hasMyPetInWorldGroup(toGroup.getName()))
                {
                    UUID groupMyPetUUID = myPetPlayer.getMyPetForWorldGroup(toGroup.getName());
                    for (InactiveMyPet inactiveMyPet : myPetPlayer.getInactiveMyPets())
                    {
                        if (inactiveMyPet.getUUID().equals(groupMyPetUUID))
                        {
                            MyPetList.setMyPetActive(inactiveMyPet);
                            MyPet activeMyPet = myPetPlayer.getMyPet();
                            activeMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NowActivePet", myPetPlayer).replace("%petname%", activeMyPet.getPetName())));
                            break;
                        }
                    }
                    if (!myPetPlayer.hasMyPet())
                    {
                        myPetPlayer.setMyPetForWorldGroup(toGroup.getName(), null);
                    }
                }

            }
            if (!myPetPlayer.hasMyPet())
            {
                myPetPlayer.getPlayer().sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NoActivePetInThisWorld", myPetPlayer)));
            }
            else
            {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (callAfterSwap)
                {
                    MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable()
                    {
                        public void run()
                        {
                            if (myPetPlayer.hasMyPet())
                            {
                                MyPet runMyPet = myPetPlayer.getMyPet();
                                switch (runMyPet.createPet())
                                {
                                    case Canceled:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnPrevent", myPet.getOwner().getLanguage())).replace("%petname%", runMyPet.getPetName()));
                                        break;
                                    case NoSpace:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnNoSpace", myPet.getOwner().getLanguage())).replace("%petname%", runMyPet.getPetName()));
                                        break;
                                    case NotAllowed:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NotAllowedHere", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
                                        break;
                                    case Dead:
                                        if (runMyPet != myPet)
                                        {
                                            runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.CallWhenDead", myPet.getOwner().getLanguage())).replace("%petname%", runMyPet.getPetName()).replace("%time%", "" + runMyPet.getRespawnTime()));
                                        }
                                        break;
                                    case Success:
                                        if (runMyPet != myPet)
                                        {
                                            runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.Call", myPet.getOwner().getLanguage())).replace("%petname%", runMyPet.getPetName()));
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
    public void onMyPetPlayerTeleport(final PlayerTeleportEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getPlayer().getName()))
        {
            final MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (myPetPlayer.hasMyPet())
            {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here)
                {

                    if (myPet.getLocation().getWorld() != event.getTo().getWorld() || myPet.getLocation().distance(event.getTo()) > 10)
                    {
                        myPet.removePet(true);
                    }

                    MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable()
                    {
                        public void run()
                        {
                            if (myPetPlayer.hasMyPet())
                            {
                                MyPet runMyPet = myPetPlayer.getMyPet();
                                switch (runMyPet.createPet())
                                {
                                    case Canceled:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnPrevent", myPet.getOwner().getLanguage())).replace("%petname%", runMyPet.getPetName()));
                                        break;
                                    case NoSpace:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnNoSpace", myPet.getOwner().getLanguage())).replace("%petname%", runMyPet.getPetName()));
                                        break;
                                    case NotAllowed:
                                        runMyPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NotAllowedHere", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
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
    public void onPlayerDeath(final PlayerDeathEvent event)
    {
        if (MyPetPlayer.isMyPetPlayer(event.getEntity()))
        {
            MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(event.getEntity());
            if (myPetPlayer.hasMyPet())
            {
                final MyPet myPet = myPetPlayer.getMyPet();
                if (myPet.getStatus() == PetState.Here && Inventory.DROP_WHEN_OWNER_DIES)
                {
                    if (myPet.getSkills().isSkillActive("Inventory"))
                    {
                        MyPetCustomInventory inv = ((Inventory) myPet.getSkills().getSkill("Inventory")).inv;
                        inv.dropContentAt(myPet.getLocation());
                    }
                }
            }
        }
    }
}