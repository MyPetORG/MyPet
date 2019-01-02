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

import de.Keyle.MyPet.api.MyPetVersion;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

import java.io.IOException;
import java.util.prefs.Preferences;

public class WebsocketHandler extends WebSocket {

    public WebsocketHandler(IHTTPSession handshakeRequest) {
        super(handshakeRequest);
    }

    @Override
    protected void onOpen() {
        System.out.println("WS: Open connection");
        if (!MyPetVersion.isPremium()) {
            try {
                this.send("{\"action\": \"TOGGLE_PREMIUM\", \"data\": \"Plugin is not premium.\"}");
            } catch (IOException ignored) {
            }
        }
        String lang = Preferences.userNodeForPackage(Main.MyPetPlugin.class).get("Language", "NO-Language");
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
        JSONObject message = parseJsonObject(m.getTextPayload());
        if (message != null && message.containsKey("action")) {
            switch ("" + message.get("action")) {
                case "PING":
                    break;
                case "CHANGE_LANGUAGE":
                    Preferences.userNodeForPackage(Main.MyPetPlugin.class).put("Language", message.get("data").toString());
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

    public JSONObject parseJsonObject(String jsonString) {
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonString);
            if (obj instanceof JSONObject) {
                return (JSONObject) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}