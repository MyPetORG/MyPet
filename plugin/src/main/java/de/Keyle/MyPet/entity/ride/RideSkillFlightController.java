package de.Keyle.MyPet.entity.ride;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skills.Ride;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Input;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives a pet's velocity from its rider's WASD/jump/sneak input when the Ride
 * skill is active.
 *
 * <p>Per-pet scheduling: one task per pet, registered on spawn and cancelled on
 * despawn. Fuel state is held in the controller instance so each pet gets its
 * own fuel value naturally (no more shared map with leaked entries).
 */
public class RideSkillFlightController {

    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private static final Map<UUID, RideSkillFlightController> controllers = new ConcurrentHashMap<>();

    /** Fuel remaining (ticks) for this pet. */
    private double fuelTicks = -1;

    public static void startForPet(MyPet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        Plugin plugin = MyPetApi.getPlugin();
        UUID key = pet.getUUID();
        stopForPet(pet);
        RideSkillFlightController controller = new RideSkillFlightController();
        controllers.put(key, controller);
        ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
            try {
                controller.tickPet(pet);
            } catch (Throwable ignored) {
            }
        }, null, 1L, 1L);
        if (task != null) {
            tasks.put(key, task);
        }
    }

    public static void stopForPet(MyPet pet) {
        UUID key = pet.getUUID();
        ScheduledTask task = tasks.remove(key);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        controllers.remove(key);
    }

    private void tickPet(MyPet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.isDead()) return;
        if (mob.getPassengers().isEmpty()) return;

        Entity passenger = mob.getPassengers().get(0);
        if (!(passenger instanceof Player rider)) return;

        if (pet.getOwner() == null || pet.getOwner().getPlayer() == null
                || !rider.getUniqueId().equals(pet.getOwner().getPlayer().getUniqueId())) {
            return;
        }

        Ride rideSkill;
        try {
            rideSkill = pet.getSkills().get(Ride.class);
        } catch (Throwable t) {
            return;
        }
        if (rideSkill == null) return;
        if (rideSkill.getActive() == null || rideSkill.getActive().getValue() == null
                || !rideSkill.getActive().getValue()) {
            return;
        }

        Input input;
        try {
            input = rider.getCurrentInput();
        } catch (Throwable t) {
            return;
        }
        if (input == null) return;

        float yaw = rider.getLocation().getYaw();
        mob.setRotation(yaw, 0);

        double baseSpeed = 0.22;
        int speedIncrease = rideSkill.getSpeedIncrease() != null && rideSkill.getSpeedIncrease().getValue() != null
                ? rideSkill.getSpeedIncrease().getValue() : 0;
        double speed = baseSpeed * (1.0 + speedIncrease / 100.0);

        double fx = 0;
        double fz = 0;
        if (input.isForward()) fx += 1;
        if (input.isBackward()) fx -= 0.5;
        if (input.isLeft()) fz -= 0.5;
        if (input.isRight()) fz += 0.5;

        double radYaw = Math.toRadians(yaw);
        double worldX = -Math.sin(radYaw) * fx - Math.cos(radYaw) * fz;
        double worldZ = Math.cos(radYaw) * fx - Math.sin(radYaw) * fz;
        worldX *= speed;
        worldZ *= speed;

        boolean canFly = rideSkill.getCanFly() != null && Boolean.TRUE.equals(rideSkill.getCanFly().getValue());
        double flyLimitSeconds = rideSkill.getFlyLimit() != null && rideSkill.getFlyLimit().getValue() != null
                ? rideSkill.getFlyLimit().getValue().doubleValue() : 0;
        double flyRegen = rideSkill.getFlyRegenRate() != null && rideSkill.getFlyRegenRate().getValue() != null
                ? rideSkill.getFlyRegenRate().getValue().doubleValue() : 0;

        if (fuelTicks < 0) {
            fuelTicks = flyLimitSeconds * 20.0;
        }

        double worldY = mob.getVelocity().getY();

        boolean unlimitedFuel = flyLimitSeconds <= 0;
        if (input.isJump()) {
            if (mob.isOnGround() || mob.isInWater() || mob.isInLava()) {
                double jumpHeight = rideSkill.getJumpHeight() != null && rideSkill.getJumpHeight().getValue() != null
                        ? rideSkill.getJumpHeight().getValue().doubleValue() : 0.5;
                worldY = Math.max(0.42, jumpHeight * 0.2);
            } else if (canFly && (unlimitedFuel || fuelTicks > 0)) {
                worldY = 0.35;
                if (!unlimitedFuel) {
                    fuelTicks = Math.max(0, fuelTicks - 1);
                }
            }
        } else {
            if (input.isSneak() && !mob.isOnGround()) {
                worldY = Math.min(worldY, -0.2);
            }
            if (flyLimitSeconds > 0) {
                double maxFuel = flyLimitSeconds * 20.0;
                double regenPerTick = flyRegen > 0 ? (flyRegen / 20.0) : 0.5;
                fuelTicks = Math.min(maxFuel, fuelTicks + regenPerTick);
            }
        }

        mob.setVelocity(new Vector(worldX, worldY, worldZ));
    }
}
