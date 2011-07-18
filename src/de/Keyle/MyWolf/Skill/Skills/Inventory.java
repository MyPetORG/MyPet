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

import org.bukkit.Material;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.Wolves;
import de.Keyle.MyWolf.Skill.MyWolfSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class Inventory extends MyWolfSkill
{
	public Inventory()
	{
		this.Name = "Inventory";
		registerSkill();
		registerSkill("InventorySmall");
		registerSkill("InventoryLarge");
	}

	@Override
	public void run(Wolves wolf, Object args)
	{
		if (MyWolfUtil.hasSkill(wolf.Abilities, "InventoryLarge") && MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills.InventoryLarge") == false)
		{
			return;
		}
		else if (MyWolfUtil.hasSkill(wolf.Abilities, "InventorySmall") && MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills.InventorySmall") == false)
		{

		}
		if (MyWolfUtil.hasSkill(wolf.Abilities, "InventorySmall") || MyWolfUtil.hasSkill(wolf.Abilities, "InventoryLarge"))
		{
			if (wolf.getLocation().getBlock().getType() != Material.STATIONARY_WATER && wolf.getLocation().getBlock().getType() != Material.WATER)
			{
				wolf.OpenInventory();
				if (wolf.isSitting() == false)
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
	public void activate(Wolves wolf, Object args)
	{
		if (MyWolfUtil.hasSkill(wolf.Abilities, "InventorySmall") == false)
		{
			if (MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills.InventorySmall") == false)
			{
				return;
			}
			wolf.Abilities.put("InventorySmall", true);
		}
		else
		{
			if (MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills.InventoryLarge") == false)
			{
				return;
			}
			wolf.Abilities.put("InventoryLarge", true);
		}
		wolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfUtil.hasSkill(wolf.Abilities, "InventoryLarge") == false ? MyWolfLanguage.getString("Msg_AddChest") : MyWolfLanguage.getString("Msg_AddChestGreater")).replace("%wolfname%", wolf.Name));
	}
}
