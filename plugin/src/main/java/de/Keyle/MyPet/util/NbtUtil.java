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

package de.Keyle.MyPet.util;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for NBT serialization to/from byte arrays.
 * Used by repository implementations for database storage.
 */
public final class NbtUtil {

    private NbtUtil() {
    }

    /**
     * Writes a CompoundBinaryTag to a GZIP-compressed byte array.
     */
    public static byte[] writeCompressed(CompoundBinaryTag compound) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(compound, baos, BinaryTagIO.Compression.GZIP);
        return baos.toByteArray();
    }

    /**
     * Reads a CompoundBinaryTag from a GZIP-compressed byte array.
     */
    public static CompoundBinaryTag readCompressed(byte[] bytes) throws IOException {
        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(bytes), BinaryTagIO.Compression.GZIP);
    }

    /**
     * Reads a CompoundBinaryTag from a GZIP-compressed InputStream.
     */
    public static CompoundBinaryTag readCompressed(InputStream inputStream) throws IOException {
        return BinaryTagIO.unlimitedReader().read(inputStream, BinaryTagIO.Compression.GZIP);
    }
}
