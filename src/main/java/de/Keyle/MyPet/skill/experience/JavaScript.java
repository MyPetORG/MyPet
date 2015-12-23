/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class JavaScript extends Experience {
    private static JavaScriptExperience jsExp = null;
    private static boolean isUsable = false;

    private double lastExpLevel = Double.NaN;
    private double lastExpRequiredExp = Double.NaN;
    private double lastExpCurrentExp = Double.NaN;
    private int lastLevel = 1;
    private double lastCurrentExp = 0.0;
    private double lastRequiredExp = 0.0;

    private MyPetScriptInfo scriptInfo;

    public JavaScript(de.Keyle.MyPet.entity.types.MyPet myPet) {
        super(myPet);

        scriptInfo = new MyPetScriptInfo();

        initScriptEngine();
    }

    public boolean isUsable() {
        return isUsable;
    }

    public int getLevel(double exp) {
        if (exp == 0) {
            return 1;
        }
        if (lastExpLevel == exp) {
            return lastLevel;
        }
        lastExpLevel = exp;
        if (jsExp != null) {
            try {
                return lastLevel = jsExp.getLevel(exp, scriptInfo);
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error:");
                MyPetLogger.write("   " + e.getLocalizedMessage());
                isUsable = false;
                return 0;
            }
        }
        return lastLevel;
    }

    public double getRequiredExp(double exp) {
        if (lastExpRequiredExp == exp) {
            return lastRequiredExp;
        }
        lastExpRequiredExp = exp;
        if (jsExp != null) {
            try {
                return lastRequiredExp = jsExp.getRequiredExp(exp, scriptInfo);
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                MyPetLogger.write("   " + e.getLocalizedMessage());
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            }
        }
        return lastRequiredExp;
    }

    public double getCurrentExp(double exp) {
        if (lastExpCurrentExp == exp) {
            return lastCurrentExp;
        }
        lastExpCurrentExp = exp;

        if (jsExp != null) {
            try {
                return lastCurrentExp = jsExp.getCurrentExp(exp, scriptInfo);
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                MyPetLogger.write("   " + e.getLocalizedMessage());
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
        if (jsExp != null) {
            try {
                return jsExp.getExpByLevel(level, scriptInfo);
            } catch (Exception e) {
                MyPetLogger.write(ChatColor.RED + "This error appeared because your Levelscript (exp.js) caused an error.");
                MyPetLogger.write("   " + e.getLocalizedMessage());
                DebugLogger.printThrowable(e);
                isUsable = false;
                return 0;
            }
        }
        return 0;
    }

    public static void reset() {
        if (isUsable) {
            Context.exit();
        }
        jsExp = null;
        isUsable = false;
    }

    private static void initScriptEngine() {
        if (jsExp == null) {
            Context cx = Context.enter();
            try {
                File jsFile = new File(MyPetPlugin.getPlugin().getDataFolder().getPath(), "exp.js");
                ScriptableObject scriptable = new ImporterTopLevel(cx);
                Scriptable scope = cx.initStandardObjects(scriptable);
                cx.evaluateReader(scope, new FileReader(jsFile), "exp.js", 0, null);

                jsExp = new JavaScriptExperience(cx, scope);
                isUsable = jsExp.init();
                if (!isUsable) {
                    Context.exit();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class JavaScriptExperience {
        Context cx;
        Scriptable scope;

        private Function getRequiredExp = null;
        private Function getLevel = null;
        private Function getCurrentExp = null;
        private Function getExpByLevel = null;

        public JavaScriptExperience(Context cx, Scriptable scope) {
            this.cx = cx;
            this.scope = scope;
        }

        public boolean init() {
            boolean usable = true;
            if (!scope.has("getRequiredExp", scope)) {
                MyPetLogger.write(ChatColor.RED + "Your levelscript (exp.js) lacks the \"getRequiredExp(exp, mypet)\" function:");
                usable = false;
            }
            if (!scope.has("getLevel", scope)) {
                MyPetLogger.write(ChatColor.RED + "Your levelscript (exp.js) lacks the \"getLevel(exp, mypet)\" function:");
                usable = false;
            }
            if (!scope.has("getCurrentExp", scope)) {
                MyPetLogger.write(ChatColor.RED + "Your levelscript (exp.js) lacks the \"getCurrentExp(exp, mypet)\" function:");
                usable = false;
            }
            if (!scope.has("getExpByLevel", scope)) {
                MyPetLogger.write(ChatColor.RED + "Your levelscript (exp.js) lacks the \"getExpByLevel(level, mypet)\" function:");
                usable = false;
            }
            if (!usable) {
                return false;
            }

            this.getRequiredExp = (Function) scope.get("getRequiredExp", scope);
            this.getLevel = (Function) scope.get("getLevel", scope);
            this.getCurrentExp = (Function) scope.get("getCurrentExp", scope);
            this.getExpByLevel = (Function) scope.get("getExpByLevel", scope);

            return true;
        }

        public int getLevel(double exp, MyPetScriptInfo mypet) {
            return ((Double) getLevel.call(cx, scope, scope, new Object[]{exp, mypet})).intValue();
        }

        public double getRequiredExp(double exp, MyPetScriptInfo mypet) {
            return (Double) getRequiredExp.call(cx, scope, scope, new Object[]{exp, mypet});
        }

        public double getCurrentExp(double exp, MyPetScriptInfo mypet) {
            return (Double) getCurrentExp.call(cx, scope, scope, new Object[]{exp, mypet});
        }

        public double getExpByLevel(int level, MyPetScriptInfo mypet) {
            return (Double) getExpByLevel.call(cx, scope, scope, new Object[]{level, mypet});
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
}