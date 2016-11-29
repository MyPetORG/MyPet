/*
 * This file is part of mypet-plugin_main
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-plugin_main is licensed under the GNU Lesser General Public License.
 *
 * mypet-plugin_main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-plugin_main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.event.MyPetPlayerJoinEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.hooks.ResourcePackApiHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// TODO convert to service
public class ResourcePackManager implements Listener {
    public static final String DOWNLOAD_LINK = "http://dl.keyle.de/mypet/MyPet.zip";
    public static final String DOWNLOAD_LINK_V3 = "http://dl.keyle.de/mypet/MyPet_v3.zip";
    protected static ResourcePackManager instance = null;

    protected Set<UUID> acceptedPlayers = new HashSet<>();

    public ResourcePackManager() {
        instance = this;

        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.8.8") >= 0) {
            activateBukkitListener();
        }
    }

    public void installResourcePack(Player player) {
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.11") >= 0) {
            player.setResourcePack(DOWNLOAD_LINK_V3);
        } else {
            player.setResourcePack(DOWNLOAD_LINK);
        }
    }

    /**
     * Returns if a player wants to use the MyPet resource pack
     *
     * @param player checked player
     * @return all members of the party
     */
    public boolean usesResourcePack(Player player) {
        return acceptedPlayers.contains(player.getUniqueId());
    }

    public void activatePlayer(Player player) {
        acceptedPlayers.add(player.getUniqueId());
    }

    private void activateBukkitListener() {
        Listener listener = new Listener() {
            @EventHandler
            public void on(PlayerResourcePackStatusEvent e) {
                switch (e.getStatus()) {
                    case SUCCESSFULLY_LOADED:
                        acceptedPlayers.add(e.getPlayer().getUniqueId());
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
        };

        Bukkit.getPluginManager().registerEvents(listener, MyPetApi.getPlugin());
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        acceptedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void on(final MyPetPlayerJoinEvent e) {
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.8.8") < 0) {
            if (!MyPetApi.getPluginHookManager().isHookActive(ResourcePackApiHook.class)) {
                return;
            }
        }

        if (e.getPlayer().isUsingResourcePack()) {
            if (!Permissions.has(e.getPlayer(), "MyPet.command.options.resourcepack")) {
                e.getPlayer().sendMessage(Translation.getString("Message.No.Allowed", e.getPlayer()));
                e.getPlayer().setUsesResourcePack(false);
                return;
            }
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
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.8.8") < 0) {
            if (!MyPetApi.getPluginHookManager().isHookActive(ResourcePackApiHook.class)) {
                return;
            }
        }
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

    public static ResourcePackManager get() {
        return instance;
    }
}