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

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatUtil {
    private static final Pattern PACKAGE_VERSION_MATCHER = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private static final Pattern MINECRAFT_VERSION_MATCHER = Pattern.compile("\\(MC: (\\d\\.\\d+(?:\\.\\d+)?)");
    private static final Pattern VERSION_MATCHER = Pattern.compile("\\d\\.\\d+(?:\\.\\d+)?");

    private String internalVersion = null;
    private String minecraftVersion = "0.0.0";

    private Map<String, Integer> compareCache = new HashMap<>();

    public CompatUtil() {
        Matcher regexMatcher = PACKAGE_VERSION_MATCHER.matcher(Bukkit.getServer().getClass().getCanonicalName());
        if (regexMatcher.find()) {
            internalVersion = regexMatcher.group(1);
        }
        regexMatcher = MINECRAFT_VERSION_MATCHER.matcher(Bukkit.getVersion());
        if (regexMatcher.find()) {
            minecraftVersion = regexMatcher.group(1);
        }

        if (internalVersion == null) {
            internalVersion = getBukkitVersionFromMinecraftVersion();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getCompatInstance(Class<? extends T> clazz, String path, String className, Object... parameters) {
        if (internalVersion == null || minecraftVersion == null) {
            return null;
        }

        String classPath = clazz.getCanonicalName();
        if (classPath.startsWith("de.Keyle.MyPet")) {
            if (MyPetVersion.isSpecialMCVersion(minecraftVersion)) {
                int special = Integer.parseInt(minecraftVersion.split("\\.")[2]);
                for (int i = special; i > 0; i--)
                    try {
                        String specVers = internalVersion + "_" + i;
                        classPath = "de.Keyle.MyPet.compat." + specVers + "." + path + (path != null && !path.equals("") ? "." : "") + className;
                        Class.forName(classPath);
                        break;
                    } catch (ClassNotFoundException ignored) {
                    }
            } else {
                classPath = "de.Keyle.MyPet.compat." + internalVersion + "." + path + (path != null && !path.equals("") ? "." : "") + className;
            }
        }

        try {
            Class<? extends T> compatClass = (Class<? extends T>) Class.forName(classPath);

            if (Modifier.isAbstract(compatClass.getModifiers())) {
                return null;
            }
            if (Modifier.isInterface(compatClass.getModifiers())) {
                return null;
            }

            Class[] paramterClasses = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                paramterClasses[i] = parameter.getClass();
            }

            Constructor<T> constructor = (Constructor<T>) compatClass.getConstructor(paramterClasses);
            return constructor.newInstance(parameters);

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getInternalVersion() {
        return internalVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public int compareWithMinecraftVersion(String version) {
        if (VERSION_MATCHER.matcher(version).find()) {
            if (compareCache.containsKey(minecraftVersion + "-::-" + version)) {
                return compareCache.get(minecraftVersion + "-::-" + version);
            }
            int compare = Util.versionCompare(minecraftVersion, version);
            compareCache.put(minecraftVersion + "-::-" + version, compare);
            return compare;
        }
        throw new IllegalArgumentException("\"version\" must be a valid Minecraft version. \"" + version + "\" given.");
    }

    private String getBukkitVersionFromMinecraftVersion() {
        HashMap<String, String> versionMap = new HashMap<>();
        try {
            InputStreamReader streamReader = new InputStreamReader(MyPetApi.getPlugin().getResource("versionmatcher.csv"), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            Bukkit.getConsoleSender().sendMessage("Trying to read");
            String line;
            while ((line = reader.readLine()) != null) {
                Bukkit.getConsoleSender().sendMessage(line);
                String[] parts = line.split(",");
                versionMap.put(parts[0], parts[1]);
            }
        } catch(Exception ignored) {
        }

        String bukkitVersion = null;
        if (versionMap.containsKey(minecraftVersion))
            bukkitVersion = versionMap.get(minecraftVersion);
        return bukkitVersion;
    }

    public boolean isCompatible(String version) {
        return compareWithMinecraftVersion(version) >= 0;
    }
}
