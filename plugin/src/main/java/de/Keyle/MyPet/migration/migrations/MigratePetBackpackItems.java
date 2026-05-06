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

package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.PetDataMigration;
import de.Keyle.MyPet.migration.SqlMigrationContext;
import de.Keyle.MyPet.util.NbtUtil;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Converts legacy backpack item encodings in the {@code pets.skills} blob to the
 * current {@code PaperItem} (v4) encoding produced by
 * {@link ItemStack#serializeAsBytes()}.
 * <p>
 * Three formats can appear in the wild:
 * <ul>
 *   <li><b>Format 1 ({@code id})</b> — NMS NBT compound. Written by the pre-Oct-31 v4
 *       code path (and 3.x). Most existing servers are on this format.</li>
 *   <li><b>Format 2 ({@code BukkitItem})</b> — Base64 of {@code BukkitObjectOutputStream}.
 *       Written by v4 snapshot builds between Oct 31 and Dec 3, 2025. Rare.</li>
 *   <li><b>Format 3 ({@code PaperItem})</b> — Base64 of {@code ItemStack#serializeAsBytes()}.
 *       The current v4 format. No conversion needed.</li>
 * </ul>
 * <p>
 * Format 2 converts with full fidelity because Bukkit's {@code ConfigurationSerializable}
 * system round-trips {@code ItemStack} across MC versions. Format 1 is converted by
 * seeding the compound with a pre-1.20.5 {@code DataVersion} and feeding it to
 * {@link ItemStack#deserializeBytes(byte[])}, which routes through Paper's
 * {@code Bukkit.getUnsafe().deserializeItem(byte[])} and runs Mojang's DataFixerUpper
 * to translate the legacy {@code Count}/{@code tag} layout into the modern
 * {@code count}/{@code components} form (enchantments, bundle contents, damage, custom
 * names, etc. all migrate to proper data components). If that path fails for any item,
 * the migration falls back to a bare {@code ItemStack(material, count)} (losing
 * enchantments / custom names / damage).
 */
@Migration(
        version = "4.0.0",
        description = "Convert pet backpack items from legacy NMS-NBT and Bukkit formats to Paper's serializeAsBytes"
)
public class MigratePetBackpackItems implements PetDataMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");
    private static final String PAPER_ITEM = "PaperItem";
    private static final String BUKKIT_ITEM = "BukkitItem";
    private static final String LEGACY_ID = "id";

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        if (!(ctx instanceof SqlMigrationContextImpl impl)) {
            throw new MigrationException("SqlMigrationContext is not a SqlMigrationContextImpl; "
                    + "cannot reach underlying Connection for blob update.");
        }
        Connection connection = impl.getConnection();
        String petsTable = ctx.getTablePrefix() + "pets";

        // Step 1: read every pet's skills blob into memory. Typical installs have hundreds
        // to low thousands of pets — well within memory limits.
        Map<String, byte[]> rows = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, skills FROM " + petsTable)) {
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getBytes(2));
            }
        } catch (SQLException e) {
            throw new MigrationException("Failed to read pets for backpack migration", e);
        }

        Stats stats = new Stats();
        Map<String, byte[]> updates = new HashMap<>();

        for (Map.Entry<String, byte[]> row : rows.entrySet()) {
            String petUuid = row.getKey();
            byte[] skillsBytes = row.getValue();
            if (skillsBytes == null || skillsBytes.length == 0) {
                stats.petsWithoutSkills++;
                continue;
            }
            try {
                CompoundBinaryTag skills = NbtUtil.readCompressed(skillsBytes);
                CompoundBinaryTag converted = convertBackpack(skills, petUuid, stats);
                if (converted != skills) {
                    updates.put(petUuid, NbtUtil.writeCompressed(converted));
                    stats.petsUpdated++;
                } else {
                    stats.petsUntouched++;
                }
            } catch (Exception e) {
                stats.petsFailed++;
                LOG.warning("Failed to process skills blob for pet " + petUuid
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Step 2: apply all updates in a single transaction. Either every conversion lands
        // or none do — avoids leaving the pets table half-converted on a crash.
        if (!updates.isEmpty()) {
            boolean previousAutoCommit;
            try {
                previousAutoCommit = connection.getAutoCommit();
            } catch (SQLException e) {
                throw new MigrationException("Failed to read autoCommit", e);
            }
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE " + petsTable + " SET skills = ? WHERE uuid = ?")) {
                    for (Map.Entry<String, byte[]> update : updates.entrySet()) {
                        ps.setBytes(1, update.getValue());
                        ps.setString(2, update.getKey());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw new MigrationException("Failed to write converted backpack blobs — rolled back", e);
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // not worth failing the migration for
                }
            }
        }

        LOG.info("Backpack item migration complete.");
        LOG.info("  Pets updated: " + stats.petsUpdated + ".");
        LOG.info("  Pets untouched (already on PaperItem or no backpack): " + stats.petsUntouched + ".");
        LOG.info("  Pets without a skills blob: " + stats.petsWithoutSkills + ".");
        LOG.info("  Pets failed (blob unreadable): " + stats.petsFailed + ".");
        LOG.info("  Items converted from BukkitItem: " + stats.itemsFromBukkit + ".");
        LOG.info("  Items converted from NMS NBT (full fidelity): " + stats.itemsFromNmsFull + ".");
        LOG.info("  Items converted from NMS NBT (material+count only): " + stats.itemsFromNmsPartial + ".");
        LOG.info("  Items dropped (unreadable): " + stats.itemsDropped + ".");
    }

    private CompoundBinaryTag convertBackpack(CompoundBinaryTag skills, String petUuid, Stats stats) {
        BinaryTag backpackTag = skills.get("Backpack");
        if (!(backpackTag instanceof CompoundBinaryTag backpack)) {
            return skills;
        }
        BinaryTag itemsTag = backpack.get("Items");
        if (!(itemsTag instanceof ListBinaryTag items) || items.size() == 0) {
            return skills;
        }

        List<BinaryTag> newItems = new ArrayList<>(items.size());
        boolean anyChanged = false;
        for (int i = 0; i < items.size(); i++) {
            BinaryTag entry = items.get(i);
            if (!(entry instanceof CompoundBinaryTag itemCompound)) {
                anyChanged = true;
                stats.itemsDropped++;
                LOG.warning("Pet " + petUuid + " item index " + i
                        + ": unexpected non-compound list entry, dropping");
                continue;
            }
            if (itemCompound.keySet().contains(PAPER_ITEM)) {
                newItems.add(itemCompound);
                continue;
            }
            CompoundBinaryTag migrated = convertItem(itemCompound, petUuid, stats);
            if (migrated == null) {
                anyChanged = true;
                stats.itemsDropped++;
                continue;
            }
            newItems.add(migrated);
            anyChanged = true;
        }

        if (!anyChanged) {
            return skills;
        }

        ListBinaryTag newItemsList = ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, newItems);
        CompoundBinaryTag newBackpack = backpack.put("Items", newItemsList);
        return skills.put("Backpack", newBackpack);
    }

    /**
     * Convert a single legacy item compound to a {@code PaperItem} compound, or return
     * null if the item cannot be decoded at all.
     */
    private CompoundBinaryTag convertItem(CompoundBinaryTag itemCompound, String petUuid, Stats stats) {
        byte slot = extractSlot(itemCompound);

        if (itemCompound.keySet().contains(BUKKIT_ITEM)) {
            ItemStack item = decodeBukkitItem(itemCompound.getString(BUKKIT_ITEM));
            if (item == null) {
                LOG.warning("Pet " + petUuid + " slot " + slot
                        + ": BukkitItem decode failed; dropping");
                return null;
            }
            stats.itemsFromBukkit++;
            return buildPaperItemCompound(slot, item);
        }

        if (itemCompound.keySet().contains(LEGACY_ID)) {
            DecodedLegacy decoded = decodeLegacyNmsItem(itemCompound);
            if (decoded == null) {
                LOG.warning("Pet " + petUuid + " slot " + slot
                        + ": legacy NMS item could not be decoded (unknown material?); dropping");
                return null;
            }
            if (decoded.fullFidelity) {
                stats.itemsFromNmsFull++;
            } else {
                stats.itemsFromNmsPartial++;
            }
            return buildPaperItemCompound(slot, decoded.itemStack);
        }

        // No recognised encoding.
        LOG.warning("Pet " + petUuid + " slot " + slot
                + ": item compound has no PaperItem / BukkitItem / id key; dropping");
        return null;
    }

    private byte extractSlot(CompoundBinaryTag itemCompound) {
        BinaryTag slotTag = itemCompound.get("Slot");
        if (slotTag instanceof ByteBinaryTag b) {
            return b.value();
        }
        return 0;
    }

    private ItemStack decodeBukkitItem(String b64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(b64);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                Object read = in.readObject();
                return read instanceof ItemStack stack ? stack : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private DecodedLegacy decodeLegacyNmsItem(CompoundBinaryTag compound) {
        String id = compound.getString(LEGACY_ID);
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }

        int count = extractCount(compound);

        // Preferred path: gzip the legacy compound (with a 1.20.4 DataVersion seed) and
        // hand it to ItemStack.deserializeBytes, which under Paper runs the bytes through
        // Mojang's DataFixerUpper from the seeded version up to the running server's data
        // version. The fixer is what translates pre-1.20.5 "tag" NBT — Enchantments,
        // BundleContents, display.Name, damage, etc. — into modern data components.
        // ItemFactory.createItemStack(String) is the wrong tool: its parser only recognises
        // the modern "id[components]" syntax and silently ignores trailing "{tag}" text,
        // returning a bare-material stack with no exception.
        try {
            CompoundBinaryTag seeded = compound.putInt("DataVersion", LEGACY_NBT_DATA_VERSION);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryTagIO.writer().write(seeded, out, BinaryTagIO.Compression.GZIP);
            ItemStack item = ItemStack.deserializeBytes(out.toByteArray());
            if (item != null && !item.isEmpty()) {
                return new DecodedLegacy(item, true);
            }
        } catch (Throwable ignored) {
            // Fall through to material + count fallback.
        }

        // Fallback: material + count — loses enchantments / custom names / damage but
        // preserves item identity so the slot isn't entirely empty.
        Material mat = Material.matchMaterial(id);
        if (mat == null) {
            return null;
        }
        try {
            return new DecodedLegacy(new ItemStack(mat, Math.max(1, count)), false);
        } catch (Throwable e) {
            return null;
        }
    }

    // 1.20.4 — last data version before the 1.20.5 "tag → components" transition.
    // Seeding pre-1.20.5 data with this value forces the fixer to run that conversion.
    private static final int LEGACY_NBT_DATA_VERSION = 3700;

    private int extractCount(CompoundBinaryTag compound) {
        BinaryTag countTag = compound.get("Count");
        if (countTag == null) {
            countTag = compound.get("count");
        }
        if (countTag instanceof ByteBinaryTag b) {
            return b.value();
        }
        if (countTag instanceof IntBinaryTag i) {
            return i.value();
        }
        return 1;
    }

    private CompoundBinaryTag buildPaperItemCompound(byte slot, ItemStack item) {
        byte[] serialized = item.serializeAsBytes();
        String b64 = Base64.getEncoder().encodeToString(serialized);
        return CompoundBinaryTag.builder()
                .putByte("Slot", slot)
                .putString(PAPER_ITEM, b64)
                .build();
    }

    private record DecodedLegacy(ItemStack itemStack, boolean fullFidelity) {
    }

    private static final class Stats {
        int petsUpdated;
        int petsUntouched;
        int petsWithoutSkills;
        int petsFailed;
        int itemsFromBukkit;
        int itemsFromNmsFull;
        int itemsFromNmsPartial;
        int itemsDropped;
    }
}
