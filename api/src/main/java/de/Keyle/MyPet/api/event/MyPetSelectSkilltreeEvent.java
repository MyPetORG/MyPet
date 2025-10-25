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

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;

public class MyPetSelectSkilltreeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    protected final StoredMyPet myPet;
    protected final Skilltree skilltree;
    private final Source source;
    
    public enum Source {
        Auto, PlayerCommand, AdminCommand, AdminCreation, BossShopPro, Shop, Other
    }

    public MyPetSelectSkilltreeEvent(StoredMyPet myPet, Skilltree skilltree, Source source) {
        this.myPet = myPet;
        this.skilltree = skilltree;
        this.source = source;
    }
    
    public Source getSource() {
        return source;
    }

    public StoredMyPet getMyPet() {
        return myPet;
    }

    public Skilltree getSkilltree() {
        return skilltree;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public Player getPlayer() {
        return myPet.getOwner().getPlayer();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}