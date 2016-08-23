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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.PluginHook;
import de.Keyle.MyPet.util.ResourcePackManager;
import de.inventivegames.rpapi.ResourcePackStatusEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

@PluginHookName("ResourcePackApi")
public class ResourcePackApiHook extends PluginHook {

    @Override
    public boolean onEnable() {
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.8.8") < 0) {
            Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void on(ResourcePackStatusEvent e) {
        if (e.getHash() == null || e.getHash().equals("mypet_resourcepack")) {
            switch (e.getStatus()) {
                case SUCCESSFULLY_LOADED:
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(e.getPlayer())) {
                        e.getPlayer().sendMessage(Translation.getString("Message.Command.Options.ResourcePack.Success", e.getPlayer()));
                    }
                    ResourcePackManager.get().activatePlayer(e.getPlayer());
                    break;
                case FAILED_DOWNLOAD:
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(e.getPlayer())) {
                        MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(e.getPlayer());
                        myPetPlayer.setUsesResourcePack(false);
                        myPetPlayer.sendMessage(Translation.getString("Message.Command.Options.ResourcePack.DownloadFailed", myPetPlayer));
                    }
                    break;
                case DECLINED:
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(e.getPlayer())) {
                        MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(e.getPlayer());
                        myPetPlayer.setUsesResourcePack(false);
                        myPetPlayer.sendMessage(Translation.getString("Message.Command.Options.ResourcePack.Declined", myPetPlayer));
                    }
                    break;
            }
        }
    }
}