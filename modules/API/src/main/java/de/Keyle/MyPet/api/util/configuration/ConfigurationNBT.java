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

package de.Keyle.MyPet.api.util.configuration;

import de.Keyle.MyPet.MyPetApi;
import de.keyle.knbt.TagBase;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagStream;

import java.io.*;

public class ConfigurationNBT {
    private File NBTFile;
    private TagCompound nbtTagCompound;

    public ConfigurationNBT(File file) {
        NBTFile = file;
    }

    public TagCompound getNBTCompound() {
        if (nbtTagCompound == null) {
            clearConfig();
        }
        return nbtTagCompound;
    }

    public static boolean save(File file, TagCompound tag) {
        ConfigurationNBT config = new ConfigurationNBT(file);
        config.nbtTagCompound = tag;
        return config.save();
    }

    public boolean save() {
        try {
            OutputStream os = new FileOutputStream(NBTFile);
            TagStream.writeTag(nbtTagCompound, os, true);
            os.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean load() {
        if (!NBTFile.exists()) {
            return false;
        }
        try {
            InputStream is = new FileInputStream(NBTFile);
            TagBase tag = TagStream.readTag(is, true);
            if (tag != null) {
                nbtTagCompound = (TagCompound) tag;
                return true;
            }
            tag = TagStream.readTag(is, false);
            if (tag != null) {
                nbtTagCompound = (TagCompound) tag;
                return true;
            } else {
                MyPetApi.getLogger().warning("Could not parse/load " + NBTFile.getName());
                return false;
            }

        } catch (IOException e) {
            return false;
        }
    }

    public void clearConfig() {
        nbtTagCompound = new TagCompound();
    }
}