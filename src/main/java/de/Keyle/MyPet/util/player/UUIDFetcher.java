/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util.player;

import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Original Author: evilmidget38
 */
public class UUIDFetcher {
    private static final int MAX_SEARCH = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/page/";
    private static final String AGENT = "minecraft";
    private static final JSONParser jsonParser = new JSONParser();

    private static final HashMap<String, UUID> fetchedUUIDs = new HashMap<String, UUID>();
    private static final Map<String, UUID> readonlyFetchedUUIDs = Collections.unmodifiableMap(fetchedUUIDs);

    public static Map<String, UUID> call(String playerName) {
        if (fetchedUUIDs.containsKey(playerName)) {
            return readonlyFetchedUUIDs;
        }
        MyPetLogger.write("get UUID for " + playerName);
        String body = buildBody(playerName);
        try {
            for (int i = 1; i < MAX_SEARCH; i++) {
                HttpURLConnection connection = createConnection(i);
                writeBody(connection, body);
                JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
                JSONArray array = (JSONArray) jsonObject.get("profiles");
                Number count = (Number) jsonObject.get("size");
                if (count.intValue() == 0) {
                    break;
                }
                for (Object profile : array) {
                    JSONObject jsonProfile = (JSONObject) profile;
                    String id = (String) jsonProfile.get("id");
                    String name = (String) jsonProfile.get("name");
                    UUID uuid = UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
                    fetchedUUIDs.put(name, uuid);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return readonlyFetchedUUIDs;
    }

    public static Map<String, UUID> call(List<String> names) {
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            String playerName = iterator.next();
            if (fetchedUUIDs.containsKey(playerName)) {
                iterator.remove();
            }
        }
        if (names.size() == 0) {
            return readonlyFetchedUUIDs;
        }
        MyPetLogger.write("get UUIDs for " + names);
        String body = buildBody(names);
        try {
            for (int i = 1; i < MAX_SEARCH; i++) {
                HttpURLConnection connection = createConnection(i);
                writeBody(connection, body);
                JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
                JSONArray array = (JSONArray) jsonObject.get("profiles");
                Number count = (Number) jsonObject.get("size");
                if (count.intValue() == 0) {
                    break;
                }
                for (Object profile : array) {
                    JSONObject jsonProfile = (JSONObject) profile;
                    String id = (String) jsonProfile.get("id");
                    String name = (String) jsonProfile.get("name");
                    UUID uuid = UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
                    fetchedUUIDs.put(name, uuid);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return readonlyFetchedUUIDs;
    }

    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(body.getBytes());
        writer.flush();
        writer.close();
    }

    private static HttpURLConnection createConnection(int page) throws Exception {
        URL url = new URL(PROFILE_URL + page);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    @SuppressWarnings("unchecked")
    private static String buildBody(String name) {
        List<JSONObject> lookups = new ArrayList<JSONObject>();
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("agent", AGENT);
        lookups.add(obj);
        return JSONValue.toJSONString(lookups);
    }

    @SuppressWarnings("unchecked")
    private static String buildBody(List<String> names) {
        List<JSONObject> lookups = new ArrayList<JSONObject>();
        for (String name : names) {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("agent", AGENT);
            lookups.add(obj);
        }
        return JSONValue.toJSONString(lookups);
    }
}