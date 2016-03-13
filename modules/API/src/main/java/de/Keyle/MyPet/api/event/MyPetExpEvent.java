/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyPetExpEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final MyPet myPet;
    private boolean isCancelled = false;
    private double oldExp;
    private double newExp;

    public MyPetExpEvent(MyPet myPet, double oldExp, double newExp) {
        this.myPet = myPet;
        this.oldExp = oldExp;
        this.newExp = newExp;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public MyPet getPet() {
        return myPet;
    }

    public double getOldExp() {
        return oldExp;
    }

    public double getNewExp() {
        return newExp;
    }

    public void setNewEXP(double newExp) {
        this.newExp = newExp;
    }

    public double getExp() {
        if (isCancelled) {
            return oldExp;
        } else {
            return newExp;
        }
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