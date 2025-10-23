package de.Keyle.MyPet.api.util.configuration;

import de.Keyle.MyPet.MyPetApi;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PetSelectionGuiCfg {

    private final YamlParser config;

    @Getter
    private final ItemStack fillerIcon;
    @Getter
    private final ItemStack previousPageIcon;
    @Getter
    private final ItemStack nextPageIcon;
    @Getter
    private final ItemStack closeIcon;
    @Getter
    private final int fillerIconSlot;
    @Getter
    private final int previousPageSlot;
    @Getter
    private final int nextPageSlot;
    @Getter
    private final int closeSlot;
    @Getter
    private final int sortSlot;

    public PetSelectionGuiCfg() {
        config = YamlParser.loadOrExtract(MyPetApi.getPlugin(), "guis/pet-selection.yml");
        fillerIcon = getIcon("Filler");
        previousPageIcon = getIcon("Previous");
        nextPageIcon = getIcon("Next");
        closeIcon = getIcon("Close");
        fillerIconSlot = getIconSlot("Filler");
        previousPageSlot = getIconSlot("Previous");
        nextPageSlot = getIconSlot("Next");
        closeSlot = getIconSlot("Close");
        sortSlot = getIconSlot("Sort");
    }

    public String getTitle() {
        return config.getString("Title", "Select your Pet");
    }

    public ItemStack getSortIcon(String currentSort) {
        String sortDisplay = config.getString("Icons.Sort.SortTypes." + currentSort, "&cError").replace("&", "§");
        Material material = Material.valueOf(config.getString("Icons.Sort.Material", "STONE"));
        String display = config.getString("Icons.Sort.Name", "&cError").replace("&", "§");
        List<String> lore = config.getStringList("Icons.Sort.Lore", new ArrayList<>());
        lore.replaceAll(s -> s.replace("&", "§").replace("%sort%", sortDisplay));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(display);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getIcon(String icon) {
        Material material = Material.valueOf(config.getString("Icons." + icon + ".Material", "STONE"));
        String display = config.getString("Icons." + icon + ".Name", "&cError").replace("&", "§");
        List<String> lore = config.getStringList("Icons." + icon + ".Lore", new ArrayList<>());
        lore.replaceAll(s -> s.replace("&", "§"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(display);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public int getIconSlot(String icon) {
        return config.getInt("Icons." + icon + ".Slot", 0);
    }
}
