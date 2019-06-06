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
import com.google.gson.JsonObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

import java.io.IOException;
import java.util.prefs.Preferences;

public class WebsocketHandler extends WebSocket {

    Gson gson = new Gson();

    public WebsocketHandler(IHTTPSession handshakeRequest) {
        super(handshakeRequest);
    }

    @Override
    protected void onOpen() {
        System.out.println("WS: Open connection");
        String lang = Preferences.userNodeForPackage(Main.MyPetPlugin.class).get("Language", "NO-Language").replace("\"", "");
        if (!lang.equals("NO-Language")) {
            try {
                this.send("{\"action\": \"CHANGE_LANGUAGE\", \"data\": \"" + lang + "\"}");
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
        System.out.println("WS: " + (initiatedByRemote ? "Remote" : "Self") + " "
                + (code != null ? code : "UnknownCloseCode")
                + (reason != null && !reason.isEmpty() ? ": " + reason : ""));
    }

    @Override
    protected void onMessage(WebSocketFrame m) {
        m.setUnmasked();
        JsonObject message = parseJsonObject(m.getTextPayload());
        if (message != null && message.has("action")) {
            switch (message.get("action").getAsString()) {
                case "PING":
                    break;
                case "CHANGE_LANGUAGE":
                    Preferences.userNodeForPackage(Main.MyPetPlugin.class).put("Language", message.get("data").getAsString());
                    break;
                case "CLOSE":
                    Main.close();
                    break;
                default:
                    System.out.println(message);
                    break;
            }
        }
    }

    @Override
    protected void onPong(WebSocketFrame pong) {
    }

    @Override
    protected void onException(IOException exception) {
        exception.printStackTrace();
    }

    public JsonObject parseJsonObject(String jsonString) {
        try {
            return gson.fromJson(jsonString, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}