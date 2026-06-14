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

import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.gui.MenuDefinition;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.ValidationException;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Process-wide registry of registered menus and their resolved {@link MenuDefinition}s.
 * Populated at {@code MyPetPlugin.onEnable} and on {@code /mypet reload}.
 */
public final class MenuRegistry {

    private final Plugin plugin;

    private record Registration<C>(MenuId<C> id, MenuHandler<C> handler, Supplier<InputStream> bundledJson) {}

    private final Map<String, Registration<?>> registrations = new HashMap<>();
    private final Map<String, MenuDefinition> resolved = new HashMap<>();

    /** Stats from the most recent {@link #loadAll}. */
    private int loaded = 0;
    private int overridesApplied = 0;
    private int overridesRejected = 0;

    public MenuRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public <C> void register(MenuId<C> id, MenuHandler<C> handler, Supplier<InputStream> bundledJson) {
        if (registrations.containsKey(id.id())) {
            throw new IllegalStateException("Menu id '" + id.id() + "' already registered");
        }
        registrations.put(id.id(), new Registration<>(id, handler, bundledJson));
    }

    /** Resolve every registered menu. Idempotent. Returns false if any override was rejected. */
    public boolean loadAll() {
        resolved.clear();
        loaded = 0;
        overridesApplied = 0;
        overridesRejected = 0;

        File menusDir = new File(plugin.getDataFolder(), "gui/menus");
        if (!menusDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            menusDir.mkdirs();
        }

        for (var entry : registrations.entrySet()) {
            String menuId = entry.getKey();
            Registration<?> reg = entry.getValue();
            MenuDefinition def = loadOne(menuId, reg, menusDir);
            if (def != null) {
                resolved.put(menuId, def);
                loaded++;
            }
        }
        return overridesRejected == 0;
    }

    public MenuDefinition definitionFor(String menuId) {
        return resolved.get(menuId);
    }

    public <C> MenuHandler<C> handlerFor(String menuId) {
        @SuppressWarnings("unchecked")
        Registration<C> reg = (Registration<C>) registrations.get(menuId);
        return reg == null ? null : reg.handler();
    }

    public int loadedCount() { return loaded; }
    public int overridesApplied() { return overridesApplied; }
    public int overridesRejected() { return overridesRejected; }

    // --- private --------------------------------------------------------------

    private MenuDefinition loadOne(String menuId, Registration<?> reg, File menusDir) {
        JsonObject bundled;
        try (InputStream in = reg.bundledJson().get()) {
            if (in == null) {
                plugin.getLogger().log(Level.SEVERE,
                    "GUI menu '" + menuId + "' has no bundled JSON; menu will not be available");
                return null;
            }
            bundled = MenuLoader.read(in);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read bundled JSON for menu '" + menuId + "'", e);
            return null;
        } catch (ValidationException e) {
            plugin.getLogger().log(Level.SEVERE,
                "Bundled JSON for menu '" + menuId + "' is invalid (this is a MyPet bug, please report): " + e.getMessage());
            return null;
        }

        File overrideFile = new File(menusDir, menuId + ".json");
        JsonObject overlay = null;
        if (overrideFile.isFile()) {
            try (InputStream in = new FileInputStream(overrideFile)) {
                overlay = MenuLoader.read(in);
            } catch (IOException | ValidationException e) {
                plugin.getLogger().log(Level.WARNING,
                    "GUI override 'plugins/MyPet/gui/menus/" + menuId + ".json' was REJECTED.\n"
                    + "                Reason: " + e.getMessage() + "\n"
                    + "                Falling back to bundled menu defaults. No menu functionality is lost.");
                overridesRejected++;
                overlay = null;
            }
        }

        JsonObject merged = overlay == null ? bundled : JsonHelpers.deepMerge(bundled, overlay);

        try {
            MenuDefinition def = MenuLoader.load(menuId, merged, 1, 6);
            if (overlay != null) overridesApplied++;
            return def;
        } catch (ValidationException e) {
            if (overlay != null) {
                plugin.getLogger().log(Level.WARNING,
                    "GUI override 'plugins/MyPet/gui/menus/" + menuId + ".json' was REJECTED.\n"
                    + "                Reason: " + e.getMessage() + "\n"
                    + "                Falling back to bundled menu defaults. No menu functionality is lost.");
                overridesRejected++;
                try {
                    return MenuLoader.load(menuId, bundled, 1, 6);
                } catch (ValidationException e2) {
                    plugin.getLogger().log(Level.SEVERE,
                        "Bundled JSON for menu '" + menuId + "' is invalid (this is a MyPet bug): " + e2.getMessage());
                    return null;
                }
            }
            plugin.getLogger().log(Level.SEVERE,
                "Bundled JSON for menu '" + menuId + "' is invalid (this is a MyPet bug): " + e.getMessage());
            return null;
        }
    }
}
