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

package de.Keyle.MyPet.event;

import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyPetLevelUpEvent extends Event
{
    private static final long serialVersionUID = -605293022023540119L;
    private static final HandlerList handlers = new HandlerList();

    private final MyWolf pet;
    private final int Level;
    private final boolean Quiet;

    public MyPetLevelUpEvent(MyWolf pet, int Level)
    {
        this.pet = pet;
        this.Level = Level;
        this.Quiet = false;
    }

    public MyPetLevelUpEvent(MyWolf pet, int Level, boolean Quiet)
    {
        this.pet = pet;
        this.Level = Level;
        this.Quiet = Quiet;
    }

    public Player getOwner()
    {
        return pet.getOwner().getPlayer();
    }

    public boolean isQuiet()
    {
        return Quiet;
    }

    public MyWolf getPet()
    {
        return pet;
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