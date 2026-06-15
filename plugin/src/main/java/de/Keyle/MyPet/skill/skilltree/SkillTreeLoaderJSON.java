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

package de.Keyle.MyPet.skill.skilltree;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.exceptions.InvalidSkilltreeException;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SkillTreeLoaderJSON {

    public static void loadSkilltrees(File skilltreePath) {
        File[] skilltreeFiles = skilltreePath.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".st.json"));
        if (skilltreeFiles != null) {
            for (File skilltreeFile : skilltreeFiles) {
                loadSkilltree(skilltreeFile);
            }
        }
    }

    public static void loadSkilltree(File skilltreeFile) {
        if (skilltreeFile.exists()) {
            try {
                loadSkilltree(loadJsonObject(skilltreeFile));
            } catch (InvalidSkilltreeException | JsonSyntaxException e) {
                MyPetApi.getLogger().warning("Error in " + skilltreeFile.getName() + " -> Skilltree not loaded.");
                MyPetApi.getLogger().warning(e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    public static void loadSkilltree(JsonObject skilltreeObject) {
        SkilltreeJsonReader reader = new SkilltreeJsonReader(skilltreeObject);
        if (!reader.has("ID")) {
            return;
        }

        String skilltreeID = reader.optString("ID").orElseThrow();
        if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeID)) {
            return;
        }

        Skilltree skilltree = new Skilltree(skilltreeID);
        SkilltreeMetadataParser.apply(reader, skilltree);
        SkillUpgradeParser.apply(reader, skilltree);

        MyPetApi.getSkilltreeManager().registerSkilltree(skilltree);
    }

    private static JsonObject loadJsonObject(File jsonFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(jsonFile.toPath()), StandardCharsets.UTF_8))) {
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        }
    }
}
