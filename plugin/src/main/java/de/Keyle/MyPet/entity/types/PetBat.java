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
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ShopInfo
@DefaultInfo(food = {Material.SPIDER_EYE}, flySpeed = 1.5419D)
public class PetBat extends PetImpl implements PetFlyingEntity {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Bat", "CanFly", true);

    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "Bat",
            SitFlightFreezer::startForPet,
            SitFlightFreezer::stopForPet
    );

    public PetBat(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public double getYSpawnOffset() {
        return 1;
    }

    /**
     * Silences the vanilla Bat flight while the pet is sitting: a Bat wanders
     * via {@code Bat#customServerAiStep} (not a Paper goal, so goal-stripping
     * misses it), which otherwise flies a "stay" bat off like a wild one.
     * Toggles the mob's AI off while sitting (it settles to the ground) and
     * back on to follow. Scheduling mirrors {@link PetWither.AutonomousAttackSuppressor}.
     */
    public static final class SitFlightFreezer {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        private SitFlightFreezer() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Bat bat)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            // 5-tick poll: the sit flag changes rarely and, unlike the brain-race
            // suppressors on other species, this needn't outrun vanilla writers every tick.
            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (bat.isDead()) return;
                    boolean freeze = pet.isSitting();
                    // hasAI() == freeze means the current state is the opposite
                    // of what we want, so flip it; otherwise leave it untouched.
                    if (bat.hasAI() == freeze) {
                        bat.setAI(!freeze);
                    }
                } catch (Throwable ignored) {
                }
            }, null, 1L, 5L);
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
    }
}
