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

package de.Keyle.MyPet.skilltreecreator;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ApiServer {

    private File skilltreeDir;

    public ApiServer(File skilltreeDir) {
        this.skilltreeDir = skilltreeDir;
    }

    @SuppressWarnings("unchecked")
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        switch (session.getMethod()) {
            case GET: {
                if (uri.equals("/api/skilltrees")) {
                    JSONArray jsonSkilltrees = new JSONArray();
                    File[] jsonFiles = skilltreeDir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".st.json"));
                    if (jsonFiles != null) {
                        for (File jsonFile : jsonFiles) {
                            JSONObject jsonSkilltree = loadJsonObject(jsonFile);
                            if (jsonSkilltree != null) {
                                jsonSkilltrees.add(jsonSkilltree);
                            }
                        }
                    }
                    return WebServer.newFixedJsonResponse(jsonSkilltrees.toJSONString());
                }
                break;
            }
            case POST: {
                if (uri.equals("/api/skilltrees/save")) {
                    try {
                        Map<String, String> bodyMap = new HashMap<>();
                        session.parseBody(bodyMap);

                        JSONArray jsonArray = loadJsonArray(bodyMap.get("postData"));
                        if (jsonArray != null) {
                            File[] jsonFiles = skilltreeDir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".st.json"));
                            if (jsonFiles != null) {
                                for (File jsonFile : jsonFiles) {
                                    jsonFile.delete();
                                }
                            }
                            for (Object o : jsonArray) {
                                JSONObject jsonSkilltree = (JSONObject) o;
                                String filename = jsonSkilltree.get("ID").toString() + ".st.json";
                                saveJsonObject(new File(skilltreeDir, filename), jsonSkilltree);
                            }
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

    public JSONArray loadJsonArray(File jsonFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONArray loadJsonArray(String jsonString) {
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonString);
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject loadJsonObject(File jsonFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONObject) {
                return (JSONObject) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveJsonObject(File jsonFile, JSONObject jsonObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonObject.toJSONString());
        String prettyJsonString = gson.toJson(je);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
            writer.write(prettyJsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
