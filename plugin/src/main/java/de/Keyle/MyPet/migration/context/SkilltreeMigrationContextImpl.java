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

package de.Keyle.MyPet.migration.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.SkilltreeMigrationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkilltreeMigrationContextImpl implements SkilltreeMigrationContext {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File skilltreeDir;

    public SkilltreeMigrationContextImpl(File skilltreeDir) {
        this.skilltreeDir = skilltreeDir;
    }

    @Override
    public List<File> getSkilltreeFiles() {
        File[] listing = skilltreeDir.listFiles((dir, name) -> name.endsWith(".st.json"));
        if (listing == null) {
            return Collections.emptyList();
        }
        List<File> files = new ArrayList<>(listing.length);
        Collections.addAll(files, listing);
        return files;
    }

    @Override
    public JsonObject readSkilltree(File file) throws MigrationException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new MigrationException("Failed to read skilltree: " + file.getName(), e);
        }
    }

    @Override
    public void writeSkilltree(File file, JsonObject data) throws MigrationException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            throw new MigrationException("Failed to write skilltree: " + file.getName(), e);
        }
    }
}
