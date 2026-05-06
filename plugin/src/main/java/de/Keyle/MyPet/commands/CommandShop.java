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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.shop.PetShop;
import de.Keyle.MyPet.util.shop.ShopManager;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
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

        final Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            String shop = shopManager.get().getDefaultShopName();
            if (shop != null) {
                if (Permissions.has(player, "MyPet.shop.access." + shop) || Permissions.has(player, "MyPet.admin")) {
                    shopManager.get().open(player);
                    return;
                }
            } else {
                final List<String> availableShops = getAvailablePetShops(player);
                if (availableShops != null && !availableShops.isEmpty()) {
                    openShopSelectionGui(player, shopManager.get(), availableShops);
                    return;
                }
            }
            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
        }
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
     * Opens a paginated inventory GUI that lists all shops the player has access to.
     * Clicking a shop icon opens that shop after a short delay.
     *
     * @param player         the player to show the selection GUI to
     * @param shopManager    the {@link ShopManager} service instance
     * @param availableShops the list of shop names the player is permitted to access
     */
    private void openShopSelectionGui(Player player, ShopManager shopManager, List<String> availableShops) {
        Map<Integer, String> shops = new HashMap<>();
        IconMenu menu = new IconMenu(Locale.getComponent("Message.Shop.Available", player), event -> {
            String shopname = shops.get(event.getPosition());
            if (shopname != null) {
                final String finalShopname = shopname;
                player.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> shopManager.open(finalShopname, player), null, 5L);

                event.setWillClose(true);
                event.setWillDestroy(true);
            }
        }, MyPetApi.getPlugin()).setPaginationIdentifier("AvailableShops");

        Queue<PetShop> filler = new ArrayDeque<>();

        for (String shopname : availableShops) {
            PetShop s = shopManager.getShop(shopname);

            int position = s.getPosition();

            if (position < 0) {
                filler.add(s);
                continue;
            }

            menu.setOption(position, makeShopIcon(s));
            shops.put(position, s.getName());
        }

        while (!filler.isEmpty()) {
            PetShop s = filler.poll();

            int position = menu.addOption(makeShopIcon(s));
            shops.put(position, s.getName());
        }

        menu.open(player);
    }

    /**
     * Creates an {@link IconMenuItem} representing a pet shop in the selection GUI,
     * using the shop's configured display name, material, and glowing state.
     *
     * @param s the pet shop to create an icon for
     * @return the constructed icon menu item
     */
    private IconMenuItem makeShopIcon(PetShop s) {
        IconMenuItem icon = new IconMenuItem();
        icon.setTitle(Util.SANITIZED_MINIMESSAGE.deserialize(s.getDisplayName()));

        SkilltreeIcon si = s.getIcon();
        Material material = Material.matchMaterial(si.getMaterial());
        if (material == null) {
            material = Material.CHEST;
        }
        icon.setMaterial(material).setGlowing(si.isGlowing());

        return icon;
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
