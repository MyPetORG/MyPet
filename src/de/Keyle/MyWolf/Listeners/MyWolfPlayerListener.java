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

package de.Keyle.MyWolf.Listeners;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.BehaviorState;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.Skill.MyWolfSkill;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.EntityWolf;
import net.minecraft.server.PathEntity;
import net.minecraft.server.PathPoint;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

public class MyWolfPlayerListener implements Listener
{
    private final int[] ControllIgnoreBlocks = {78, 6, 31, 37, 38, 39, 40, 44, 50, 51, 59, 65, 66, 67, 69, 70, 72, 75, 76, 77, 90, 96};

    @EventHandler()
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if (event.getRightClicked() instanceof Wolf && ((Wolf) event.getRightClicked()).isTamed())
        {
            if (event.getPlayer().getItemInHand().getType() == MyWolfConfig.ControlItem)
            {
                Wolf w = (Wolf) event.getRightClicked();
                if (MyWolfPlugin.MWWolves.containsKey(event.getPlayer().getName()))
                {
                    MyWolf Wolf = MyWolfPlugin.MWWolves.get(event.getPlayer().getName());
                    Wolf.ResetSitTimer();
                    if (Wolf.getID() == w.getEntityId())
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler()
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) && event.getPlayer().getItemInHand().getType() == MyWolfConfig.ControlItem && MyWolfPlugin.MWWolves.containsKey(event.getPlayer().getName())) // && cb.cv.WolfControlItemSneak == event.getPlayer().isSneaking()
        {
            MyWolf Wolf = MyWolfPlugin.MWWolves.get(event.getPlayer().getName());
            if (Wolf.Status == WolfState.Here && !Wolf.isSitting())
            {
                if (!MyWolfPermissions.has(event.getPlayer(), "MyWolf.Skills.control.walk"))
                {
                    return;
                }
                if (!MyWolfSkill.hasSkill(Wolf.Abilities, "Control"))
                {
                    return;
                }
                Block block = event.getPlayer().getTargetBlock(null, 100);
                if (block != null)
                {
                    for (int i : ControllIgnoreBlocks)
                    {
                        if (block.getTypeId() == i)
                        {
                            block = block.getRelative(BlockFace.DOWN);
                            break;
                        }
                    }
                    PathPoint[] loc = {new PathPoint(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ())};
                    EntityWolf wolf = ((CraftWolf) Wolf.Wolf).getHandle();
                    wolf.setPathEntity(new PathEntity(loc));
                    Wolf.ResetSitTimer();
                    if (!MyWolfPermissions.has(event.getPlayer(), "MyWolf.Skills.control.attack"))
                    {
                        return;
                    }
                    for (Entity e : Wolf.Wolf.getNearbyEntities(1, 1, 1))
                    {
                        if (e instanceof LivingEntity)
                        {
                            if (Wolf.Behavior == BehaviorState.Raid)
                            {
                                if (e instanceof Player || (e instanceof Wolf && ((Wolf) e).isTamed()))
                                {
                                    continue;
                                }
                            }
                            if (e instanceof Player)
                            {
                                if (e != Wolf.getOwner() && !MyWolfUtil.isNPC((Player) e) && e.getWorld().getPVP())
                                {
                                    Wolf.Wolf.setTarget((LivingEntity) e);
                                }
                            }
                            else
                            {
                                MyWolfPlugin.MWWolves.get(event.getPlayer().getName()).Wolf.setTarget((LivingEntity) e);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler()
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        if (MyWolfPlugin.MWWolves.containsKey(event.getPlayer().getName()))
        {
            MyWolf Wolf = MyWolfPlugin.MWWolves.get(event.getPlayer().getName());
            /*
            // For future of the client mod

            String EntityIDs = Wolf.getID() + "\0";
            for(MyWolf w : MyWolfPlugin.MWWolves.values())
            {
                if(w.Status == WolfState.Here)
                {
                    EntityIDs += w.getID() + "\0";
                }
            }
            event.getPlayer().sendPluginMessage(MyWolfPlugin.Plugin,"MyWolfByKeyle",EntityIDs.getBytes());
            */

            if (Wolf.Status == WolfState.Dead)
            {
                Wolf.Timer();
            }
            else if (MyWolfUtil.getDistance(Wolf.getLocation(), event.getPlayer().getLocation()) < 75)
            {
                Wolf.ResetSitTimer();
                Wolf.createWolf(Wolf.isSitting());
            }
            else
            {
                Wolf.Status = WolfState.Despawned;
            }
            /*
            Wolf.hpbar.setX(SpoutCraftPlayer.getPlayer(event.getPlayer()).getMainScreen().getHealthBar().getX());
            Wolf.hpbar.setY(SpoutCraftPlayer.getPlayer(event.getPlayer()).getMainScreen().getHealthBar().getY()-30);
            Wolf.hpbar.setHeight(SpoutCraftPlayer.getPlayer(event.getPlayer()).getMainScreen().getHealthBar().getHeight());
            SpoutCraftPlayer.getPlayer(event.getPlayer()).getMainScreen().attachWidget(Wolf.hpbar);
            Wolf.updateHPbar();
            */
        }
    }

    @EventHandler()
    public void onPlayerPortal(final PlayerPortalEvent event)
    {
        if (MyWolfPlugin.MWWolves.containsKey(event.getPlayer().getName()))
        {
            MyWolf Wolf = MyWolfPlugin.MWWolves.get(event.getPlayer().getName());

            if (Wolf.Status == WolfState.Dead)
            {
                Wolf.Timer();
            }
            else if (MyWolfUtil.getDistance(Wolf.getLocation(), event.getPlayer().getLocation()) < 75)
            {
                Wolf.ResetSitTimer();
                Wolf.createWolf(Wolf.isSitting());
            }
            else
            {
                Wolf.Status = WolfState.Despawned;
            }
        }
    }

    @EventHandler()
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        if (MyWolfPlugin.MWWolves.containsKey(event.getPlayer().getName()))
        {
            MyWolf Wolf = MyWolfPlugin.MWWolves.get(event.getPlayer().getName());

            if (Wolf.Status == WolfState.Here)
            {
                Wolf.removeWolf();
                if (Wolf.getLocation() == null)
                {
                    Wolf.setLocation(event.getPlayer().getLocation());
                }
            }
            Wolf.StopTimer();
            MyWolfPlugin.Plugin.SaveWolves(MyWolfPlugin.MWWolvesConfig);
        }
    }

    @EventHandler()
    public void onPlayerMove(final PlayerMoveEvent event)
    {
        if (MyWolfPlugin.MWWolves.containsKey(event.getPlayer().getName()))
        {
            MyWolf Wolf = MyWolfPlugin.MWWolves.get(event.getPlayer().getName());

            Wolf.ResetSitTimer();
            if (Wolf.Status == WolfState.Here)
            {
                if (Wolf.getLocation().getWorld() != event.getPlayer().getLocation().getWorld())
                {
                    if (!Wolf.isSitting())
                    {
                        Wolf.removeWolf();
                        Wolf.setLocation(event.getPlayer().getLocation());
                        Wolf.createWolf(false);
                    }
                    else
                    {
                        Wolf.removeWolf();
                    }
                }
                else if (MyWolfUtil.getDistance(Wolf.getLocation(), event.getPlayer().getLocation()) > 75)
                {
                    Wolf.removeWolf();
                }
            }
            else if (Wolf.Status == WolfState.Despawned)
            {
                if (Wolf.getLocation().getWorld() == event.getPlayer().getLocation().getWorld())
                {
                    if (MyWolfUtil.getDistance(Wolf.getLocation(), event.getPlayer().getLocation()) < 75)
                    {
                        Wolf.createWolf(Wolf.isSitting());
                    }
                }
            }
        }
    }
}
