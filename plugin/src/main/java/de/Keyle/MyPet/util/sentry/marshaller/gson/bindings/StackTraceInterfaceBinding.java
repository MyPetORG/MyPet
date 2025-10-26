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

package de.Keyle.MyPet.util.sentry.marshaller.gson.bindings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.StackTraceInterface;

import java.util.*;
import java.util.regex.Pattern;

import static de.Keyle.MyPet.util.sentry.marshaller.gson.GsonMarshaller.writeObject;

public class StackTraceInterfaceBinding implements InterfaceBinding<StackTraceInterface> {

    private static final String FRAMES_PARAMETER = "frames";
    private static final String FILENAME_PARAMETER = "filename";
    private static final String FUNCTION_PARAMETER = "function";
    private static final String MODULE_PARAMETER = "module";
    private static final String LINE_NO_PARAMETER = "lineno";
    private static final String COL_NO_PARAMETER = "colno";
    private static final String ABSOLUTE_PATH_PARAMETER = "abs_path";
    private static final String IN_APP_PARAMETER = "in_app";
    private static final String VARIABLES_PARAMETER = "vars";
    private static final String PLATFORM_PARAMTER = "platform";
    private static List<Pattern> inAppBlacklistRegexps = new ArrayList<>();
    private Collection<String> inAppFrames = Collections.emptyList();
    private boolean removeCommonFramesWithEnclosing = true;

    public StackTraceInterfaceBinding() {
    }

    private JsonObject writeFrame(SentryStackTraceElement stackTraceElement, boolean commonWithEnclosing) {
        JsonObject generator = new JsonObject();
        generator.addProperty(FILENAME_PARAMETER, stackTraceElement.getFileName());
        generator.addProperty(MODULE_PARAMETER, stackTraceElement.getModule());
        boolean inApp = (!this.removeCommonFramesWithEnclosing || !commonWithEnclosing) && this.isFrameInApp(stackTraceElement);
        generator.addProperty(IN_APP_PARAMETER, inApp);
        generator.addProperty(FUNCTION_PARAMETER, stackTraceElement.getFunction());
        generator.addProperty(LINE_NO_PARAMETER, stackTraceElement.getLineno());
        if (stackTraceElement.getColno() != null) {
            generator.addProperty(COL_NO_PARAMETER, stackTraceElement.getColno());
        }

        if (stackTraceElement.getPlatform() != null) {
            generator.addProperty(PLATFORM_PARAMTER, stackTraceElement.getPlatform());
        }

        if (stackTraceElement.getAbsPath() != null) {
            generator.addProperty(ABSOLUTE_PATH_PARAMETER, stackTraceElement.getAbsPath());
        }

        if (stackTraceElement.getLocals() != null && !stackTraceElement.getLocals().isEmpty()) {
            JsonObject vars = new JsonObject();
            for (Map.Entry<String, Object> stringObjectEntry : stackTraceElement.getLocals().entrySet()) {
                writeObject(vars, stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
            generator.add(VARIABLES_PARAMETER, vars);
        }

        return generator;
    }

    private boolean isFrameInApp(SentryStackTraceElement stackTraceElement) {
        Iterator i$ = this.inAppFrames.iterator();

        String inAppFrame;
        String className;
        do {
            if (!i$.hasNext()) {
                return false;
            }

            inAppFrame = (String) i$.next();
            className = stackTraceElement.getModule();
        } while (!className.startsWith(inAppFrame) || this.isBlacklistedFromInApp(className));

        return true;
    }

    private boolean isBlacklistedFromInApp(String className) {
        Iterator i$ = inAppBlacklistRegexps.iterator();

        boolean found;
        do {
            if (!i$.hasNext()) {
                return false;
            }

            Pattern inAppBlacklistRegexp = (Pattern) i$.next();
            found = inAppBlacklistRegexp.matcher(className).find();
        } while (!found);

        return true;
    }

    public JsonElement writeInterface(StackTraceInterface stackTraceInterface) {
        JsonObject generator = new JsonObject();
        JsonArray frames = new JsonArray();
        SentryStackTraceElement[] sentryStackTrace = stackTraceInterface.getStackTrace();
        int commonWithEnclosing = stackTraceInterface.getFramesCommonWithEnclosing();

        for (int i = sentryStackTrace.length - 1; i >= 0; --i) {
            frames.add(this.writeFrame(sentryStackTrace[i], commonWithEnclosing-- > 0));
        }

        generator.add(FRAMES_PARAMETER, frames);
        return generator;
    }

    public void setRemoveCommonFramesWithEnclosing(boolean removeCommonFramesWithEnclosing) {
        this.removeCommonFramesWithEnclosing = removeCommonFramesWithEnclosing;
    }

    public void setInAppFrames(Collection<String> inAppFrames) {
        this.inAppFrames = inAppFrames;
    }

    static {
        inAppBlacklistRegexps.add(Pattern.compile("\\$\\$FastClass[a-zA-Z]*CGLIB\\$\\$"));
        inAppBlacklistRegexps.add(Pattern.compile("\\$\\$Enhancer[a-zA-Z]*CGLIB\\$\\$"));
    }
}
