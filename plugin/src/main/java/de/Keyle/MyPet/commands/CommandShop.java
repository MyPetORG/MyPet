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

package de.Keyle.MyPet.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.PetShopSelectionContext;
import de.Keyle.MyPet.util.shop.PetShop;
import de.Keyle.MyPet.util.shop.ShopManager;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles the {@code /petshop} command, which opens the pet shop GUI where players
 * can browse and purchase pets using an economy plugin.
 *
 * <p>Registered aliases: {@code /petsh}, {@code /psh}</p>
 *
 * <h3>Usage</h3>
 * <ul>
 *   <li>{@code /petshop} -- opens the default shop, or a selection GUI if no default is configured</li>
 *   <li>{@code /petshop <name>} -- opens the shop with the given name directly</li>
 *   <li>{@code /petshop <name> <player>} -- opens the named shop <em>for another player</em>;
 *       usable from the console, from command blocks, and from menu plugins that dispatch a
 *       console command such as {@code petshop all %player%}</li>
 * </ul>
 *
 * <h3>Permissions</h3>
 * <ul>
 *   <li>{@code MyPet.shop.access.<shopname>} -- required to access a specific shop</li>
 *   <li>{@code MyPet.shop.access.*} -- grants access to all shops</li>
 *   <li>{@code MyPet.command.shop.other} -- required to open a shop for another player;
 *       non-player senders (console, command blocks) are admitted unconditionally</li>
 * </ul>
 *
 * <p>Requires a Vault-compatible economy plugin to be active, and is disabled in worlds
 * where MyPet is disabled. The self-service forms are player-only; the
 * {@code /petshop <name> <player>} form is the console entry point and deliberately does
 * <em>not</em> require the target to hold {@code MyPet.shop.access.<name>} -- the sender's
 * permission is the authorization, which is what lets a menu plugin open a shop for a
 * player who holds no shop nodes at all.</p>
 */
public class CommandShop {

