/*
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyPetSelectSetActiveEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    private final InactiveMyPet inactiveMyPet;
    private boolean isCanceled = false;

    public MyPetSelectSetActiveEvent(InactiveMyPet inactiveMyPet)
    {
        this.inactiveMyPet = inactiveMyPet;
    }

    public InactiveMyPet getPet()
    {
        return inactiveMyPet;
    }

    public MyPetPlayer getOwner()
    {
        return inactiveMyPet.getPetOwner();
    }

    public HandlerList getHandlers()
    {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public boolean isCancelled()
    {
        return isCanceled;
    }

    public void setCancelled(boolean flag)
    {
        isCanceled = flag;
    }
}