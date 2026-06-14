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

public final class BorderSectionType {

    public static final SectionType<BorderSection> INSTANCE = SectionType.register(
        "border",
        BorderSection.class,
        new Renderer(),
        new Codec()
    );

    private BorderSectionType() {}

    private static final class Renderer implements SectionRenderer<BorderSection> {

        @Override public boolean decorative() { return true; }

        @Override
        public void render(BorderSection s, Inventory inv, RenderContext ctx) {
            int rows = ctx.instance().definition().rows();
            int t = s.thickness();
            org.bukkit.inventory.ItemStack stack = MenuRenderHelpers.toItemStack(s.item(), ctx);
            for (int slot : ownedSlots(s, rows)) {
                inv.setItem(slot, stack);
            }
        }

        @Override
        public ClickResult onClick(BorderSection s, ClickPayload payload, RenderContext ctx) {
            return ClickResult.NO_OP;
        }

        @Override
        public Set<Integer> ownedSlots(BorderSection s) {
            return Set.of();
        }

        private Set<Integer> ownedSlots(BorderSection s, int rows) {
            Set<Integer> set = new HashSet<>();
            int t = Math.min(s.thickness(), Math.min(rows, 9) / 2);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < 9; c++) {
                    if (r < t || r >= rows - t || c < t || c >= 9 - t) {
                        set.add(r * 9 + c);
                    }
                }
            }
            return set;
        }
    }

    private static final class Codec implements JsonCodec<BorderSection> {

        @Override
        public BorderSection decode(String id, JsonObject raw, CodecContext ctx) {
            int t = raw.has("thickness") ? raw.get("thickness").getAsInt() : 1;
            if (t < 1) throw new ValidationException(
                ctx.menuId() + ".sections." + id + ".thickness must be >= 1");
            ItemAppearance item = JsonHelpers.parseItem(raw.getAsJsonObject("item"),
                ctx.menuId() + ".sections." + id + ".item");
            return new BorderSection(id, INSTANCE, t, item);
        }

        @Override
        public JsonObject encode(BorderSection section) { return new JsonObject(); }
    }
}
