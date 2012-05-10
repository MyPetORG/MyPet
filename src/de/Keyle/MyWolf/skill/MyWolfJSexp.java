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

package de.Keyle.MyWolf.skill;

import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import de.Keyle.MyWolf.util.MyWolfUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;

public class MyWolfJSexp
{
    public static String expScript = null;
    private int lvl = 1;
    private MyWolf MWolf;
    private double lastExp = 0;
    private double requiredExp = 0;
    private double currentExp = 0;
    private MyWolfExperience MWolfExperience;

    public MyWolfJSexp(MyWolf MWolf, MyWolfExperience MWolfExperience)
    {
        this.MWolf = MWolf;
        this.MWolfExperience = MWolfExperience;
    }

    public boolean isUsable()
    {
        return expScript != null;
    }

    public int getLvl()
    {
        if (lastExp != MWolfExperience.getExp())
        {
            update();
        }
        return lvl;
    }

    public double getRequiredExp()
    {
        if (lastExp != MWolfExperience.getExp())
        {
            update();
        }
        return requiredExp;
    }

    public double getCurrentExp()
    {
        if (lastExp != MWolfExperience.getExp())
        {
            update();
        }
        return currentExp;
    }

    private boolean update()
    {
        try
        {
            ScriptEngine se = parseJS();
            lvl = ((Double) se.get("lvl")).intValue();
            requiredExp = ((Double) se.get("requiredExp"));
            currentExp = ((Double) se.get("currentExp"));
            return true;
        }
        catch (ScriptException e)
        {

            MyWolfUtil.getLogger().info("Error in EXP-Script!");
            MyWolfUtil.getDebugLogger().info("Error in EXP-Script!");
            MyWolfUtil.getDebugLogger().info("   " + e.getMessage());
            expScript = null;
            return false;
        }
        catch (Exception e)
        {
            MyWolfUtil.getLogger().info("EXP-Script doesn't return a valid value!");
            MyWolfUtil.getDebugLogger().warning("EXP-Script doesn't return a valid value!");
            expScript = null;
            return false;
        }
    }

    public static boolean setScriptPath(String path)
    {
        try
        {
            expScript = MyWolfUtil.readFileAsString(path);
            return true;
        }
        catch (IOException e)
        {
            expScript = null;
            return false;
        }
    }

    private ScriptEngine parseJS() throws ScriptException
    {
        if (expScript != null)
        {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");
            engine.put("lvl", 1);
            engine.put("requiredExp", 0);
            engine.put("currentExp", 0);

            engine.put("Exp", MWolfExperience.getExp());
            engine.put("name", MWolf.Name);
            engine.put("player", MWolf.getOwner().getName());

            engine.eval(expScript);

            return engine;
        }
        else
        {
            return null;
        }
    }
}