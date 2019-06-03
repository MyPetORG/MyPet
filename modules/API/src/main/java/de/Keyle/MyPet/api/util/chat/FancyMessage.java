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

package de.Keyle.MyPet.api.util.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.chat.parts.MessagePart;
import de.Keyle.MyPet.api.util.chat.parts.Text;
import de.Keyle.MyPet.api.util.chat.parts.Translation;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FancyMessage {

    final static Pattern colorStylePattern = Pattern.compile("(?i)" + '§' + "([0-9a-fk-or])");
    final static Gson gson = new Gson();

    private final List<MessagePart> messageParts;

    public FancyMessage(final String firstPartText) {
        this();
        messageParts.add(new Text(firstPartText));
    }

    public FancyMessage() {
        messageParts = new ArrayList<>();
    }

    public FancyMessage color(final ChatColor color) {
        if (!color.isColor()) {
            throw new IllegalArgumentException(color.name() + " is not a color");
        }
        latest().setColor(color);
        return this;
    }

    public FancyMessage style(final ChatColor... styles) {
        for (final ChatColor style : styles) {
            if (!style.isFormat()) {
                throw new IllegalArgumentException(style.name() + " is not a style");
            }
        }
        latest().setStyles(styles);
        return this;
    }

    public FancyMessage file(final String path) {
        onClick("open_file", path);
        return this;
    }

    public FancyMessage link(final String url) {
        onClick("open_url", url);
        return this;
    }

    public FancyMessage suggest(final String command) {
        onClick("suggest_command", command);
        return this;
    }

    public FancyMessage command(final String command) {
        onClick("run_command", command);
        return this;
    }

    public FancyMessage itemTooltip(final String itemJSON) {
        onHover("show_item", itemJSON);
        return this;
    }

    public FancyMessage itemTooltip(final ItemTooltip itemJSON) {
        onHover("show_item", itemJSON.toJSONString());
        return this;
    }

    public FancyMessage tooltip(final String text) {
        onHover("show_text", text);
        return this;
    }

    public FancyMessage then(final Object obj) {
        messageParts.add(new Text(obj.toString()));
        return this;
    }

    public FancyMessage thenTranslate(final String id) {
        messageParts.add(new Translation(id));
        return this;
    }

    public FancyMessage translateUsing(final String id, Object... using) {
        messageParts.add(new Translation(id, using));
        return this;
    }

    @SuppressWarnings("unchecked")
    public String toJSONString() {
        JsonArray parts = new JsonArray();

        for (final MessagePart part : messageParts) {
            parts.add(part.toJson());
        }

        return gson.toJson(parts);
    }

    @SuppressWarnings("unchecked")
    public JsonArray toJSON() {
        JsonArray parts = new JsonArray();

        for (final MessagePart part : messageParts) {
            parts.add(part.toJson());
        }

        return parts;
    }

    private MessagePart latest() {
        return messageParts.get(messageParts.size() - 1);
    }

    private void onClick(final String name, final String data) {
        final MessagePart latest = latest();
        latest.setClickActionName(name);
        latest.setClickActionData(data);
    }

    private void onHover(final String name, final String data) {
        final MessagePart latest = latest();
        latest.setHoverActionName(name);
        latest.setHoverActionData(data);
    }
}