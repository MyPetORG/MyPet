/*
* Copyright (C) 2011 Keyle
*
* This file is part of MyWolf.
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

package de.Keyle.MyWolf;

import net.minecraft.server.EntityWolf;
import net.minecraft.server.PathEntity;
import net.minecraft.server.PathPoint;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftWolf;

public class MyWolfPlayerListener extends PlayerListener{
	
	private ConfigBuffer cb;
	
	public MyWolfPlayerListener(ConfigBuffer cb) {
		this.cb = cb;
    }
    
	@Override
	public void onPlayerInteract(final PlayerInteractEvent event)
	{
		 if(event.getAction().equals(Action.RIGHT_CLICK_AIR) && event.getPlayer().getItemInHand().getType() == cb.cv.WolfControlItem && cb.cv.WolfControlItemSneak == event.getPlayer().isSneaking() && cb.mWolves.containsKey(event.getPlayer().getName()))
		 {
			 if(cb.mWolves.get(event.getPlayer().getName()).isThere = true && cb.mWolves.get(event.getPlayer().getName()).isDead == false && cb.mWolves.get(event.getPlayer().getName()).MyWolf.isSitting() == false)
			 {
				if(cb.Permissions != null && cb.Permissions.has(event.getPlayer(), "mywolf.control.walk") == false)
				{
					return;
				}
				 Block block = event.getPlayer().getTargetBlock(null, 300);
	             if (block != null) {
	            	 
	            	 PathPoint[] loc = {new PathPoint(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ())};
	    			 EntityWolf wolf =((CraftWolf) cb.mWolves.get(event.getPlayer().getName()).MyWolf).getHandle();
	    			 wolf.a(new PathEntity(loc));
	    			 if(cb.Permissions != null && cb.Permissions.has(event.getPlayer(), "mywolf.control.attack") == false)
	    			 {
	    				 return;
	    			 }
	    			 for(Entity e : cb.mWolves.get(event.getPlayer().getName()).MyWolf.getNearbyEntities(1, 1, 1))
	    			 {
	    				 if(e instanceof LivingEntity)
	    				 {
	    					 if(e instanceof Player)
	    					 {
	    						 if(((Player)e).equals(cb.mWolves.get(event.getPlayer().getName()).getPlayer()) == false && cb.isNPC(event.getPlayer()) == false)
	    						 {
	    							 cb.mWolves.get(event.getPlayer().getName()).MyWolf.setTarget((LivingEntity)e);
	    						 }
	    					 }
	    					 else
	    					 {
	    						 cb.mWolves.get(event.getPlayer().getName()).MyWolf.setTarget((LivingEntity)e);
	    					 }
	    				 }
	    			 }
	             }
			 }
		 }
	}
	
    @Override
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
    	if (cb.mWolves.containsKey(event.getPlayer().getName()))
    	{
    		double dist = Math.sqrt(Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getX() - event.getPlayer().getLocation().getX(), 2.0D) + Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getZ() - event.getPlayer().getLocation().getZ(), 2.0D));
			if (dist < 75) 
    		{
				cb.mWolves.get(event.getPlayer().getName()).createWolf(cb.mWolves.get(event.getPlayer().getName()).isSitting);
   			}
    	}
    }
    
    @Override
    public void onPlayerQuit(PlayerQuitEvent event) 
    {
    	if (cb.mWolves.containsKey(event.getPlayer().getName()))
    	{
	    	if(cb.mWolves.get(event.getPlayer().getName()).isThere == true && cb.mWolves.get(event.getPlayer().getName()).isDead == false)
	    	{
	    		cb.mWolves.get(event.getPlayer().getName()).removeWolf();
	    		if(cb.mWolves.get(event.getPlayer().getName()).getLoc() == null)
	    		{
	    			cb.mWolves.get(event.getPlayer().getName()).WolfLocation = event.getPlayer().getLocation();
	    		}
	    	}
	    	cb.Plugin.SaveWolves();
    	}
    }
    
	@Override
    public void onPlayerMove(PlayerMoveEvent event)
    {
		//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("0");
		if (cb.mWolves.containsKey(event.getPlayer().getName()))
		{
			//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("1");
			if(cb.mWolves.get(event.getPlayer().getName()).isThere == false && cb.mWolves.get(event.getPlayer().getName()).isDead == false)
			{
				
				double dist = Math.sqrt(Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getX() - event.getPlayer().getLocation().getX(), 2.0D) + Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getZ() - event.getPlayer().getLocation().getZ(), 2.0D));
				//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.1 -- " + dist);
				if (cb.mWolves.get(event.getPlayer().getName()).RespawnTime == 0 && dist < 75 && cb.mWolves.get(event.getPlayer().getName()).WolfLocation.getWorld() == event.getPlayer().getLocation().getWorld()) 
	    		{
					//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.1.1");
					if(cb.mWolves.get(event.getPlayer().getName()).isDead == false)
					{
						cb.mWolves.get(event.getPlayer().getName()).createWolf(true);
					}
	    		}
			}
			else
			{
				//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.2");
				if(cb.mWolves.get(event.getPlayer().getName()).isDead == false && cb.mWolves.get(event.getPlayer().getName()).isSitting() == true )
				{
					
					double dist = Math.sqrt(Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getX() - event.getPlayer().getLocation().getX(), 2.0D) + Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getZ() - event.getPlayer().getLocation().getZ(), 2.0D));
					//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.2.1 -- " + dist);
					if (dist > 75 || cb.mWolves.get(event.getPlayer().getName()).WolfLocation.getWorld() != event.getPlayer().getLocation().getWorld()) 
	        		{
	    				//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.2.1.1");
	        			cb.mWolves.get(event.getPlayer().getName()).removeWolf();
	       			}
	        		else if (cb.mWolves.get(event.getPlayer().getName()).isThere == false && cb.mWolves.get(event.getPlayer().getName()).RespawnTime == 0)
	        		{
	        			//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.2.1.2");
	        			if(cb.mWolves.get(event.getPlayer().getName()).isDead == false)
						{
	        				cb.mWolves.get(event.getPlayer().getName()).createWolf(true);
						}
	        		}
				}
				else if(cb.mWolves.get(event.getPlayer().getName()).isDead == false)
				{
					
					double dist = Math.sqrt(Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getX() - event.getPlayer().getLocation().getX(), 2.0D) + Math.pow(cb.mWolves.get(event.getPlayer().getName()).getLoc().getZ() - event.getPlayer().getLocation().getZ(), 2.0D));
					//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.2.2 -- " + dist);
					if (cb.mWolves.get(event.getPlayer().getName()).RespawnTime == 0 && dist > 75 || cb.mWolves.get(event.getPlayer().getName()).WolfLocation.getWorld() != event.getPlayer().getLocation().getWorld() && cb.mWolves.get(event.getPlayer().getName()).isDead == false) 
	        		{
	    				//if(event.getPlayer().getName().equalsIgnoreCase("Keyle")) cb.log.info("2.2.2.1");
	    				cb.mWolves.get(event.getPlayer().getName()).removeWolf();
	    				cb.mWolves.get(event.getPlayer().getName()).WolfLocation = event.getPlayer().getLocation();
	    				cb.mWolves.get(event.getPlayer().getName()).createWolf(false);
	       			}
				}
			}
		}
    }
}