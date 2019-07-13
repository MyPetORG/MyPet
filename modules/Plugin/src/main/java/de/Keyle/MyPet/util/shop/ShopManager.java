/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.exceptions.InvalidSkilltreeException;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.api.util.service.types.ShopService;
import de.Keyle.MyPet.util.hooks.VaultHook;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ServiceName("ShopManager")
@Load(Load.State.OnReady)
public class ShopManager implements ShopService {

    YamlConfiguration config;
    protected Map<String, PetShop> shops = new HashMap<>();
    protected String defaultShop = null;

    @Override
    public boolean onEnable() {
        File petConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "pet-shops.yml");
        config = new YamlConfiguration();

        if (petConfigFile.exists()) {
            try {
                config.load(petConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        ConfigurationSection shops = config.getConfigurationSection("Shops");

        if (shops != null) {
            this.shops.clear();
            for (String name : shops.getKeys(false)) {
                PetShop shop = new PetShop(name);
                try {
                    shop.load(shops.getConfigurationSection(name));
                    if (defaultShop == null && shop.isDefault()) {
                        defaultShop = name;
                    }
                    this.shops.put(name, shop);
                } catch (InvalidSkilltreeException e) {
                    MyPetApi.getMyPetLogger().warning("Your config file is invalid for shop:", name);
                    MyPetApi.getMyPetLogger().warning(e.getMessage());
                } catch (Exception e) {
                    MyPetApi.getMyPetLogger().warning("Your config file is invalid for shop:", name);
                }
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        //TODO close all shops
    }

    public void open(String name, Player player) {
        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            player.sendMessage(Translation.getString("Message.No.Economy", player));
            return;
        }
        PetShop shop = shops.get(name);
        if (shop != null) {
            shop.open(player);
        } else {
            player.sendMessage(Translation.getString("Message.Shop.NotFound", player));
        }
    }

    public void open(Player player) {
        if (defaultShop != null) {
            open(defaultShop, player);
        } else {
            if (!MyPetApi.getPluginHookManager().isHookActive(VaultHook.class)) {
                player.sendMessage(Translation.getString("Message.No.Economy", player));
                return;
            }
            player.sendMessage(Translation.getString("Message.No.Allowed", player));
        }
    }

    public PetShop getShop(String name) {
        return shops.get(name);
    }

    public String getDefaultShopName() {
        return defaultShop;
    }

    @Override
    public Set<String> getShopNames() {
        return Collections.unmodifiableSet(shops.keySet());
    }
}