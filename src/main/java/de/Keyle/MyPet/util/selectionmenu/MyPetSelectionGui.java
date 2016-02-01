/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.iconmenu.IconMenu;
import de.Keyle.MyPet.util.iconmenu.IconMenuItem;
import de.Keyle.MyPet.util.locale.Translation;
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

    public void open(final RepositoryCallback<InactiveMyPet> callback) {
        MyPetList.getInactiveMyPets(player, new RepositoryCallback<List<InactiveMyPet>>() {
            @Override
            public void callback(List<InactiveMyPet> pets) {
                if (pets.size() > 0) {
                    final Map<Integer, InactiveMyPet> petSlotList = new HashMap<>();
                    IconMenu menu = new IconMenu(title, 54, new IconMenu.OptionClickEventHandler() {
                        @Override
                        public void onOptionClick(IconMenu.OptionClickEvent event) {
                            if (petSlotList.containsKey(event.getPosition())) {
                                InactiveMyPet myPet = petSlotList.get(event.getPosition());
                                if (myPet != null && callback != null) {
                                    callback.callback(myPet);
                                }
                            }
                            event.setWillClose(true);
                            event.setWillDestroy(true);
                        }
                    }, MyPetPlugin.getPlugin());

                    WorldGroup wg = WorldGroup.getGroupByWorld(player.getPlayer().getWorld().getName());
                    for (int i = 0; i < pets.size() && i < 54; i++) {
                        InactiveMyPet mypet = pets.get(i);

                        if (!mypet.getWorldGroup().equals("") && !mypet.getWorldGroup().equals(wg.getName())) {
                            continue;
                        }

                        if (player.hasMyPet() && player.getMyPet().getUUID().equals(mypet.getUUID())) {
                            continue;
                        }

                        SpawnerEggTypes egg = SpawnerEggTypes.getEggType(mypet.getPetType());
                        List<String> lore = new ArrayList<>();
                        lore.add(RESET + Translation.getString("Name.Hunger", player) + ": " + GOLD + mypet.getHungerValue());
                        if (mypet.getRespawnTime() > 0) {
                            lore.add(RESET + Translation.getString("Name.Respawntime", player) + ": " + GOLD + mypet.getRespawnTime() + "sec");
                        } else {
                            lore.add(RESET + Translation.getString("Name.HP", player) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
                        }
                        lore.add(RESET + Translation.getString("Name.Exp", player) + ": " + GOLD + String.format("%1.2f", mypet.getExp()));
                        lore.add(RESET + Translation.getString("Name.Type", player) + ": " + GOLD + mypet.getPetType().getTypeName());
                        lore.add(RESET + Translation.getString("Name.Skilltree", player) + ": " + GOLD + (mypet.getSkillTree() != null ? mypet.getSkillTree().getDisplayName() : "-"));
                        int pos = menu.addOption(new IconMenuItem().setMaterial(Material.MONSTER_EGG).setData(egg.getColor()).setTitle(RESET + mypet.getPetName()).addLore(lore).setGlowing(egg.isGlowing()));
                        petSlotList.put(pos, mypet);
                    }

                    menu.open(player.getPlayer());
                }
            }
        });
    }
}