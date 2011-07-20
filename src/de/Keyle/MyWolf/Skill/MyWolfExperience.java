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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.entity.CreatureType;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.event.LevelUpEvent;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class MyWolfExperience
{
	private double Factor;
	MyWolf Wolf;

	private double Exp = 0;

	public static String JSreader;

	public static Map<CreatureType, Double> MobEXP = new HashMap<CreatureType, Double>();
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
		MobEXP.put(CreatureType.COW, 0.25);
		MobEXP.put(CreatureType.PIG, 0.25);
		MobEXP.put(CreatureType.CHICKEN, 0.1);
		MobEXP.put(CreatureType.SQUID, 0.25);
		MobEXP.put(CreatureType.SHEEP, 0.25);
	}

	public MyWolfExperience(double Factor, MyWolf Wolf)
	{
		this.Wolf = Wolf;
		this.Factor = Factor;
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
		if (JSreader != null)
		{
			return parseJS();
		}
		else
		{
			int tmplvl = 1;
			for (double i = Factor * Factor; i <= this.Exp; i = i * Factor)
			{
				tmplvl++;
			}
			return tmplvl;
		}
	}

	public double getrequireEXP()
	{
		return Math.pow(Factor, this.getLevel() + 1);
	}

	public int parseJS()
	{
		if(JSreader != null)
		{
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("js");
			engine.put("lvl", 1);
			engine.put("EXP", Exp);
			engine.put("factor", Factor);
			try
			{
				engine.eval(JSreader);
			}
			catch (ScriptException e)
			{
				MyWolfUtil.Log.info("[MyWolf] Error in EXP-Script!");
				return 1;
			}
			int tmplvl = 1;
			try
			{
				tmplvl = ((Double) engine.get("lvl")).intValue();
			}
			catch (Exception e)
			{
				MyWolfUtil.Log.info("[MyWolf] EXP-Script doesn't return valid value!");
			}
			return tmplvl;
		}
		else
		{
			return 1;
		}
	}
}
