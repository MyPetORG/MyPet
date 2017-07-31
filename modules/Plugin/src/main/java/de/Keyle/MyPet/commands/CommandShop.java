/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

import com.google.common.base.Optional;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.inventory.IconMenu;
import de.Keyle.MyPet.api.util.inventory.IconMenuItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.hooks.VaultHook;
import de.Keyle.MyPet.util.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.ChatColor.RESET;

public class CommandShop implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!MyPetApi.getPluginHookManager().isHookActive(VaultHook.class)) {
            sender.sendMessage(Translation.getString("Message.No.Economy", sender));
            return true;
        }
        Player player = null;

        if (sender instanceof ConsoleCommandSender && args.length < 2) {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }

        if (args.length > 1) {
            if (!(sender instanceof Player) || Permissions.has((Player) sender, "MyPet.admin")) {
                player = Bukkit.getPlayer(args[1]);
                if (player == null || !player.isOnline()) {
                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", sender));
                    return true;
                }
            }
        }
        if (player == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You can't use this command from server console!");
                return true;
            }
            player = (Player) sender;
        }

        final Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            if (args.length > 0) {
                String shop = args[0];
                if (Permissions.has(player, "MyPet.shop.access." + shop) || Permissions.has(player, "MyPet.admin")) {
                    shopManager.get().open(shop, player);
                } else {
                    player.sendMessage(Translation.getString("Message.No.Allowed", player));
                }

            } else {
                String shop = shopManager.get().getDefaultShopName();
                if (shop != null) {
                    if (Permissions.has(player, "MyPet.shop.access." + shop) || Permissions.has(player, "MyPet.admin")) {
                        shopManager.get().open(player);
                        return true;
                    }
                } else {
                    final List<String> availableShops = getAvailablePetShops(player);
                    if (availableShops != null && availableShops.size() > 0) {
                        final Player finalPlayer = player;
                        IconMenu menu = new IconMenu(Translation.getString("Message.Shop.Available", player), new IconMenu.OptionClickEventHandler() {
                            @Override
                            public void onOptionClick(IconMenu.OptionClickEvent event) {
                                String shopname = null;
                                try {
                                    shopname = availableShops.get(event.getPosition());
                                } catch (Exception ignored) {
                                }
                                if (shopname != null) {
                                    final String finalShopname = shopname;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            shopManager.get().open(finalShopname, finalPlayer);
                                        }
                                    }.runTaskLater(MyPetApi.getPlugin(), 5L);

                                    event.setWillClose(true);
                                    event.setWillDestroy(true);
                                }
                            }
                        }, MyPetApi.getPlugin());
                        for (String shopname : availableShops) {
                            IconMenuItem icon = new IconMenuItem();
                            icon.setTitle(RESET + Colorizer.setColors(shopManager.get().getShop(shopname).getDisplayName()));
                            icon.setMaterial(Material.CHEST);
                            menu.addOption(icon);
                        }

                        menu.open(player);
                        return true;
                    }
                }
                player.sendMessage(Translation.getString("Message.No.Allowed", player));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (strings.length == 1) {
                    return getAvailablePetShops(player);
                } else if (strings.length == 2) {
                    if (Permissions.has(player, "MyPet.admin")) {
                        return null;
                    }
                }
            } else {
                if (strings.length == 1) {
                    return new ArrayList<>(shopManager.get().getShopNames());
                } else if (strings.length == 2) {
                    return null;
                }
            }
        }
        return CommandAdmin.EMPTY_LIST;
    }

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