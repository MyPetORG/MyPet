package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import org.bukkit.entity.Player;

public interface ShopService extends ServiceContainer {
    void open(String name, Player player);

    void open(Player player);
}