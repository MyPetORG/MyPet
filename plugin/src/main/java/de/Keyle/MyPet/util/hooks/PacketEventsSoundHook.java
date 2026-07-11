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

package de.Keyle.MyPet.util.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.util.sound.PetSoundRegistry;
import de.Keyle.MyPet.util.sound.PetSoundService;
import de.Keyle.MyPet.util.sound.SoundPacketAdapter;
import de.Keyle.MyPet.util.sound.SoundPacketContext;
import de.Keyle.MyPet.util.sound.SoundPacketInterceptor;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.logging.Level;

/**
 * PacketEvents-backed implementation of {@link SoundPacketAdapter}.
 * Handles both ENTITY_SOUND_EFFECT (entity-tied, resolves by id) and
 * SOUND_EFFECT (positional, resolves by spatial proximity). Activated
 * by {@code ServiceManager} when the {@code packetevents} plugin is
 * installed; selected by {@code PetSoundService} as the preferred
 * adapter when present.
 */
@ServiceName("packetevents")
@RequiresPlugin("packetevents")
@Load(Load.State.Hooks)
public final class PacketEventsSoundHook implements ServiceContainer, SoundPacketAdapter {

    private PacketListenerAbstract listener;
    private boolean volumeControl = true;
    private PetSoundService.Mode mode = PetSoundService.Mode.PER_OWNER;

    @Override
    public void loadConfig(ConfigurationSection config) {
        config.addDefault("Volume-Control", true);
        config.addDefault("Volume-Mode", "per-owner");
        volumeControl = config.getBoolean("Volume-Control", true);
        mode = parseMode(config.getString("Volume-Mode", "per-owner"));
    }

    private PetSoundService.Mode parseMode(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return PetSoundService.Mode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            MyPetApi.getLogger().warning(
                    "Invalid packetevents.Volume-Mode '" + raw + "' in hooks-config.yml; falling back to per-owner.");
            return PetSoundService.Mode.PER_OWNER;
        }
    }

    @Override
    public boolean isAvailable() {
        return volumeControl;
    }

    @Override
    public PetSoundService.Mode getMode() {
        return mode;
    }

    @Override
    public synchronized void register(Plugin plugin, SoundPacketInterceptor interceptor) {
        if (listener != null) return;
        listener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                // With no pets spawned, skip before any wrapper decode — this
                // runs on the Netty thread for every outbound sound packet.
                if (PetSoundRegistry.size() == 0) return;
                PacketTypeCommon type = event.getPacketType();
                try {
                    if (type == PacketType.Play.Server.ENTITY_SOUND_EFFECT) {
                        handleEntitySound(event, interceptor);
                    } else if (type == PacketType.Play.Server.SOUND_EFFECT) {
                        handlePositionalSound(event, interceptor);
                    }
                } catch (Throwable t) {
                    MyPetApi.getLogger().log(Level.WARNING,
                            "PacketEventsSoundHook listener threw; packet passed through unchanged", t);
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }

    private static void handleEntitySound(PacketSendEvent event, SoundPacketInterceptor interceptor) {
        WrapperPlayServerEntitySoundEffect wrapper = new WrapperPlayServerEntitySoundEffect(event);
        Pet pet = PetSoundRegistry.find(wrapper.getEntityId());
        if (pet == null) return;
        Player bukkitPlayer = (event.getPlayer() instanceof Player p) ? p : null;
        SoundPacketContext ctx = new SoundPacketContext(
                bukkitPlayer, pet, keyOf(wrapper.getSound()),
                wrapper.getVolume(), wrapper.getPitch());
        applyMultiplier(event, wrapper, ctx, wrapper.getVolume(), interceptor);
    }

    private static void handlePositionalSound(PacketSendEvent event, SoundPacketInterceptor interceptor) {
        Player recipient = (event.getPlayer() instanceof Player p) ? p : null;
        World world = recipient != null ? recipient.getWorld() : null;
        if (world == null) return;
        WrapperPlayServerSoundEffect wrapper = new WrapperPlayServerSoundEffect(event);
        Vector3i pos = wrapper.getEffectPosition();
        // Packet position is stored at 1/8-block precision (Mojang convention).
        double x = pos.getX() / 8.0;
        double y = pos.getY() / 8.0;
        double z = pos.getZ() / 8.0;
        Pet pet = PetSoundRegistry.findAtPosition(world, x, y, z);
        if (pet == null) return;
        SoundPacketContext ctx = new SoundPacketContext(
                recipient, pet, keyOf(wrapper.getSound()),
                wrapper.getVolume(), wrapper.getPitch());
        applyMultiplier(event, wrapper, ctx, wrapper.getVolume(), interceptor);
    }

    private static void applyMultiplier(PacketSendEvent event,
                                        PacketWrapper<?> wrapper,
                                        SoundPacketContext ctx,
                                        float originalVolume,
                                        SoundPacketInterceptor interceptor) {
        float multiplier = interceptor.computeMultiplier(ctx);
        if (multiplier == 1f) return;
        if (multiplier == 0f) {
            event.setCancelled(true);
            return;
        }
        float newVolume = originalVolume * multiplier;
        if (wrapper instanceof WrapperPlayServerEntitySoundEffect ew) ew.setVolume(newVolume);
        else if (wrapper instanceof WrapperPlayServerSoundEffect sw) sw.setVolume(newVolume);
        // PacketWrapper(PacketSendEvent) does NOT auto-register itself as the
        // event's lastUsedWrapper, so we must do it explicitly or markForReEncode
        // has nothing to re-serialize from.
        event.setLastUsedWrapper(wrapper);
        event.markForReEncode(true);
    }

    private static Key keyOf(Sound sound) {
        if (sound == null) return null;
        ResourceLocation rl = sound.getSoundId();
        return rl == null ? null : Key.key(rl.getNamespace(), rl.getKey());
    }

    @Override
    public synchronized void unregister() {
        if (listener == null) return;
        PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        listener = null;
    }

    @Override
    public String name() {
        return "PacketEvents";
    }
}
