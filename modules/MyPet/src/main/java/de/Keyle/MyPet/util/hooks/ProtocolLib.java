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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.base.Throwables;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

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

                if (MyPetApi.getCompatUtil().getInternalVersion().equals("v1_7_R4")) {
                    boolean activate = true;
                    try {
                        Class.forName("org.spigotmc.SpigotConfig");
                    } catch (Throwable throwable) {
                        activate = false;
                    }
                    if (activate) {
                        registerCompatFix_1_8();
                        MyPetApi.getLogger().info("1.8 Version hack fix activated.");
                    }
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

    private static void registerCompatFix_1_8() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(MyPetApi.getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY_LIVING, PacketType.Play.Server.ENTITY_METADATA) {

                    Method getHandleMethod = null;

                    private final EnumMap<DyeColor, Integer> convertedDyeColors = new EnumMap<DyeColor, Integer>(DyeColor.class) {
                        {
                            put(DyeColor.WHITE, 15);
                            put(DyeColor.ORANGE, 14);
                            put(DyeColor.MAGENTA, 13);
                            put(DyeColor.LIGHT_BLUE, 12);
                            put(DyeColor.YELLOW, 11);
                            put(DyeColor.LIME, 10);
                            put(DyeColor.PINK, 9);
                            put(DyeColor.GRAY, 8);
                            put(DyeColor.SILVER, 7);
                            put(DyeColor.CYAN, 6);
                            put(DyeColor.PURPLE, 5);
                            put(DyeColor.BLUE, 4);
                            put(DyeColor.BROWN, 3);
                            put(DyeColor.GREEN, 2);
                            put(DyeColor.RED, 1);
                            put(DyeColor.BLACK, 0);
                        }
                    };

                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.isCancelled()) {
                            return;
                        }

                        Player player = event.getPlayer();
                        if (!isPlayerRunningv1_8(player)) {
                            return;
                        }

                        PacketContainer newPacketContainer = event.getPacket().deepClone();
                        event.setPacket(newPacketContainer);

                        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {

                            Entity entity = newPacketContainer.getEntityModifier(event).readSafely(0);
                            if (entity != null && entity instanceof MyPetBukkitEntity) {
                                MyPetBukkitEntity petEntity = (MyPetBukkitEntity) entity;
                                List<WrappedWatchableObject> wrappedWatchableObjectList = newPacketContainer.getDataWatcherModifier().read(0).getWatchableObjects();
                                newPacketContainer.getDataWatcherModifier().write(0, new WrappedDataWatcher(fixMetadata(petEntity, wrappedWatchableObjectList)));
                            }
                        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {

                            Entity entity = newPacketContainer.getEntityModifier(event).read(0);
                            if (entity != null && entity instanceof MyPetBukkitEntity) {
                                MyPetBukkitEntity petEntity = (MyPetBukkitEntity) entity;

                                List<WrappedWatchableObject> wrappedWatchableObjectList = newPacketContainer.getWatchableCollectionModifier().read(0);
                                newPacketContainer.getWatchableCollectionModifier().write(0, fixMetadata(petEntity, wrappedWatchableObjectList));
                            }
                        }
                    }

                    private List<WrappedWatchableObject> fixMetadata(MyPetBukkitEntity petEntity, List<WrappedWatchableObject> wrappedWatchableObjectList) {
                        if (petEntity == null || wrappedWatchableObjectList == null) {
                            return wrappedWatchableObjectList;
                        }

                        if (petEntity.getMyPet() instanceof MyPetBaby && hasKey(12, wrappedWatchableObjectList)) {
                            Object object = getKeyValue(12, wrappedWatchableObjectList);
                            if (object instanceof Integer) {
                                int value = ((Number) object).intValue();
                                removeKey(12, wrappedWatchableObjectList);

                                if (petEntity.getPetType() == MyPetType.Horse) {
                                    if (value == -24000) {
                                        value = -1;
                                    }
                                }
                                wrappedWatchableObjectList.add(new WrappedWatchableObject(12, (byte) value));
                            }
                        }
                        if (petEntity.getPetType() == MyPetType.Wolf && hasKey(20, wrappedWatchableObjectList)) {
                            Object object = getKeyValue(20, wrappedWatchableObjectList);

                            if (object instanceof Byte) {
                                DyeColor color = DyeColor.getByWoolData((byte) ((Byte) object & 0xF));
                                removeKey(20, wrappedWatchableObjectList);
                                wrappedWatchableObjectList.add(new WrappedWatchableObject(20, (byte) ((this.convertedDyeColors.get(color)) & 0xF)));
                            }
                        }
                        if (petEntity.getPetType() == MyPetType.Enderman && hasKey(16, wrappedWatchableObjectList)) {
                            Object object = getKeyValue(16, wrappedWatchableObjectList);
                            if (object instanceof Byte) {
                                removeKey(16, wrappedWatchableObjectList);
                                wrappedWatchableObjectList.add(new WrappedWatchableObject(16, Short.valueOf((Byte) object)));
                            }
                        }

                        return wrappedWatchableObjectList;
                    }

                    private boolean hasKey(int key, List<WrappedWatchableObject> wrappedWatchableObjectList) {
                        for (WrappedWatchableObject next : wrappedWatchableObjectList) {
                            if (next.getIndex() == key) {
                                return true;
                            }
                        }
                        return false;
                    }

                    private Object getKeyValue(int key, List<WrappedWatchableObject> wrappedWatchableObjectList) {
                        for (WrappedWatchableObject next : wrappedWatchableObjectList) {
                            if (next.getIndex() == key) {
                                return next.getValue();
                            }
                        }
                        return null;
                    }

                    private void removeKey(int key, List<WrappedWatchableObject> wrappedWatchableObjectList) {
                        for (Iterator<WrappedWatchableObject> wrappedWatchableObjectIterator = wrappedWatchableObjectList.iterator(); wrappedWatchableObjectIterator.hasNext(); ) {
                            WrappedWatchableObject next = wrappedWatchableObjectIterator.next();
                            if (next.getIndex() == key) {
                                wrappedWatchableObjectIterator.remove();
                                break;
                            }
                        }
                    }

                    @SuppressWarnings("unchecked")
                    private boolean isPlayerRunningv1_8(Player player) {
                        try {
                            if (getHandleMethod == null) {
                                Class entityClass = Class.forName("org.bukkit.craftbukkit." + MyPetApi.getCompatUtil().getInternalVersion() + ".entity.CraftEntity");
                                getHandleMethod = entityClass.getDeclaredMethod("getHandle");
                                getHandleMethod.setAccessible(true);
                            }
                            Object nmsPlayer = getHandleMethod.invoke(player);
                            Object playerConnection = Util.getField(nmsPlayer.getClass(), "playerConnection").get(nmsPlayer);
                            Object networkManager = Util.getField(playerConnection.getClass(), "networkManager").get(playerConnection);

                            Method getVersionMethod = Util.getMethod(networkManager.getClass(), "getVersion");
                            return (Integer) getVersionMethod.invoke(networkManager) > 5;
                        } catch (Exception exception) {
                            throw Throwables.propagate(exception);
                        }
                    }
                }
        );
    }

    public static void disable() {
        if (active) {
            ProtocolLibrary.getProtocolManager().removePacketListeners(MyPetApi.getPlugin());
        }
    }
}