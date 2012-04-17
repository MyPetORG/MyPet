/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.listeners;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.skill.skills.Control;
import de.Keyle.MyWolf.skill.skills.Inventory;
import de.Keyle.MyWolf.util.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

public class MyWolfPlayerListener implements Listener
{
    private final int[] ControllIgnoreBlocks = {6, 27, 28, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 90, 92, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 116, 117, 118, 119};

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if ((event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && event.getPlayer().getItemInHand().getType() == Control.Item && MyWolfList.hasMyWolf(event.getPlayer()))
        {
            MyWolf MWolf = MyWolfList.getMyWolf(event.getPlayer());
            if (MWolf.Status == WolfState.Here && !MWolf.isSitting())
            {
                if (MWolf.SkillSystem.hasSkill("Control") && MWolf.SkillSystem.getSkill("Control").getLevel() > 0)
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
                        ((Control) MWolf.SkillSystem.getSkill("Control")).setMoveTo(block.getLocation());
                        MWolf.ResetSitTimer();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event)
    {
        if (event.isCancelled() || event.getPlayer().getItemInHand().getType() != MyWolfConfig.LeashItem || !(event.getRightClicked() instanceof LivingEntity))
        {
            return;
        }
        if (MyWolfList.hasMyWolf(event.getPlayer()))
        {
            MyWolf MWolf = MyWolfList.getMyWolf(event.getPlayer());
            if (event.getRightClicked() != MWolf.Wolf)
            {
                MWolf.Wolf.getHandle().Goaltarget = ((CraftLivingEntity) event.getRightClicked()).getHandle();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        if (MyWolfPermissions.has(event.getPlayer(), "MyWolf.user.leash"))
        {
            if (MyWolfList.hasInactiveMyWolf(event.getPlayer()))
            {
                /*
                if (MyWolfConfig.HeroesSkill && MyWolfUtil.getServer().getPluginManager().getPlugin("Heroes") != null && MyWolfConfig.HeroesPlugin != null)
                {
                    if (!MyWolfConfig.HeroesPlugin.getCharacterManager().getHero(event.getPlayer()).hasAccessToSkill("MyWolf"))
                    {
                        return;
                    }
                }
                */
                MyWolfList.setMyWolfActive(event.getPlayer(), true);
            }
            if (MyWolfList.hasMyWolf(event.getPlayer()))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(event.getPlayer());
                if (MWolf.Status == WolfState.Dead)
                {
                    event.getPlayer().sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_RespawnIn").replace("%wolfname%", MWolf.Name).replace("%time%", "" + MWolf.RespawnTime)));
                }
                else if (MyWolfUtil.getDistance(MWolf.getLocation(), event.getPlayer().getLocation()) < 75)
                {
                    MWolf.ResetSitTimer();
                    MWolf.createWolf(MWolf.isSitting());
                }
                else
                {
                    MWolf.Status = WolfState.Despawned;
                }
            }
        }
        else
        {
            if (MyWolfList.hasMyWolf(event.getPlayer()))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(event.getPlayer());

                if (MWolf.Status == WolfState.Here)
                {
                    MWolf.removeWolf();
                    if (MWolf.getLocation() == null)
                    {
                        MWolf.setLocation(event.getPlayer().getLocation());
                    }
                }
                MyWolfList.setMyWolfActive(event.getPlayer(), false);
            }
        }

        // For the future -> client mod
        /*
        String EntityIDs = MWolf.getID() + "\0";
        for(MyWolf MW : MyWolfList.getMyWolfList())
        {
            if(w.Status == WolfState.Here)
            {
                EntityIDs += w.getID() + "\0";
            }
        }
        event.getPlayer().sendPluginMessage(MyWolfPlugin.Plugin,"MyWolfByKeyle",EntityIDs.getBytes());
        */
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        if (MyWolfList.hasMyWolf(event.getPlayer()))
        {
            MyWolf MWolf = MyWolfList.getMyWolf(event.getPlayer());

            if (MWolf.Status == WolfState.Here)
            {
                MWolf.removeWolf();
                if (MWolf.getLocation() == null)
                {
                    MWolf.setLocation(event.getPlayer().getLocation());
                }
            }
            MyWolfPlugin.getPlugin().saveWolves(MyWolfPlugin.NBTWolvesFile);
            MyWolfPlugin.getPlugin().getTimer().resetTimer();
        }
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event)
    {
        if (MyWolfPermissions.has(event.getPlayer(), "MyWolf.user.leash"))
        {
            if (MyWolfList.hasInactiveMyWolf(event.getPlayer()))
            {
                MyWolfList.setMyWolfActive(event.getPlayer(), true);
            }
            if (MyWolfList.hasMyWolf(event.getPlayer()))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(event.getPlayer());

                MWolf.ResetSitTimer();
                if (MWolf.Status == WolfState.Here)
                {
                    if (Inventory.WolfChestOpened.contains(event.getPlayer()))
                    {
                        MWolf.setSitting(false);
                        Inventory.WolfChestOpened.remove(event.getPlayer());
                    }
                    if (MWolf.getLocation().getWorld() != event.getPlayer().getLocation().getWorld())
                    {
                        if (!MWolf.isSitting())
                        {
                            MWolf.removeWolf();
                            MWolf.setLocation(event.getPlayer().getLocation());
                            MWolf.createWolf(false);
                        }
                        else
                        {
                            MWolf.removeWolf();
                        }
                    }
                    else if (MyWolfUtil.getDistance(MWolf.getLocation(), event.getPlayer().getLocation()) > 75)
                    {
                        MWolf.removeWolf();
                    }
                }
                else if (MWolf.Status == WolfState.Despawned)
                {
                    if (MWolf.getLocation().getWorld() == event.getPlayer().getLocation().getWorld())
                    {
                        if (MyWolfUtil.getDistance(MWolf.getLocation(), event.getPlayer().getLocation()) < 75)
                        {
                            MWolf.createWolf(MWolf.isSitting());
                        }
                    }
                }
            }
        }
        else if (MyWolfList.hasMyWolf(event.getPlayer()))
        {
            MyWolfList.setMyWolfActive(event.getPlayer(), false);
        }
    }
}
