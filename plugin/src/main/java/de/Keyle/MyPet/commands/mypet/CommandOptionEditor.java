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

package de.Keyle.MyPet.commands.mypet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.webeditor.WebEditorManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Provides the {@code /mypet editor} subcommands, mounted under {@code /mypet}:
 * <pre>
 *   /mypet editor              - open a web-editor session, returns a URL
 *   /mypet editor trust &lt;code&gt; - authorize the connecting browser
 *   /mypet editor close        - end the active session
 * </pre>
 *
 * <p>Admin-gated ({@code MyPet.admin.editor} or the {@code MyPet.admin} bundle). Delegates to the singleton
 * {@link WebEditorManager}. NOTE: behavior end-to-end needs a running server +
 * relay.
 */
public class CommandOptionEditor {

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("editor")
                .requires(AdminPermissions.requiresNode(AdminPermissions.EDITOR))
                .then(Commands.literal("trust")
                        .then(Commands.argument("code", StringArgumentType.string())
                                .executes(ctx -> {
                                    trust(ctx.getSource().getSender(), StringArgumentType.getString(ctx, "code"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("close")
                        .executes(ctx -> {
                            close(ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(ctx -> {
                    open(ctx.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    private void open(CommandSender sender) {
        if (!MyPetGlobal.WebEditor.ENABLED.get()) {
            sender.sendMessage(Component.text("The MyPet web editor is disabled in the server config.", NamedTextColor.RED));
            return;
        }
        UUID owner = ownerOf(sender);
        sender.sendMessage(Component.text("Connecting you to an editor session…", NamedTextColor.GRAY));
        // Opening serializes configs + makes HTTP calls to the relay — keep it off the main thread.
        Bukkit.getAsyncScheduler().runNow(MyPetApi.getPlugin(), task -> {
            try {
                String url = WebEditorManager.getInstance().open(owner);
                sender.sendMessage(Component.text("Open the MyPet web editor: ", NamedTextColor.GREEN)
                        .append(Component.text(url, NamedTextColor.AQUA)
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(url))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to open in your browser")))));
            } catch (IllegalStateException e) {
                sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            } catch (Exception e) {
                sender.sendMessage(Component.text("Failed to open the web editor: " + e.getMessage(), NamedTextColor.RED));
                MyPetApi.getLogger().warning("WebEditor: open failed: " + e.getMessage());
            }
        });
    }

    private void trust(CommandSender sender, String code) {
        try {
            boolean ok = WebEditorManager.getInstance().trust(ownerOf(sender), code);
            sender.sendMessage(ok
                    ? Component.text("Browser authorized — the editor is now live.", NamedTextColor.GREEN)
                    : Component.text("No matching pending authorization. Run /mypet editor first.", NamedTextColor.RED));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Trust failed: " + e.getMessage(), NamedTextColor.RED));
            MyPetApi.getLogger().warning("WebEditor: trust failed: " + e.getMessage());
        }
    }

    private void close(CommandSender sender) {
        boolean closed = WebEditorManager.getInstance().close();
        sender.sendMessage(closed
                ? Component.text("Web editor session closed.", NamedTextColor.GREEN)
                : Component.text("No active web editor session.", NamedTextColor.YELLOW));
    }

    /** Owner id for a session: the player's UUID, or the console sentinel for the server console. */
    private static UUID ownerOf(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : WebEditorManager.CONSOLE_OWNER;
    }
}
