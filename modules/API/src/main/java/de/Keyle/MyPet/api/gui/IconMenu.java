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
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconMenu implements Listener {
    private IconMenuInventory inventory;
    private String title;
    private OptionClickEventHandler handler;
    protected Map<Integer, IconMenuItem> options = new HashMap<>(54);

    public IconMenu(String title, OptionClickEventHandler handler, Plugin plugin) {
        this.title = title;
        this.handler = handler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSize() {
        int size = 0;
        for (int i : options.keySet()) {
            if (++i > size) {
                size = i;
            }
        }
        return (int) (Math.ceil(size / 9.) * 9);
    }

    public IconMenu setOption(int position, IconMenuItem icon) {
        if (position >= 0 && position < 54) {
            options.put(position, icon);
        }
        return this;
    }

    public int addOption(IconMenuItem icon) {
        for (int i = 0; i < 54; i++) {
            if (!options.containsKey(i)) {
                options.put(i, icon);
                return i;
            }
        }
        return -1;
    }

    public IconMenuItem getOption(int position) {
        return options.get(position);
    }

    public void open(HumanEntity player) {
        if (player.isSleeping()) {
            player.sendMessage(Translation.getString("Message.No.CanUse", player));
            return;
        }
        if (inventory == null) {
            inventory = MyPetApi.getCompatUtil().getComapatInstance(IconMenuInventory.class, "util.iconmenu", "IconMenuInventory");
        }
        inventory.open(this, player);
    }

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

    public void destroy() {
        if (inventory != null) {
            inventory.close();
            handler = null;
            inventory = null;
        }
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    void onInventoryClose(InventoryCloseEvent event) {
        if (inventory != null && inventory.isMenuInventory(event.getInventory()) && inventory.getViewers().size() == 0) {
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
            event.setCursor(null);
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < getSize() && options.containsKey(slot)) {
                final IconMenu.OptionClickEvent e = new IconMenu.OptionClickEvent((Player) event.getWhoClicked(), slot, this, options.get(slot));
                handler.onOptionClick(e);

                final Player p = (Player) event.getWhoClicked();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (e.willClose()) {
                            p.closeInventory();
                        }
                        if (e.willDestroy()) {
                            destroy();
                        }
                    }
                }.runTaskLater(MyPetApi.getPlugin(), 0);
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

    public interface OptionClickEventHandler {
        void onOptionClick(OptionClickEvent event);
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
            this.destroy = true;
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