package de.Keyle.MyPet.entity.ride;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skills.Ride;
import org.bukkit.Input;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives a pet's velocity from its rider's WASD/jump/sneak input when the Ride
 * skill is active.
 *
 * <p>Runs as a single per-tick scheduler iterating every active pet with a
 * rider. Reads input via {@code Player.getCurrentInput()}.
 * Handles fly fuel depletion and regeneration.
 */
public class RideSkillFlightController extends BukkitRunnable {

    /** Fuel remaining (ticks) per pet UUID. */
    private final Map<UUID, Double> fuelByPet = new HashMap<>();

    public static void start(Plugin plugin) {
        new RideSkillFlightController().runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void run() {
        for (MyPet pet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            try {
                tickPet(pet);
            } catch (Throwable t) {
                // Swallow to protect the scheduler from one bad pet
            }
        }
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

        UUID petId = mob.getUniqueId();
        double fuelTicks = fuelByPet.getOrDefault(petId, flyLimitSeconds * 20.0);

        double worldY = mob.getVelocity().getY();

        if (input.isJump()) {
            if (mob.isOnGround() || mob.isInWater() || mob.isInLava()) {
                double jumpHeight = rideSkill.getJumpHeight() != null && rideSkill.getJumpHeight().getValue() != null
                        ? rideSkill.getJumpHeight().getValue().doubleValue() : 0.5;
                worldY = Math.max(0.42, jumpHeight * 0.2);
            } else if (canFly && fuelTicks > 0) {
                worldY = 0.35;
                fuelTicks = Math.max(0, fuelTicks - 1);
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

        fuelByPet.put(petId, fuelTicks);
        mob.setVelocity(new Vector(worldX, worldY, worldZ));
    }
}
