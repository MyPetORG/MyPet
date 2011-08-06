/*
* Copyright (C) 2011 Keyle
*
* This file is part of MyWolf.
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

package de.Keyle.MyWolf.Skill.Skills;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.Skill.MyWolfSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.Material;
import org.getspout.spout.inventory.CustomMCInventory;

public class Inventory extends MyWolfSkill
{
    public Inventory()
    {
        super("Inventory");
        registerSkill();
    }

    @Override
    public void run(MyWolf wolf, Object args)
    {
        if (!MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills.Inventory"))
        {
            return;
        }
        if (MyWolfUtil.hasSkill(wolf.Abilities, "Inventory"))
        {
            if (wolf.getLocation().getBlock().getType() != Material.STATIONARY_WATER && wolf.getLocation().getBlock().getType() != Material.WATER)
            {
                wolf.OpenInventory();
                if (!wolf.isSitting())
                {
                    ConfigBuffer.WolfChestOpened.add(wolf.getOwner());
                }
                wolf.Wolf.setSitting(true);
            }
            else
            {
                wolf.sendMessageToOwner(MyWolfLanguage.getString("Msg_InventorySwimming"));
            }
        }
    }

    @Override
    public void activate(MyWolf wolf, Object args)
    {
        if (!MyWolfUtil.hasSkill(wolf.Abilities, "Inventory"))
        {
            wolf.Abilities.put("Inventory", true);
        }
        if (wolf.inv.getSize() >= 54)
        {
            return;
        }
        if (!MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills.Inventory." + (wolf.inv.getSize() + 9)))
        {
            return;
        }
        CustomMCInventory newinv = new CustomMCInventory(wolf.inv.getSize() + 9, wolf.Name + "\'s Inventory");
        for (int i = 0 ; i < wolf.inv.getSize() ; i++)
        {
            newinv.setItem(i, wolf.inv.getItem(i));
        }
        wolf.inv = newinv;
        wolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Inventory")).replace("%wolfname%", wolf.Name).replace("%size%", "" + wolf.inv.getSize()));
    }
}
