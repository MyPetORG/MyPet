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

package de.Keyle.MyWolf.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import de.Keyle.MyWolf.MyWolf;

public class LevelUpEvent extends Event implements Cancellable
{
	private static final long serialVersionUID = -605293022023540119L;

	protected MyWolf wolf;
	protected boolean cancelled;
	protected Location location = null;
	protected int Level;

	public LevelUpEvent(MyWolf wolf, int Level)
	{
		super("LevelUpEvent");
		this.wolf = wolf;
		this.Level = Level;
	}

	public LevelUpEvent(MyWolf wolf, int Level, Location location)
	{
		super("LevelUpEvent");
		this.wolf = wolf;
		this.location = location;
		this.Level = Level;
	}

	public Player getOwner()
	{
		return wolf.getOwner();
	}

	public MyWolf getWolf()
	{
		return wolf;
	}

	public Location getLocation()
	{
		return location;
	}

	public int getLevel()
	{
		return Level;
	}

	@Override
	public boolean isCancelled()
	{
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancel)
	{
		this.cancelled = cancel;
	}
}
