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

public class MyWolfSpoutEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final MyWolf Wolf;
    private MyWolfSpoutEventReason eventReason = MyWolfSpoutEventReason.Nothing;

    public enum MyWolfSpoutEventReason
    {
        Nothing, Name, Call
    }

    public MyWolfSpoutEvent(MyWolf Wolf, MyWolfSpoutEventReason eventReason)
    {
        this.Wolf = Wolf;
        this.eventReason = eventReason;
    }

    public Player getOwner()
    {
        return Wolf.getOwner().getPlayer();
    }

    public MyWolf getWolf()
    {
        return Wolf;
    }

    public MyWolfSpoutEventReason getEventReason()
    {
        return eventReason;
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