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

package de.Keyle.MyWolf.event;

import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyWolfLevelUpEvent extends Event
{
    private static final long serialVersionUID = -605293022023540119L;
    private static final HandlerList handlers = new HandlerList();

    private final MyWolf Wolf;
    private final int Level;
    private final boolean Quiet;

    public MyWolfLevelUpEvent(MyWolf Wolf, int Level)
    {
        this.Wolf = Wolf;
        this.Level = Level;
        this.Quiet = false;
    }

    public MyWolfLevelUpEvent(MyWolf Wolf, int Level, boolean Quiet)
    {
        this.Wolf = Wolf;
        this.Level = Level;
        this.Quiet = Quiet;
    }

    public Player getOwner()
    {
        return Wolf.getOwner().getPlayer();
    }

    public boolean isQuiet()
    {
        return Quiet;
    }

    public MyWolf getWolf()
    {
        return Wolf;
    }

    public int getLevel()
    {
        return Level;
    }

    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}