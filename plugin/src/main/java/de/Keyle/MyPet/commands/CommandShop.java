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
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.PetShopSelectionContext;
import de.Keyle.MyPet.util.shop.PetShop;
import de.Keyle.MyPet.util.shop.ShopManager;
import io.papermc.paper.command.brigadier.Commands;
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
 * </ul>
 *
 * <h3>Permissions</h3>
 * <ul>
 *   <li>{@code MyPet.shop.access.<shopname>} -- required to access a specific shop</li>
 *   <li>{@code MyPet.admin} -- grants access to all shops</li>
 * </ul>
 *
 * <p>Requires a Vault-compatible economy plugin to be active. The command is only
 * usable by players and is disabled in worlds where MyPet is disabled.</p>
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
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            executeDefault((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        List<String> shops = getAvailablePetShops(player);
                                        if (shops != null) {
                                            shops.forEach(builder::suggest);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    executeNamed((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "name"));
                                    return Command.SINGLE_SUCCESS;
                                }))
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
            if (Permissions.has(player, "MyPet.shop.access." + shopName) || Permissions.has(player, "MyPet.admin")) {
                shopManager.get().open(shopName, player);
            } else {
                player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            }
        }
    }

    /**
     * Returns the list of shop names the given player has permission to access.
     * Admin players (with {@code MyPet.admin}) receive all shops.
     *
     * @param player the player to check permissions for
     * @return the list of accessible shop names, or {@code null} if the {@link ShopManager} service is not available
     */
    public List<String> getAvailablePetShops(Player player) {
        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            if (Permissions.has(player, "MyPet.admin")) {
                return new ArrayList<>(shopManager.get().getShopNames());
            }
            List<String> shops = new ArrayList<>();
            for (String shop : shopManager.get().getShopNames()) {
                if (Permissions.has(player, "MyPet.shop.access." + shop)) {
                    shops.add(shop);
                }
            }
            return shops;
        }
        return null;
    }
}
