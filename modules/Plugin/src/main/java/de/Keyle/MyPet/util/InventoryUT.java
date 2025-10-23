package de.Keyle.MyPet.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.IntStream;

public class InventoryUT {
    public static Inventory createFilledInventory(Player player, String title, int size, ItemStack fillItem) {
        Inventory inv = Bukkit.createInventory(player, size, title);
        IntStream.range(0,8).forEach(i -> inv.setItem(i, fillItem));
        IntStream.range(size - 9, size - 1).forEach(i -> inv.setItem(i, fillItem));
        return inv;
    }

    public static ItemStack getItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null)
            meta.setDisplayName(name);
        if (lore != null)
            meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
