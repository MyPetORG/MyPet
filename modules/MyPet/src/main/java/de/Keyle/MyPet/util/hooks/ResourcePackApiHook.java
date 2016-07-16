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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.event.MyPetPlayerJoinEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.rpapi.ResourcePackAPI;
import org.inventivetalent.rpapi.ResourcePackStatusEvent;

@PluginHookName("ResourcePackApi")
public class ResourcePackApiHook implements Listener, PluginHook {

    public static final String DOWNLOAD_LINK = "http://dl.keyle.de/mypet/MyPet.zip";

    @Override
    public boolean onEnable() {
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public void installResourcePack(Player player) {
        ResourcePackAPI.setResourcepack(player, DOWNLOAD_LINK, "mypet_resourcepack");
    }

    public boolean useIcons(Player player) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            return MyPetApi.getPlayerManager().getMyPetPlayer(player).isUsingResourcePack();
        }
        return Configuration.Misc.ACTIVATE_RESOURCEPACK_BY_DEFAULT;
    }

    @EventHandler
    public void on(final MyPetPlayerJoinEvent e) {
        if (e.getPlayer().isUsingResourcePack()) {
            e.getPlayer().sendMessage(Translation.getString("Message.Command.Options.ResourcePack.Prompt", e.getPlayer()));
            new BukkitRunnable() {
                @Override
                public void run() {
                    installResourcePack(e.getPlayer().getPlayer());
                }
            }.runTaskLater(MyPetApi.getPlugin(), 30L);
        }
    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        if (Configuration.Misc.ACTIVATE_RESOURCEPACK_BY_DEFAULT) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    MyPetApi.getRepository().isMyPetPlayer(event.getPlayer(), new RepositoryCallback<Boolean>() {
                        @Override
                        public void callback(final Boolean result) {
                            if (!result) {
                                installResourcePack(event.getPlayer());
                            }
                        }
                    });
                }
            }.runTaskLater(MyPetApi.getPlugin(), 30L);
        }
    }

    @EventHandler
    public void on(ResourcePackStatusEvent e) {
        if (e.getHash() == null || e.getHash().equals("mypet_resourcepack")) {
            switch (e.getStatus()) {
                case SUCCESSFULLY_LOADED:
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(e.getPlayer())) {
                        e.getPlayer().sendMessage(Translation.getString("Message.Command.Options.ResourcePack.Success", e.getPlayer()));
                    }
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