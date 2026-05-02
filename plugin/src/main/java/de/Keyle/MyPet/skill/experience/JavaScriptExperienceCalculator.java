/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import com.google.common.hash.Hashing;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.exceptions.MyPetExperienceCalculatorInitException;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculator;
import de.Keyle.MyPet.api.util.ErrorUtil;
import org.mozilla.javascript.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaScriptExperienceCalculator implements ExperienceCalculator {

    protected JavaScriptExperience jsExp = null;
    protected boolean isUsable = false;
    protected File scriptFile = new File(MyPetApi.getPlugin().getDataFolder().getPath(), "exp.js");

    public JavaScriptExperienceCalculator() {
        File dataFolder = MyPetApi.getPlugin().getDataFolder();
        if (!new File(dataFolder, "exp.js").exists()) {
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
            return jsExp.getExpByLevel(level, myPet.getPetType().name(), myPet.getWorldGroup());
        } catch (Exception e) {
            MyPetApi.getLogger().warning("This error appeared because your Levelscript (exp.js) caused an error.");
            MyPetApi.getLogger().warning("   " + e.getLocalizedMessage());
            ErrorUtil.reportWarning("JavaScriptExperienceCalculator operation failed", e);
            isUsable = false;
        }
        return 0;
    }

    private void initScriptEngine() {
        try {
            Context cx = Context.enter();
            jsExp = new JavaScriptExperience(cx);
            isUsable = jsExp.init();
            if (!isUsable) {
                Context.exit();
            }
        } catch (EvaluatorException e) {
            throw new MyPetExperienceCalculatorInitException(e.getMessage());
        }
    }

    @Override
    public long getVersion() {
        try {
            return com.google.common.io.Files.asByteSource(scriptFile).hash(Hashing.sha256()).asLong();
        } catch (IOException e) {
            ErrorUtil.report(e);
        }
        return 0;
    }

    @Override
    public String getIdentifier() {
        return "JavaScript";
    }

    class JavaScriptExperience {

        Context cx;
        Scriptable scope;

        private Function getExpByLevel = null;

        public JavaScriptExperience(Context cx) {
            ScriptableObject scriptable = new ImporterTopLevel(cx);
            Scriptable scope = cx.initStandardObjects(scriptable);
            try {
                String content = Files.readString(Path.of(scriptFile.getAbsolutePath()));
                content = "function print(msg) {\n" +
                        "  java.lang.MyPetApi.getLogger().info('[MyPet][JS] ' + msg);\n" +
                        "}\n\n" + content;
                cx.evaluateString(scope, content, "exp.js", 0, null);
            } catch (IOException e) {
                ErrorUtil.reportWarning("JavaScriptExperienceCalculator operation failed", e);
            }
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

        public double getExpByLevel(int level, String name, String worldgroup) {
            return ((Number) getExpByLevel.call(cx, scope, scope, new Object[]{level, name, worldgroup})).doubleValue();
        }
    }
}