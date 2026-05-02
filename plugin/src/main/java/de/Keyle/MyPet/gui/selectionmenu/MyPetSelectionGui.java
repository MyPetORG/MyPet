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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.Util;
import java.util.function.Consumer;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.services.EggIconService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class MyPetSelectionGui {

    private final MyPetPlayer player;
    private final Component title;
    private final int page;

    public MyPetSelectionGui(MyPetPlayer player, Component title) {
        this.player = player;
        this.title = title;
        this.page = 1;
    }

    public MyPetSelectionGui(MyPetPlayer player, Component title, int page) {
        this.player = player;
        this.title = title;
        this.page = page;
    }

    public void open(List<StoredMyPet> pets, final Consumer<StoredMyPet> callback) {
        List<StoredMyPet> pagedPets;
        // restrict the number of pets to 54 per page
        int startIndex = (page - 1) * 54;
        int endIndex = Math.min(startIndex + 54, pets.size());
        if (startIndex < pets.size()) {
            pagedPets = pets.subList(startIndex, endIndex);
        } else {
            pagedPets = pets.subList(0, Math.min(54, pets.size()));
        }


        final Map<Integer, StoredMyPet> petSlotList = new HashMap<>();
        WorldGroup wg = WorldGroup.getGroupByWorld(player.getPlayer().getWorld().getName());

        IconMenu menu = new IconMenu(title, event -> {
            if (petSlotList.containsKey(event.getPosition())) {
                StoredMyPet storedMyPet = petSlotList.get(event.getPosition());
                if (storedMyPet != null && callback != null) {
                    callback.accept(storedMyPet);
                }
            }

            event.setWillClose(true);
            event.setWillDestroy(true);
        }, MyPetApi.getPlugin()).setPaginationIdentifier("SelectMyPet");

        int nextPosition = 0;

        for (StoredMyPet currentPet : pagedPets) {
            if (currentPet.getWorldGroup().isEmpty() || !currentPet.getWorldGroup().equals(wg.getName()))
                continue;

            if (player.hasMyPet() && player.getMyPet().getUUID().equals(currentPet.getUUID()))
                continue;

            IconMenuItem icon = new IconMenuItem();

            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM)
                icon.addLoreLine(Component.text().append(Translation.getComponent("Name.Hunger", player)).append(Component.text(": ")).append(Component.text(Math.round(currentPet.getSaturation())).color(NamedTextColor.GOLD)).build());

            if (currentPet.getRespawnTime() > 0) {
                icon.addLoreLine(Component.text().append(Translation.getComponent("Name.Respawntime", player)).append(Component.text(": ")).append(Component.text(currentPet.getRespawnTime() + "sec").color(NamedTextColor.GOLD)).build());
            } else {
                icon.addLoreLine(Component.text().append(Translation.getComponent("Name.HP", player)).append(Component.text(": ")).append(Component.text(String.format("%1.2f", currentPet.getHealth())).color(NamedTextColor.GOLD)).build());
            }

            int level = currentPet.getLevel();
            if (level > 0) {
                icon.addLoreLine(Component.text().append(Translation.getComponent("Name.Level", player)).append(Component.text(": ")).append(Component.text(level).color(NamedTextColor.GOLD)).build());
            } else {
                icon.addLoreLine(Component.text().append(Translation.getComponent("Name.Exp", player)).append(Component.text(": ")).append(Component.text(String.format("%1.2f", currentPet.getExp())).color(NamedTextColor.GOLD)).build());
            }

            icon.addLoreLine(Component.text().append(Translation.getComponent("Name.Type", player)).append(Component.text(": ")).append(Translation.getComponent("Name." + currentPet.getPetType().name(), player).color(NamedTextColor.GOLD)).build());
            icon.addLoreLine(Component.text().append(Translation.getComponent("Name.Skilltree", player)).append(Component.text(": ")).append(Util.SANITIZED_MINIMESSAGE.deserialize(currentPet.getSkilltree() != null ? currentPet.getSkilltree().getDisplayName() : "-").color(NamedTextColor.GOLD)).build());

            icon.setTitle(currentPet.getDisplayName());
            Optional<EggIconService> egg = MyPetApi.getServiceManager().getService(EggIconService.class);
            egg.ifPresent(service -> service.updateIcon(currentPet.getPetType(), icon));

            int currentPosition = nextPosition++;

            menu.setOption(currentPosition, icon);
            petSlotList.put(currentPosition, currentPet);
        }
        menu.open(player.getPlayer());
    }
}