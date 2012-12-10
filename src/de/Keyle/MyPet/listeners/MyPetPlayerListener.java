/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.skill.skills.Ride;
import de.Keyle.MyPet.util.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MyPetPlayerListener implements Listener
{
    private final int[] ControllIgnoreBlocks = {6, 27, 28, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 90, 92, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 116, 117, 118, 119};

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if ((event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && event.getPlayer().getItemInHand().getType() == Control.item && MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.status == PetState.Here && myPet.getCraftPet().canMove())
            {
                if (myPet.getSkillSystem().getSkillLevel("Control") > 0)
                {
                    if (myPet.getSkillSystem().hasSkill("Behavior"))
                    {
                        Behavior behavior = (Behavior) myPet.getSkillSystem().getSkill("Behavior");
                        if (behavior.getLevel() > 0)
                        {
                            if (behavior.getBehavior() == BehaviorState.Aggressive || behavior.getBehavior() == BehaviorState.Farm)
                            {
                                event.getPlayer().sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_ControlAggroFarm").replace("%petname%", myPet.petName).replace("%mode%", "" + behavior.getBehavior().name())));
                                return;
                            }
                        }
                    }
                    if (myPet.getSkillSystem().hasSkill("Ride"))
                    {
                        Ride ride = (Ride) myPet.getSkillSystem().getSkill("Ride");
                        if (ride.getLevel() > 0)
                        {
                            if (myPet.getCraftPet().getHandle().isRidden())
                            {
                                return;
                            }
                        }
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
                        ((Control) myPet.getSkillSystem().getSkill("Control")).setMoveTo(block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        MyPetUtil.getDebugLogger().info("PlayerJoin: " + event.getPlayer().getName() + "     ----------------------------------");
        MyPetUtil.getDebugLogger().info("   - MyPetPlayer: " + (MyPetPlayer.isMyPetPlayer(event.getPlayer().getName()) ? MyPetPlayer.getMyPetPlayer(event.getPlayer().getName()).toString() : "false"));

        MyPetUtil.getDebugLogger().info("   - has an active MyPet: " + MyPetList.hasMyPet(event.getPlayer()));
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            if (!MyPetPermissions.has(event.getPlayer(), "MyPet.user.keep." + MyPetList.getMyPet(event.getPlayer()).getPetType().getTypeName()))
            {
                MyPetUtil.getDebugLogger().info("set MyPet of " + event.getPlayer().getName() + " to inactive");
                MyPetList.setMyPetInactive(event.getPlayer());
            }
        }
        MyPetUtil.getDebugLogger().info("   - has still an active MyPet: " + MyPetList.hasMyPet(event.getPlayer()));
        if (!MyPetList.hasMyPet(event.getPlayer()) && MyPetList.hasInactiveMyPets(event.getPlayer()))
        {
            for (InactiveMyPet inactiveMyPet : MyPetList.getInactiveMyPets(event.getPlayer()))
            {
                if (MyPetPermissions.has(event.getPlayer(), "MyPet.user.keep." + inactiveMyPet.getPetType().getTypeName()))
                {
                    MyPetList.setMyPetActive(inactiveMyPet);
                    break;
                }
            }
        }
        MyPetUtil.getDebugLogger().info("   - has an active MyPet: " + MyPetList.hasMyPet(event.getPlayer()));
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.status == PetState.Dead)
            {
                event.getPlayer().sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime)));
            }
            else if (MyPetUtil.getDistance2D(myPet.getLocation(), event.getPlayer().getLocation()) < 75 && myPet.getLocation().getWorld() == event.getPlayer().getLocation().getWorld())
            {
                myPet.createPet();
            }
            else
            {
                myPet.status = PetState.Despawned;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet myPet = MyPetList.getMyPet(event.getPlayer());
            if (myPet.getSkillSystem().hasSkill("Behavior"))
            {
                Behavior behavior = (Behavior) myPet.getSkillSystem().getSkill("Behavior");
                if (behavior.getLevel() > 0)
                {
                    if (behavior.getBehavior() == BehaviorState.Aggressive)
                    {
                        behavior.setBehavior(BehaviorState.Normal);
                    }
                }
            }
            myPet.removePet();
            MyPetPlugin.getPlugin().savePets(false);
            MyPetPlugin.getPlugin().getTimer().resetTimer();
        }
    }
}