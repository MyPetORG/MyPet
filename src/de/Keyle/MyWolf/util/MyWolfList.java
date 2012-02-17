/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util;


import de.Keyle.MyWolf.MyWolf;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyWolfList
{
    private static final Map<String, MyWolf> mWolves = new HashMap<String, MyWolf>();
    private static final List<MyWolf> lWolves = new ArrayList<MyWolf>();
    
    public static void addMyWolf(MyWolf MW)
    {
        mWolves.put(MW.getOwnerName(),MW);
        lWolves.add(MW);
    }
    
    public static void removeMyWolf(MyWolf MW)
    {
       lWolves.remove(MW);
        mWolves.remove(MW.getOwnerName());
    }
    
    public static void removeMyWolf(String Owner)
    {
        lWolves.remove(mWolves.get(Owner));
        mWolves.remove(Owner);
    }

    public static void removeMyWolf(Player Owner)
    {
        removeMyWolf(Owner.getName());
    }

    public static MyWolf getMyWolf(int EntityID)
    {
        for(MyWolf wolf : lWolves)
        {
            if(wolf.getID() == EntityID)
            {
                return wolf;
            }
        }
        return null;
    }
    
    public static MyWolf getMyWolf(String owner)
    {
        if(mWolves.containsKey(owner))
        {
            return mWolves.get(owner);
        }
        return null;
    }

    public static MyWolf getMyWolf(Player owner)
    {
        return getMyWolf(owner.getName());
    }

    public static List<MyWolf> getMyWolfList()
    {
        return lWolves;
    }

    public static boolean hasMyWolf(Player player)
    {
       return mWolves.containsKey(player.getName());
    }

    public static boolean hasMyWolf(String player)
    {
        return mWolves.containsKey(player);
    }

    public static boolean isMyWolf(int EnityID)
    {
        return getMyWolf(EnityID) != null;
    }

    public static void clearList()
    {
        mWolves.clear();
        lWolves.clear();
    }
}
