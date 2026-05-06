/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.migration.migrations.entitysnapshot;

import de.Keyle.MyPet.api.util.ErrorUtil;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayOutputStream;

/**
 * Decodes a legacy adventure-nbt {@link CompoundBinaryTag} into a Bukkit
 * {@link ItemStack} via Paper's vanilla codec
 * ({@code ItemStack.deserializeBytes()}). Used by the EntitySnapshot
 * migration's legacy readers; will be deleted at v5 alongside the readers.
 */
public final class LegacyNbtItemDecoder {

    private LegacyNbtItemDecoder() {
    }

    public static ItemStack decode(CompoundBinaryTag compound) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryTagIO.writer().write(compound, out, BinaryTagIO.Compression.GZIP);
            return ItemStack.deserializeBytes(out.toByteArray());
        } catch (Throwable e) {
            ErrorUtil.report(e);
            return ItemStack.empty();
        }
    }
}
