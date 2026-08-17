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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.papermc.paper.world.WeatheringCopperState;
import org.bukkit.Material;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ShopInfo(displayName = "Copper Golem")
@DefaultInfo(food = {Material.COPPER_INGOT}, leashFlags = {"UserCreated"}, flySpeed = 0.4405D)
public class PetCopperGolem extends PetImpl {

    public static final ConfigKey<Boolean> CAN_OXIDIZE = ConfigKey.bool("CopperGolem", "CanOxidize", true);


    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("oxidation", CopperGolem.class, WeatheringCopperState.class, CopperGolem::setWeatheringState),
            () -> OptionSpec.ofFlag("waxed",     CopperGolem.class,                              g -> g.setOxidizing(CopperGolem.Oxidizing.waxed()))
    );

    /** Wires {@link OxidationManager} for cap-at-WEATHERED + CanOxidize freeze. */
    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "CopperGolem",
            OxidationManager::startForPet,
            OxidationManager::stopForPet
    );

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED = ConfigKey.stringList(
            "CopperGolem", "Brain.Disabled",
            "behavior:TransportItemsBetweenContainers");

    public PetCopperGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Per-second check that caps oxidation at {@code WEATHERED} (and
     * downgrades from {@code OXIDIZED} defensively to avoid vanilla's
     * {@code turnToStatue()} entity→block swap), or freezes at the current
     * state when {@link #CAN_OXIDIZE} is disabled.
     */
    public static final class OxidationManager {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        private OxidationManager() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof CopperGolem golem)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            enforce(golem);
            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (golem.isDead()) return;
                    enforce(golem);
                } catch (Throwable ignored) {
                }
            }, null, 20L, 20L);
            if (task != null) {
                tasks.put(key, task);
            }
        }

        public static void stopForPet(Pet pet) {
            ScheduledTask task = tasks.remove(pet.getUUID());
            if (task != null) {
                try {
                    task.cancel();
                } catch (Exception ignored) {
                }
            }
        }

        private static void enforce(CopperGolem golem) {
            try {
                if (!CAN_OXIDIZE.get()) {
                    if (!(golem.getOxidizing() instanceof CopperGolem.Oxidizing.Waxed)) {
                        golem.setOxidizing(CopperGolem.Oxidizing.waxed());
                    }
                    return;
                }
                if (golem.getWeatheringState() == WeatheringCopperState.OXIDIZED) {
                    golem.setWeatheringState(WeatheringCopperState.WEATHERED);
                }
                if (golem.getWeatheringState() == WeatheringCopperState.WEATHERED
                        && !(golem.getOxidizing() instanceof CopperGolem.Oxidizing.Waxed)) {
                    golem.setOxidizing(CopperGolem.Oxidizing.waxed());
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
