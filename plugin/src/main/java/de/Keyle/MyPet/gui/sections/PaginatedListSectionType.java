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
import java.util.List;
import java.util.Set;

public final class PaginatedListSectionType {

    public static final SectionType<PaginatedListSection> INSTANCE = SectionType.register(
        "paginated-list",
        PaginatedListSection.class,
        new Renderer(),
        new Codec()
    );

    private PaginatedListSectionType() {}

    // --- Renderer -------------------------------------------------------------

    private static final class Renderer implements SectionRenderer<PaginatedListSection> {

        @Override
        public void render(PaginatedListSection s, Inventory inv, RenderContext ctx) {
            de.Keyle.MyPet.gui.MenuInstanceImpl inst = (de.Keyle.MyPet.gui.MenuInstanceImpl) ctx.instance();
            int page = inst.pageIndex(s.id());
            List<?> items = inst.<Object>typedHandler().templateItems(inst.context(), s.id());

            int cap = s.slotCapacity();
            int from = page * cap;

            for (int i = 0; i < cap; i++) {
                int slot = (s.row() + i / s.width()) * 9 + (s.col() + i % s.width());
                int idx = from + i;
                if (idx < items.size()) {
                    PlaceholderCatalog perItem = new PlaceholderCatalog();
                    perItem.add(inst.<Object>typedHandler().placeholders(inst.context(), s.id(), idx));
                    RenderContext perItemCtx = new RenderContext(ctx.viewer(), inst, perItem);
                    ItemAppearance customized = inst.<Object>typedHandler()
                        .customizeTemplateItem(inst.context(), s.id(), idx, s.template());
                    inv.setItem(slot, MenuRenderHelpers.toItemStack(customized, perItemCtx));
                } else {
                    inv.setItem(slot, null);
                }
            }
        }

        @Override
        public ClickResult onClick(PaginatedListSection s, ClickPayload payload, RenderContext ctx) {
            return ClickResult.DELEGATE_TO_HANDLER;
        }

        @Override
        public Set<Integer> ownedSlots(PaginatedListSection s) {
            Set<Integer> set = new HashSet<>();
            for (int dr = 0; dr < s.height(); dr++) {
                for (int dc = 0; dc < s.width(); dc++) {
                    set.add((s.row() + dr) * 9 + (s.col() + dc));
                }
            }
            return set;
        }
    }

    // --- Codec ----------------------------------------------------------------

    private static final class Codec implements JsonCodec<PaginatedListSection> {

        @Override
        public PaginatedListSection decode(String id, JsonObject raw, CodecContext ctx) {
            if (!raw.has("region")) {
                throw new ValidationException(ctx.menuId() + ".sections." + id + ".region is required");
            }
            JsonObject region = raw.getAsJsonObject("region");
            int col = JsonHelpers.requireInt(region, "col", ctx.menuId() + ".sections." + id + ".region");
            int row = JsonHelpers.requireInt(region, "row", ctx.menuId() + ".sections." + id + ".region");
            int width = JsonHelpers.requireInt(region, "width", ctx.menuId() + ".sections." + id + ".region");
            int height = JsonHelpers.requireInt(region, "height", ctx.menuId() + ".sections." + id + ".region");
            ctx.requireRegionInBounds(id + ".region", col, row, width, height);

            ItemAppearance template = JsonHelpers.parseItem(raw.getAsJsonObject("template"),
                ctx.menuId() + ".sections." + id + ".template");

            // Page-button refs are optional. Either both are present (paginated) or
            // both are absent (the list silently truncates overflow).
            String prevId = raw.has("previous-page-section")
                ? raw.get("previous-page-section").getAsString() : null;
            String nextId = raw.has("next-page-section")
                ? raw.get("next-page-section").getAsString() : null;
            if ((prevId == null) != (nextId == null)) {
                throw new ValidationException(ctx.menuId() + ".sections." + id
                    + ": previous-page-section and next-page-section must both be set or both be absent");
            }
            if (prevId != null) ctx.requireSibling(id + ".previous-page-section", prevId);
            if (nextId != null) ctx.requireSibling(id + ".next-page-section", nextId);

            SoundSpec pageChange = JsonHelpers.parseSoundOrSilent(raw.get("sound-on-page-change"),
                ctx.menuId() + ".sections." + id + ".sound-on-page-change");
            SoundSpec templateClick = JsonHelpers.parseSoundOrSilent(raw.get("sound-on-template-click"),
                ctx.menuId() + ".sections." + id + ".sound-on-template-click");

            return new PaginatedListSection(id, INSTANCE, col, row, width, height,
                template, prevId, nextId, pageChange, templateClick);
        }

        @Override
        public JsonObject encode(PaginatedListSection section) {
            return new JsonObject();
        }
    }
}
