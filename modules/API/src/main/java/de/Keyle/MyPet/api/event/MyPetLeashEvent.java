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
import de.Keyle.MyPet.api.util.Since;
import org.bukkit.event.HandlerList;

@Deprecated
@Since("24.11.2016")
public class MyPetLeashEvent extends MyPetCreateEvent {
    public MyPetLeashEvent(MyPet myPet) {
        super(myPet, Source.Leash);
    }

    @Deprecated
    @Since("24.11.2016")
    public MyPetPlayer getLeasher() {
        return getOwner();
    }

    @Deprecated
    @Since("24.11.2016")
    public MyPet getPet() {
        return (MyPet) getMyPet();
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}