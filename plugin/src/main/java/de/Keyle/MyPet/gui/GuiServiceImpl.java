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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.gui.menus.ToolboxStationHold;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * {@link GuiService} implementation. Plugins call {@link #registerMenu} to publish
 * a menu; openMenu / closeMenu / refreshMenu / popBack go through the dispatcher.
 */
@ServiceName("GuiService")
public final class GuiServiceImpl implements GuiService {

    private Plugin plugin;
    private MenuRegistry registry;
    private MenuDispatcher dispatcher;

    /** No-arg constructor used by {@link de.Keyle.MyPet.api.util.service.ServiceManager}. */
    public GuiServiceImpl() {
    }

    @Override
    public boolean onEnable() {
        this.plugin = MyPetApi.getPlugin();
        this.registry = new MenuRegistry(plugin);
        this.dispatcher = new MenuDispatcher(plugin);
        // ESC-pop wiring: when a parent pops back, GuiServiceImpl reopens it using its captured context.
        this.dispatcher.reopenCallback = (entry, viewer) -> {
            MenuDefinition base = registry.definitionFor(entry.menuId());
            if (base == null) return;
            MenuDefinition def = entry.handler().transformDefinition(entry.context(), base);
            MenuInstanceImpl reopened = new MenuInstanceImpl(def, viewer, entry.handler(), entry.context());
            reopened.restoreFromSnapshot(entry.snapshot());
            dispatcher.open(reopened);
        };
        dispatcher.register();
        ToolboxStationHold.register(plugin);
        SectionTypeRegistry.ensureBuiltinsRegistered();
        registry.loadAll();
        return true;
    }

    /** Called from `/mypet reload`. Re-reads all bundled+overlay files. */
    public ReloadResult reload() {
        boolean clean = registry.loadAll();
        return new ReloadResult(registry.loadedCount(), registry.overridesApplied(), registry.overridesRejected());
    }

    public record ReloadResult(int loaded, int overridesApplied, int rejected) {}

    // --- GuiService -----------------------------------------------------------

    @Override
    public <C> void registerMenu(MenuId<C> id, MenuHandler<C> handler, Supplier<InputStream> bundledJson) {
        registry.register(id, handler, bundledJson);
    }

    @Override
    public <C> void openMenu(Player viewer, MenuId<C> id, C context) {
        MenuDefinition base = registry.definitionFor(id.id());
        if (base == null) {
            plugin.getLogger().warning("Tried to open unknown/unloaded menu: " + id.id());
            return;
        }
        @SuppressWarnings("unchecked")
        MenuHandler<Object> handler = (MenuHandler<Object>) registry.<C>handlerFor(id.id());
        MenuDefinition def = handler.transformDefinition(context, base);
        MenuInstanceImpl inst = new MenuInstanceImpl(def, viewer, handler, context);
        dispatcher.open(inst);
    }

    @Override
    public void refreshMenu(Player viewer) {
        dispatcher.refresh(viewer);
    }

    @Override
    public void closeMenu(Player viewer) {
        viewer.closeInventory();
    }

    @Override
    public OptionalInt addToOpenStorage(Player viewer, ItemStack item) {
        return dispatcher.addToOpenStorage(viewer, item);
    }

    @Override
    public SectionTypeRegistry getSectionTypeRegistry() {
        // SectionTypeRegistry's API is all static; the instance carries no state.
        // Callers use the static methods directly.
        return null;
    }
}
