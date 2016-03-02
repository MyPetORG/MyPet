/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
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
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class ProtocolLib {
    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("ProtocolLib")) {
            try {
                // reverse dragon facing direction
                if (Bukkit.getVersion().contains("1.9")) {
                    registerEnderDragonFix_1_9();
                } else {
                    registerEnderDragonFix();
                }

                MyPetApi.getLogger().info("ProtocolLib hook activated.");

                active = true;
            } catch (Exception e) {
                active = false;
            }
        }
    }

    public static boolean isActive() {
        return active;
    }

    private static void registerEnderDragonFix_1_9() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(MyPetApi.getPlugin(), PacketType.Play.Server.ENTITY_LOOK, PacketType.Play.Server.ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_TELEPORT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();

                        final Entity entity = packet.getEntityModifier(event).readSafely(0);

                        // Now - are we dealing with an invisible slime?
                        if (entity != null && entity instanceof MyPetBukkitEntity && ((MyPetBukkitEntity) entity).getPetType() == MyPetType.EnderDragon) {

                            if (packet.getType() == PacketType.Play.Server.ENTITY_LOOK) {
                                //MyPetLogger.write("ENTITY_LOOK: " + packet.getBytes().getValues());

                                byte angle = packet.getBytes().read(0);
                                angle += Byte.MAX_VALUE;
                                packet.getBytes().write(0, angle);
                            } else if (packet.getType() == PacketType.Play.Server.ENTITY_MOVE_LOOK) {
                                //MyPetLogger.write("ENTITY_MOVE_LOOK: " + packet.getBytes().getValues());

                                byte angle = packet.getBytes().read(0);
                                angle += Byte.MAX_VALUE;
                                packet.getBytes().write(0, angle);
                            } else if (packet.getType() == PacketType.Play.Server.ENTITY_TELEPORT) {
                                //MyPetLogger.write("ENTITY_TELEPORT: " + packet.getBytes().getValues());

                                byte angle = packet.getBytes().read(1);
                                angle += Byte.MAX_VALUE;
                                packet.getBytes().write(1, angle);
                            }
                        }
                    }
                });
    }

    private static void registerEnderDragonFix() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(MyPetApi.getPlugin(), PacketType.Play.Server.ENTITY_LOOK, PacketType.Play.Server.ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_TELEPORT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();

                        final Entity entity = packet.getEntityModifier(event).readSafely(0);

                        // Now - are we dealing with an invisible slime?
                        if (entity != null && entity instanceof MyPetBukkitEntity && ((MyPetBukkitEntity) entity).getPetType() == MyPetType.EnderDragon) {

                            if (packet.getType() == PacketType.Play.Server.ENTITY_LOOK) {
                                //MyPetLogger.write("ENTITY_LOOK: " + packet.getBytes().getValues());

                                byte angle = packet.getBytes().read(3);
                                angle += Byte.MAX_VALUE;
                                packet.getBytes().write(3, angle);
                            } else if (packet.getType() == PacketType.Play.Server.ENTITY_MOVE_LOOK) {
                                //MyPetLogger.write("ENTITY_MOVE_LOOK: " + packet.getBytes().getValues());

                                byte angle = packet.getBytes().read(3);
                                angle += Byte.MAX_VALUE;
                                packet.getBytes().write(3, angle);
                            } else if (packet.getType() == PacketType.Play.Server.ENTITY_TELEPORT) {
                                //MyPetLogger.write("ENTITY_TELEPORT: " + packet.getBytes().getValues());

                                byte angle = packet.getBytes().read(1);
                                angle += Byte.MAX_VALUE;
                                packet.getBytes().write(1, angle);
                            }
                        }
                    }
                });
    }

    public static void disable() {
        if (active) {
            ProtocolLibrary.getProtocolManager().removePacketListeners(MyPetApi.getPlugin());
        }
    }
}