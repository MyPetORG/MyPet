/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.gui;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.locale.Locale;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A chest-based GUI menu backed by Bukkit inventories. Supports optional
 * pagination (config-driven row count, auto-paging, prev/next buttons)
 * and click handling via {@link OptionClickEventHandler}.
 * <p>
 * Lifecycle: construct → {@link #setOption}/{@link #addOption} →
 * {@link #open(HumanEntity)} → user clicks → handler fires →
 * auto-close/destroy (or keep open via event flags). Call
 * {@link #destroy()} explicitly if the menu should close without a click.
 * <p>
 * Registers itself as a Bukkit listener on construction and
 * unregisters on {@link #destroy()}.
 */
public class IconMenu implements Listener {

    private final Plugin plugin;
    protected Map<Integer, IconMenuItem> options = new HashMap<>(54);
    private IconMenuInventory inventory;
    @Setter
    @Getter
    private Component title;
    private @Nullable String paginationBasePath;
    private @Nullable Integer pageSizeInSlots;
    private int currentPageIndex;
    private OptionClickEventHandler handler;
    private int maximumOptionPosition;
    private int nextVacantOptionPosition;

    /**
     * Creates a new menu and registers its event listeners.
     *
     * @param title   the inventory title shown to the player
     * @param handler callback invoked when a player clicks a slot
     * @param plugin  owning plugin (for event registration)
     */
    public IconMenu(Component title, OptionClickEventHandler handler, Plugin plugin) {
        this.title = title;
        this.handler = handler;
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Returns the inventory size in slots. When pagination is active,
     * returns the fixed page size; otherwise auto-sizes to fit all
     * options (rounded up to the next multiple of 9, clamped 9–54).
     */
    public int getSize() {
        if (pageSizeInSlots != null)
            return pageSizeInSlots;

        int size = maximumOptionPosition + 1;
        int roundedSize = (int) (Math.ceil(size / 9.) * 9);
        // Clamp to Bukkit's valid inventory size range: 9-54 slots (1-6 rows)
        return Math.max(9, Math.min(54, roundedSize));
    }

    /**
     * Enables pagination by reading row count from the plugin's config
     * at {@code MyPet.Pagination.<identifier>.TotalRows}. The last row
     * is reserved for prev/next navigation buttons.
     */
    public IconMenu setPaginationIdentifier(String identifier) {
        this.paginationBasePath = "MyPet.Pagination." + identifier;

        int totalRows = plugin.getConfig().getInt(paginationBasePath + ".TotalRows", -1);

        if (totalRows > 0) {
            if (totalRows > 6)
                totalRows = 6;
            this.pageSizeInSlots = totalRows * 9;
        }
        return this;
    }

    private void advanceNextVacantOptionPosition() {
        do {
            ++nextVacantOptionPosition;
        } while (options.containsKey(nextVacantOptionPosition));
    }

    /** Places an item at an explicit slot position. */
    public void setOption(int position, IconMenuItem icon) {
        if (position < 0)
            return;

        if (position > maximumOptionPosition)
            maximumOptionPosition = position;

        if (position == nextVacantOptionPosition)
            advanceNextVacantOptionPosition();

        options.put(position, icon);
    }

    /**
     * Appends an item at the next vacant slot and returns its position.
     */
    public int addOption(IconMenuItem icon) {
        int position = nextVacantOptionPosition;

        options.put(position, icon);
        advanceNextVacantOptionPosition();

        return position;
    }

    private int getNumberOfPages() {
        if (pageSizeInSlots == null)
            return 1;

        int pageCapacity = pageSizeInSlots - 9;
        return ((maximumOptionPosition + 1) + (pageCapacity - 1)) / pageCapacity;
    }

    private Component substituteVariablesAndColors(String input) {
        String template = input;

        if (pageSizeInSlots != null) {
            template = template
                    .replace("{currentPage}", String.valueOf(currentPageIndex + 1))
                    .replace("{numberOfPages}", String.valueOf(getNumberOfPages()));
        }

        return MiniMessage.miniMessage().deserialize(template);
    }

    private IconMenuItem makeConfigurableItem(String key) {
        IconMenuItem result = new IconMenuItem();

        try {
            String materialString = plugin.getConfig().getString(paginationBasePath + "." + key + ".Type");
            result.setMaterial(Material.valueOf(materialString));
        } catch (Exception ignored) {
        }

        try {
            String titleString = plugin.getConfig().getString(paginationBasePath + "." + key + ".Title");
            result.setTitle(substituteVariablesAndColors(titleString));
        } catch (Exception ignored) {
        }

        try {
            List<String> loreLines = plugin.getConfig().getStringList(paginationBasePath + "." + key + ".Lore");
            for (String loreLine : loreLines)
                result.addLoreLine(substituteVariablesAndColors(loreLine));
        } catch (Exception ignored) {
        }

        return result;
    }

    /**
     * Returns the item at a display-space slot, accounting for
     * pagination offset and navigation buttons. Returns {@code null}
     * for empty slots in the navigation row.
     */
    public IconMenuItem getOption(int position) {
        if (pageSizeInSlots != null) {
            if (position == pageSizeInSlots - 9)
                return makeConfigurableItem("PreviousPage");

            if (position == pageSizeInSlots - 1)
                return makeConfigurableItem("NextPage");

            // Last row is always empty, besides the navigation-buttons
            if (position > pageSizeInSlots - 9 && position < pageSizeInSlots - 1)
                return null;

            int pageCapacity = pageSizeInSlots - 9;
            int optionsOffset = currentPageIndex * pageCapacity;

            position += optionsOffset;
        }

        return options.get(position);
    }

    /** Opens the menu for the given player. No-op if the player is sleeping. */
    public void open(HumanEntity player) {
        if (player.isSleeping()) {
            player.sendMessage(Locale.getComponent("Message.No.CanUse", player));
            return;
        }
        if (inventory == null) {
            inventory = new IconMenuInventory();
        }
        inventory.open(this, player);
    }

    /** Re-renders all slots into the open inventory. Resizes if needed. */
    public void update() {
        if (inventory != null) {
            if (getSize() != inventory.getSize()) {
                List<HumanEntity> viewers = inventory.getViewers();
                inventory.close();
                for (HumanEntity viewer : viewers) {
                    this.open(viewer);
                }
            } else {
                inventory.update(this);
            }
        }
    }

    /** Closes the inventory for all viewers and unregisters event listeners. */
    public void destroy() {
        if (inventory != null) {
            inventory.close();
            handler = null;
            inventory = null;
        }
        HandlerList.unregisterAll(this);
    }

    private void previousPage() {
        if (currentPageIndex == 0)
            return;

        --currentPageIndex;
        update();
    }

    private void nextPage() {
        if (pageSizeInSlots == null)
            return;

        if (currentPageIndex == getNumberOfPages() - 1)
            return;

        ++currentPageIndex;
        update();
    }

    @EventHandler
    void onInventoryClose(InventoryCloseEvent event) {
        if (inventory != null && inventory.isMenuInventory(event.getInventory()) && inventory.getViewers().isEmpty()) {
            inventory = null;
        }
    }

    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        destroy();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void on(InventoryClickEvent event) {
        if (inventory != null && inventory.isMenuInventory(event.getInventory())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            int slot = event.getRawSlot();

            if (slot < 0 || slot >= getSize())
                return;

            int absolutePosition = slot;

            if (pageSizeInSlots != null) {
                if (slot == pageSizeInSlots - 9) {
                    previousPage();
                    return;
                }

                if (slot == pageSizeInSlots - 1) {
                    nextPage();
                    return;
                }

                int pageCapacity = pageSizeInSlots - 9;
                absolutePosition += currentPageIndex * pageCapacity;
            }

            if (options.containsKey(absolutePosition)) {
                final IconMenu.OptionClickEvent e = new IconMenu.OptionClickEvent((Player) event.getWhoClicked(), absolutePosition, this, options.get(absolutePosition));
                handler.onOptionClick(e);

                final Player p = (Player) event.getWhoClicked();
                p.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> {
                    if (e.willClose()) {
                        p.closeInventory();
                    }
                    if (e.willDestroy()) {
                        destroy();
                    }
                }, null, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onMonitor(InventoryClickEvent event) {
        if (inventory != null && inventory.isMenuInventory(event.getInventory())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    /** Callback interface for handling menu item clicks. */
    public interface OptionClickEventHandler {
        void onOptionClick(OptionClickEvent event);
    }

    /**
     * Event fired when a player clicks a valid option slot. Handlers
     * can toggle {@link #setWillClose} and {@link #setWillDestroy} to
     * control post-click behaviour (defaults: both {@code true}).
     */
    public static class OptionClickEvent {
        @Getter
        private final Player player;
        @Getter
        private final int position;
        @Getter
        private final IconMenuItem option;
        private boolean close;
        private boolean destroy;
        @Getter
        private final IconMenu menu;

        public OptionClickEvent(Player player, int position, IconMenu menu, IconMenuItem option) {
            this.player = player;
            this.position = position;
            this.menu = menu;
            this.close = true;
            this.destroy = true;
            this.option = option;
        }

        /** Whether the inventory will be closed after the handler returns. */
        public boolean willClose() {
            return close;
        }

        /** Whether the menu will be destroyed after the handler returns. */
        public boolean willDestroy() {
            return destroy;
        }

        /** Set to {@code false} to keep the inventory open after the click. */
        public void setWillClose(boolean close) {
            this.close = close;
        }

        /** Set to {@code false} to keep the menu alive for re-use after the click. */
        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }
    }
}