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

package de.Keyle.MyPet.api.util.inventory.meta;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagString;

public class SkullMeta implements IconMeta {
    String name = null;
    String texture = null;

    /**
     * Gets the owner of the skull.
     *
     * @return the owner if the skull
     */
    public String getOwner() {
        return name;
    }

    /**
     * Checks to see if the skull has an owner.
     *
     * @return true if the skull has an owner
     */
    public boolean hasOwner() {
        return name != null;
    }

    /**
     * Sets the owner of the skull.
     * <p/>
     * Plugins should check that hasOwner() returns true before calling this
     * plugin.
     *
     * @param name the new owner of the skull
     * @return true if the owner was successfully set
     */
    public boolean setOwner(String name) {
        if (name != null && name.length() > 16) {
            return false;
        } else {
            this.name = name;
            return true;
        }
    }

    /**
     * Gets the texture URL of the skull.
     *
     * @return the texture URL if the skull
     */
    public String getTexture() {
        return texture;
    }

    /**
     * Checks to see if the skull has a texture URL.
     *
     * @return true if the skull has a texture URL
     */
    public boolean hasTexture() {
        return texture != null;
    }

    /**
     * Sets the texture URL of the skull.
     * <p/>
     * Plugins should check that hasTexture() returns true before calling this
     * plugin.
     *
     * @param url the URL to the texture of the skull
     * @return true if the URL was successfully set
     */
    public boolean setTexture(String url) {
        if (hasOwner()) {
            this.texture = url;
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void applyTo(TagCompound tag) {
        if (hasOwner()) {
            TagCompound ownerTag = new TagCompound();


            ownerTag.put("Name", new TagString(getOwner()));

            if (hasTexture()) {
                TagCompound propertiesTag = new TagCompound();
                TagList textureList = new TagList();
                TagCompound textureTag = new TagCompound();
                JsonObject jsonObject = new JsonObject();
                JsonObject texturesObject = new JsonObject();
                JsonObject skinObject = new JsonObject();
                jsonObject.add("textures", texturesObject);
                texturesObject.add("SKIN", skinObject);
                skinObject.addProperty("url", getTexture());
                String base64 = BaseEncoding.base64Url().encode(new Gson().toJson(jsonObject).getBytes());
                textureTag.put("Value", new TagString(base64));
                textureList.addTag(textureTag);
                propertiesTag.put("textures", textureList);
                ownerTag.put("Properties", propertiesTag);
            }

            tag.put("SkullOwner", ownerTag);
        }
    }

    public SkullMeta clone() {
        SkullMeta meta = new SkullMeta();
        meta.name = this.name;
        meta.texture = this.texture;
        return meta;
    }
}
