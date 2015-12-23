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

import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Original Author: evilmidget38
 */
public class UUIDFetcher {
    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private static final JSONParser jsonParser = new JSONParser();
    private static boolean rateLimiting = true;

    private static final HashMap<String, UUID> fetchedUUIDs = new HashMap<>();
    private static final Map<String, UUID> readonlyFetchedUUIDs = Collections.unmodifiableMap(fetchedUUIDs);

    private UUIDFetcher() {
    }

    public static void limitRate(boolean flag) {
        rateLimiting = flag;
    }

    public static Map<String, UUID> call(String name) {
        ArrayList<String> single = new ArrayList<>();
        single.add(name);
        return call(single);
    }

    public static Map<String, UUID> call(List<String> names) {
        names = new ArrayList<>(names);
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

        int count = names.size();

        DebugLogger.info("get UUIDs for " + names.size() + " player(s)");
        int requests = (int) Math.ceil(names.size() / PROFILES_PER_REQUEST);
        try {
            for (int i = 0; i < requests; i++) {
                HttpURLConnection connection = createConnection();
                String body = JSONArray.toJSONString(names.subList(i * 100, Math.min((i + 1) * 100, names.size())));
                writeBody(connection, body);
                JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
                count -= array.size();
                for (Object profile : array) {
                    JSONObject jsonProfile = (JSONObject) profile;
                    String id = (String) jsonProfile.get("id");
                    String name = (String) jsonProfile.get("name");
                    UUID uuid = UUIDFetcher.getUUID(id);
                    fetchedUUIDs.put(name, uuid);
                }
                if (rateLimiting && i != requests - 1) {
                    Thread.sleep(100L);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (count > 0) {
            MyPetLogger.write("Can not get UUIDs for " + count + " players. Pets of these player may be lost.");
        }
        return readonlyFetchedUUIDs;
    }

    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        OutputStream stream = connection.getOutputStream();
        stream.write(body.getBytes());
        stream.flush();
        stream.close();
    }

    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URL(PROFILE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    private static UUID getUUID(String id) {
        if (id.contains("-")) {
            return UUID.fromString(id);
        }
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }
}