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
import de.Keyle.MyPet.gui.MenuRenderHelpers;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Set;

public final class FillSectionType {

    public static final SectionType<FillSection> INSTANCE = SectionType.register(
        "fill",
        FillSection.class,
        new Renderer(),
        new Codec()
    );

    private FillSectionType() {}

    private static final class Renderer implements SectionRenderer<FillSection> {

        @Override public boolean decorative() { return true; }

        @Override
        public void render(FillSection s, Inventory inv, RenderContext ctx) {
            int rows = ctx.instance().definition().rows();
            org.bukkit.inventory.ItemStack stack = MenuRenderHelpers.toItemStack(s.item(), ctx);
            FillSection.Region r = s.region();
            if (r == null) {
                for (int i = 0; i < rows * 9; i++) inv.setItem(i, stack);
            } else {
                for (int dr = 0; dr < r.height(); dr++) {
                    for (int dc = 0; dc < r.width(); dc++) {
                        inv.setItem((r.row() + dr) * 9 + (r.col() + dc), stack);
                    }
                }
            }
        }

        @Override
        public ClickResult onClick(FillSection s, ClickPayload payload, RenderContext ctx) {
            return ClickResult.NO_OP;
        }

        @Override
        public Set<Integer> ownedSlots(FillSection s) { return Set.of(); }
    }

    private static final class Codec implements JsonCodec<FillSection> {

        @Override
        public FillSection decode(String id, JsonObject raw, CodecContext ctx) {
            ItemAppearance item = JsonHelpers.parseItem(raw.getAsJsonObject("item"),
                ctx.menuId() + ".sections." + id + ".item");
            FillSection.Region region = null;
            if (raw.has("region") && raw.get("region").isJsonObject()) {
                JsonObject reg = raw.getAsJsonObject("region");
                int col = JsonHelpers.requireInt(reg, "col", ctx.menuId() + ".sections." + id + ".region");
                int row = JsonHelpers.requireInt(reg, "row", ctx.menuId() + ".sections." + id + ".region");
                int width = JsonHelpers.requireInt(reg, "width", ctx.menuId() + ".sections." + id + ".region");
                int height = JsonHelpers.requireInt(reg, "height", ctx.menuId() + ".sections." + id + ".region");
                ctx.requireRegionInBounds(id + ".region", col, row, width, height);
                region = new FillSection.Region(col, row, width, height);
            }
            return new FillSection(id, INSTANCE, region, item);
        }

        @Override
        public JsonObject encode(FillSection section) { return new JsonObject(); }
    }
}
