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

import de.Keyle.MyWolf.MyWolf;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class MyWolfLevelUpEvent extends Event
{
    private final MyWolf wolf;
    private final int Level;

    public MyWolfLevelUpEvent(MyWolf wolf, int Level)
    {
        super("MyWolfLevelUpEvent");
        this.wolf = wolf;
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

    public int getLevel()
    {
        return Level;
    }
}
