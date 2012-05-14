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

package de.Keyle.MyPet.skill;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.MyPetUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;

public class MyPetJSexp
{
    public static String expScript = null;
    private int lvl = 1;
    private MyPet MPet;
    private double lastExp = 0;
    private double requiredExp = 0;
    private double currentExp = 0;
    private MyPetExperience MPetExperience;

    public MyPetJSexp(MyPet MPet, MyPetExperience MPetExperience)
    {
        this.MPet = MPet;
        this.MPetExperience = MPetExperience;
    }

    public boolean isUsable()
    {
        return expScript != null;
    }

    public int getLvl()
    {
        if (lastExp != MPetExperience.getExp())
        {
            update();
        }
        return lvl;
    }

    public double getRequiredExp()
    {
        if (lastExp != MPetExperience.getExp())
        {
            update();
        }
        return requiredExp;
    }

    public double getCurrentExp()
    {
        if (lastExp != MPetExperience.getExp())
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

            MyPetUtil.getLogger().info("Error in EXP-Script!");
            MyPetUtil.getDebugLogger().info("Error in EXP-Script!");
            MyPetUtil.getDebugLogger().info("   " + e.getMessage());
            expScript = null;
            return false;
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().info("EXP-Script doesn't return a valid value!");
            MyPetUtil.getDebugLogger().warning("EXP-Script doesn't return a valid value!");
            expScript = null;
            return false;
        }
    }

    public static boolean setScriptPath(String path)
    {
        try
        {
            expScript = MyPetUtil.readFileAsString(path);
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

            engine.put("Exp", MPetExperience.getExp());
            engine.put("name", MPet.Name);
            engine.put("player", MPet.getOwner().getName());

            engine.eval(expScript);

            return engine;
        }
        else
        {
            return null;
        }
    }
}