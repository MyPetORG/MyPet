package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Renders a private "stay" hint above sitting pets — a single
 * {@link Particle#BLOCK_MARKER BLOCK_MARKER} of {@link Material#BARRIER BARRIER}
 * shown only to the owner, every 60 ticks.
 */
public class PetSitParticleController extends BukkitRunnable {

    private static final PetSitParticleController INSTANCE = new PetSitParticleController();

    public static void start(Plugin plugin) {
        INSTANCE.runTaskTimer(plugin, 60L, 60L);
    }

    @Override
    public void run() {
        for (MyPet pet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            if (!pet.isSitting()) continue;
            Mob mob = pet.getBukkitEntity();
            if (mob == null || mob.isDead()) continue;
            Player owner = pet.getOwner().getPlayer();
            if (owner == null || !owner.isOnline()) continue;
            owner.spawnParticle(
                    Particle.BLOCK_MARKER,
                    mob.getLocation().add(0, mob.getEyeHeight() + 1, 0),
                    1, 0F, 0F, 0F, 0F,
                    Material.BARRIER.createBlockData());
        }
    }
}
