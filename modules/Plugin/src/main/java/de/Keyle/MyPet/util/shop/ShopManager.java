package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
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

@Load(Load.State.OnReady)
@ServiceName("ShopService")
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
                shop.load(shops.getConfigurationSection(name));
                if (defaultShop == null && shop.isDefault()) {
                    defaultShop = name;
                }
                this.shops.put(name, shop);
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        //TODO close all shops
    }

    @Override
    public String getServiceName() {
        return "ShopService";
    }

    public void open(String name, Player player) {
        if (!MyPetApi.getPluginHookManager().isHookActive(VaultHook.class)) {
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