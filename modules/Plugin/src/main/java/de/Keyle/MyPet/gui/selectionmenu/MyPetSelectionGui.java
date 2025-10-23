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

package de.Keyle.MyPet.gui.selectionmenu;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.configuration.PetSelectionGuiCfg;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EggIconService;
import de.Keyle.MyPet.util.InventoryUT;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

public class MyPetSelectionGui implements Listener {

    private final PetSelectionGuiCfg cfg = MyPetApi.getConfigurationManager().getPetSelectionGuiConfig();
    private final MyPetPlayer player;
    private final String title;

    private List<Inventory> inventories;
    private final HashMap<Inventory, HashMap<Integer, StoredMyPet>> slots = new HashMap<>();

    private final List<StoredMyPet> providedPets = new ArrayList<>();

    public MyPetSelectionGui(MyPetPlayer player, List<StoredMyPet> providedPets, String title) {
        this.player = player;
        this.providedPets.addAll(providedPets);
        this.title = title != null ? title : cfg.getTitle();
        MyPetApi.getPlugin().registerListener(this);
    }

    public void open() {
        open(0);
    }

    public void open(int page) {
        initialize(providedPets);
        player.getPlayer().openInventory(inventories.get(page) != null ? inventories.get(page) : inventories.get(page - 1));
    }

    private void initialize(List<StoredMyPet> entries) {
        slots.clear();
        HashMap<Integer, StoredMyPet> invSlots = new HashMap<>();
        List<Inventory> inventories = new ArrayList<>();
        Inventory inv = null;

        WorldGroup wg = WorldGroup.getGroupByWorld(player.getPlayer().getWorld().getName());
        entries = entries.stream().filter(entry ->
                        !((entry.getWorldGroup().isEmpty() || !entry.getWorldGroup().equals(wg.getName()))
                                || player.hasMyPet() && player.getMyPet().getUUID().equals(entry.getUUID())))
                .collect(Collectors.toList());

        for (int i = 0; i < entries.size(); i++) {
            int invDex = i % 27 + 9;
            if (invDex == 9) {
                if (inv != null)
                    inventories.add(inv);
                inv = InventoryUT.createFilledInventory(player.getPlayer(), title, 45, cfg.getIcon("FillerItem"));
                inv.setItem(cfg.getCloseSlot(), cfg.getCloseIcon());
                inv.setItem(cfg.getPreviousPageSlot(), cfg.getPreviousPageIcon());
                inv.setItem(cfg.getNextPageSlot(), cfg.getNextPageIcon());
                inv.setItem(cfg.getSortSlot(), cfg.getSortIcon("Ascending"));

                invSlots = new HashMap<>();
            }
            if (entries.get(i) == null)
                break;
            StoredMyPet entry = entries.get(i);
            if (entry.getWorldGroup().isEmpty() || !entry.getWorldGroup().equals(wg.getName()))
                continue;
            invSlots.put(invDex, entry);
            slots.put(inv, invSlots);
            inv.setItem(invDex, getPetIcon(entry));
        }
        inventories.add(inv == null ? InventoryUT.createFilledInventory(player.getPlayer(), title, 45, cfg.getFillerIcon()) : inv);
        this.inventories = inventories;
    }

    public ItemStack getPetIcon(StoredMyPet entry) {
        List<String> lore = new ArrayList<>();
        if (Configuration.HungerSystem.USE_HUNGER_SYSTEM)
            lore.add(RESET + Translation.getString("Name.Hunger", player) + ": " + GOLD + Math.round(entry.getSaturation()));
        if (entry.getRespawnTime() > 0) {
            lore.add(RESET + Translation.getString("Name.Respawntime", player) + ": " + GOLD + entry.getRespawnTime() + "sec");
        } else {
            lore.add(RESET + Translation.getString("Name.HP", player) + ": " + GOLD + String.format("%1.2f", entry.getHealth()));
        }
        boolean levelFound = false;
        if (entry.getInfo().containsKey("storage")) {
            TagCompound storage = entry.getInfo().getAs("storage", TagCompound.class);
            if (storage.containsKey("level")) {
                lore.add(RESET + Translation.getString("Name.Level", player) + ": " + GOLD + storage.getAs("level", TagInt.class).getIntData());
                levelFound = true;
            }
        }
        if (!levelFound)
            lore.add(RESET + Translation.getString("Name.Exp", player) + ": " + GOLD + String.format("%1.2f", entry.getExp()));

        lore.add(RESET + Translation.getString("Name.Type", player) + ": " + GOLD + Translation.getString("Name." + entry.getPetType().name(), player));
        lore.add(RESET + Translation.getString("Name.Skilltree", player) + ": " + GOLD + Colorizer.setColors(entry.getSkilltree() != null ? entry.getSkilltree().getDisplayName() : "-"));
        IconMenuItem icon = new IconMenuItem();
        Optional<EggIconService> egg = MyPetApi.getServiceManager().getService(EggIconService.class);
        egg.ifPresent(service -> service.updateIcon(entry.getPetType(), icon));
        return icon.asItemStack();
    }

    void click(InventoryClickEvent event) {
        int invdex = inventories.indexOf(event.getClickedInventory());
        if (invdex < 0) return;
        event.setCancelled(true);
        int slot = event.getSlot();
        int size = inventories.size();

        if(slot == cfg.getCloseSlot()) {
            player.getPlayer().closeInventory(); // Close inventory
        } else if(slot == cfg.getPreviousPageSlot()) {
            open((size + invdex - 1) % size); // Previous page
        } else if(slot == cfg.getNextPageSlot()) {
            open((invdex + 1) % size); // Next page
        } else if(slot == cfg.getSortSlot()) {

        } else if(slots.isEmpty() || slots.get(inventories.get(invdex)).get(slot) == null) {
            StoredMyPet entry = slots.get(inventories.get(invdex)).get(slot);
            if (event.isLeftClick()) {
                switchPet(entry);
            } else if (event.isRightClick()) {
                // TODO: Maybe a new releasing feature?
            }
        }
    }

    private void switchPet(StoredMyPet entry) {
        MyPetPlayer owner = entry.getOwner();
        Optional<MyPet> activePet = MyPetApi.getMyPetManager().activateMyPet(entry);
        if (activePet.isPresent() && owner.isOnline()) {
            Player player = owner.getPlayer();
            activePet.get().getOwner().sendMessage(Util.formatText(Translation.getString("Message.Npc.ChosenPet", owner), activePet.get().getPetName()));
            WorldGroup wg = WorldGroup.getGroupByWorld(player.getWorld().getName());
            owner.setMyPetForWorldGroup(wg, activePet.get().getUUID());

            switch (activePet.get().createEntity()) {
                case Canceled:
                    owner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", owner), activePet.get().getPetName()));
                    break;
                case NoSpace:
                    owner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", owner), activePet.get().getPetName()));
                    break;
                case NotAllowed:
                    owner.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", owner), activePet.get().getPetName()));
                    break;
                case Dead:
                    if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                        owner.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", owner), activePet.get().getPetName()));
                    } else {
                        owner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", owner), activePet.get().getPetName(), activePet.get().getRespawnTime()));
                    }
                    break;
                case Spectator:
                    player.getPlayer().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Spectator", owner), activePet.get().getPetName()));
                    break;
            }
        }
    }

    @EventHandler
    void onInventoryClick(InventoryClickEvent event) {
        for (Inventory inv : inventories)
            if (event.getClickedInventory() == inv)
                click(event);
    }
}