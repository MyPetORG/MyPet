/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyPetLevelEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final MyPet myPet;
    private final int level;
    private final boolean beQuiet;

    public MyPetLevelEvent(MyPet myPet, int Level) {
        this.myPet = myPet;
        this.level = Level;
        this.beQuiet = true;
    }

    public MyPetLevelEvent(MyPet myPet, int level, boolean beQuiet) {
        this.myPet = myPet;
        this.level = level;
        this.beQuiet = beQuiet;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public boolean isQuiet() {
        return beQuiet;
    }

    public MyPet getPet() {
        return myPet;
    }

    public int getLevel() {
        return level;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}