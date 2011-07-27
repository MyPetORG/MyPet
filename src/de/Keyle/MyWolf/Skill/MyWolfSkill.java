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

package de.Keyle.MyWolf.Skill;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class MyWolfSkill
{
	protected String Name;

	public String getName()
	{
		return this.Name;
	}

	public final void registerSkill()
	{
		try
		{
			ConfigBuffer.registerSkill(this.Name, this);
		}
		catch (Exception e)
		{
			MyWolfUtil.Log.info("[MyWolf] " + e.getMessage());
		}
	}

	public final void registerSkill(String Name)
	{
		try
		{
			ConfigBuffer.registerSkill(Name, this);
		}
		catch (Exception e)
		{
			MyWolfUtil.Log.info("[MyWolf] " + e.getMessage());
		}
	}

	public void run(MyWolf wolf, Object args)
	{
	}

	public void activate(MyWolf wolf, Object args)
	{
		if (!MyWolfPermissions.has(wolf.getOwner(), "MyWolf.Skills." + this.Name))
		{
			return;
		}
		wolf.Abilities.put(this.Name, true);
	}
}
