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

package Java.MyPet.event;

import Java.MyPet.entity.types.MyPet;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyPetLevelUpEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final MyPet myPet;
    private final int Level;
    private final boolean beQuiet;

    public MyPetLevelUpEvent(MyPet myPet, int Level)
    {
        this.myPet = myPet;
        this.Level = Level;
        this.beQuiet = false;
    }

    public MyPetLevelUpEvent(MyPet myPet, int level, boolean beQuiet)
    {
        this.myPet = myPet;
        this.Level = level;
        this.beQuiet = beQuiet;
    }

    public Player getOwner()
    {
        return myPet.getOwner().getPlayer();
    }

    public boolean isQuiet()
    {
        return beQuiet;
    }

    public MyPet getPet()
    {
        return myPet;
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