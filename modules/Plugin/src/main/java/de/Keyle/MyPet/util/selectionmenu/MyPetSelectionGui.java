/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.util.selectionmenu;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.inventory.IconMenu;
import de.Keyle.MyPet.api.util.inventory.IconMenuItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

public class MyPetSelectionGui {
    MyPetPlayer player;
    String title;

    public MyPetSelectionGui(MyPetPlayer player) {
        this(player, Translation.getString("Message.SelectMyPet", player));
    }

    public MyPetSelectionGui(MyPetPlayer player, String title) {
        this.player = player;
        this.title = title;
    }

    public void open(final RepositoryCallback<StoredMyPet> callback) {
        MyPetApi.getRepository().getMyPets(player, new RepositoryCallback<List<StoredMyPet>>() {
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
        if (pets.size() > 0) {
            if (page < 1 || Math.ceil(pets.size() / 45.) < page) {
                page = 1;
            }

            final Map<Integer, StoredMyPet> petSlotList = new HashMap<>();
            WorldGroup wg = WorldGroup.getGroupByWorld(player.getPlayer().getWorld().getName());

            Iterator<StoredMyPet> iterator = pets.iterator();
            while (iterator.hasNext()) {
                StoredMyPet mypet = iterator.next();
                if (mypet.getWorldGroup().equals("") || !mypet.getWorldGroup().equals(wg.getName())) {
                    iterator.remove();
                }

                if (player.hasMyPet() && player.getMyPet().getUUID().equals(mypet.getUUID())) {
                    iterator.remove();
                }
            }

            final int previousPage = page == 1 ? (int) Math.ceil(pets.size() / 45.) : page - 1;
            final int nextPage = page == Math.ceil(pets.size() / 45.) ? 1 : page + 1;

            IconMenu menu = new IconMenu(title, new IconMenu.OptionClickEventHandler() {
                @Override
                public void onOptionClick(IconMenu.OptionClickEvent event) {
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
                }
            }, MyPetApi.getPlugin());

            int pagePets = pets.size() - (page - 1) * 45;
            for (int i = 0; i < pagePets && i <= 45; i++) {
                StoredMyPet mypet = pets.get(i + ((page - 1) * 45));

                SpawnerEggTypes egg = SpawnerEggTypes.getEggType(mypet.getPetType());
                List<String> lore = new ArrayList<>();
                lore.add(RESET + Translation.getString("Name.Hunger", player) + ": " + GOLD + Math.round(mypet.getSaturation()));
                if (mypet.getRespawnTime() > 0) {
                    lore.add(RESET + Translation.getString("Name.Respawntime", player) + ": " + GOLD + mypet.getRespawnTime() + "sec");
                } else {
                    lore.add(RESET + Translation.getString("Name.HP", player) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
                }
                lore.add(RESET + Translation.getString("Name.Exp", player) + ": " + GOLD + String.format("%1.2f", mypet.getExp()));
                lore.add(RESET + Translation.getString("Name.Type", player) + ": " + GOLD + mypet.getPetType().name());
                lore.add(RESET + Translation.getString("Name.Skilltree", player) + ": " + GOLD + (mypet.getSkilltree() != null ? mypet.getSkilltree().getDisplayName() : "-"));

                IconMenuItem icon = new IconMenuItem();
                icon.setMaterial(Material.MONSTER_EGG);
                icon.setGlowing(egg.isGlowing());
                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                    TagCompound entityTag = new TagCompound();
                    entityTag.put("id", new TagString(egg.getEggName()));
                    icon.addTag("EntityTag", entityTag);
                } else {
                    icon.setData(egg.getColor());
                }

                int pos = menu.addOption(icon);
                petSlotList.put(pos, mypet);
            }

            if (previousPage != page) {
                menu.setOption(45, new IconMenuItem()
                        .setMaterial(Material.SIGN)
                        .setTitle("" + previousPage + " ≪≪")
                );
            }

            if (previousPage != page) {
                menu.setOption(53, new IconMenuItem()
                        .setMaterial(Material.SIGN)
                        .setTitle(ChatColor.BOLD + "≫≫ " + ChatColor.RESET + nextPage)
                );
            }

            menu.open(player.getPlayer());
        }
    }
}