/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.Util;
import java.util.function.Consumer;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.services.EggIconService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.*;

public class MyPetAdminSelectionGui {
    MyPetPlayer petOwner;
    Player admin;
    Component title;

    public MyPetAdminSelectionGui(MyPetPlayer player, Player admin, Component title) {
        this.petOwner = player;
        this.admin = admin;
        this.title = title;
    }

    public void open(final Consumer<StoredPet> callback) {
        MyPetPlugin.getInstance().getRepository().getPets(petOwner).thenAccept(pets -> {
            admin.getScheduler().run(MyPetApi.getPlugin(), folaTask -> open(pets, callback), null);
        });
    }

    public void open(List<StoredPet> pets, final Consumer<StoredPet> callback) {
        open(pets, 1, callback);
    }

    public void open(final List<StoredPet> pets, int page, final Consumer<StoredPet> callback) {
        if (!pets.isEmpty()) {
            if (page < 1 || Math.ceil(pets.size() / 45.) < page) {
                page = 1;
            }

            final Map<Integer, StoredPet> petSlotList = new HashMap<>();
            WorldGroup wg = WorldGroup.getGroupByWorld(petOwner.getPlayer().getWorld().getName());

            Iterator<StoredPet> iterator = pets.iterator();
            while (iterator.hasNext()) {
                StoredPet mypet = iterator.next();
                if (mypet.getWorldGroup().isEmpty()
                        || !mypet.getWorldGroup().equals(wg.getName())
                        || (petOwner.hasMyPet() && petOwner.getMyPet().getUUID().equals(mypet.getUUID()))) {
                    iterator.remove();
                }
            }

            final int previousPage = page == 1 ? (int) Math.ceil(pets.size() / 45.) : page - 1;
            final int nextPage = page == Math.ceil(pets.size() / 45.) ? 1 : page + 1;

            IconMenu menu = new IconMenu(title, event -> {
                if (event.getPosition() == 45) {
                    admin.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> open(pets, previousPage, callback), null, 1L);
                } else if (event.getPosition() == 53) {
                    admin.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> open(pets, nextPage, callback), null, 1L);

                } else if (event.getPosition() > 45) {
                    return;
                } else if (petSlotList.containsKey(event.getPosition())) {
                    StoredPet storedPet = petSlotList.get(event.getPosition());
                    if (storedPet != null && callback != null) {
                        callback.accept(storedPet);
                    }
                }
                event.setWillClose(true);
                event.setWillDestroy(true);
            }, MyPetApi.getPlugin());

            int pagePets = pets.size() - (page - 1) * 45;
            for (int i = 0; i < pagePets && i < 45; i++) {
                StoredPet mypet = pets.get(i + ((page - 1) * 45));

                IconMenuItem icon = new IconMenuItem();
                icon.addLoreLine(Component.text().append(Locale.getComponent("Name.Hunger", admin)).append(Component.text(": ")).append(Component.text(Math.round(mypet.getSaturation())).color(NamedTextColor.GOLD)).build());
                if (mypet.getRespawnTime() > 0) {
                    icon.addLoreLine(Component.text().append(Locale.getComponent("Name.Respawntime", admin)).append(Component.text(": ")).append(Component.text(mypet.getRespawnTime() + "sec").color(NamedTextColor.GOLD)).build());
                } else {
                    icon.addLoreLine(Component.text().append(Locale.getComponent("Name.HP", admin)).append(Component.text(": ")).append(Component.text(String.format("%1.2f", mypet.getHealth())).color(NamedTextColor.GOLD)).build());
                }
                int level = mypet.getLevel();
                if (level > 0) {
                    icon.addLoreLine(Component.text().append(Locale.getComponent("Name.Level", admin)).append(Component.text(": ")).append(Component.text(level).color(NamedTextColor.GOLD)).build());
                } else {
                    icon.addLoreLine(Component.text().append(Locale.getComponent("Name.Exp", admin)).append(Component.text(": ")).append(Component.text(String.format("%1.2f", mypet.getExp())).color(NamedTextColor.GOLD)).build());
                }
                icon.addLoreLine(Component.text().append(Locale.getComponent("Name.Type", admin)).append(Component.text(": ")).append(Locale.getComponent("Name." + mypet.getPetType().name(), admin).color(NamedTextColor.GOLD)).build());
                icon.addLoreLine(Component.text().append(Locale.getComponent("Name.Skilltree", admin)).append(Component.text(": ")).append(Util.SANITIZED_MINIMESSAGE.deserialize(mypet.getSkilltree() != null ? mypet.getSkilltree().getDisplayName() : "-").color(NamedTextColor.GOLD)).build());

                icon.setTitle(mypet.getDisplayName());
                Optional<EggIconService> egg = MyPetApi.getServiceManager().getService(EggIconService.class);
                egg.ifPresent(service -> service.updateIcon(mypet.getPetType(), icon));

                int pos = menu.addOption(icon);
                petSlotList.put(pos, mypet);
            }

            if (previousPage != page) {
                menu.setOption(45, new IconMenuItem()
                        .setMaterial(Material.OAK_SIGN)
                        .setTitle(Component.text(previousPage + " ≪≪"))
                );
            }

            if (previousPage != page) {
                menu.setOption(53, new IconMenuItem()
                        .setMaterial(Material.OAK_SIGN)
                        .setTitle(Component.text("≫≫ " + nextPage))
                );
            }

            menu.open(admin);
        }
    }
}