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

package de.Keyle.MyPet.dialog;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.dialog.TextPromptSpec;
import de.Keyle.MyPet.util.CompatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Text-input prompt backed by a real anvil rename field, opened via
 * {@link MenuType#ANVIL} (Paper 1.21+). The player types into the rename box;
 * clicking the result slot submits, closing without clicking cancels. On servers
 * older than 1.21 {@link #open} returns false so the caller falls back to chat —
 * {@code createInventory(InventoryType.ANVIL)} produces a non-functional anvil.
 */
public final class AnvilFallbackProvider implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private boolean registered = false;

    public AnvilFallbackProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        sessions.clear();
        registered = false;
    }

    /** Whether a functional anvil container API ({@link MenuType#ANVIL}) is available. */
    public boolean isUsable() {
        return CompatUtil.minecraftVersionEqualsOrAbove("1.21");
    }

    /** Returns true if the anvil opened; false means the caller should fall back to chat. */
    public boolean open(Player viewer, TextPromptSpec spec, Consumer<String> onResult, Runnable onCancel) {
        if (!isUsable()) return false;
        try {
            AnvilView view = MenuType.ANVIL.create(viewer, spec.title());
            ItemStack input = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = input.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(spec.initialValue()));
                input.setItemMeta(meta);
            }
            view.getTopInventory().setItem(0, input);
            view.setRepairCost(0);
            sessions.put(viewer.getUniqueId(), new Session(spec, onResult, onCancel));
            viewer.openInventory(view);
            return true;
        } catch (Throwable t) {
            sessions.remove(viewer.getUniqueId());
            plugin.getLogger().fine("AnvilFallbackProvider.open failed: " + t.getMessage());
            return false;
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView() instanceof AnvilView view)) return;
        if (!(view.getPlayer() instanceof Player viewer)) return;
        Session session = sessions.get(viewer.getUniqueId());
        if (session == null) return;
        // Always produce a free, takeable result so the player can submit even
        // when the name is unchanged.
        String rename = view.getRenameText();
        ItemStack result = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            String name = rename == null || rename.isEmpty() ? session.spec.initialValue() : rename;
            // Preview the typed name with its MiniMessage formatting applied, matching
            // how the pet name renders once saved. Disable the vanilla item-name italic
            // so colors read cleanly.
            meta.displayName(Util.SANITIZED_MINIMESSAGE.deserialize(name)
                .decoration(TextDecoration.ITALIC, false));
            result.setItemMeta(meta);
        }
        event.setResult(result);
        view.setRepairCost(0);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        Session session = sessions.get(viewer.getUniqueId());
        if (session == null) return;
        if (!(event.getInventory() instanceof AnvilInventory)) return;

        // A rename prompt has no movable items — cancel every click; only the
        // result slot submits. This also keeps the input name-tag locked in place.
        event.setCancelled(true);
        if (event.getRawSlot() != 2) return;
        if (!(event.getView() instanceof AnvilView view)) return;

        String text = view.getRenameText();
        if (text == null) text = "";
        if (text.length() > session.spec.maxLength()) {
            text = text.substring(0, session.spec.maxLength());
        }
        sessions.remove(viewer.getUniqueId());
        session.completed = true;
        final String result = text;
        Bukkit.getScheduler().runTask(plugin, () -> {
            viewer.closeInventory();
            try {
                session.onResult.accept(result);
            } catch (Throwable t) {
                plugin.getLogger().warning("AnvilFallbackProvider onResult threw: " + t.getMessage());
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        if (!(event.getInventory() instanceof AnvilInventory)) return;
        Session session = sessions.remove(viewer.getUniqueId());
        if (session == null || session.completed) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                session.onCancel.run();
            } catch (Throwable t) {
                plugin.getLogger().warning("AnvilFallbackProvider onCancel threw: " + t.getMessage());
            }
        });
    }

    private static final class Session {
        final TextPromptSpec spec;
        final Consumer<String> onResult;
        final Runnable onCancel;
        boolean completed;

        Session(TextPromptSpec spec, Consumer<String> onResult, Runnable onCancel) {
            this.spec = spec;
            this.onResult = onResult;
            this.onCancel = onCancel;
        }
    }
}
