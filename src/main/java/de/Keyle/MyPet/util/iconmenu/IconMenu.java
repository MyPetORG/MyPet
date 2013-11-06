/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util.iconmenu;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class IconMenu implements Listener {
    IconMenuInventory inventory;
    private int size;
    private OptionClickEventHandler handler;
    private Plugin plugin;
    private List<Inventory> inventoryList = new ArrayList<Inventory>();

    public IconMenu(String name, int size, OptionClickEventHandler handler, Plugin plugin) {
        this.size = size;
        this.handler = handler;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        inventory = new IconMenuInventory(size, name);
    }

    public IconMenu setOption(int position, IconMenuItem icon) {
        inventory.getMinecraftInventory().items[position] = icon;
        return this;
    }

    public int addOption(IconMenuItem icon) {
        IconMenuInventory.MinecraftInventory mi = inventory.getMinecraftInventory();
        for (int i = 0; i < mi.items.length; i++) {
            if (mi.items[i] == null) {
                mi.items[i] = icon;
                return i;
            }
        }
        return -1;
    }

    public IconMenuItem getOption(int position) {
        return inventory.getMinecraftInventory().items[position];
    }

    public void update() {
        inventory.getMinecraftInventory().update();
    }

    public void open(Player player) {
        Inventory openInv = player.openInventory(inventory.getCraftBukkitInventory()).getTopInventory();
        if (openInv == null) {
            return;
        }
        inventoryList.add(openInv);
    }

    public void destroy() {
        for (HumanEntity viewer : inventory.getMinecraftInventory().getViewers()) {
            viewer.closeInventory();
        }
        HandlerList.unregisterAll(this);
        handler = null;
        plugin = null;
        inventoryList.clear();
    }

    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin) && inventory != null) {
            List<HumanEntity> viewers = new ArrayList<HumanEntity>(inventory.getMinecraftInventory().getViewers());
            for (HumanEntity viewer : viewers) {
                viewer.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onInventoryClick(InventoryClickEvent event) {
        if (inventoryList.contains(event.getInventory())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < size && inventory.getMinecraftInventory().items[slot] != null) {
                OptionClickEvent e = new OptionClickEvent((Player) event.getWhoClicked(), slot, this, inventory.getMinecraftInventory().items[slot]);
                handler.onOptionClick(e);
                if (e.willClose()) {
                    final Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                }
                if (e.willDestroy()) {
                    destroy();
                }
            }
        }
    }

    @EventHandler
    void onInventoryClose(InventoryCloseEvent event) {
        inventoryList.remove(event.getInventory());
    }

    public interface OptionClickEventHandler {
        public void onOptionClick(OptionClickEvent event);
    }

    public class OptionClickEvent {
        private Player player;
        private int position;
        private IconMenuItem option;
        private boolean close;
        private boolean destroy;
        private IconMenu menu;

        public OptionClickEvent(Player player, int position, IconMenu menu, IconMenuItem option) {
            this.player = player;
            this.position = position;
            this.menu = menu;
            this.close = true;
            this.destroy = false;
            this.option = option;
        }

        public Player getPlayer() {
            return player;
        }

        public int getPosition() {
            return position;
        }

        public IconMenuItem getOption() {
            return option;
        }

        public IconMenu getMenu() {
            return menu;
        }

        public boolean willClose() {
            return close;
        }

        public boolean willDestroy() {
            return destroy;
        }

        public void setWillClose(boolean close) {
            this.close = close;
        }

        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }
    }
}