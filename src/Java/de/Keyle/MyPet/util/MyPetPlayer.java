/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyPetPlayer implements IScheduler
{
    private static List<MyPetPlayer> playerList = new ArrayList<MyPetPlayer>();

    private String playerName;
    private boolean customData = false;

    private boolean autoRespawn = false;
    private int autoRespawnMin = 1;
    private UUID lastActiveMyPetUUID;

    private MyPetPlayer(String playerName)
    {
        this.playerName = playerName;
    }

    public String getName()
    {
        return playerName;
    }

    public boolean hasCustomData()
    {
        return customData;
    }

    // Custom Data -----------------------------------------------------------------

    public void setAutoRespawnEnabled(boolean flag)
    {
        autoRespawn = flag;
        customData = true;
    }

    public boolean hasAutoRespawnEnabled()
    {
        return autoRespawn;
    }

    public void setAutoRespawnMin(int value)
    {
        autoRespawnMin = value;
        customData = true;
    }

    public int getAutoRespawnMin()
    {
        return autoRespawnMin;
    }

    public void setLastActiveMyPetUUID(UUID myPetUUID)
    {
        lastActiveMyPetUUID = myPetUUID;
        customData = true;
    }

    public UUID getLastActiveMyPetUUID()
    {
        return lastActiveMyPetUUID;
    }

    // -----------------------------------------------------------------------------

    public boolean isOnline()
    {
        return getPlayer() != null && getPlayer().isOnline();
    }

    public boolean isMyPetAdmin()
    {
        return isOnline() && MyPetPermissions.has(getPlayer(), "MyPet.admin", false);
    }

    public boolean hasMyPets()
    {
        return MyPetList.hasActiveMyPets(playerName);
    }

    public List<MyPet> getMyPets()
    {
        return MyPetList.getActiveMyPets(playerName);
    }

    public boolean hasInactiveMyPets()
    {
        return MyPetList.hasInactiveMyPets(playerName);
    }

    public List<InactiveMyPet> getInactiveMyPets()
    {
        return MyPetList.getInactiveMyPets(playerName);
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

    public static MyPetPlayer[] getMyPetPlayers()
    {
        MyPetPlayer[] playerArray = new MyPetPlayer[playerList.size()];
        int playerCounter = 0;
        for (MyPetPlayer player : playerList)
        {
            playerArray[playerCounter++] = player;
        }
        return playerArray;
    }

    public void schedule()
    {
        if (!isOnline())
        {
            return;
        }
        if (hasMyPets())
        {
            for (MyPet myPet : getMyPets())
            {
                if (!MyPetPermissions.has(this.getPlayer(), "MyPet.user.keep." + myPet.getPetType().getTypeName()))
                {
                    MyPetUtil.getDebugLogger().info("set MyPet of " + this.getName() + " to inactive");
                    MyPetList.setMyPetInactive(myPet);
                }
            }
        }
        if (!hasMyPets() && hasInactiveMyPets())
        {
            for (InactiveMyPet inactiveMyPet : getInactiveMyPets())
            {
                if (MyPetPermissions.has(this.getPlayer(), "MyPet.user.keep." + inactiveMyPet.getPetType().getTypeName()))
                {
                    MyPetUtil.getDebugLogger().info("set MyPet " + inactiveMyPet.getPetName() + " of " + this.getName() + " to active");
                    MyPetList.setMyPetActive(inactiveMyPet);
                    break;
                }
            }
        }
        if (hasMyPets())
        {
            for (MyPet myPet : getMyPets())
            {
                if (myPet.getStatus() == PetState.Here)
                {
                    if (myPet.getLocation().getWorld() != this.getPlayer().getLocation().getWorld() || myPet.getLocation().distance(this.getPlayer().getLocation()) > 75)
                    {
                        if (!myPet.getCraftPet().canMove())
                        {
                            myPet.removePet();
                            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Despawn")).replace("%petname%", myPet.petName));
                        }
                    }
                }
            }
        }
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
        else if (obj instanceof AnimalTamer)
        {
            return ((AnimalTamer) obj).getName().equals(playerName);
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