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
import de.Keyle.MyPet.gui.context.BackpackContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The single Bukkit listener for every GUI event. Routes by {@link MenuInstanceImpl}
 * identity (we placed it as the {@code InventoryHolder} when the inventory was created).
 * Owns the per-viewer navigation stack.
 */
public final class MenuDispatcher implements Listener {

    private final Plugin plugin;
    private final Map<UUID, MenuInstanceImpl> visibleByViewer = new HashMap<>();
    private final Map<UUID, Deque<StackEntry>> stackByViewer = new HashMap<>();

    /** Set true while we're mid-navigation so we don't treat the synthetic OPEN_NEW as user close. */
    private final Set<UUID> mutating = new HashSet<>();

    public MenuDispatcher(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- Public API used by GuiServiceImpl ------------------------------------

    /** Open a menu for the viewer. Pushes the previous menu onto the stack if any. */
    public void open(MenuInstanceImpl newInst) {
        Player viewer = newInst.viewer();
        UUID id = viewer.getUniqueId();
        MenuInstanceImpl current = visibleByViewer.get(id);

        if (current != null) {
            stackByViewer.computeIfAbsent(id, k -> new ArrayDeque<>())
                         .push(new StackEntry(current.definition().menuId(),
                                              current.typedHandler(),
                                              current.context(),
                                              current.snapshot()));
            mutating.add(id);
            try {
                current.typedHandler().onClose(current, CloseReason.NAVIGATED_AWAY);
            } finally {
                mutating.remove(id);
            }
        }

        visibleByViewer.put(id, newInst);
        newInst.dispatcherRef.set(this);

        MenuRenderer.renderAll(newInst, newInst.getInventory());
        populateStorageContents(newInst);

        mutating.add(id);
        try {
            viewer.openInventory(newInst.getInventory());
        } finally {
            mutating.remove(id);
        }

        newInst.typedHandler().onOpen(newInst, newInst.context());
        SoundDispatch.play(viewer, newInst.definition().soundOnOpen());
    }

    public void refresh(Player viewer) {
        MenuInstanceImpl inst = visibleByViewer.get(viewer.getUniqueId());
        if (inst != null) MenuRenderer.renderAll(inst, inst.getInventory());
    }

    public void requestClose(MenuInstanceImpl inst) {
        if (visibleByViewer.get(inst.viewer().getUniqueId()) != inst) return;
        mutating.add(inst.viewer().getUniqueId());
        try {
            extractStorageAndPersist(inst);
            inst.typedHandler().onClose(inst, CloseReason.PLUGIN_CLOSED);
            stackByViewer.remove(inst.viewer().getUniqueId());
            visibleByViewer.remove(inst.viewer().getUniqueId());
            SoundDispatch.play(inst.viewer(), inst.definition().soundOnClose());
            inst.viewer().closeInventory();
        } finally {
            mutating.remove(inst.viewer().getUniqueId());
        }
    }

    public boolean requestPopBack(MenuInstanceImpl inst) {
        if (!inst.definition().escSupportsBack()) return false;
        Deque<StackEntry> stack = stackByViewer.get(inst.viewer().getUniqueId());
        if (stack == null || stack.isEmpty()) return false;
        popBackImpl(inst, stack);
        return true;
    }

    public void playSoundForViewer(Player viewer, SoundSpec spec) {
        SoundDispatch.play(viewer, spec);
    }

    // --- Bukkit events --------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        // event.getInventory() returns the top inventory directly. Going through
        // event.getView() would call InventoryView.getTopInventory(), which fails
        // with IncompatibleClassChangeError on Paper 1.20.x where InventoryView is
        // still a class but our compiled bytecode emits invokeinterface (1.21+).
        Inventory top = event.getInventory();
        if (!(top.getHolder(false) instanceof MenuInstanceImpl inst)) return;
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        int rawSlot = event.getRawSlot();
        int slotsInTop = inst.getInventory().getSize();

        // Click in player's own inventory: allow free interaction so they can
        // rearrange their items. Shift-click into a menu with no storage region
        // would dump items onto buttons — block that specific case.
        if (rawSlot >= slotsInTop) {
            if (event.isShiftClick() && !hasStorageSection(inst)) {
                event.setCancelled(true);
                viewer.updateInventory();
            }
            return;
        }

        Section maybeStorage = findStorageSectionForSlot(inst, rawSlot, slotsInTop);
        if (maybeStorage != null) {
            return;
        }

        // A click outside the window (rawSlot == -999) is vanilla's "drop the
        // cursor item" gesture. Let it through instead of swallowing it in the
        // catch-all cancel below — storage is persisted as a snapshot of the
        // real slots on close, so a dropped cursor item (which never occupied a
        // storage slot) can't dupe or be lost.
        if (rawSlot == -999) {
            return;
        }

        event.setCancelled(true);
        // Force the client to re-render after cancellation. Creative-mode clicks
        // (especially middle-click clone and number-key swap) and a few other
        // ClickTypes optimistically apply on the client; without an explicit
        // resync, the client retains the predicted state even though we cancelled.
        viewer.updateInventory();

        // Bukkit fires a synthetic ClickType.DOUBLE_CLICK after the two real clicks
        // that make up a double-click (vanilla uses it to collect matching items into
        // the cursor). Routing that event would re-run our handler logic a third
        // time per double-click — triple sounds, triple state flips. Storage regions
        // return before reaching this point so vanilla collect-into-cursor still works
        // there as expected.
        if (event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) return;

        // Decorative sections (border / fill) paint over many slots but yield clicks
        // to any non-decorative section that also claims them. Without this, clicks
        // on a page-button slot in the border perimeter would be consumed by the
        // border's NO_OP handler before reaching the slot section.
        for (var entry : inst.definition().sections().entrySet()) {
            Section s = entry.getValue();
            @SuppressWarnings({"rawtypes", "unchecked"})
            SectionRenderer renderer = (SectionRenderer) s.type().renderer();
            if (renderer.decorative()) continue;
            // Skip slots the handler has hidden — clicks on them should be no-ops.
            if (s instanceof SlotSection
                && !inst.<Object>typedHandler().isSlotVisible(inst.context(), s.id())) {
                continue;
            }
            if (sectionOwnsSlot(s, rawSlot, inst.definition().rows())) {
                routeClick(inst, viewer, s, rawSlot, event);
                return;
            }
        }
        // Fell through: slot is only owned by decorative sections, so the click
        // remains cancelled (already done above) and no handler runs.
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getInventory();
        if (!(top.getHolder(false) instanceof MenuInstanceImpl inst)) return;
        int slotsInTop = inst.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot >= slotsInTop) continue;
            if (findStorageSectionForSlot(inst, slot, slotsInTop) == null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();
        if (!(top.getHolder(false) instanceof MenuInstanceImpl inst)) return;
        UUID id = inst.viewer().getUniqueId();
        if (mutating.contains(id)) return;

        InventoryCloseEvent.Reason reason = event.getReason();
        if (reason == InventoryCloseEvent.Reason.PLAYER) {
            if (inst.definition().escSupportsBack()) {
                Deque<StackEntry> stack = stackByViewer.get(id);
                if (stack != null && !stack.isEmpty()) {
                    popBackImpl(inst, stack);
                    return;
                }
            }
            playerCloseImpl(inst, CloseReason.PLAYER_CLOSED);
        } else if (reason == InventoryCloseEvent.Reason.DISCONNECT) {
            playerCloseImpl(inst, CloseReason.DISCONNECT);
        } else if (reason == InventoryCloseEvent.Reason.PLUGIN) {
            playerCloseImpl(inst, CloseReason.PLUGIN_CLOSED);
        } else {
            playerCloseImpl(inst, CloseReason.PLAYER_CLOSED);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MenuInstanceImpl inst = visibleByViewer.remove(event.getPlayer().getUniqueId());
        stackByViewer.remove(event.getPlayer().getUniqueId());
        if (inst != null) {
            extractStorageAndPersist(inst);
            inst.typedHandler().onClose(inst, CloseReason.DISCONNECT);
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        for (var entry : new HashMap<>(visibleByViewer).entrySet()) {
            MenuInstanceImpl inst = entry.getValue();
            extractStorageAndPersist(inst);
            inst.typedHandler().onClose(inst, CloseReason.PLUGIN_DISABLE);
            inst.viewer().closeInventory();
        }
        visibleByViewer.clear();
        stackByViewer.clear();
    }

    // --- Helpers --------------------------------------------------------------

    private void routeClick(MenuInstanceImpl inst, Player viewer, Section s, int slot,
                            InventoryClickEvent event) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        SectionRenderer renderer = (SectionRenderer) s.type().renderer();

        int itemIndex = -1;
        if (s instanceof PaginatedListSection plist) {
            int localRow = (slot / 9) - plist.row();
            int localCol = (slot % 9) - plist.col();
            int onPage = localRow * plist.width() + localCol;
            int page = inst.pageIndex(plist.id());
            itemIndex = page * plist.slotCapacity() + onPage;

            // Empty slots inside the paginated region: click is a silent no-op
            // (no template click sound, no delegation to the handler).
            int totalItems = inst.<Object>typedHandler().templateItems(inst.context(), plist.id()).size();
            if (itemIndex >= totalItems) return;
        } else if (s instanceof ValueBarSection vb) {
            itemIndex = (slot % 9) - vb.col();
        }

        ClickPayload payload = new ClickPayload(slot, itemIndex, event.getClick(),
            event.isShiftClick(), event.getCursor());

        PlaceholderCatalog catalog = new PlaceholderCatalog();
        catalog.add(inst.<Object>typedHandler().placeholders(inst.context(), s.id(), itemIndex));
        RenderContext rctx = new RenderContext(viewer, inst, catalog);

        SectionRenderer.ClickResult result = renderer.onClick(s, payload, rctx);

        SoundSpec clickSound = clickSoundFor(s);
        SoundDispatch.play(viewer, clickSound);

        switch (result) {
            case NO_OP -> {}
            case REFRESH_SECTION -> inst.refreshSection(s.id());
            case CLOSE -> requestClose(inst);
            case DELEGATE_TO_HANDLER -> {
                Section paginating = pageButtonOwner(inst, s.id());
                if (paginating instanceof PaginatedListSection plist) {
                    int totalItems = inst.<Object>typedHandler().templateItems(inst.context(), plist.id()).size();
                    int cap = plist.slotCapacity();
                    int totalPages = Math.max(1, (totalItems + cap - 1) / cap);
                    int curr = inst.pageIndex(plist.id());
                    int next = s.id().equals(plist.previousPageSectionId())
                        ? Math.max(0, curr - 1)
                        : Math.min(totalPages - 1, curr + 1);
                    if (next != curr) {
                        inst.setPageIndex(plist.id(), next);
                        // Full re-render so decorative sections repaint correctly under
                        // hidden page-buttons, and previously-hidden buttons re-appear.
                        MenuRenderer.renderAll(inst, inst.getInventory());
                        SoundDispatch.play(viewer, plist.soundOnPageChange());
                    }
                    return;
                }
                inst.<Object>typedHandler().onClick(inst, s.id(), payload);
            }
        }
    }

    private static SoundSpec clickSoundFor(Section s) {
        return switch (s) {
            case SlotSection sl -> sl.soundOnClick();
            case PaginatedListSection p -> p.soundOnTemplateClick();
            case ValueBarSection v -> v.soundOnClick();
            default -> SoundSpec.Silent.INSTANCE;
        };
    }

    private static @Nullable Section pageButtonOwner(MenuInstanceImpl inst, String sectionId) {
        for (var entry : inst.definition().sections().values().stream().toList()) {
            if (entry instanceof PaginatedListSection p
                && (sectionId.equals(p.previousPageSectionId()) || sectionId.equals(p.nextPageSectionId()))) {
                return p;
            }
        }
        return null;
    }

    private static @Nullable Section findStorageSectionForSlot(MenuInstanceImpl inst, int slot, int slotsInTop) {
        if (slot >= slotsInTop) return null;
        for (Section s : inst.definition().sections().values()) {
            if (s instanceof StorageSection st && sectionOwnsSlot(st, slot, inst.definition().rows())) {
                return st;
            }
        }
        return null;
    }

    private static boolean hasStorageSection(MenuInstanceImpl inst) {
        for (Section s : inst.definition().sections().values()) {
            if (s instanceof StorageSection) return true;
        }
        return false;
    }

    private static boolean sectionOwnsSlot(Section s, int slot, int rows) {
        return switch (s) {
            case SlotSection sl -> slot == sl.row() * 9 + sl.col();
            case PaginatedListSection p -> slotInRegion(slot, p.col(), p.row(), p.width(), p.height());
            case StorageSection st -> slotInRegion(slot, st.col(), st.row(), st.width(), st.height());
            case ValueBarSection v -> slotInRegion(slot, v.col(), v.row(), v.width(), 1);
            case BorderSection b -> slotInBorder(slot, rows, b.thickness());
            case FillSection f -> f.region() == null
                ? slot < rows * 9
                : slotInRegion(slot, f.region().col(), f.region().row(), f.region().width(), f.region().height());
            case CustomSection c -> false;
        };
    }

    private static boolean slotInRegion(int slot, int col, int row, int w, int h) {
        int sc = slot % 9, sr = slot / 9;
        return sc >= col && sc < col + w && sr >= row && sr < row + h;
    }

    private static boolean slotInBorder(int slot, int rows, int t) {
        int sc = slot % 9, sr = slot / 9;
        if (sr < t || sr >= rows - t) return true;
        return sc < t || sc >= 9 - t;
    }

    private void playerCloseImpl(MenuInstanceImpl inst, CloseReason reason) {
        UUID id = inst.viewer().getUniqueId();
        extractStorageAndPersist(inst);
        inst.typedHandler().onClose(inst, reason);
        stackByViewer.remove(id);
        visibleByViewer.remove(id);
        SoundDispatch.play(inst.viewer(), inst.definition().soundOnClose());
    }

    private void popBackImpl(MenuInstanceImpl inst, Deque<StackEntry> stack) {
        UUID id = inst.viewer().getUniqueId();
        mutating.add(id);
        try {
            extractStorageAndPersist(inst);
            inst.typedHandler().onClose(inst, CloseReason.POPPED_BACK);
            SoundDispatch.play(inst.viewer(), inst.definition().effectiveSoundOnBack());

            StackEntry parent = stack.pop();
            visibleByViewer.remove(id);
            // Defer the reopen by one tick: Bukkit doesn't reliably honor an
            // openInventory(...) call made from inside an InventoryCloseEvent
            // handler — the view stays half-synced and the new menu's holder
            // isn't recognized when later clicks arrive. Running the reopen on
            // the next tick lets Bukkit finish the close transition first.
            Player viewer = inst.viewer();
            Bukkit.getScheduler().runTask(plugin, () -> reopenCallback.accept(parent, viewer));
        } finally {
            mutating.remove(id);
        }
    }

    /** Wired by GuiServiceImpl. Called with the parent stack entry AND the original viewer. */
    public java.util.function.BiConsumer<StackEntry, Player> reopenCallback = (e, v) -> {};

    private void populateStorageContents(MenuInstanceImpl inst) {
        for (Section s : inst.definition().sections().values()) {
            if (s instanceof StorageSection st) {
                ItemStack[] contents = inst.<Object>typedHandler().storageContents(inst.context(), st.id());
                int from = 0;
                for (int dr = 0; dr < st.height(); dr++) {
                    for (int dc = 0; dc < st.width(); dc++) {
                        int slot = (st.row() + dr) * 9 + (st.col() + dc);
                        inst.getInventory().setItem(slot, from < contents.length ? contents[from] : null);
                        from++;
                    }
                }
            }
        }
    }

    /**
     * Inserts {@code item} into the live storage section of the menu the viewer
     * currently has open, so the addition is visible immediately and survives the
     * snapshot-on-close in {@link #extractStorageAndPersist}. Stacking matches
     * vanilla (top up existing stacks, then fill empties).
     *
     * @return the amount that did not fit, or empty when the viewer has no open
     *         menu with a storage section (caller should fall back to its own store)
     */
    public OptionalInt addToOpenStorage(Player viewer, ItemStack item) {
        if (item == null || item.getType().isAir()) return OptionalInt.of(0);
        MenuInstanceImpl inst = visibleByViewer.get(viewer.getUniqueId());
        if (inst == null) return OptionalInt.empty();

        StorageSection target = null;
        for (Section s : inst.definition().sections().values()) {
            if (s instanceof StorageSection st) { target = st; break; }
        }
        if (target == null) return OptionalInt.empty();

        // Insert via a scratch inventory so Bukkit handles stacking, then write the
        // result back into just the section's slots of the live menu inventory.
        int cap = target.slotCapacity();
        int scratchSize = ((cap + 8) / 9) * 9;
        Inventory live = inst.getInventory();
        Inventory scratch = Bukkit.createInventory(null, scratchSize);
        int[] slots = new int[cap];
        int idx = 0;
        for (int dr = 0; dr < target.height(); dr++) {
            for (int dc = 0; dc < target.width(); dc++) {
                int slot = (target.row() + dr) * 9 + (target.col() + dc);
                slots[idx] = slot;
                scratch.setItem(idx, live.getItem(slot));
                idx++;
            }
        }

        Map<Integer, ItemStack> leftover = scratch.addItem(item.clone());

        for (int i = 0; i < cap; i++) {
            live.setItem(slots[i], scratch.getItem(i));
        }

        int remaining = 0;
        for (ItemStack rem : leftover.values()) {
            if (rem != null) remaining += rem.getAmount();
        }
        // Padding slots (when capacity isn't a multiple of 9) aren't part of the
        // section, so anything Bukkit placed there didn't actually fit.
        for (int i = cap; i < scratchSize; i++) {
            ItemStack pad = scratch.getItem(i);
            if (pad != null) remaining += pad.getAmount();
        }
        return OptionalInt.of(remaining);
    }

    private void extractStorageAndPersist(MenuInstanceImpl inst) {
        for (Section s : inst.definition().sections().values()) {
            if (s instanceof StorageSection st) {
                int cap = st.slotCapacity();
                ItemStack[] out = new ItemStack[cap];
                int idx = 0;
                for (int dr = 0; dr < st.height(); dr++) {
                    for (int dc = 0; dc < st.width(); dc++) {
                        int slot = (st.row() + dr) * 9 + (st.col() + dc);
                        out[idx++] = inst.getInventory().getItem(slot);
                    }
                }
                inst.<Object>typedHandler().persistStorage(inst.context(), st.id(), out);
            }
        }
    }

    /** Snapshot entry recorded when a menu is pushed onto a viewer's stack. */
    public record StackEntry(String menuId, MenuHandler<Object> handler, Object context, MenuInstanceSnapshot snapshot) {}
}
