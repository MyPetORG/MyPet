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

import de.Keyle.MyPet.api.Util;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.protocols.websockets.WebSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer extends NanoWSD {

    static Map<String, String> MIME_TYPES = new HashMap<>();
    static Response NOT_FOUND = Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found");

    ApiHandler apiHandler;
    List<WebsocketHandler> websocketHandlers = new ArrayList<>();

    public WebServer(File skilltreeDir) throws IOException {
        super(64712);
        apiHandler = new ApiHandler(skilltreeDir);

        setTempFileManagerFactory(new FileManager());
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession ihttpSession) {
        if (ihttpSession.getUri().equals("/websocket")) {
            WebsocketHandler websocketHandler = new WebsocketHandler(ihttpSession) {
                @Override
                protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
                    websocketHandlers.remove(this);
                    super.onClose(code, reason, initiatedByRemote);
                }
            };
            websocketHandlers.add(websocketHandler);
            return websocketHandler;
        }
        return null;
    }

    public void sendToWebsockets(String message) {
        for (WebsocketHandler handler : websocketHandlers) {
            try {
                handler.send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/")) {
            uri = "/index.html";
        }

        System.out.println(session.getMethod().name() + ": " + uri);

        if (uri.startsWith("/api/")) {
            return apiHandler.handle(session);
        } else {
            return serveFile(uri);
        }
    }

    @Override
    public void stop() {
        sendToWebsockets("{\"action\": \"SERVER_STOP\", \"message\": \"Server stopped\"}");

        for (WebsocketHandler handler : websocketHandlers) {
            try {
                handler.close(CloseCode.GoingAway, "Server stopped", false);
            } catch (IOException ignored) {
            }
        }

        super.stop();
    }

    public Response serveFile(String uri) {
        try {
            return newResourceResponse("gui" + uri);
        } catch (FileNotFoundException ignored) {
            try {
                return newResourceResponse("gui/index.html");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return NOT_FOUND;
    }

    public static String getMimeType(String fileName) {
        String extension = Util.getFileExtension(fileName);
        if (MIME_TYPES.containsKey(extension)) {
            return MIME_TYPES.get(extension);
        }
        return "text/plain";
    }

    public static Response newFixedJsonResponse(String jsonString) {
        return Response.newFixedLengthResponse(Status.OK, "application/json", jsonString);
    }

    public static Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res = Response.newFixedLengthResponse(Status.OK, mime, new FileInputStream(file), (long) ((int) file.length()));
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    public static Response newResourceResponse(String file) throws FileNotFoundException {
        String mime = getMimeType(file);
        Response res;
        try {
            res = Response.newChunkedResponse(Status.OK, mime, ClassLoader.getSystemResource(file).openStream());
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    static {
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("css", "text/css");
    }
}
