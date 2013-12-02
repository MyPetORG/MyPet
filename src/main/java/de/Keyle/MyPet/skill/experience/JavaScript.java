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
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class JavaScript extends Experience {
    private static IExperience expInv = null;
    private static boolean isUsable = false;

    private double lastExp = Double.NaN;
    private int lastLevel = 1;
    private double lastCurrentExp = 0.0;
    private double lastRequiredExp = 0.0;

    private final MyPetScriptInfo scriptInfo;

    public JavaScript(de.Keyle.MyPet.entity.types.MyPet myPet) {
        super(myPet);
        scriptInfo = new MyPetScriptInfo();

        try {
            initScriptEngine();
        } catch (ScriptException e) {
            MyPetLogger.write(ChatColor.RED + "Error in EXP-Script!");
            DebugLogger.warning("Error in EXP-Script!");
            isUsable = false;
            return;
        }
        isUsable = true;

        getLevel(0);
        getRequiredExp(0);
        getCurrentExp(0);
        getExpByLevel(2);
    }

    public boolean isUsable() {
        return isUsable;
    }

    public int getLevel(double exp) {
        if (exp == 0) {
            return 1;
        }
        if (lastExp == exp) {
            return lastLevel;
        }
        lastExp = exp;
        if (expInv != null) {
            try {
                return lastLevel = expInv.getLevel(exp, scriptInfo);
            } catch (UndeclaredThrowableException e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                try {
                    MyPetLogger.write(ChatColor.RED + e.getUndeclaredThrowable().getCause().getLocalizedMessage());
                } catch (Exception ignored) {
                }
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            }
        }
        return lastLevel;
    }

    public double getRequiredExp(double exp) {
        if (lastExp == exp) {
            return lastRequiredExp;
        }
        lastExp = exp;
        if (expInv != null) {
            try {
                return lastRequiredExp = expInv.getRequiredExp(exp, scriptInfo);
            } catch (UndeclaredThrowableException e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                try {
                    MyPetLogger.write(ChatColor.RED + e.getUndeclaredThrowable().getCause().getLocalizedMessage());
                } catch (Exception ignored) {
                }
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            }
        }
        return lastRequiredExp;
    }

    public double getCurrentExp(double exp) {
        if (lastExp == exp) {
            return lastCurrentExp;
        }
        lastExp = exp;

        if (expInv != null) {
            try {
                return lastCurrentExp = expInv.getCurrentExp(exp, scriptInfo);
            } catch (UndeclaredThrowableException e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                try {
                    MyPetLogger.write(ChatColor.RED + e.getUndeclaredThrowable().getCause().getLocalizedMessage());
                } catch (Exception ignored) {
                }
                isUsable = false;
                return 0;
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            }
        }
        return lastCurrentExp;
    }

    @Override
    public double getExpByLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        if (expInv != null) {
            try {
                return expInv.getExpByLevel(level, scriptInfo);
            } catch (UndeclaredThrowableException e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                try {
                    MyPetLogger.write(ChatColor.RED + e.getUndeclaredThrowable().getCause().getLocalizedMessage());
                } catch (Exception ignored) {
                }
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            }
        }
        return 0;
    }

    public static void reset() {
        expInv = null;
        isUsable = false;
    }

    private static void initScriptEngine() throws ScriptException {
        if (expInv == null) {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine scriptEngine = manager.getEngineByName("js");
            if (scriptEngine != null) {
                try {
                    String expScript;
                    try {
                        expScript = Util.readFileAsString(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "exp.js");
                        DebugLogger.info("Custom EXP-Script (exp.js) loaded!");
                    } catch (IOException e) {
                        DebugLogger.info("No custom EXP-Script found (exp.js).");
                        isUsable = false;
                        return;
                    }
                    scriptEngine.eval(expScript);
                    if (scriptEngine instanceof Invocable) {
                        Invocable inv = (Invocable) scriptEngine;
                        expInv = inv.getInterface(IExperience.class);
                        isUsable = true;
                    } else {
                        isUsable = false;
                    }
                } catch (ScriptException e) {
                    e.printStackTrace();
                    DebugLogger.printThrowable(e);
                    isUsable = false;
                }
            } else {
                isUsable = false;
            }
        }
    }

    class MyPetScriptInfo {
        public String getType() {
            return getMyPet().getPetType().getTypeName();
        }

        public String getOwnerName() {
            return getMyPet().getOwner().getName();
        }

        public String getSkilltree() {
            return getMyPet().getSkillTree() != null ? getMyPet().getSkillTree().getName() : "";
        }

        public String getUUID() {
            return getMyPet().getUUID().toString();
        }

        public String getWorldGroup() {
            return getMyPet().getWorldGroup();
        }
    }

    interface IExperience {
        public abstract int getLevel(double exp, MyPetScriptInfo mypet) throws InvocationTargetException;

        public abstract double getRequiredExp(double exp, MyPetScriptInfo mypet) throws InvocationTargetException;

        public abstract double getCurrentExp(double exp, MyPetScriptInfo mypet) throws InvocationTargetException;

        public abstract double getExpByLevel(int level, MyPetScriptInfo mypet) throws InvocationTargetException;
    }
}