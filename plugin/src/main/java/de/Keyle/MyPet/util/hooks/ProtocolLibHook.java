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

package de.Keyle.MyPet.util.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@PluginHookName("ProtocolLib")
public class ProtocolLibHook implements PluginHook {

    private Set<Player> tempBlockedPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public boolean onEnable() {
        try {
            registerSyncEnderDragonInteractionFix();
            registerEnderDragonRotationFix();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public void onDisable() {
        try {
            if (ProtocolLibrary.getProtocolManager() != null) {
                ProtocolLibrary.getProtocolManager().removePacketListeners(MyPetApi.getPlugin());
            }
        } catch (Exception ignored) {
        }
    }

    private void registerSyncEnderDragonInteractionFix() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(MyPetApi.getPlugin(), PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {

                if (event.isPlayerTemporary() || event.isCancelled()) {
                    return;
                }

                //Prevent click-spamming causing Network-Issues for entire server. Basically cheap rate-limiting
                if (tempBlockedPlayers.contains(event.getPlayer())) {
                    return;
                } else {
                    tempBlockedPlayers.add(event.getPlayer());
                    //Register Rate-Limit-Clear-Task
                    Bukkit.getScheduler().runTaskLaterAsynchronously(MyPetApi.getPlugin(), () -> tempBlockedPlayers.remove(event.getPlayer()), 2L);
                }

                PacketContainer packet = event.getPacket();
                if (packet.getType() == PacketType.Play.Client.USE_ENTITY) {
                    try {
                        Entity ent = ensureMainThread(() -> {
                            int id = packet.getIntegers().read(0);

                            Entity entity = null;
                            try {
                                entity = packet.getEntityModifier(event).readSafely(0);
                            } catch (RuntimeException ignored) {
                            }
                            if (entity == null && event.getPlayer() != null) {
                                entity = MyPetApi.getPlatformHelper().getEntity(id, event.getPlayer().getWorld());
                            }
                            return entity;
                        });
                        if (ent != null) {
                            packet.getIntegers().write(0, ent.getEntityId());
                        }
                    } catch (TimeoutException e) {
                        // Assume the main thread is blocked and should free this netty thread.
                    } catch (Exception e) {
                        ErrorUtil.reportWarning("Third-party plugin integration failed", e);
                    }
                }
            }
        });
    }

    private <T> T ensureMainThread(Supplier<T> supplier) throws ExecutionException, InterruptedException, TimeoutException {
        if (Bukkit.isPrimaryThread()) {
            return supplier.get();
        } else {
            return Bukkit.getServer().getScheduler().callSyncMethod(MyPetApi.getPlugin(), supplier::get)
                    .get(100, TimeUnit.MILLISECONDS);
        }
    }

    protected List<PacketType> getFixedPackets() {
        List<PacketType> types = new ArrayList<>();
        for (PacketType pt : PacketType.Play.Server.getInstance().values()) {
            switch (pt.name()) {
                case "ENTITY_LOOK":
                case "ENTITY_MOVE_LOOK":
                case "REL_ENTITY_MOVE_LOOK":
                case "ENTITY_HEAD_ROTATION":
                case "ENTITY_TELEPORT":
                    if (pt.isSupported()) {
                        types.add(pt);
                    }
            }
        }
        return types;
    }

    private void registerEnderDragonRotationFix() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(MyPetApi.getPlugin(), getFixedPackets()) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.isPlayerTemporary() || event.isCancelled()) {
                            return;
                        }

                        PacketContainer packet = event.getPacket();
                        int id = packet.getIntegers().read(0);

                        Entity entity = null;
                        try {
                            entity = ensureMainThread(() -> MyPetApi.getPlatformHelper().getEntity(id, event.getPlayer().getWorld()));
                        } catch (TimeoutException e) {
                            // Assume the main thread is blocked and should free this netty thread.
                            return;
                        } catch (Exception e) {
                            ErrorUtil.reportWarning("Third-party plugin integration failed", e);
                        }

                        if (PetEntityMarker.isMarked(entity) && MyPetApi.getMyPetManager().getMyPetFromEntity(entity).getPetType().equals(MyPetType.byName("EnderDragon"))) {
                            byte angle = packet.getBytes().read(0);
                            angle += Byte.MAX_VALUE;
                            packet.getBytes().write(0, angle);
                        }
                    }
                });
    }
}
