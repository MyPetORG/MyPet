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

import de.Keyle.MyPet.MyPetApi;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemTooltip {

    private static final Pattern MINECRAFT_VERSION_MATCHER = Pattern.compile("\\(MC: \\d\\.(\\d+)(?:\\.\\d+)?\\)");
    private static int minorVersion = Integer.MIN_VALUE;

    protected Material material = Material.STONE;
    protected String title = "";
    protected List<String> lore = new ArrayList<>();

    protected String oldItem;
    protected boolean hasChanged = true;

    public ItemTooltip() {
        Matcher regexMatcher = MINECRAFT_VERSION_MATCHER.matcher(Bukkit.getVersion());
        if (regexMatcher.find()) {
            String version = regexMatcher.group(1);
            minorVersion = Integer.parseInt(version);
        }
    }

    public ItemTooltip setMaterial(Material material) {
        Validate.notNull(material, "Material cannot be null");
        if (this.material != material) {
            this.material = material;
            hasChanged = true;
        }
        return this;
    }

    public ItemTooltip setTitle(String title) {
        Validate.notNull(title, "Title cannot be null");
        if (!this.title.equals(title)) {
            this.title = title;
            hasChanged = true;
        }
        return this;
    }

    public ItemTooltip setLore(String... lore) {
        Validate.notNull(lore, "Lore cannot be null");
        this.lore.clear();
        Collections.addAll(this.lore, lore);
        hasChanged = true;
        return this;
    }

    public ItemTooltip addLoreLine(String line) {
        Validate.notNull(line, "Line cannot be null");
        this.lore.add(line);
        hasChanged = true;
        return this;
    }

    public ItemTooltip addLore(List<String> lore) {
        Validate.notNull(lore, "Lore cannot be null");
        if (lore.size() > 0) {
            this.lore.addAll(lore);
            hasChanged = true;
        }
        return this;
    }

    public Material getMaterial() {
        return material;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLore() {
        return lore;
    }

    @SuppressWarnings("unchecked")
    public String toJSONString() {
        if (!hasChanged) {
            return oldItem;
        }

        String jsonString = "{id:";

        if (MyPetApi.getCompatUtil().isCompatible("1.13")) {
            jsonString += "\"" + material.getKey().toString() + "\"";
        } else if (MyPetApi.getCompatUtil().isCompatible("1.8")) {
            jsonString += material.name().toLowerCase();
        } else {
            jsonString += material.getId();
        }

        if (lore.size() > 0 || !title.equals("")) {
            jsonString += ",tag:{display:{";
            if (!title.equals("")) {
                jsonString += "Name:\"";
                if (MyPetApi.getCompatUtil().isCompatible("1.13")) {
                    jsonString += "{\\\"text\\\":\\\"";
                }
                jsonString += title.replaceAll("\"", "\\\\\"").replaceAll("\'", "\\\'");
                if (MyPetApi.getCompatUtil().isCompatible("1.13")) {
                    jsonString += "\\\"}";
                }
                jsonString += "\"";
                if (lore.size() > 0) {
                    jsonString += ",";
                }
            }
            if (lore.size() > 0) {
                jsonString += "Lore:[";
                for (int i = 0; i < lore.size(); i++) {
                    if (i > 0) {
                        jsonString += ",";
                    }
                    if (minorVersion == 7 && lore.get(i).contains(":")) {
                        jsonString += "a:";
                    }
                    jsonString += "\"" + lore.get(i).replaceAll("\"", "\\\\\"").replaceAll("\'", "\\\'") + "\"";

                }
                jsonString += "]";
            }
            jsonString += "}}";
        }
        jsonString += ", Count:1}";

        hasChanged = false;
        oldItem = jsonString;

        return oldItem;
    }
}