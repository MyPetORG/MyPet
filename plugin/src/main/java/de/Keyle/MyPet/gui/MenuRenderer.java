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

package de.Keyle.MyPet.gui;

import de.Keyle.MyPet.api.gui.*;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Renders one menu definition into a Bukkit {@link Inventory}. Sections are
 * processed in priority order: Fill, Border, PaginatedList, Storage, Slot, Custom.
 */
public final class MenuRenderer {

    private MenuRenderer() {}

    public static void renderAll(MenuInstanceImpl instance, Inventory inv) {
        MenuDefinition def = instance.definition();
        List<Map.Entry<String, Section>> entries = new ArrayList<>(def.sections().entrySet());
        entries.sort(Comparator.comparingInt(e -> priority(e.getValue())));
        for (var entry : entries) renderSection(entry.getValue(), instance, inv);
    }

    public static void renderSection(Section section, MenuInstanceImpl instance, Inventory inv) {
        PlaceholderCatalog catalog = new PlaceholderCatalog();
        TagAdder.add(catalog, instance, section.id(), -1);

        // Handler-controlled visibility (per Beacon's eligible-buff gating, etc.).
        if (section instanceof SlotSection) {
            if (!instance.<Object>typedHandler().isSlotVisible(instance.context(), section.id())) {
                return;
            }
        }

        // Page-button slots: optionally hide at boundary, otherwise inject pagination placeholders.
        if (section instanceof SlotSection slot) {
            PaginationInfo info = paginationInfoFor(instance, slot.id());
            if (info != null) {
                boolean atBoundary = info.isPrevious() && info.currentPage() == 0
                    || !info.isPrevious() && info.currentPage() >= info.totalPages() - 1;
                if (atBoundary && slot.hideAtBoundary()) {
                    // Skip rendering this slot; whatever lower-priority decorative
                    // section (border / fill) already painted in this slot remains visible.
                    // Do NOT setItem(null) here — that would erase the border item.
                    return;
                }
                int targetDisplayPage = info.isPrevious()
                    ? Math.max(1, info.currentPage())            // internal target = currentPage - 1; display = target + 1
                    : Math.min(info.totalPages(), info.currentPage() + 2);
                catalog.add(TagResolver.builder()
                    .resolver(Placeholder.unparsed("current_page", String.valueOf(info.currentPage() + 1)))
                    .resolver(Placeholder.unparsed("max_page", String.valueOf(info.totalPages())))
                    .resolver(Placeholder.unparsed("target_page", String.valueOf(targetDisplayPage)))
                    .build());
            }
        }

        RenderContext ctx = new RenderContext(instance.viewer(), instance, catalog);
        @SuppressWarnings({"rawtypes", "unchecked"})
        SectionRenderer renderer = (SectionRenderer) section.type().renderer();
        renderer.render(section, inv, ctx);
    }

    /**
     * Returns pagination state if {@code sectionId} is referenced by any
     * {@link PaginatedListSection} as one of its page-button section refs, otherwise null.
     */
    private static @Nullable PaginationInfo paginationInfoFor(MenuInstanceImpl instance, String sectionId) {
        for (Section s : instance.definition().sections().values()) {
            if (!(s instanceof PaginatedListSection plist)) continue;
            boolean isPrev = sectionId.equals(plist.previousPageSectionId());
            boolean isNext = sectionId.equals(plist.nextPageSectionId());
            if (!isPrev && !isNext) continue;

            int page = instance.pageIndex(plist.id());
            int totalItems = instance.<Object>typedHandler().templateItems(instance.context(), plist.id()).size();
            int cap = plist.slotCapacity();
            int totalPages = Math.max(1, (totalItems + cap - 1) / cap);
            return new PaginationInfo(page, totalPages, isPrev);
        }
        return null;
    }

    private record PaginationInfo(int currentPage, int totalPages, boolean isPrevious) {}

    static int priority(Section s) {
        return switch (s) {
            case FillSection f -> 0;
            case BorderSection b -> 1;
            case PaginatedListSection p -> 2;
            case StorageSection st -> 3;
            case ValueBarSection v -> 4;
            case SlotSection sl -> 5;
            case CustomSection c -> 6;
        };
    }

    /** Tiny helper to bridge `MenuHandler.placeholders` into the render-time catalog. */
    static final class TagAdder {
        static void add(PlaceholderCatalog catalog, MenuInstanceImpl inst, String sectionId, int itemIndex) {
            catalog.add(inst.<Object>typedHandler().placeholders(inst.context(), sectionId, itemIndex));
        }
    }
}
