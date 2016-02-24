/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyPetTargetEvent extends Event implements Cancellable {
    protected static final HandlerList handlers = new HandlerList();

    protected final ActiveMyPet myPet;
    protected boolean isCancelled = false;
    protected Entity target;

    public MyPetTargetEvent(ActiveMyPet myPet, Entity target) {
        this.myPet = myPet;
        this.target = target;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public ActiveMyPet getPet() {
        return myPet;
    }

    public Entity getTarget() {
        return target;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean flag) {
        isCancelled = flag;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}