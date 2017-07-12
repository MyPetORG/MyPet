package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import org.bukkit.entity.Player;

import java.util.Set;

public interface ShopService extends ServiceContainer {
    void open(String name, Player player);

    void open(Player player);

    Set<String> getShopNames();

    String getDefaultShopName();
}