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

package de.Keyle.MyPet.util.hooks.citizens;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.ShopService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;

public class ShopTrait extends Trait {

    private String shop = "";

    public ShopTrait() {
        super("mypet-shop");
    }

    public void load(DataKey key) throws NPCLoadException {
        shop = key.getString("shop", null);
    }

    public void save(DataKey key) {
        key.setString("shop", this.shop);
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    @EventHandler
    public void onRightClick(final NPCRightClickEvent npcEvent) {
        if (this.npc != npcEvent.getNPC()) {
            return;
        }

        final Player player = npcEvent.getClicker();

        if (!Permissions.has(player, "MyPet.npc.shop")) {
            player.sendMessage(Translation.getString("Message.No.Allowed", player));
            return;
        }

        List<ShopService> shopServiceList = MyPetApi.getServiceManager().getServices(ShopService.class);
        for (ShopService service : shopServiceList) {
            service.open(shop, player);
        }
    }

    @Override
    public String toString() {
        return "MyPetWalletTrait{shop: " + shop + "}";
    }
}
