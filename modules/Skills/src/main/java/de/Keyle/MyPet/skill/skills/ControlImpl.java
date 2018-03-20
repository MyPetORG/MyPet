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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skills.Control;
import org.bukkit.Location;

public class ControlImpl implements Control {
    private Location moveTo;
    private Location prevMoveTo;
    private boolean active = false;
    private MyPet myPet;

    public ControlImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void reset() {
        active = false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String toPrettyString() {
        return "";
    }

    public Location getLocation() {
        Location tmpMoveTo = moveTo;
        moveTo = null;
        return tmpMoveTo;
    }

    public Location getLocation(boolean delete) {
        Location tmpMoveTo = moveTo;
        if (delete) {
            moveTo = null;
        }
        return tmpMoveTo;
    }

    public void setMoveTo(Location loc) {
        if (!active) {
            return;
        }
        if (prevMoveTo != null) {
            if (MyPetApi.getPlatformHelper().distanceSquared(loc, prevMoveTo) > 1) {
                moveTo = loc;
                prevMoveTo = loc;
            }
        } else {
            moveTo = loc;
        }
    }

    @Override
    public String toString() {
        return "ControlImpl{" +
                "active=" + active +
                '}';
    }
}