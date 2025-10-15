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

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EggIconService;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;

public class MyPetAdminSelectionGui {
    MyPetPlayer petOwner;
    Player admin;
    String title;
    
    public MyPetAdminSelectionGui(MyPetPlayer player, Player admin,  String title) {
        this.petOwner = player;
        this.admin = admin;
        this.title = title;
    }

    public void open(final RepositoryCallback<StoredMyPet> callback) {
        MyPetApi.getRepository().getMyPets(petOwner, new RepositoryCallback<List<StoredMyPet>>() {
            @Override
            public void callback(List<StoredMyPet> pets) {
                open(pets, callback);
            }
        });
    }

    public void open(List<StoredMyPet> pets, final RepositoryCallback<StoredMyPet> callback) {
        open(pets, 1, callback);
    }

    public void open(final List<StoredMyPet> pets, int page, final RepositoryCallback<StoredMyPet> callback) {
        if (!pets.isEmpty()) {
            if (page < 1 || Math.ceil(pets.size() / 45.) < page) {
                page = 1;
            }

            final Map<Integer, StoredMyPet> petSlotList = new HashMap<>();
            WorldGroup wg = WorldGroup.getGroupByWorld(petOwner.getPlayer().getWorld().getName());

            Iterator<StoredMyPet> iterator = pets.iterator();
            while (iterator.hasNext()) {
                StoredMyPet mypet = iterator.next();
                if (mypet.getWorldGroup().isEmpty() || !mypet.getWorldGroup().equals(wg.getName())) {
                    iterator.remove();
                }

                if (petOwner.hasMyPet() && petOwner.getMyPet().getUUID().equals(mypet.getUUID())) {
                    iterator.remove();
                }
            }

            final int previousPage = page == 1 ? (int) Math.ceil(pets.size() / 45.) : page - 1;
            final int nextPage = page == Math.ceil(pets.size() / 45.) ? 1 : page + 1;

            IconMenu menu = new IconMenu(title, event -> {
                if (event.getPosition() == 45) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            open(pets, previousPage, callback);
                        }
                    }.runTaskLater(MyPetApi.getPlugin(), 1L);
                } else if (event.getPosition() == 53) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            open(pets, nextPage, callback);
                        }
                    }.runTaskLater(MyPetApi.getPlugin(), 1L);

                } else if (event.getPosition() > 45) {
                    return;
                } else if (petSlotList.containsKey(event.getPosition())) {
                    StoredMyPet storedMyPet = petSlotList.get(event.getPosition());
                    if (storedMyPet != null && callback != null) {
                        callback.callback(storedMyPet);
                    }
                }
                event.setWillClose(true);
                event.setWillDestroy(true);
            }, MyPetApi.getPlugin());

            int pagePets = pets.size() - (page - 1) * 45;
            for (int i = 0; i < pagePets && i <= 45; i++) {
                StoredMyPet mypet = pets.get(i + ((page - 1) * 45));

                List<String> lore = new ArrayList<>();
                lore.add(RESET + Translation.getString("Name.Hunger", admin) + ": " + GOLD + Math.round(mypet.getSaturation()));
                if (mypet.getRespawnTime() > 0) {
                    lore.add(RESET + Translation.getString("Name.Respawntime", admin) + ": " + GOLD + mypet.getRespawnTime() + "sec");
                } else {
                    lore.add(RESET + Translation.getString("Name.HP", admin) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
                }
                boolean levelFound = false;
                if (mypet.getInfo().containsKey("storage")) {
                    TagCompound storage = mypet.getInfo().getAs("storage", TagCompound.class);
                    if (storage.containsKey("level")) {
                        lore.add(RESET + Translation.getString("Name.Level", admin) + ": " + GOLD + storage.getAs("level", TagInt.class).getIntData());
                        levelFound = true;
                    }
                }
                if (!levelFound) {
                    lore.add(RESET + Translation.getString("Name.Exp", admin) + ": " + GOLD + String.format("%1.2f", mypet.getExp()));
                }
                lore.add(RESET + Translation.getString("Name.Type", admin) + ": " + GOLD + Translation.getString("Name." + mypet.getPetType().name(), admin));
                lore.add(RESET + Translation.getString("Name.Skilltree", admin) + ": " + GOLD + Colorizer.setColors(mypet.getSkilltree() != null ? mypet.getSkilltree().getDisplayName() : "-"));

                IconMenuItem icon = new IconMenuItem();
                icon.setTitle(RESET + mypet.getPetName());
                icon.addLore(lore);
                Optional<EggIconService> egg = MyPetApi.getServiceManager().getService(EggIconService.class);
                egg.ifPresent(service -> service.updateIcon(mypet.getPetType(), icon));

                int pos = menu.addOption(icon);
                petSlotList.put(pos, mypet);
            }

            if (previousPage != page) {
                menu.setOption(45, new IconMenuItem()
                        .setMaterial(EnumSelector.find(Material.class, "SIGN", "OAK_SIGN"))
                        .setTitle("" + previousPage + " ≪≪")
                );
            }

            if (previousPage != page) {
                menu.setOption(53, new IconMenuItem()
                        .setMaterial(EnumSelector.find(Material.class, "SIGN", "OAK_SIGN"))
                        .setTitle(ChatColor.BOLD + "≫≫ " + ChatColor.RESET + nextPage)
                );
            }

            menu.open(admin); 
        }
    }
}