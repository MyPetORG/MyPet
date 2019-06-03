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

package de.Keyle.MyPet.api.util.chat.parts;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.ChatColor;

public class MessagePart {
    @Getter @Setter @Accessors(chain = true)
    ChatColor color = null;
    @Getter @Setter @Accessors(chain = true)
    ChatColor[] styles = null;
    @Getter @Setter @Accessors(chain = true)
    String clickActionName = null;
    @Getter @Setter @Accessors(chain = true)
    String clickActionData = null;
    @Getter @Setter @Accessors(chain = true)
    String hoverActionName = null;
    @Getter @Setter @Accessors(chain = true)
    String hoverActionData = null;

    @SuppressWarnings("unchecked")
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (color != null) {
            json.addProperty("color", color.name().toLowerCase());
        }
        if (styles != null) {
            for (final Style style : Style.values()) {
                json.addProperty(style.getName(), "true");
            }
        }
        if (clickActionName != null && clickActionData != null) {
            JsonObject actionJson = new JsonObject();
            json.add("clickEvent", actionJson);
            actionJson.addProperty("action", clickActionName);
            actionJson.addProperty("value", clickActionData);
        }
        if (hoverActionName != null && hoverActionData != null) {
            JsonObject hoverJson = new JsonObject();
            json.add("hoverEvent", hoverJson);
            hoverJson.addProperty("action", hoverActionName);
            hoverJson.addProperty("value", hoverActionData);
        }
        return json;
    }
}