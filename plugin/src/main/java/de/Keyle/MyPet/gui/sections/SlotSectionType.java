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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Built-in `slot` section. One slot, either single-appearance ({@code item}) or
 * multi-state ({@code states} + {@code default-state}).
 */
public final class SlotSectionType {

    public static final SectionType<SlotSection> INSTANCE = SectionType.register(
        "slot",
        SlotSection.class,
        new Renderer(),
        new Codec()
    );

    private SlotSectionType() {}

    // --- Renderer -------------------------------------------------------------

    private static final class Renderer implements SectionRenderer<SlotSection> {

        @Override
        public void render(SlotSection s, Inventory inv, RenderContext ctx) {
            ItemAppearance appearance = currentAppearance(s, ctx);
            de.Keyle.MyPet.gui.MenuInstanceImpl inst = (de.Keyle.MyPet.gui.MenuInstanceImpl) ctx.instance();
            appearance = inst.<Object>typedHandler().customizeSlotItem(inst.context(), s.id(), appearance);
            int slot = s.row() * 9 + s.col();
            inv.setItem(slot, MenuRenderHelpers.toItemStack(appearance, ctx));
        }

        @Override
        public ClickResult onClick(SlotSection s, ClickPayload payload, RenderContext ctx) {
            return ClickResult.DELEGATE_TO_HANDLER;
        }

        @Override
        public Set<Integer> ownedSlots(SlotSection s) {
            return Set.of(s.row() * 9 + s.col());
        }

        private ItemAppearance currentAppearance(SlotSection s, RenderContext ctx) {
            if (s.item() != null) return s.item();
            String state = ctx.instance().getSlotState(s.id());
            ItemAppearance a = s.states().get(state);
            if (a == null) a = s.states().get(s.defaultState());
            return a;
        }
    }

    // --- Codec ----------------------------------------------------------------

    private static final class Codec implements JsonCodec<SlotSection> {

        @Override
        public SlotSection decode(String id, JsonObject raw, CodecContext ctx) {
            int[] pos = JsonHelpers.parsePosition(raw.getAsJsonObject("position"),
                ctx.menuId() + ".sections." + id + ".position", ctx.rows());

            ItemAppearance item = null;
            Map<String, ItemAppearance> states = null;
            String defaultState = null;
            boolean hasItem = raw.has("item");
            boolean hasStates = raw.has("states");
            if (hasItem == hasStates) {
                throw new ValidationException(ctx.menuId() + ".sections." + id
                    + ": exactly one of `item` or `states` must be present");
            }
            if (hasItem) {
                item = JsonHelpers.parseItem(raw.getAsJsonObject("item"),
                    ctx.menuId() + ".sections." + id + ".item");
            } else {
                JsonObject stObj = raw.getAsJsonObject("states");
                states = new LinkedHashMap<>();
                for (String stateName : stObj.keySet()) {
                    JsonObject entry = stObj.getAsJsonObject(stateName);
                    states.put(stateName, JsonHelpers.parseItem(entry.getAsJsonObject("item"),
                        ctx.menuId() + ".sections." + id + ".states." + stateName + ".item"));
                }
                defaultState = JsonHelpers.requireString(raw, "default-state",
                    ctx.menuId() + ".sections." + id);
                if (!states.containsKey(defaultState)) {
                    throw new ValidationException(ctx.menuId() + ".sections." + id
                        + ".default-state references unknown state: " + defaultState);
                }
            }

            SoundSpec click = JsonHelpers.parseSoundOrSilent(raw.get("sound-on-click"),
                ctx.menuId() + ".sections." + id + ".sound-on-click");

            boolean hideAtBoundary = JsonHelpers.optBoolean(raw, "hide-at-boundary", true);

            return new SlotSection(id, INSTANCE, pos[0], pos[1], item, states, defaultState, click, hideAtBoundary);
        }

        @Override
        public JsonObject encode(SlotSection section) {
            return new JsonObject();
        }
    }
}