    /**
     * Registers the {@code /petshop} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petshop")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                executeDefault(player);
                            } else {
                                sendConsoleUsage(ctx.getSource().getSender());
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    suggestShopNames(ctx.getSource().getSender()).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        executeNamed(player, StringArgumentType.getString(ctx, "name"));
                                    } else {
                                        sendConsoleUsage(ctx.getSource().getSender());
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .requires(ctx -> {
                                            var sender = ctx.getSender();
                                            return !(sender instanceof Player p)
                                                    || Permissions.has(p, AdminPermissions.SHOP_OTHER);
                                        })
                                        .suggests((ctx, builder) -> {
                                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            executeForTarget(ctx.getSource().getSender(),
                                                    StringArgumentType.getString(ctx, "name"),
                                                    StringArgumentType.getString(ctx, "player"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .build(),
                "Opens the pet shop",
                List.of("petsh", "psh")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Shop",
                "/petshop",
                null,
                30,
                player -> true
        ));
    }

    /**
     * Executes {@code /petshop} with no arguments. Opens the default shop if one is configured
     * and the player has access; otherwise opens a shop selection GUI listing all accessible shops.
     *
     * @param player the player executing the command
     */
    private void executeDefault(Player player) {
        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            player.sendMessage(Locale.getComponent("Message.No.Economy", player));
            return;
        }
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isEmpty()) return;
        ShopManager manager = shopManager.get();

        // Always route through the selection menu when there are multiple accessible
        // shops, even when one is marked Default — admins use the GUI to discover
        // alternate shops, and a Default flag does not imply "skip selection."
        // /petshop <name> remains the direct-open shortcut.
        List<PetShop> accessible = new ArrayList<>();
        List<String> names = getAvailablePetShops(player);
        if (names != null) {
            for (String name : names) {
                PetShop shop = manager.getShop(name);
                if (shop != null) accessible.add(shop);
            }
        }

        if (accessible.isEmpty()) {
            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            return;
        }
        if (accessible.size() == 1) {
            accessible.get(0).open(player);
            return;
        }

        MyPetApi.getGuiService().openMenu(
            player,
            (MenuId<PetShopSelectionContext>) (MenuId<?>) MenuIds.PET_SHOP_SELECTION,
            new PetShopSelectionContext(player, accessible)
        );
    }

    /**
     * Executes {@code /petshop <name> <player>}: opens the named shop for another player.
     * This is the entry point for console senders, command blocks and menu plugins that
     * dispatch {@code petshop <shop> %player%}.
     *
     * <p>Brigadier has already authorized the sender through {@code MyPet.command.shop.other}
     * (non-players pass unconditionally), so the target's own {@code MyPet.shop.access.<name>}
     * node is intentionally not re-checked here. The target's world group still decides
     * whether MyPet may be used where they stand.</p>
     *
     * <p>The menu is opened through the target's entity scheduler so the call runs on the
     * region thread that owns them, which is what Folia requires.</p>
     *
     * @param sender     the sender that failures are reported back to
     * @param shopName   the name of the shop to open
     * @param targetName the name of the player to open the shop for
     */
    private void executeForTarget(CommandSender sender, String shopName, String targetName) {
        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            sender.sendMessage(Locale.getComponent("Message.No.Economy", sender));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Locale.getComponent("Message.No.PlayerOnline", sender));
            return;
        }
        if (WorldGroup.getGroupByWorld(target.getWorld()).isDisabled()) {
            sender.sendMessage(Locale.getComponent("Message.No.AllowedHere", sender));
            return;
        }

        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isEmpty()) return;

        PetShop shop = shopManager.get().getShop(shopName);
        if (shop == null) {
            sender.sendMessage(Locale.getComponent("Message.Shop.NotFound", sender));
            return;
        }

        target.getScheduler().run(MyPetApi.getPlugin(), task -> shop.open(target), null);
    }

    /**
     * Tells a non-player sender that the self-service forms need a player, and points at the
     * form that does work without one.
     */
    private void sendConsoleUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Only a player can open a shop for themselves. Use ")
                .color(NamedTextColor.RED)
                .append(Component.text("/petshop <shop> <player>").color(NamedTextColor.GOLD))
                .append(Component.text(" to open a shop for someone.").color(NamedTextColor.RED)));
    }

    /**
     * Shop names suggested for the {@code name} argument: the shops the sender may access
     * when a player is typing, or every configured shop for console and command blocks.
     */
    private List<String> suggestShopNames(CommandSender sender) {
        if (sender instanceof Player player) {
            List<String> shops = getAvailablePetShops(player);
            return shops != null ? shops : List.of();
        }
        return MyPetApi.getServiceManager().getService(ShopManager.class)
                .map(manager -> List.copyOf(manager.getShopNames()))
                .orElse(List.of());
    }

    /**
     * Executes {@code /petshop <name>}. Opens the specified shop if it exists and the player
     * has the required permission.
     *
     * @param player   the player executing the command
     * @param shopName the name of the shop to open
     */
    private void executeNamed(Player player, String shopName) {
        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            player.sendMessage(Locale.getComponent("Message.No.Economy", player));
            return;
        }
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        final Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            if (Permissions.has(player, "MyPet.shop.access." + shopName)) {
                shopManager.get().open(shopName, player);
            } else {
                player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            }
        }
    }

    /**
     * Returns the list of shop names the given player has permission to access.
     * Access is determined solely by {@code MyPet.shop.access.<shopName>}
     * (a wildcard grant of {@code MyPet.shop.access.*} yields all shops).
     *
     * @param player the player to check permissions for
     * @return the list of accessible shop names, or {@code null} if the {@link ShopManager} service is not available
     */
    public List<String> getAvailablePetShops(Player player) {
        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            return shopManager.get().getShopNames().stream()
                    .filter(name -> Permissions.has(player, "MyPet.shop.access." + name))
                    .toList();
        }
        return null;
    }
}
