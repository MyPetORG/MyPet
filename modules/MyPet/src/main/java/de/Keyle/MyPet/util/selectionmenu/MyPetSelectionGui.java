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
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (pets.size() > 0) {
            final Map<Integer, StoredMyPet> petSlotList = new HashMap<>();
            IconMenu menu = new IconMenu(title, new IconMenu.OptionClickEventHandler() {
                @Override
                public void onOptionClick(IconMenu.OptionClickEvent event) {
                    if (petSlotList.containsKey(event.getPosition())) {
                        StoredMyPet storedMyPet = petSlotList.get(event.getPosition());
                        if (storedMyPet != null && callback != null) {
                            callback.callback(storedMyPet);
                        }
                    }
                    event.setWillClose(true);
                    event.setWillDestroy(true);
                }
            }, MyPetApi.getPlugin());

            WorldGroup wg = WorldGroup.getGroupByWorld(player.getPlayer().getWorld().getName());
            for (int i = 0; i < pets.size() && i < 54; i++) {
                StoredMyPet mypet = pets.get(i);

                if (!mypet.getWorldGroup().equals("") && !mypet.getWorldGroup().equals(wg.getName())) {
                    continue;
                }

                if (player.hasMyPet() && player.getMyPet().getUUID().equals(mypet.getUUID())) {
                    continue;
                }

                SpawnerEggTypes egg = SpawnerEggTypes.getEggType(mypet.getPetType());
                List<String> lore = new ArrayList<>();
                lore.add(RESET + Translation.getString("Name.Hunger", player) + ": " + GOLD + Math.round(mypet.getHungerValue()));
                if (mypet.getRespawnTime() > 0) {
                    lore.add(RESET + Translation.getString("Name.Respawntime", player) + ": " + GOLD + mypet.getRespawnTime() + "sec");
                } else {
                    lore.add(RESET + Translation.getString("Name.HP", player) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
                }
                lore.add(RESET + Translation.getString("Name.Exp", player) + ": " + GOLD + String.format("%1.2f", mypet.getExp()));
                lore.add(RESET + Translation.getString("Name.Type", player) + ": " + GOLD + mypet.getPetType().name());
                lore.add(RESET + Translation.getString("Name.Skilltree", player) + ": " + GOLD + (mypet.getSkilltree() != null ? mypet.getSkilltree().getDisplayName() : "-"));

                TagCompound entityTag = new TagCompound();
                entityTag.put("id", new TagString(mypet.getPetType().getMinecraftName()));
                int pos = menu.addOption(
                        new IconMenuItem()
                                .setMaterial(Material.MONSTER_EGG)
                                .setData(egg.getColor())
                                .setTitle(RESET + mypet.getPetName())
                                .addLore(lore)
                                .setGlowing(egg.isGlowing())
                                .addTag("EntityTag", entityTag)
                );
                petSlotList.put(pos, mypet);
            }

            menu.open(player.getPlayer());
        }
    }
}