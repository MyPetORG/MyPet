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

package de.Keyle.MyPet.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyPetPlayer
{
    private static List<MyPetPlayer> playerList = new ArrayList<MyPetPlayer>();

    private String name;
    private UUID entityUUID;

    public MyPetPlayer(String name)
    {
        this.name = name;
    }

    public MyPetPlayer(String name, UUID entityUUID)
    {
        this.name = name;
        this.entityUUID = entityUUID;
    }

    public void setUUID(UUID entityUUID)
    {
        this.entityUUID = entityUUID;
    }

    public String getName()
    {
        return name;
    }

    public UUID getEntityUUID()
    {
        return entityUUID;
    }

    public boolean isOnline()
    {
        return getPlayer() != null;
    }

    public Player getPlayer()
    {
        if (entityUUID != null)
        {
            return MyPetUtil.getPlayerByUUID(entityUUID);
        }
        else
        {
            return MyPetUtil.getServer().getPlayer(name);
        }
    }

    public static MyPetPlayer getMyPetPlayer(String name, UUID entityUUID)
    {
        for (MyPetPlayer myPetPlayer : playerList)
        {
            if (myPetPlayer.getName().equals(name) && myPetPlayer.getEntityUUID().equals(entityUUID))
            {
                return myPetPlayer;
            }
        }
        MyPetPlayer myPetPlayer = new MyPetPlayer(name, entityUUID);
        playerList.add(myPetPlayer);
        return myPetPlayer;
    }

    public static MyPetPlayer getMyPetPlayer(String name)
    {
        for (MyPetPlayer myPetPlayer : playerList)
        {
            if (myPetPlayer.getName().equals(name))
            {
                return myPetPlayer;
            }
        }
        MyPetPlayer myPetPlayer = new MyPetPlayer(name);
        playerList.add(myPetPlayer);
        return myPetPlayer;
    }

    public static boolean isMyPetPlayer(String name, UUID entityUUID)
    {
        for (MyPetPlayer myPetPlayer : playerList)
        {
            if (myPetPlayer.getName().equals(name) && myPetPlayer.getEntityUUID().equals(entityUUID))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isMyPetPlayer(String name)
    {
        for (MyPetPlayer myPetPlayer : playerList)
        {
            if (myPetPlayer.getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    public static List<MyPetPlayer> getPlayerList()
    {
        return playerList;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj instanceof Player)
        {
            Player player = (Player) obj;
            return (entityUUID == null || entityUUID.equals(player.getUniqueId())) && name.equals(player.getName());
        }
        if (obj instanceof OfflinePlayer)
        {
            return ((OfflinePlayer) obj).getName().equals(name);
        }
        if (obj instanceof MyPetPlayer)
        {
            return this == obj;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "MyPetPlayer{name=" + name + ", UUID=" + (entityUUID != null ? entityUUID.toString() : "") + "}";
    }
}
