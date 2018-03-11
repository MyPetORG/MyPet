/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

public class MyPetLevelUpEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final MyPet myPet;
    private final int level;
    private final int fromLevel;
    private final boolean beQuiet;

    public MyPetLevelUpEvent(MyPet myPet, int Level, int fromLevel) {
        this.myPet = myPet;
        this.level = Level;
        this.fromLevel = fromLevel;
        this.beQuiet = false;
    }

    public MyPetLevelUpEvent(MyPet myPet, int level, int fromLevel, boolean beQuiet) {
        this.myPet = myPet;
        this.level = level;
        this.fromLevel = fromLevel;
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

    public int fromLevel() {
        return fromLevel;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}