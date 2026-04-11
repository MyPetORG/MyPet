package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders custom-coloured potion particles around MyPet pets via a per-tick
 * {@code Particle.DUST} scheduler.
 *
 * <p>Pets register themselves via {@link #show(MyPet, Color)} and deregister via
 * {@link #hide(MyPet)}. The controller runs a single scheduler iterating all
 * active particles and spawns 1-2 DUST particles per tick around each pet's
 * bounding box.
 */
public class PetPotionParticleController extends BukkitRunnable {

    private static final PetPotionParticleController INSTANCE = new PetPotionParticleController();

    private final Map<UUID, Color> activeByPet = new HashMap<>();

    public static void start(Plugin plugin) {
        INSTANCE.runTaskTimer(plugin, 1L, 2L);
    }

    public static void show(MyPet pet, Color color) {
        if (pet == null || color == null) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        INSTANCE.activeByPet.put(mob.getUniqueId(), color);
    }

    public static void hide(MyPet pet) {
        if (pet == null) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        INSTANCE.activeByPet.remove(mob.getUniqueId());
    }

    @Override
    public void run() {
        if (activeByPet.isEmpty()) return;
        for (MyPet pet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            Mob mob = pet.getBukkitEntity();
            if (mob == null || mob.isDead()) continue;
            Color color = activeByPet.get(mob.getUniqueId());
            if (color == null) continue;

            Particle.DustOptions options = new Particle.DustOptions(color, 1.0f);
            double width = mob.getWidth();
            double height = mob.getHeight();
            mob.getWorld().spawnParticle(
                    Particle.DUST,
                    mob.getLocation().add(0, height * 0.5, 0),
                    3,
                    width * 0.5, height * 0.5, width * 0.5,
                    0.0,
                    options);
        }
    }
}
