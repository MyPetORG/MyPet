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

import de.Keyle.MyPet.api.dialog.TextPromptSpec;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** Chat-based text prompt fallback using {@link ConversationFactory} + a single {@link StringPrompt}. */
@SuppressWarnings("removal")
public final class ChatPromptFallback {

    private final Plugin plugin;

    public ChatPromptFallback(Plugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, TextPromptSpec spec, Consumer<String> onResult, Runnable onCancel) {
        StringPrompt prompt = new StringPrompt() {
            @Override
            public @NotNull String getPromptText(@NotNull ConversationContext context) {
                return PlainTextComponentSerializer.plainText().serialize(spec.prompt());
            }

            @Override
            public Prompt acceptInput(@NotNull ConversationContext context, String input) {
                if (input == null || "cancel".equalsIgnoreCase(input.trim())) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try { onCancel.run(); } catch (Throwable t) {
                            plugin.getLogger().warning("ChatPromptFallback onCancel threw: " + t.getMessage());
                        }
                    });
                    return Prompt.END_OF_CONVERSATION;
                }
                String trimmed = input.trim();
                if (trimmed.length() > spec.maxLength()) {
                    trimmed = trimmed.substring(0, spec.maxLength());
                }
                final String result = trimmed;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try { onResult.accept(result); } catch (Throwable t) {
                        plugin.getLogger().warning("ChatPromptFallback onResult threw: " + t.getMessage());
                    }
                });
                return Prompt.END_OF_CONVERSATION;
            }
        };

        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(prompt)
            .withLocalEcho(false)
            .withEscapeSequence("cancel")
            .withTimeout(60)
            .thatExcludesNonPlayersWithMessage("Players only.");

        Conversable conversable = viewer;
        conversable.beginConversation(factory.buildConversation(conversable));
    }
}
