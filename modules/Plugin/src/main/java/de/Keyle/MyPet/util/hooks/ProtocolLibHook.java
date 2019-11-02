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
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetBukkitPart;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import org.bukkit.DyeColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@PluginHookName("ProtocolLib")
public class ProtocolLibHook implements PluginHook {

    protected boolean checkTemporaryPlayers = false;

    @Override
    public boolean onEnable() {
        try {
            registerEnderDragonInteractionFix();

            checkTemporaryPlayers = ReflectionUtil.getMethod(PacketEvent.class, "isPlayerTemporary") != null;

            // reverse dragon facing direction
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                registerEnderDragonRotationFix19();
            } else {
                registerEnderDragonRotationFixLegacy();
            }

            if (MyPetApi.getCompatUtil().getInternalVersion().equals("v1_7_R4")) {
                if (MyPetApi.getPlatformHelper().isSpigot()) {
                    registerCompatFix_1_8();
                }
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
                        try {
                            entity = packet.getEntityModifier(event).readSafely(0);
                        } catch (RuntimeException ignored) {
                        }
                        if (entity == null) {
                            entity = MyPetApi.getPlatformHelper().getEntity(id, event.getPlayer().getWorld());
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

    private void registerCompatFix_1_8() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(
                        MyPetApi.getPlugin(), ListenerPriority.HIGHEST,
                        PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                        PacketType.Play.Server.ENTITY_METADATA
                ) {
                    Class entityClass = ReflectionUtil.getClass("org.bukkit.craftbukkit." + MyPetApi.getCompatUtil().getInternalVersion() + ".entity.CraftEntity");
                    Method getHandleMethod = ReflectionUtil.getMethod(entityClass, "getHandle");

                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if ((checkTemporaryPlayers && event.isPlayerTemporary()) || event.isCancelled()) {
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
                            if (entity instanceof MyPetBukkitEntity) {
                                MyPetBukkitEntity petEntity = (MyPetBukkitEntity) entity;
                                List<WrappedWatchableObject> wrappedWatchableObjectList = newPacketContainer.getDataWatcherModifier().read(0).getWatchableObjects();
                                newPacketContainer.getDataWatcherModifier().write(0, new WrappedDataWatcher(fixMetadata(petEntity, wrappedWatchableObjectList)));
                            }
                        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {

                            Entity entity = newPacketContainer.getEntityModifier(event).read(0);
                            if (entity instanceof MyPetBukkitEntity) {
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
                                wrappedWatchableObjectList.add(new WrappedWatchableObject(20, (byte) ((15 - color.ordinal()) & 0xF)));
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

                    private boolean isPlayerRunningv1_8(Player player) {
                        try {
                            Object nmsPlayer = getHandleMethod.invoke(player);
                            Object playerConnection = ReflectionUtil.getFieldValue(nmsPlayer.getClass(), nmsPlayer, "playerConnection");
                            Object networkManager = ReflectionUtil.getFieldValue(playerConnection.getClass(), playerConnection, "networkManager");

                            Method getVersionMethod = ReflectionUtil.getMethod(networkManager.getClass(), "getVersion");
                            return (Integer) getVersionMethod.invoke(networkManager) > 5;
                        } catch (Exception exception) {
                            return false;
                        }
                    }
                }
        );
    }
}