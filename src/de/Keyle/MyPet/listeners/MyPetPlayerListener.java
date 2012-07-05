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
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.util.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

public class MyPetPlayerListener implements Listener
{
    private final int[] ControllIgnoreBlocks = {6, 27, 28, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 90, 92, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 116, 117, 118, 119};

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if ((event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && event.getPlayer().getItemInHand().getType() == Control.Item && MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet MPet = MyPetList.getMyPet(event.getPlayer());
            if (MPet.Status == PetState.Here && !MPet.isSitting())
            {
                if (MPet.getSkillSystem().hasSkill("Control") && MPet.getSkillSystem().getSkill("Control").getLevel() > 0)
                {
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
                        ((Control) MPet.getSkillSystem().getSkill("Control")).setMoveTo(block.getLocation());
                        MPet.ResetSitTimer();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event)
    {
        if (event.isCancelled() || event.getPlayer().getItemInHand().getType() != MyPetConfig.LeashItem || !(event.getRightClicked() instanceof LivingEntity))
        {
            return;
        }
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet MPet = MyPetList.getMyPet(event.getPlayer());
            if (event.getRightClicked() != MPet.getPet())
            {
                MPet.getPet().getHandle().Goaltarget = ((CraftLivingEntity) event.getRightClicked()).getHandle();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        MyPetUtil.getDebugLogger().info("PlayerJoin: " + event.getPlayer().getName() + "     ----------------------------------");
        MyPetUtil.getDebugLogger().info("MyPetPlayer: " + (MyPetPlayer.isMyPetPlayer(event.getPlayer().getName()) ? MyPetPlayer.getMyPetPlayer(event.getPlayer().getName()).toString() : "false"));
        MyPetUtil.getDebugLogger().info("has MyPet-permission: " + MyPetPermissions.has(event.getPlayer(), "MyPet.user.leash"));
        if (MyPetPermissions.has(event.getPlayer(), "MyPet.user.leash"))
        {
            if (MyPetList.hasInactiveMyPet(event.getPlayer()))
            {
                /*
                if (MyPetConfig.HeroesSkill && MyPetUtil.getServer().getPluginManager().getPlugin("Heroes") != null && MyPetConfig.HeroesPlugin != null)
                {
                    if (!MyPetConfig.HeroesPlugin.getCharacterManager().getHero(event.getPlayer()).hasAccessToSkill("MyPet"))
                    {
                        return;
                    }
                }
                */
                MyPetList.setMyPetActive(event.getPlayer(), true);
            }
            MyPetUtil.getDebugLogger().info("has MyPet: " + MyPetList.hasMyPet(event.getPlayer()));
            if (MyPetList.hasMyPet(event.getPlayer()))
            {
                MyPet MPet = MyPetList.getMyPet(event.getPlayer());
                if (MPet.Status == PetState.Dead)
                {
                    event.getPlayer().sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", MPet.Name).replace("%time%", "" + MPet.RespawnTime)));
                }
                else if (MyPetUtil.getDistance(MPet.getLocation(), event.getPlayer().getLocation()) < 75 && MPet.getLocation().getWorld() == event.getPlayer().getLocation().getWorld())
                {
                    MPet.ResetSitTimer();
                    MPet.createPet();
                }
                else
                {
                    MPet.Status = PetState.Despawned;
                }
            }
            MyPetUtil.getDebugLogger().info("-------------------------------------------------------------");
        }
        else
        {
            if (MyPetList.hasMyPet(event.getPlayer()))
            {
                MyPet MPet = MyPetList.getMyPet(event.getPlayer());

                MPet.removePet();
                MyPetList.setMyPetActive(event.getPlayer(), false);
            }
        }

        // For the future -> client mod
        /*
        String EntityIDs = MPet.getID() + "\0";
        for(MyPet MW : MyPetList.getMyPetList())
        {
            if(w.Status == PetState.Here)
            {
                EntityIDs += w.getID() + "\0";
            }
        }
        event.getPlayer().sendPluginMessage(MyPetPlugin.Plugin,"MyPetByKeyle",EntityIDs.getBytes());
        */
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPet MPet = MyPetList.getMyPet(event.getPlayer());
            MPet.removePet();
            MyPetPlugin.getPlugin().savePets(MyPetPlugin.NBTPetFile);
            MyPetPlugin.getPlugin().getTimer().resetTimer();
        }
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event)
    {
        if (MyPetPermissions.has(event.getPlayer(), "MyPet.user.leash"))
        {
            if (MyPetList.hasInactiveMyPet(event.getPlayer()))
            {
                MyPetList.setMyPetActive(event.getPlayer(), true);
            }
            if (MyPetList.hasMyPet(event.getPlayer()))
            {
                MyPet MPet = MyPetList.getMyPet(event.getPlayer());
                if (MPet.Status == PetState.Here)
                {
                    MPet.ResetSitTimer();
                    if (MPet.getLocation().getWorld() != event.getPlayer().getLocation().getWorld() || MyPetUtil.getDistance(MPet.getLocation(), event.getPlayer().getLocation()) > 75)
                    {
                        if (MPet.isSitting())
                        {
                            MPet.removePet();
                        }
                        else
                        {
                            MPet.removePet();
                            MPet.setLocation(event.getPlayer().getLocation());
                            MPet.createPet();
                            MPet.setSitting(false);
                        }
                    }
                }
            }
        }
        else if (MyPetList.hasMyPet(event.getPlayer()))
        {
            MyPetList.setMyPetActive(event.getPlayer(), false);
        }
    }
}