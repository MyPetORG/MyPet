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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Concrete {@link MenuInstance}. One per opened menu per viewer. Implements
 * {@link InventoryHolder} so the dispatcher can route inventory events back here.
 */
public final class MenuInstanceImpl implements MenuInstance, InventoryHolder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final MenuDefinition definition;
    private final Player viewer;
    private final MenuHandler<Object> handler;       // erased; service stores typed version
    private final Object context;
    private final Inventory inventory;

    private final Map<String, Integer> pageIndices = new HashMap<>();
    private final Map<String, String>  slotStates  = new HashMap<>();

    /** Set by GuiServiceImpl right after construction. Used to route ad-hoc actions back. */
    AtomicReference<MenuDispatcher> dispatcherRef = new AtomicReference<>();

    public MenuInstanceImpl(MenuDefinition definition, Player viewer,
                            MenuHandler<Object> handler, Object context) {
        this.definition = definition;
        this.viewer = viewer;
        this.handler = handler;
        this.context = context;

        // Dynamic title parts come in as handler-supplied placeholders (uncolored);
        // the JSON title carries the styling. Paper renders any <lang>/translatable
        // in the resulting title server-side via GlobalTranslator before sending.
        Component title = MINI.deserialize(definition.titleMiniMessage(), handler.titlePlaceholders(context));
        this.inventory = Bukkit.createInventory(this, definition.rows() * 9, title);

        // Initialize default slot states for multi-state slots.
        for (var entry : definition.sections().entrySet()) {
            Section s = entry.getValue();
            if (s instanceof SlotSection slot && slot.states() != null) {
                slotStates.put(slot.id(), slot.defaultState());
            }
        }
    }

    // --- InventoryHolder ------------------------------------------------------

    @Override public Inventory getInventory() { return inventory; }

    // --- MenuInstance ---------------------------------------------------------

    @Override public MenuDefinition definition() { return definition; }
    @Override public Player viewer() { return viewer; }

    @Override
    public void refreshSection(String sectionId) {
        Section s = definition.sections().get(sectionId);
        if (s == null) return;
        MenuRenderer.renderSection(s, this, inventory);
    }

    @Override
    public void close() {
        MenuDispatcher d = dispatcherRef.get();
        if (d != null) d.requestClose(this);
        else viewer.closeInventory();
    }

    @Override
    public boolean popBack() {
        MenuDispatcher d = dispatcherRef.get();
        return d != null && d.requestPopBack(this);
    }

    @Override
    public void playSound(SoundSpec spec) {
        MenuDispatcher d = dispatcherRef.get();
        if (d != null) d.playSoundForViewer(viewer, spec);
    }

    @Override
    public <S extends Section> S section(String id, Class<S> type) {
        Section s = definition.sections().get(id);
        if (s == null) throw new IllegalArgumentException("Unknown section: " + id);
        if (!type.isInstance(s)) {
            throw new IllegalArgumentException("Section '" + id + "' is " + s.getClass().getSimpleName()
                + ", not " + type.getSimpleName());
        }
        return type.cast(s);
    }

    @Override
    public void setSlotState(String slotSectionId, String stateName) {
        Section s = definition.sections().get(slotSectionId);
        if (!(s instanceof SlotSection slot) || slot.states() == null) {
            throw new IllegalArgumentException("Section '" + slotSectionId + "' is not a multi-state slot");
        }
        if (!slot.states().containsKey(stateName)) {
            throw new IllegalArgumentException("Section '" + slotSectionId + "' has no state '" + stateName + "'");
        }
        slotStates.put(slotSectionId, stateName);
        refreshSection(slotSectionId);
    }

    @Override
    public String getSlotState(String slotSectionId) {
        Section s = definition.sections().get(slotSectionId);
        if (!(s instanceof SlotSection slot) || slot.states() == null) return null;
        return slotStates.getOrDefault(slotSectionId, slot.defaultState());
    }

    // --- Public accessors used by dispatcher / renderer / handlers -----------

    public Object context() { return context; }

    @SuppressWarnings("unchecked")
    public <C> MenuHandler<C> typedHandler() { return (MenuHandler<C>) handler; }

    public int pageIndex(String sectionId) { return pageIndices.getOrDefault(sectionId, 0); }
    public void setPageIndex(String sectionId, int page) { pageIndices.put(sectionId, page); }

    Map<String, Integer> pageIndicesView() { return Map.copyOf(pageIndices); }
    Map<String, String>  slotStatesView()  { return Map.copyOf(slotStates); }

    void restoreFromSnapshot(MenuInstanceSnapshot snapshot) {
        pageIndices.clear();
        pageIndices.putAll(snapshot.pageIndices());
        slotStates.clear();
        slotStates.putAll(snapshot.slotStates());
    }

    public MenuInstanceSnapshot snapshot() {
        return new MenuInstanceSnapshot(pageIndicesView(), slotStatesView());
    }
}
