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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildIntegrityTest {

    @TempDir
    File dir;

    private static final List<String> SHIPPED = Arrays.asList(
            "de/Keyle/MyPet/MyPetPlugin.class",
            "de/Keyle/MyPet/entity/PetImpl.class",
            "de/Keyle/MyPet/entity/PetImpl$1.class");

    private File jar(String... entries) throws IOException {
        File f = new File(dir, "MyPet.jar");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            for (String entry : entries) {
                out.putNextEntry(new ZipEntry(entry));
                out.write(entry.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return f;
    }

    private static String index(List<String> classes) {
        return String.join("\n", classes) + "\n";
    }

    @Test
    void jarWithExactlyTheShippedClassesIsUnmodified() throws IOException {
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/entity/PetImpl.class",
                "de/Keyle/MyPet/entity/PetImpl$1.class", "plugin.yml", "locale/en.properties");
        assertTrue(BuildIntegrity.matchesClassIndex(jar, index(SHIPPED)));
    }

    @Test
    void jarWithAnExtraMyPetClassIsModified() throws IOException {
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/entity/PetImpl.class",
                "de/Keyle/MyPet/entity/PetImpl$1.class", "de/Keyle/MyPet/minetown/PlayerFollowDistances.class");
        assertFalse(BuildIntegrity.matchesClassIndex(jar, index(SHIPPED)));
    }

    @Test
    void jarMissingAShippedClassIsModified() throws IOException {
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/entity/PetImpl.class");
        assertFalse(BuildIntegrity.matchesClassIndex(jar, index(SHIPPED)));
    }

    @Test
    void relocatedLibrariesAndForeignClassesAreNotCompared() throws IOException {
        // bStats is relocated under de.Keyle.MyPet.util.metrics at shade time, so it is in the jar
        // but not in the compile-time index; classes outside the MyPet package are not MyPet's.
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/entity/PetImpl.class",
                "de/Keyle/MyPet/entity/PetImpl$1.class", "de/Keyle/MyPet/util/metrics/bukkit/Metrics.class",
                "net/kyori/adventure/nbt/BinaryTag.class");
        assertTrue(BuildIntegrity.matchesClassIndex(jar, index(SHIPPED)));
    }

    @Test
    void myPet3RelocatedLibrariesAndTheSentryPathAreNotCompared() throws IOException {
        // MyPet 3 relocates Hikari, knbt, gson, bson, MongoDB and Sentry under its package too.
        // MyPet's own reporter shares the Sentry path, so the index may list it while the
        // comparison leaves that path out on both sides.
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/entity/PetImpl.class",
                "de/Keyle/MyPet/entity/PetImpl$1.class", "de/Keyle/MyPet/util/hikari/HikariDataSource.class",
                "de/Keyle/MyPet/util/nbt/TagCompound.class", "de/Keyle/MyPet/util/sentry/Sentry.class");
        String withReporter = index(SHIPPED) + "de/Keyle/MyPet/util/sentry/SentryErrorReporter.class\n";
        assertTrue(BuildIntegrity.matchesClassIndex(jar, withReporter));
    }

    @Test
    void indexOrderAndBlankLinesDoNotMatter() throws IOException {
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/entity/PetImpl.class",
                "de/Keyle/MyPet/entity/PetImpl$1.class");
        String shuffled = "\nde/Keyle/MyPet/entity/PetImpl$1.class\r\nde/Keyle/MyPet/MyPetPlugin.class\n\nde/Keyle/MyPet/entity/PetImpl.class";
        assertTrue(BuildIntegrity.matchesClassIndex(jar, shuffled));
    }

    @Test
    void missingIndexCannotProveModification() throws IOException {
        File jar = jar("de/Keyle/MyPet/MyPetPlugin.class", "de/Keyle/MyPet/anything/Extra.class");
        assertTrue(BuildIntegrity.matchesClassIndex(jar, null));
        assertTrue(BuildIntegrity.matchesClassIndex(jar, "   \n"));
    }

    @Test
    void unreadableJarCannotProveModification() {
        assertTrue(BuildIntegrity.matchesClassIndex(new File(dir, "missing.jar"), index(SHIPPED)));
    }

    @Test
    void classIndexResourcePathIsStable() {
        assertEquals("/de/Keyle/MyPet/class-index.txt", BuildIntegrity.CLASS_INDEX_RESOURCE);
    }
}
