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

package de.Keyle.MyPet.skilltreecreator;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiHandler {

    private File skilltreeDir;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ApiHandler(File skilltreeDir) {
        this.skilltreeDir = skilltreeDir;
    }

    @SuppressWarnings("unchecked")
    public Response handle(IHTTPSession session) {
        String uri = session.getUri();

        switch (session.getMethod()) {
            case GET: {
                if (uri.equals("/api/skilltrees")) {
                    JsonArray jsonSkilltrees = new JsonArray();
                    File[] jsonFiles = skilltreeDir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".st.json"));
                    if (jsonFiles != null) {
                        for (File jsonFile : jsonFiles) {
                            JsonObject jsonSkilltree = loadJsonObject(jsonFile);
                            if (jsonSkilltree != null) {
                                jsonSkilltrees.add(jsonSkilltree);
                            }
                        }
                    }
                    return WebServer.newFixedJsonResponse(gson.toJson(jsonSkilltrees));
                }
                break;
            }
            case POST: {
                if (uri.equals("/api/skilltrees/save")) {
                    try {
                        Map<String, String> bodyMap = new HashMap<>();
                        session.parseBody(bodyMap);

                        JsonArray jsonArray = loadJsonArray(bodyMap.get("postData"));
                        if (jsonArray != null) {
                            File[] jsonFiles = skilltreeDir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".st.json"));
                            if (jsonFiles != null) {
                                for (File jsonFile : jsonFiles) {
                                    jsonFile.delete();
                                }
                            }
                            jsonArray.forEach(jsonSkilltree -> {
                                String filename = jsonSkilltree.getAsJsonObject().get("ID").getAsString() + ".st.json";
                                saveJsonObject(new File(skilltreeDir, filename), jsonSkilltree.getAsJsonObject());
                            });
                        }
                        return WebServer.newFixedJsonResponse("{\"message\":\"DONE\"}");
                    } catch (IOException | NanoHTTPD.ResponseException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }

        return WebServer.NOT_FOUND;
    }

    public JsonArray loadJsonArray(File jsonFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
            return gson.fromJson(reader, JsonArray.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JsonArray loadJsonArray(String jsonString) {
        try {
            return gson.fromJson(jsonString, JsonArray.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JsonObject loadJsonObject(File jsonFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
            return gson.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveJsonObject(File jsonFile, JsonObject jsonObject) {
        String prettyJsonString = gson.toJson(jsonObject).replace("\\u003c", "<").replace("\\u003e", ">");
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8));
            writer.write(prettyJsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
