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

package de.Keyle.MyPet.util.sound;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.util.hooks.PacketEventsSoundHook;
import de.Keyle.MyPet.util.hooks.ProtocolLibSoundHook;
import org.bukkit.entity.Mob;

import java.util.Optional;

/**
 * Central service for the pet living-sound volume feature. Picks an
 * available packet library at OnReady, then registers a single
 * outbound ENTITY_SOUND listener that scales / cancels packets per
 * the chosen hook's {@link Mode}.
 *
 * <p>The mode is owned by each {@code SoundPacketAdapter} hook (read
 * from its own {@code Volume-Mode} key in {@code hooks-config.yml});
 * after picking, this service copies the picked hook's mode into the
 * volatile {@link #MODE} the interceptor reads on every packet.
 *
 * <p>If neither PacketEvents nor ProtocolLib is present, the setting
 * remains editable and persistent — it just has no audible effect.
 * The service logs one warning so admins know why.
 */
@ServiceName("PetSoundService")
@Load(Load.State.OnReady)
public final class PetSoundService implements ServiceContainer {

    public enum Mode {
        /** Owner's setting scales the volume for every nearby listener of THEIR pet's sounds. */
        PER_OWNER,
        /** Listener's own setting scales every pet sound THEY hear, regardless of pet ownership. */
        PER_PLAYER
    }

    public static volatile Mode MODE = Mode.PER_OWNER;

    private SoundPacketAdapter activeAdapter;

    @Override
    public boolean onEnable() {
        // Force the global lifecycle hook to register before any pet spawns.
        // (Touching the class triggers its <clinit>.)
        @SuppressWarnings("unused")
        Object touch = PetSoundLifecycleHook.GLOBAL_HOOK;

        // Cold-path: seed the registry with pets that already exist (e.g. restored
        // from the repository before this service activated). Subsequent spawns
        // come in through the lifecycle hook.
        seedRegistryWithActivePets();

        activeAdapter = pickAdapter();
        if (activeAdapter == null) {
            MyPetApi.getLogger().warning(
                    "Pet volume setting is stored but not active — "
                            + "install PacketEvents (recommended) or ProtocolLib to enable.");
            return true; // service itself is enabled; feature is just dormant
        }

        MODE = activeAdapter.getMode();
        SoundPacketInterceptor interceptor = new SoundPacketInterceptor();
        activeAdapter.register(MyPetApi.getPlugin(), interceptor);
        MyPetApi.getLogger().info(
                "Pet volume listener active (" + activeAdapter.name() + ", mode=" + MODE + ").");
        return true;
    }

    @Override
    public void onDisable() {
        if (activeAdapter != null) {
            activeAdapter.unregister();
            activeAdapter = null;
        }
        PetSoundRegistry.clear();
    }

    /**
     * Picks the first available packet adapter. PacketEvents is preferred over
     * ProtocolLib because it tracks new MC protocol versions faster, reducing
     * the chance of a feature outage on a Paper bump. Each adapter is a
     * {@code @RequiresPlugin}-gated hook that {@code ServiceManager} only
     * activates when its corresponding plugin is installed.
     */
    private SoundPacketAdapter pickAdapter() {
        Optional<PacketEventsSoundHook> pe = MyPetApi.getServiceManager().getService(PacketEventsSoundHook.class);
        if (pe.isPresent() && pe.get().isAvailable()) return pe.get();
        Optional<ProtocolLibSoundHook> pl = MyPetApi.getServiceManager().getService(ProtocolLibSoundHook.class);
        if (pl.isPresent() && pl.get().isAvailable()) return pl.get();
        return null;
    }

    private void seedRegistryWithActivePets() {
        Pet[] active = MyPetApi.getPetManager().getAllActivePets();
        for (Pet pet : active) {
            Mob mob = pet.getBukkitEntity();
            if (mob != null) {
                PetSoundRegistry.add(mob.getEntityId(), pet);
            }
        }
    }
}
