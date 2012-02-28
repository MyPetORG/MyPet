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


import de.Keyle.MyWolf.InactiveMyWolf;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class MyWolfList
{
    private static final Map<OfflinePlayer, MyWolf> mActiveWolves = new HashMap<OfflinePlayer, MyWolf>();
    private static final List<MyWolf> lActiveWolves = new ArrayList<MyWolf>();

    private static final Map<OfflinePlayer, InactiveMyWolf> mInctiveWolves = new HashMap<OfflinePlayer, InactiveMyWolf>();
    private static final List<InactiveMyWolf> lInactiveWolves = new ArrayList<InactiveMyWolf>();

    // Active -------------------------------------------------------------------

    public static MyWolf getMyWolf(InactiveMyWolf IMWolf)
    {
        if(IMWolf.getOwner().isOnline())
        {
            MyWolf AMWolf = new MyWolf(IMWolf.getOwner());
            AMWolf.setHealth(IMWolf.getHealth());
            AMWolf.setLocation(IMWolf.getLocation());
            AMWolf.Name = IMWolf.getName();
            AMWolf.RespawnTime = IMWolf.getRespawnTime();

            if (AMWolf.RespawnTime > 0)
            {
                AMWolf.Status = MyWolf.WolfState.Dead;
            }
            else
            {
                AMWolf.Status = MyWolf.WolfState.Despawned;
            }

            AMWolf.Experience.setExp(IMWolf.getExp());
            Collection<MyWolfGenericSkill> Skills = AMWolf.SkillSystem.getSkills();
            if(Skills.size() > 0)
            {
                for(MyWolfGenericSkill Skill : Skills)
                {
                    if(IMWolf.getSkills().hasKey(Skill.getName()))
                    {
                        Skill.load(IMWolf.getSkills().getCompound(Skill.getName()));
                    }
                }
            }
            return AMWolf;
        }
        return null;
    }
    
    public static void addMyWolf(MyWolf MW)
    {
        mActiveWolves.put(MW.getOwner(), MW);
        lActiveWolves.add(MW);
    }
    
    public static void removeMyWolf(MyWolf MW)
    {
       lActiveWolves.remove(MW);
        mActiveWolves.remove(MW.getOwner());
    }
    
    public static void removeMyWolf(OfflinePlayer Owner)
    {
        lActiveWolves.remove(mActiveWolves.get(Owner));
        mActiveWolves.remove(Owner);
    }

    public static MyWolf getMyWolf(int EntityID)
    {
        for(MyWolf wolf : lActiveWolves)
        {
            if(wolf.Wolf.getEntityId() == EntityID)
            {
                return wolf;
            }
        }
        return null;
    }
    
    public static MyWolf getMyWolf(OfflinePlayer owner)
    {
        if(mActiveWolves.containsKey(owner))
        {
            return mActiveWolves.get(owner);
        }
        return null;
    }

    public static List<MyWolf> getMyWolfList()
    {
        return lActiveWolves;
    }

    public static boolean hasMyWolf(OfflinePlayer player)
    {
       return mActiveWolves.containsKey(player);
    }

    public static boolean isMyWolf(int EnityID)
    {
        return getMyWolf(EnityID) != null;
    }

    // Inactive -----------------------------------------------------------------

    public static List<InactiveMyWolf> getInactiveMyWolfList()
    {
        return lInactiveWolves;
    }

    public static boolean hasInactiveMyWolf(OfflinePlayer player)
    {
        return mInctiveWolves.containsKey(player);
    }

    public static InactiveMyWolf getInactiveMyWolf(MyWolf AMWolf)
    {
        InactiveMyWolf IAMWolf = new InactiveMyWolf(MyWolfPlugin.getPlugin().getServer().getOfflinePlayer(AMWolf.getOwner().getName()));
        IAMWolf.setExp(AMWolf.Experience.getExp());
        IAMWolf.setHealth(AMWolf.getHealth());
        IAMWolf.setLocation(AMWolf.getLocation());
        IAMWolf.setRespawnTime(AMWolf.RespawnTime);
        IAMWolf.setSitting(IAMWolf.isSitting());
        IAMWolf.setSkills(AMWolf.SkillSystem.getSkills());

        return IAMWolf;
    }

    public static void removeInactiveMyWolf(InactiveMyWolf IMWolf)
    {
        mInctiveWolves.remove(IMWolf.getOwner());
        lInactiveWolves.remove(IMWolf);
    }

    public static void addInactiveMyWolf(InactiveMyWolf IMWolf)
    {
        mInctiveWolves.put(IMWolf.getOwner(),IMWolf);
        lInactiveWolves.add(IMWolf);
    }

    // All ----------------------------------------------------------------------

    public static void setMyWolfActive(OfflinePlayer Owner, boolean Activate)
    {
        if(Activate)
        {
            if(mInctiveWolves.containsKey(Owner) && mInctiveWolves.get(Owner).getOwner().isOnline())
            {
                InactiveMyWolf IMWolf = mInctiveWolves.get(Owner);
                MyWolf AMWolf = getMyWolf(IMWolf);
                addMyWolf(AMWolf);
                removeInactiveMyWolf(IMWolf);
            }
        }
        else
        {
            if(mActiveWolves.containsKey(Owner))
            {
                MyWolf AMWolf = mActiveWolves.get(Owner);
                InactiveMyWolf IAMWolf = getInactiveMyWolf(AMWolf);
                AMWolf.removeWolf();
                removeMyWolf(AMWolf);
                addInactiveMyWolf(IAMWolf);
            }
        }
    }

    public static void clearList()
    {
        mActiveWolves.clear();
        lActiveWolves.clear();
        mInctiveWolves.clear();
        lInactiveWolves.clear();
    }
}
