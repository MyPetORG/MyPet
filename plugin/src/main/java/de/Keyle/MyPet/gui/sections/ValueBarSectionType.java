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
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.MenuRenderHelpers;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Set;

public final class ValueBarSectionType {

    public static final SectionType<ValueBarSection> INSTANCE = SectionType.register(
        "value-bar",
        ValueBarSection.class,
        new Renderer(),
        new Codec()
    );

    private ValueBarSectionType() {}

    // --- Renderer -------------------------------------------------------------

    private static final class Renderer implements SectionRenderer<ValueBarSection> {

        @Override
        public void render(ValueBarSection s, Inventory inv, RenderContext ctx) {
            MenuInstanceImpl inst = (MenuInstanceImpl) ctx.instance();
            int raw = inst.<Object>typedHandler().valueBarPosition(inst.context(), s.id());
            int position = Math.max(0, Math.min(s.width() - 1, raw));

            for (int i = 0; i < s.width(); i++) {
                int slot = s.row() * 9 + s.col() + i;
                ItemAppearance appearance = i <= position ? s.highItem() : s.lowItem();
                PlaceholderCatalog perCell = new PlaceholderCatalog();
                perCell.add(inst.<Object>typedHandler().placeholders(inst.context(), s.id(), i));
                perCell.add(TagResolver.resolver(
                    Placeholder.unparsed("percent", String.valueOf(s.percentAt(i)))));
                RenderContext cellCtx = new RenderContext(ctx.viewer(), inst, perCell);
                inv.setItem(slot, MenuRenderHelpers.toItemStack(appearance, cellCtx));
            }
        }

        @Override
        public ClickResult onClick(ValueBarSection s, ClickPayload payload, RenderContext ctx) {
            return ClickResult.DELEGATE_TO_HANDLER;
        }

        @Override
        public Set<Integer> ownedSlots(ValueBarSection s) {
            Set<Integer> set = new HashSet<>();
            for (int i = 0; i < s.width(); i++) {
                set.add(s.row() * 9 + s.col() + i);
            }
            return set;
        }
    }

    // --- Codec ----------------------------------------------------------------

    private static final class Codec implements JsonCodec<ValueBarSection> {

        @Override
        public ValueBarSection decode(String id, JsonObject raw, CodecContext ctx) {
            if (!raw.has("region")) {
                throw new ValidationException(ctx.menuId() + ".sections." + id + ".region is required");
            }
            JsonObject region = raw.getAsJsonObject("region");
            int col = JsonHelpers.requireInt(region, "col", ctx.menuId() + ".sections." + id + ".region");
            int row = JsonHelpers.requireInt(region, "row", ctx.menuId() + ".sections." + id + ".region");
            int width = JsonHelpers.requireInt(region, "width", ctx.menuId() + ".sections." + id + ".region");
            ctx.requireRegionInBounds(id + ".region", col, row, width, 1);

            if (!raw.has("low-item") || !raw.has("high-item")) {
                throw new ValidationException(ctx.menuId() + ".sections." + id
                    + ": low-item and high-item are required");
            }
            ItemAppearance lowItem = JsonHelpers.parseItem(raw.getAsJsonObject("low-item"),
                ctx.menuId() + ".sections." + id + ".low-item");
            ItemAppearance highItem = JsonHelpers.parseItem(raw.getAsJsonObject("high-item"),
                ctx.menuId() + ".sections." + id + ".high-item");

            SoundSpec click = JsonHelpers.parseSoundOrSilent(raw.get("sound-on-click"),
                ctx.menuId() + ".sections." + id + ".sound-on-click");

            return new ValueBarSection(id, INSTANCE, col, row, width, lowItem, highItem, click);
        }

        @Override
        public JsonObject encode(ValueBarSection section) {
            return new JsonObject();
        }
    }
}
