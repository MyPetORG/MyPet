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
