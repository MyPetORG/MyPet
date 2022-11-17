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
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetBukkitPart;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@PluginHookName("ProtocolLib")
public class ProtocolLibHook implements PluginHook {

    protected boolean checkTemporaryPlayers = false;
    private Set<Player> tempBlockedPlayers = new HashSet<>();

    @Override
    public boolean onEnable() {
        try {
        	if(MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.17") < 0) {
        		//This is async
        		registerEnderDragonInteractionFix();
        	} else {
                //This is not - 1.17+ does NOT like async stuff
                registerSyncEnderDragonInteractionFix();
            }

            checkTemporaryPlayers = ReflectionUtil.getMethod(PacketEvent.class, "isPlayerTemporary") != null;

            // reverse dragon facing direction
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                registerEnderDragonRotationFix19();
            } else {
                registerEnderDragonRotationFixLegacy();
            }
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

    private void registerEnderDragonInteractionFix() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(MyPetApi.getPlugin(), PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {

                if ((checkTemporaryPlayers && event.isPlayerTemporary()) || event.isCancelled()) {
                    return;
                }
                PacketContainer packet = event.getPacket();
                if (packet.getType() == PacketType.Play.Client.USE_ENTITY) {

                    int id = packet.getIntegers().read(0);

                    Entity entity = null;
                    try {
                        entity = packet.getEntityModifier(event).readSafely(0);
                    } catch (RuntimeException ignored) {
                    }
                    if (entity == null && event.getPlayer() != null) {
                        entity = MyPetApi.getPlatformHelper().getEntity(id, event.getPlayer().getWorld());
                    }
                    if (entity instanceof MyPetBukkitPart) {
                        entity = ((MyPetBukkitPart) entity).getPetOwner();
                        packet.getIntegers().write(0, entity.getEntityId());
                    }
                }
            }
        });
    }

    private void registerSyncEnderDragonInteractionFix() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(MyPetApi.getPlugin(), PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {

                if ((checkTemporaryPlayers && event.isPlayerTemporary()) || event.isCancelled()) {
                    return;
                }

                //Prevent click-spamming causing Network-Issues for entire server. Basically cheap rate-limiting
                if (tempBlockedPlayers.contains(event.getPlayer())) {
                    return;
                } else {
                    tempBlockedPlayers.add(event.getPlayer());
                    //Register Rate-Limit-Clear-Task
                    Bukkit.getScheduler().runTaskLaterAsynchronously(MyPetApi.getPlugin(), () -> {
                        tempBlockedPlayers.remove(event.getPlayer());
                    }, 2L);
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
                            if (entity instanceof MyPetBukkitPart) {
                                entity = ((MyPetBukkitPart) entity).getPetOwner();
                            }
                            return entity;
                        });
                        if(ent != null) {
                            packet.getIntegers().write(0, ent.getEntityId());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private <T> T ensureMainThread(Supplier<T> supplier) throws ExecutionException, InterruptedException {
        if(Bukkit.isPrimaryThread()) {
            return supplier.get();
        } else {
            return Bukkit.getServer().getScheduler().callSyncMethod(MyPetApi.getPlugin(), supplier::get).get();
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

    private void registerEnderDragonRotationFix19() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(MyPetApi.getPlugin(), getFixedPackets()) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if ((checkTemporaryPlayers && event.isPlayerTemporary()) || event.isCancelled()) {
                            return;
                        }

                        PacketContainer packet = event.getPacket();
                        int id = packet.getIntegers().read(0);

                        Entity entity = null;
                        if(MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.17") < 0) {
                            try {
                               entity = packet.getEntityModifier(event).readSafely(0);
                            } catch (RuntimeException ignored) {
                            }
                        }
                        if (entity == null) {
                            if(MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.17") >= 0) { //1.17+ does not like async
                                try {
                                    entity = ensureMainThread(() -> MyPetApi.getPlatformHelper().getEntity(id, event.getPlayer().getWorld()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                entity = MyPetApi.getPlatformHelper().getEntity(id, event.getPlayer().getWorld());
                            }
                        }

                        if (entity instanceof MyPetBukkitEntity && ((MyPetBukkitEntity) entity).getPetType() == MyPetType.EnderDragon) {
                            byte angle = packet.getBytes().read(0);
                            angle += Byte.MAX_VALUE;
                            packet.getBytes().write(0, angle);
                        }
                    }
                });
    }

    private void registerEnderDragonRotationFixLegacy() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(MyPetApi.getPlugin(), getFixedPackets()) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if ((checkTemporaryPlayers && event.isPlayerTemporary()) || event.isCancelled()) {
                            return;
                        }

                        PacketContainer packet = event.getPacket();

                        final Entity entity = packet.getEntityModifier(event).readSafely(0);

                        if (entity instanceof MyPetBukkitEntity && ((MyPetBukkitEntity) entity).getPetType() == MyPetType.EnderDragon) {

                            switch (packet.getType().name()) {
                                case "ENTITY_TELEPORT":
                                case "ENTITY_HEAD_ROTATION": {
                                    byte angle = packet.getBytes().read(0);
                                    angle += Byte.MAX_VALUE;
                                    packet.getBytes().write(0, angle);
                                    break;
                                }
                                case "ENTITY_LOOK":
                                case "ENTITY_MOVE_LOOK":
                                case "REL_ENTITY_MOVE_LOOK":
                                case "VEHICLE_MOVE":
                                    byte angle = packet.getBytes().read(3);
                                    angle += Byte.MAX_VALUE;
                                    packet.getBytes().write(3, angle);
                                    break;
                            }
                        }
                    }
                });
    }
}