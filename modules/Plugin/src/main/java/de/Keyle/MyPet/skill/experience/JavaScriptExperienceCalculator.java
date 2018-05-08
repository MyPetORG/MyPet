/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculator;
import lombok.Getter;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class JavaScriptExperienceCalculator implements ExperienceCalculator {

    protected JavaScriptExperience jsExp = null;
    protected boolean isUsable = false;
    protected File scriptFile = new File(MyPetApi.getPlugin().getDataFolder().getPath(), "exp.js");

    public JavaScriptExperienceCalculator() {
        if (!new File(MyPetApi.getPlugin().getDataFolder(), "rhino.jar").exists()) {
            MyPetApi.getLogger().warning("rhino.jar is missing. Please download it here (https://github.com/mozilla/rhino/releases) and put it into the MyPet folder.");
            return;
        }
        if (!new File(MyPetApi.getPlugin().getDataFolder(), "exp.js").exists()) {
            MyPetApi.getLogger().warning("exp.js file is missing.");
            return;
        }
        initScriptEngine();
    }

    public boolean isUsable() {
        return isUsable;
    }

    public double getExpByLevel(MyPet myPet, int level) {
        if (level <= 1) {
            return 0;
        }
        try {
            MyPetScriptInfo scriptInfo = new MyPetScriptInfo(myPet.getPetType(), myPet.getWorldGroup());
            return jsExp.getExpByLevel(level, scriptInfo);
        } catch (Exception e) {
            MyPetApi.getLogger().warning("This error appeared because your Levelscript (exp.js) caused an error.");
            MyPetApi.getLogger().warning("   " + e.getLocalizedMessage());
            e.printStackTrace();
            isUsable = false;
        }
        return 0;
    }

    private void initScriptEngine() {
        Context cx = Context.enter();
        try {
            ScriptableObject scriptable = new ImporterTopLevel(cx);
            Scriptable scope = cx.initStandardObjects(scriptable);
            cx.evaluateReader(scope, new FileReader(scriptFile), "exp.js", 0, null);

            jsExp = new JavaScriptExperience(cx, scope);
            isUsable = jsExp.init();
            if (!isUsable) {
                Context.exit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getVersion() {
        return Util.getSha256FromFile(scriptFile);
    }

    @Override
    public String getIdentifier() {
        return "JavaScript";
    }

    class JavaScriptExperience {

        Context cx;
        Scriptable scope;

        private Function getExpByLevel = null;

        public JavaScriptExperience(Context cx, Scriptable scope) {
            this.cx = cx;
            this.scope = scope;
        }

        public boolean init() {
            if (!scope.has("getExpByLevel", scope)) {
                MyPetApi.getLogger().warning("Your levelscript (exp.js) lacks the \"getExpByLevel(level, info)\" function.");
                return false;
            }

            this.getExpByLevel = (Function) scope.get("getExpByLevel", scope);

            return true;
        }

        public double getExpByLevel(int level, MyPetScriptInfo info) {
            return ((Number) getExpByLevel.call(cx, scope, scope, new Object[]{level, info})).doubleValue();
        }
    }

    class MyPetScriptInfo {

        @Getter private final MyPetType type;
        @Getter private final String worldGroup;

        public MyPetScriptInfo(MyPetType type, String worldGroup) {
            this.type = type;
            this.worldGroup = worldGroup;
        }
    }
}