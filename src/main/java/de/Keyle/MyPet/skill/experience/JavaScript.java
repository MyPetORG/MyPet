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

package de.Keyle.MyPet.skill.experience;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;

public class JavaScript extends Experience
{
    private static String expScript = null;
    private ScriptEngine scriptEngine = null;
    private boolean isUsable = false;

    private double lastExpL = Double.NaN;
    private double lastExpC = Double.NaN;
    private double lastExpR = Double.NaN;
    private int lastLevel = 1;
    private double lastCurrentExp = 0.0;
    private double lastRequiredExp = 0.0;

    public JavaScript(MyPet myPet)
    {
        super(myPet);

        if (expScript == null)
        {
            if (setScriptPath(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "exp.js"))
            {
                MyPetLogger.write("Custom EXP-Script loaded!");
                DebugLogger.info("loaded exp.js.");
            }
            else
            {
                MyPetLogger.write("No custom EXP-Script found (exp.js).");
                DebugLogger.info("exp.js not loaded.");
                isUsable = false;
                return;
            }
        }

        try
        {
            initScriptEngine();
        }
        catch (ScriptException e)
        {
            MyPetLogger.write(ChatColor.RED + "Error in EXP-Script!");
            DebugLogger.warning("Error in EXP-Script!");
            isUsable = false;
            return;
        }
        isUsable = true;

        getLevel(0);
        getRequiredExp(0);
        getCurrentExp(0);
    }

    public boolean isUsable()
    {
        return isUsable;
    }

    public int getLevel(double exp)
    {
        if (lastExpL == exp)
        {
            return lastLevel;
        }
        lastExpL = exp;
        if (scriptEngine instanceof Invocable)
        {
            try
            {
                Object result = ((Invocable) scriptEngine).invokeFunction("getLevel", exp);
                lastLevel = ((Double) result).intValue();
            }
            catch (ScriptException e)
            {
                MyPetLogger.write(ChatColor.RED + "Error in EXP-Script!");
                DebugLogger.warning("Error in EXP-Script!");
                isUsable = false;
            }
            catch (NoSuchMethodException e)
            {
                MyPetLogger.write(ChatColor.RED + "getRequiredExp(exp) Method is missing!");
                DebugLogger.warning("getRequiredExp(exp) Method is missing!");
                isUsable = false;
            }
        }
        return lastLevel;
    }

    public double getRequiredExp(double exp)
    {
        if (lastExpR == exp)
        {
            return lastRequiredExp;
        }
        lastExpR = exp;
        if (scriptEngine instanceof Invocable)
        {
            try
            {
                Object result = ((Invocable) scriptEngine).invokeFunction("getRequiredExp", exp);
                lastRequiredExp = (Double) result;
            }
            catch (ScriptException e)
            {
                MyPetLogger.write(ChatColor.RED + "Error in EXP-Script!");
                DebugLogger.warning("Error in EXP-Script!");
                isUsable = false;
            }
            catch (NoSuchMethodException e)
            {
                MyPetLogger.write(ChatColor.RED + "getRequiredExp(exp) Method is missing!");
                DebugLogger.warning("getRequiredExp(exp) Method is missing!");
                isUsable = false;
            }
        }
        return lastRequiredExp;
    }

    public double getCurrentExp(double exp)
    {
        if (lastExpC == exp)
        {
            return lastCurrentExp;
        }
        lastExpC = exp;
        if (scriptEngine instanceof Invocable)
        {
            try
            {
                Object result = ((Invocable) scriptEngine).invokeFunction("getCurrentExp", exp);
                lastCurrentExp = (Double) result;
            }
            catch (ScriptException e)
            {
                MyPetLogger.write(ChatColor.RED + "Error in EXP-Script!");
                DebugLogger.warning("Error in EXP-Script!");
                isUsable = false;
            }
            catch (NoSuchMethodException e)
            {
                MyPetLogger.write(ChatColor.RED + "getCurrentExp(exp) Method is missing!");
                DebugLogger.warning("getCurrentExp(exp) Method is missing!");
                isUsable = false;
            }

        }

        return lastCurrentExp;
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

    public static void reset()
    {
        expScript = null;
    }

    private ScriptEngine initScriptEngine() throws ScriptException
    {
        ScriptEngineManager manager = new ScriptEngineManager();
        scriptEngine = manager.getEngineByName("js");

        scriptEngine.eval("MyPet = new Object();" +
                "MyPet.getType = function() { return \"" + getMyPet().getPetType().getTypeName() + "\"; };" +
                "MyPet.getOwnerName = function() { return \"" + getMyPet().getOwner().getName() + "\"; };");

        scriptEngine.eval(expScript);

        return scriptEngine;
    }
}