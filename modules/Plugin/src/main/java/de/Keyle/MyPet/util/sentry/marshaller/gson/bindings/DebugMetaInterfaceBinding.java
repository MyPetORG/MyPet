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
import io.sentry.event.interfaces.DebugMetaInterface;

import java.io.IOException;

public class DebugMetaInterfaceBinding implements InterfaceBinding<DebugMetaInterface> {

    private static final String IMAGES = "images";
    private static final String UUID = "uuid";
    private static final String TYPE = "type";

    public DebugMetaInterfaceBinding() {
    }

    public JsonElement writeInterface(DebugMetaInterface debugMetaInterface) throws IOException {
        JsonObject generator = new JsonObject();
        this.writeDebugImages(generator, debugMetaInterface);
        return generator;
    }

    private void writeDebugImages(JsonObject generator, DebugMetaInterface debugMetaInterface) throws IOException {
        JsonArray images = new JsonArray();
        for (DebugMetaInterface.DebugImage debugImage : debugMetaInterface.getDebugImages()) {
            JsonObject image = new JsonObject();
            image.addProperty(UUID, debugImage.getUuid());
            image.addProperty(TYPE, debugImage.getType());
            images.add(image);
        }
        generator.add(IMAGES, images);
    }
}