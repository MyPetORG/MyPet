package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.entity.Slime;

import java.lang.reflect.Method;

/**
 * Reflective bridge to vanilla's cube-mob {@code MoveControl} (currently
 * {@code net.minecraft.world.entity.monster.Slime$SlimeMoveControl} — Mojang has not yet
 * renamed it for the broader cube-mob category).
 *
 * <p>Vanilla {@code SlimeMoveControl.tick()} runs every tick after our goal and {@code rotlerp}s
 * the cube mob's body yaw toward its own internal {@code yRot} field. Because we strip the vanilla
 * goals ({@code SlimeRandomDirectionGoal}) that normally write that field, the slime body stays
 * locked at its spawn rotation regardless of where we call {@code setRotation()}. Writing the
 * field via this helper gives the vanilla rotlerp the correct target, so the body smoothly tracks
 * the hop direction.
 *
 * <p>This is a deliberate, surgical exception to the codebase's "no NMS reflection" stance.
 * The method has been stable for 10+ years. Mojang-mapped names are used directly because
 * Paper 1.20.5+ exposes the server jar under mojmap at runtime.
 *
 * <p><b>Implementation note:</b> uses {@link Method} + {@code setAccessible(true)} rather than
 * {@link java.lang.invoke.MethodHandle} because {@code SlimeMoveControl} is a package-private
 * inner class — {@code MethodHandles.Lookup.findVirtual} would fail JLS access checks even though
 * the method itself is public. {@code Method.setAccessible(true)} bypasses those checks.
 *
 * <p><b>Fail-soft:</b> if the lookup ever breaks (e.g., a rename in a future MC version),
 * a single warning is logged and subsequent calls become no-ops. Movement still works; only the
 * body-rotation visual degrades to the pre-fix behavior.
 */
public final class CubeMobMoveControlAccess {

    private CubeMobMoveControlAccess() {}

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static volatile Method getHandleMethod;
    private static volatile Method getMoveControlMethod;
    private static volatile Method setDirectionMethod;

    private static synchronized void tryInit(Slime slime) {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            Class<?> nmsMob      = Class.forName("net.minecraft.world.entity.Mob");

            getHandleMethod = craftEntity.getMethod("getHandle");
            getMoveControlMethod = nmsMob.getMethod("getMoveControl");

            // Prefer the direct inner-class name — stable and cleanest.
            Class<?> slimeMoveControlClass;
            try {
                slimeMoveControlClass = Class.forName(
                        "net.minecraft.world.entity.monster.Slime$SlimeMoveControl");
            } catch (ClassNotFoundException e) {
                // Fallback: resolve the runtime class from an actual instance.
                Object handle = getHandleMethod.invoke(slime);
                Object mc = getMoveControlMethod.invoke(handle);
                slimeMoveControlClass = mc.getClass();
            }

            setDirectionMethod = slimeMoveControlClass.getDeclaredMethod(
                    "setDirection", float.class, boolean.class);
            setDirectionMethod.setAccessible(true);
            available = true;
        } catch (Throwable t) {
            MyPetApi.getLogger().warning(
                    "CubeMobMoveControlAccess unavailable — cube mob body rotation will be stuck. Cause: " + t);
        }
    }

    /**
     * Writes the wanted yaw into vanilla {@code SlimeMoveControl}'s internal direction field so
     * the next vanilla MoveControl tick rotates the slime body toward {@code yaw}.
     * Vanilla rotlerps by max 90° per tick, so a 180° turn takes ~2 ticks visually.
     *
     * <p>Fail-soft: if the underlying reflection lookup ever breaks this becomes a no-op and the
     * slime's body will appear stuck at its initial rotation — but movement still works correctly.
     */
    public static void setDirection(Slime slime, float yaw) {
        if (!initialized) tryInit(slime);
        if (!available) return;
        try {
            Object handle = getHandleMethod.invoke(slime);
            Object mc = getMoveControlMethod.invoke(handle);
            setDirectionMethod.invoke(mc, yaw, false);
        } catch (Throwable t) {
            // Don't spam logs — drop silently after a successful init.
        }
    }
}
