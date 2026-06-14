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

package de.Keyle.MyPet.gui.sections;

import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.gui.JsonHelpers;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Storage region: free-input slots. Initial contents come from the handler;
 * final contents flow back to the handler on close. The dispatcher special-cases
 * drag/click routing for storage slots so vanilla item-movement works natively.
 */
public final class StorageSectionType {

    public static final SectionType<StorageSection> INSTANCE = SectionType.register(
        "storage",
        StorageSection.class,
        new Renderer(),
        new Codec()
    );

    private StorageSectionType() {}

    private static final class Renderer implements SectionRenderer<StorageSection> {

        @Override
        public void render(StorageSection s, Inventory inv, RenderContext ctx) {
            // Contents populated by MenuInstanceImpl on first open via MenuHandler.storageContents.
            // Re-renders here are no-ops — slots already hold their current player-edited contents.
        }

        @Override
        public ClickResult onClick(StorageSection s, ClickPayload payload, RenderContext ctx) {
            return ClickResult.NO_OP;
        }

        @Override
        public void onClose(StorageSection s, CloseReason reason, RenderContext ctx) {
            // Snapshot + persist happens in MenuInstanceImpl which holds the Inventory.
        }

        @Override
        public Set<Integer> ownedSlots(StorageSection s) {
            Set<Integer> set = new HashSet<>();
            for (int dr = 0; dr < s.height(); dr++) {
                for (int dc = 0; dc < s.width(); dc++) {
                    set.add((s.row() + dr) * 9 + (s.col() + dc));
                }
            }
            return set;
        }
    }

    private static final class Codec implements JsonCodec<StorageSection> {

        @Override
        public StorageSection decode(String id, JsonObject raw, CodecContext ctx) {
            JsonObject region = raw.getAsJsonObject("region");
            if (region == null) throw new ValidationException(
                ctx.menuId() + ".sections." + id + ".region is required");
            int col = JsonHelpers.requireInt(region, "col", ctx.menuId() + ".sections." + id + ".region");
            int row = JsonHelpers.requireInt(region, "row", ctx.menuId() + ".sections." + id + ".region");
            int width = JsonHelpers.requireInt(region, "width", ctx.menuId() + ".sections." + id + ".region");
            int height = JsonHelpers.requireInt(region, "height", ctx.menuId() + ".sections." + id + ".region");
            ctx.requireRegionInBounds(id + ".region", col, row, width, height);

            String storageId = JsonHelpers.requireString(raw, "storage-id",
                ctx.menuId() + ".sections." + id);
            return new StorageSection(id, INSTANCE, col, row, width, height, storageId);
        }

        @Override
        public JsonObject encode(StorageSection section) { return new JsonObject(); }
    }
}
