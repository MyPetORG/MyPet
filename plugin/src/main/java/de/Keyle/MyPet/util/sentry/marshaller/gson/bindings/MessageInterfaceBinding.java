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

package de.Keyle.MyPet.util.sentry.marshaller.gson.bindings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.sentry.event.interfaces.MessageInterface;
import io.sentry.util.Util;

public class MessageInterfaceBinding implements InterfaceBinding<MessageInterface> {

    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 1000;
    private static final String MESSAGE_PARAMETER = "message";
    private static final String PARAMS_PARAMETER = "params";
    private static final String FORMATTED_PARAMETER = "formatted";
    private final int maxMessageLength;

    public MessageInterfaceBinding() {
        this.maxMessageLength = DEFAULT_MAX_MESSAGE_LENGTH;
    }

    public MessageInterfaceBinding(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public JsonElement writeInterface(MessageInterface messageInterface) {
        JsonObject generator = new JsonObject();
        generator.addProperty(MESSAGE_PARAMETER, Util.trimString(messageInterface.getMessage(), this.maxMessageLength));
        JsonArray params = new JsonArray();

        for (String parameter : messageInterface.getParameters()) {
            params.add(parameter);
        }
        generator.add(PARAMS_PARAMETER, params);

        if (messageInterface.getFormatted() != null) {
            generator.addProperty(FORMATTED_PARAMETER, Util.trimString(messageInterface.getFormatted(), this.maxMessageLength));
        }
        return generator;
    }
}
