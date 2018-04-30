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

package de.Keyle.MyPet.api.util.chat;

import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.chat.parts.MessagePart;
import de.Keyle.MyPet.api.util.chat.parts.Text;
import de.Keyle.MyPet.api.util.chat.parts.Translation;
import org.bukkit.ChatColor;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class FancyMessage {
    private final List<MessagePart> messageParts;

    public FancyMessage(final String firstPartText) {
        messageParts = new ArrayList<>();
        messageParts.add(new Text(firstPartText));
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

    @SuppressWarnings("unchecked")
    public String toJSONString() {
        JSONArray parts = new JSONArray();

        for (final MessagePart part : messageParts) {
            parts.add(part.toJson());
        }

        return parts.toJSONString();
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