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

public class MyPetPlayer
{
    private static List<MyPetPlayer> playerList = new ArrayList<MyPetPlayer>();

    private String playerName;

    private MyPetPlayer(String playerName)
    {
        this.playerName = playerName;
    }

    public String getName()
    {
        return playerName;
    }

    public boolean isOnline()
    {
        return getPlayer() != null && getPlayer().isOnline();
    }

    public Player getPlayer()
    {
        return MyPetUtil.getServer().getPlayer(playerName);
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

    public static MyPetPlayer getMyPetPlayer(Player player)
    {
        return MyPetPlayer.getMyPetPlayer(player.getName());
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
        else if (obj instanceof Player)
        {
            Player player = (Player) obj;
            return playerName.equals(player.getName());
        }
        else if (obj instanceof OfflinePlayer)
        {
            return ((OfflinePlayer) obj).getName().equals(playerName);
        }
        else if (obj instanceof MyPetPlayer)
        {
            return this == obj;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "MyPetPlayer{name=" + playerName + "}";
    }
}
