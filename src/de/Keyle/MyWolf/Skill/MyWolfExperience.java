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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.entity.CreatureType;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.event.LevelUpEvent;

public class MyWolfExperience
{
	private double Faktor;
	MyWolf Wolf;

	private double Exp = 0;

	public final static Map<CreatureType, Double> MobEXP = new HashMap<CreatureType, Double>();
	static
	{
		MobEXP.put(CreatureType.SKELETON, 1.1);
		MobEXP.put(CreatureType.ZOMBIE, 1.1);
		MobEXP.put(CreatureType.SPIDER, 1.05);
		MobEXP.put(CreatureType.WOLF, 0.5);
		MobEXP.put(CreatureType.CREEPER, 1.55);
		MobEXP.put(CreatureType.GHAST, 0.85);
		MobEXP.put(CreatureType.PIG_ZOMBIE, 1.1);
		MobEXP.put(CreatureType.GIANT, 10.75);
	}

	public MyWolfExperience(double Faktor, MyWolf Wolf)
	{
		this.Wolf = Wolf;
		this.Faktor = Faktor;
	}

	public void setExp(double Exp)
	{
		int tmplvl = getLevel();
		this.Exp = Exp;
		for (int i = tmplvl; i < getLevel(); i++)
		{
			MyWolfPlugin.Plugin.getServer().getPluginManager().callEvent(new LevelUpEvent(Wolf, i + 1));
		}
	}

	public double getExp()
	{
		return Exp;
	}

	public void addExp(double Exp)
	{
		int tmplvl = getLevel();
		this.Exp += Exp;

		for (int i = tmplvl; i < getLevel(); i++)
		{
			MyWolfPlugin.Plugin.getServer().getPluginManager().callEvent(new LevelUpEvent(Wolf, i + 1));
		}
	}

	public void addEXP(CreatureType type)
	{
		if (MobEXP.containsKey(type))
		{
			int tmplvl = getLevel();
			Exp += MobEXP.get(type) + Math.random();
			Logger.getLogger("Minecraft").info("exp: " + Exp);
			for (int i = tmplvl; i < getLevel(); i++)
			{
				MyWolfPlugin.Plugin.getServer().getPluginManager().callEvent(new LevelUpEvent(Wolf, i + 1));
			}
		}
	}

	public int getLevel()
	{
		int tmplvl = 1;
		for (double i = Faktor * Faktor; i <= this.Exp; i = i * Faktor)
		{
			tmplvl++;
		}
		return tmplvl;
	}

	public double getrequireEXP()
	{
		return Math.pow(Faktor, this.getLevel() + 1);
	}
}
