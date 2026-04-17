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
import de.Keyle.MyPet.api.util.ErrorUtil;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class ConfigurationNBT {
    private File NBTFile;
    private CompoundBinaryTag nbtTagCompound;

    public ConfigurationNBT(File file) {
        NBTFile = file;
    }

    public static boolean save(File file, CompoundBinaryTag tag) {
        ConfigurationNBT config = new ConfigurationNBT(file);
        config.nbtTagCompound = tag;
        return config.save();
    }

    public CompoundBinaryTag getNBTCompound() {
        if (nbtTagCompound == null) {
            clearConfig();
        }
        return nbtTagCompound;
    }

    public boolean save() {
        try (OutputStream os = Files.newOutputStream(NBTFile.toPath())) {
            BinaryTagIO.writer().write(nbtTagCompound, os, BinaryTagIO.Compression.GZIP);
            return true;
        } catch (IOException e) {
            ErrorUtil.report(e);
            return false;
        }
    }

    public boolean load() {
        if (!NBTFile.exists()) {
            return false;
        }
        try (InputStream is = Files.newInputStream(NBTFile.toPath())) {
            // Try compressed first
            try {
                CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(is, BinaryTagIO.Compression.GZIP);
                nbtTagCompound = tag;
                return true;
            } catch (IOException ignored) {
                // Try uncompressed
            }
        } catch (IOException e) {
            return false;
        }

        // Retry with uncompressed
        try (InputStream is = Files.newInputStream(NBTFile.toPath())) {
            CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(is, BinaryTagIO.Compression.NONE);
            nbtTagCompound = tag;
            return true;
        } catch (IOException e) {
            MyPetApi.getLogger().warning("Could not parse/load " + NBTFile.getName());
            return false;
        }
    }

    public void clearConfig() {
        nbtTagCompound = CompoundBinaryTag.empty();
    }
}