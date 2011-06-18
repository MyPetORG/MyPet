package de.Keyle.MyWolf;

import org.bukkitcontrib.event.inventory.InventoryCloseEvent;
import org.bukkitcontrib.event.inventory.InventoryListener;

	public class MyWolfInventoryListener extends InventoryListener
	{
		ConfigBuffer cb;
		
		public MyWolfInventoryListener(ConfigBuffer cb)
		{
			this.cb = cb;
		}
		
		@Override
		public void onInventoryClose(InventoryCloseEvent event)
		{
			if(cb.WolfChestOpened.contains(event.getPlayer()) && cb.mWolves.containsKey(event.getPlayer().getName()))
			{
				cb.mWolves.get(event.getPlayer().getName()).MyWolf.setSitting(false);
				cb.WolfChestOpened.remove(event.getPlayer());
			}
		}
	}