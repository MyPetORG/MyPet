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

import de.Keyle.MyPet.api.dialog.ConfirmPromptSpec;
import de.Keyle.MyPet.api.dialog.DialogService;
import de.Keyle.MyPet.api.dialog.TextPromptSpec;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Default {@link DialogService} implementation. Text prompts try an anvil rename
 * first, falling back to a chat conversation; confirm prompts open the
 * {@code pet-release-confirm} chest menu.
 */
@ServiceName("DialogService")
public final class DialogServiceImpl implements DialogService {

    private Plugin plugin;
    private AnvilFallbackProvider anvilProvider;
    private ChatPromptFallback chatProvider;

    /** No-arg constructor used by the service manager. */
    public DialogServiceImpl() {}

    /** Called from {@link de.Keyle.MyPet.MyPetPlugin#onEnable()} after the service is registered. */
    public void init(Plugin plugin) {
        this.plugin = plugin;
        this.anvilProvider = new AnvilFallbackProvider(plugin);
        this.anvilProvider.register();
        this.chatProvider = new ChatPromptFallback(plugin);
    }

    @Override
    public void onDisable() {
        if (anvilProvider != null) {
            anvilProvider.shutdown();
        }
    }

    @Override
    public void promptText(Player viewer, TextPromptSpec spec, Consumer<String> onResult, Runnable onCancel) {
        if (anvilProvider != null && anvilProvider.open(viewer, spec, onResult, onCancel)) {
            return;
        }
        chatProvider.open(viewer, spec, onResult, onCancel);
    }

    @Override
    public void promptConfirm(Player viewer, ConfirmPromptSpec spec, Runnable onYes, Runnable onNo) {
        ConfirmFallbackMenu.open(viewer, spec, onYes, onNo);
    }
}
