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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.MinecraftKey;
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
 * ProtocolLib-backed implementation of {@link SoundPacketAdapter}.
 * Handles both ENTITY_SOUND (entity-tied, resolves by id) and
 * NAMED_SOUND_EFFECT (positional, resolves by spatial proximity).
 * Activated by {@code ServiceManager} when the {@code ProtocolLib}
 * plugin is installed; selected by {@code PetSoundService} as the
 * fallback adapter when PacketEvents is not present.
 */
@ServiceName("ProtocolLib")
@RequiresPlugin("ProtocolLib")
@Load(Load.State.Hooks)
public final class ProtocolLibSoundHook implements ServiceContainer, SoundPacketAdapter {

    private PacketAdapter listener;
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
                    "Invalid ProtocolLib.Volume-Mode '" + raw + "' in hooks-config.yml; falling back to per-owner.");
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
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        listener = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.ENTITY_SOUND,
                PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    if (event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND) {
                        handleEntitySound(event, interceptor);
                    } else if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                        handlePositionalSound(event, interceptor);
                    }
                } catch (Throwable t) {
                    MyPetApi.getLogger().log(Level.WARNING,
                            "ProtocolLibSoundHook listener threw; packet passed through unchanged", t);
                }
            }
        };
        manager.addPacketListener(listener);
    }

    private static void handleEntitySound(PacketEvent event, SoundPacketInterceptor interceptor) {
        PacketContainer packet = event.getPacket();
        Pet pet = PetSoundRegistry.find(packet.getIntegers().read(0));
        if (pet == null) return;
        float currentVolume = packet.getFloat().read(0);
        float currentPitch = packet.getFloat().read(1);
        SoundPacketContext ctx = new SoundPacketContext(
                event.getPlayer(), pet, extractSoundKey(packet), currentVolume, currentPitch);
        applyMultiplier(event, ctx, currentVolume, interceptor);
    }

    private static void handlePositionalSound(PacketEvent event, SoundPacketInterceptor interceptor) {
        Player recipient = event.getPlayer();
        World world = recipient != null ? recipient.getWorld() : null;
        if (world == null) return;
        PacketContainer packet = event.getPacket();
        // NAMED_SOUND_EFFECT packet layout: X, Y, Z (int, *8), volume, pitch, then long seed.
        double x = packet.getIntegers().read(0) / 8.0;
        double y = packet.getIntegers().read(1) / 8.0;
        double z = packet.getIntegers().read(2) / 8.0;
        Pet pet = PetSoundRegistry.findAtPosition(world, x, y, z);
        if (pet == null) return;
        float currentVolume = packet.getFloat().read(0);
        float currentPitch = packet.getFloat().read(1);
        SoundPacketContext ctx = new SoundPacketContext(
                recipient, pet, extractSoundKey(packet), currentVolume, currentPitch);
        applyMultiplier(event, ctx, currentVolume, interceptor);
    }

    /**
     * Sound key extraction across MC versions. Modern packets (1.20.5+) wrap
     * the sound as a {@code Holder<SoundEvent>} accessible via
     * {@code getSoundEffects()}. Older packets used a raw MinecraftKey via
     * {@code getMinecraftKeys()}. Either modifier may be empty on a given
     * version — both reads are wrapped in try/catch and we fall back to null
     * (only used for diagnostic logging, not for policy decisions).
     */
    private static Key extractSoundKey(PacketContainer packet) {
        try {
            org.bukkit.Sound sound = packet.getSoundEffects().read(0);
            if (sound != null) return Key.key(sound.getKey().toString());
        } catch (Throwable ignored) {
            // Modifier had zero fields or wrong type for this version — try legacy below.
        }
        try {
            MinecraftKey mcKey = packet.getMinecraftKeys().read(0);
            if (mcKey != null) return Key.key(mcKey.getFullKey());
        } catch (Throwable ignored) {
            // Both modifiers came up empty; key stays null.
        }
        return null;
    }

    private static void applyMultiplier(PacketEvent event,
                                        SoundPacketContext ctx,
                                        float originalVolume,
                                        SoundPacketInterceptor interceptor) {
        float multiplier = interceptor.computeMultiplier(ctx);
        if (multiplier == 1f) return;
        if (multiplier == 0f) {
            event.setCancelled(true);
            return;
        }
        event.getPacket().getFloat().write(0, originalVolume * multiplier);
    }

    @Override
    public synchronized void unregister() {
        if (listener == null) return;
        ProtocolLibrary.getProtocolManager().removePacketListener(listener);
        listener = null;
    }

    @Override
    public String name() {
        return "ProtocolLib";
    }
}
