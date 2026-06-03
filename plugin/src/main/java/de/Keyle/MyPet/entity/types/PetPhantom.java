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
import de.Keyle.MyPet.api.entity.PetSunSensitive;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ShopInfo
@DefaultInfo(food = {Material.ROTTEN_FLESH}, flySpeed = 1.5419D)
public class PetPhantom extends PetImpl implements PetFlyingEntity, PetSunSensitive {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Phantom", "CanFly", true);
    public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Phantom", "PreventDaylightBurn", true);


    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            PetCreationOptions.sizeSpec(Phantom.class, 64, Phantom::setSize)
    );

    /** Per-tick orbit driver — see {@link OrbitController}. */
    public static final PetLifecycleHook ORBIT_CONTROLLER_HOOK = new PetLifecycleHook(
            "Phantom",
            OrbitController::startForPet,
            OrbitController::stopForPet
    );

    public PetPhantom(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Drives Phantom flight by writing {@code Phantom.moveTargetPoint}
     * directly each tick to an orbit point above the owner. Vanilla
     * {@code PhantomMoveControl} (entity-tick, not a goal — survives
     * {@code removeAllGoals}) reads {@code moveTargetPoint} and applies
     * velocity toward it. Bypasses {@code PhantomCircleAroundAnchorGoal}
     * entirely: even retained, its {@code selectNext()} computes
     * {@code moveTargetPoint.y = anchor.y + (-4 + height)} with
     * {@code height} ∈ [-4, +5], producing sub-anchor targets that scrape
     * the ground when the anchor is at owner Y. Bukkit's {@code Phantom}
     * surface exposes only {@code setAnchorLocation}; the {@code moveTargetPoint}
     * field has no public accessor, so we reach in via NMS reflection.
     */
    public static final class OrbitController {

        /** Horizontal orbit radius in blocks. Kept well within the 16-block follow-distance teleport threshold. */
        private static final double RADIUS = 6.0;
        /** Altitude above the owner where the phantom orbits — high enough to clear the player and most overhangs. */
        private static final double ALTITUDE = 8.0;
        /** Angular step per tick in radians. 4° ⇒ full orbit in ~4.5 s (90 ticks). */
        private static final double ANGLE_STEP = Math.toRadians(4.0);

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
        private static final Map<UUID, Double> angles = new ConcurrentHashMap<>();

        // NMS reflection — fail-soft init. Same pattern as BrainAccess
        // and CubeMobMoveControlAccess, scoped here because the target
        // field is Phantom-specific and shouldn't live in shared infra.
        private static volatile boolean initialized = false;
        private static volatile boolean available = false;
        private static volatile Method getHandleMethod;
        private static volatile Field moveTargetPointField;
        private static volatile Constructor<?> vec3Constructor;

        private OrbitController() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Phantom phantom)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            // Stagger initial angle by UUID so multiple phantom pets owned by
            // the same player don't all sit at the same orbit position.
            angles.put(key, (key.hashCode() & 0xFFFF) / (double) 0xFFFF * 2.0 * Math.PI);

            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (phantom.isDead()) return;
                    MyPetPlayer owner = pet.getOwner();
                    if (owner == null) return;
                    Player ownerPlayer = owner.getPlayer();
                    if (ownerPlayer == null) return;

                    double angle = angles.compute(key, (k, v) -> (v == null ? 0.0 : v) + ANGLE_STEP);
                    Location loc = ownerPlayer.getLocation();
                    setMoveTargetPoint(phantom,
                            loc.getX() + RADIUS * Math.cos(angle),
                            loc.getY() + ALTITUDE,
                            loc.getZ() + RADIUS * Math.sin(angle));
                } catch (Throwable ignored) {
                }
            }, null, 1L, 1L);
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
            angles.remove(pet.getUUID());
        }

        private static synchronized void tryInit() {
            if (initialized) return;
            initialized = true;
            try {
                Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
                Class<?> nmsPhantom = Class.forName("net.minecraft.world.entity.monster.Phantom");
                Class<?> nmsVec3 = Class.forName("net.minecraft.world.phys.Vec3");

                getHandleMethod = craftEntity.getMethod("getHandle");
                Field f = nmsPhantom.getDeclaredField("moveTargetPoint");
                f.setAccessible(true);
                moveTargetPointField = f;
                vec3Constructor = nmsVec3.getConstructor(double.class, double.class, double.class);

                available = true;
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "PetPhantom.OrbitController: NMS reflection unavailable — Phantom pets will fly toward world spawn (Vec3.ZERO) instead of orbiting the owner. Cause: " + t);
            }
        }

        private static void setMoveTargetPoint(Phantom phantom, double x, double y, double z) {
            if (!initialized) tryInit();
            if (!available) return;
            try {
                Object handle = getHandleMethod.invoke(phantom);
                Object vec3 = vec3Constructor.newInstance(x, y, z);
                moveTargetPointField.set(handle, vec3);
            } catch (Throwable t) {
                // Don't spam logs — drop silently after a successful init.
            }
        }
    }
}
