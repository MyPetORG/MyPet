/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.util.sentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Tells a MyPet jar as built by CI from one that was modified afterwards.
 * <p>
 * The build writes the names of every class under MyPet's package into
 * {@value #CLASS_INDEX_RESOURCE} inside the jar. A fork that repackages a release jar with its
 * own classes added (keeping the Sentry DSN from the manifest) changes that set, and the error
 * reporter then stays off instead of filing the fork's errors against the upstream project.
 * <p>
 * A jar without the index, such as an IDE run, cannot be checked and counts as unmodified; those
 * builds carry no DSN anyway.
 */
public final class BuildIntegrity {

    public static final String CLASS_INDEX_RESOURCE = "/de/Keyle/MyPet/class-index.txt";

    private static final String CLASS_PREFIX = "de/Keyle/MyPet/";
    /**
     * Shaded libraries are relocated under these paths at jar time, so the compile-time index lacks
     * them. MyPet's own reporter shares the Sentry SDK's path, so that path is left out of both sides.
     */
    private static final String[] RELOCATED_PREFIXES = {
            "de/Keyle/MyPet/util/metrics/", "de/Keyle/MyPet/util/hikari/", "de/Keyle/MyPet/util/nbt/",
            "de/Keyle/MyPet/util/bson/", "de/Keyle/MyPet/util/gson/", "de/Keyle/MyPet/util/mongodb/",
            "de/Keyle/MyPet/util/sentry/"
    };

    private BuildIntegrity() {
    }

    /**
     * Whether the given jar holds exactly the MyPet classes the build put into it.
     *
     * @param jar the plugin's jar file
     * @return false only when the jar is readable, carries an index, and its classes differ from it
     */
    public static boolean isUnmodified(File jar) {
        return matchesClassIndex(jar, readIndexResource());
    }

    /**
     * Compares the MyPet classes in a jar with an index of expected entry names, one per line.
     *
     * @param jar   the jar to inspect
     * @param index the expected class entries, or null when there is no index
     * @return false only when both sides could be read and the sets differ
     */
    static boolean matchesClassIndex(File jar, String index) {
        Set<String> expected = parseIndex(index);
        if (expected.isEmpty()) {
            return true;
        }
        Set<String> actual = new HashSet<>();
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (isComparedClass(name)) {
                    actual.add(name);
                }
            }
        } catch (IOException e) {
            return true;
        }
        return actual.equals(expected);
    }

    private static boolean isComparedClass(String name) {
        if (!name.startsWith(CLASS_PREFIX) || !name.endsWith(".class")) {
            return false;
        }
        for (String prefix : RELOCATED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> parseIndex(String index) {
        Set<String> names = new HashSet<>();
        if (index == null) {
            return names;
        }
        for (String line : index.split("\n")) {
            String name = line.trim();
            if (isComparedClass(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private static String readIndexResource() {
        try (InputStream in = BuildIntegrity.class.getResourceAsStream(CLASS_INDEX_RESOURCE)) {
            if (in == null) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line).append('\n');
                }
            }
            return text.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
