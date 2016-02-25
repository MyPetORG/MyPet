/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatUtil {
    private static final Pattern PACKAGE_VERSION_MATCHER = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");

    private String internalVersion = null;

    public CompatUtil() {
        Matcher regexMatcher = PACKAGE_VERSION_MATCHER.matcher(Bukkit.getServer().getClass().getCanonicalName());
        if (regexMatcher.find()) {
            internalVersion = regexMatcher.group(1);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getComapatInstance(Class<? extends T> clazz, String path, String className) {
        if (internalVersion == null) {
            return null;
        }

        String classPath = clazz.getCanonicalName();
        if (classPath.startsWith("de.Keyle.MyPet")) {
            classPath = "de.Keyle.MyPet.compat." + internalVersion + "." + path + (path != null && !path.equals("") ? "." : "") + className;
        }

        try {
            Class<? extends T> compatClass = (Class<? extends T>) Class.forName(classPath);

            if (Modifier.isAbstract(compatClass.getModifiers())) {
                return null;
            }
            if (Modifier.isInterface(compatClass.getModifiers())) {
                return null;
            }

            T instance = compatClass.newInstance();

            return instance;

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        return null;
    }

    public <T> T getComapatInstance(Class<? extends T> clazz, String path) {
        String className = clazz.getCanonicalName();
        className = className.substring(className.lastIndexOf(".") + 1);
        return getComapatInstance(clazz, path, className);
    }

    public <T> T getComapatInstance(Class<? extends T> clazz) {
        String classPath = clazz.getCanonicalName();
        if (classPath.startsWith("de.Keyle.MyPet")) {
            classPath = classPath.substring(15);
        }
        classPath = classPath.substring(0, classPath.lastIndexOf("."));
        return getComapatInstance(clazz, classPath);
    }

    public String getInternalVersion() {
        return internalVersion;
    }
}
