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

package de.Keyle.MyPet.api.dialog;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Opens text and confirm prompts for a viewer using whichever transport
 * the implementation has available (anvil rename, chat conversation, or
 * a chest-menu confirm). Callbacks fire on the main thread.
 */
public interface DialogService extends ServiceContainer {

    /** Opens a text-input prompt. Calls {@code onResult} with the submitted text, or {@code onCancel} if dismissed. */
    void promptText(Player viewer, TextPromptSpec spec, Consumer<String> onResult, Runnable onCancel);

    /** Opens a yes/no confirm prompt. Calls {@code onYes} on confirm and {@code onNo} on cancel/decline. */
    void promptConfirm(Player viewer, ConfirmPromptSpec spec, Runnable onYes, Runnable onNo);
}
