package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wither;


/**
 * Applies the universal MyPet domain flags onto the live Bukkit {@link Mob}:
 * baby/adult, sitting pose, tamed-owner relationship, and the Wither boss-bar
 * suppression.
 *
 * <p>Per-type visual state (collar colour, variant, saddle, etc.) is handled
 * directly on the live entity via Bukkit setters at the call site — no field
 * cache exists on the {@code My{Type}} domain objects to push through this
 * class.
 */
public final class PetVisualSyncer {

    private PetVisualSyncer() {
    }

    /**
     * Synchronises universal MyPet flags onto the Bukkit mob. Called from
     * {@code VanillaMobSpawner.configureMob} pre-spawn and from
     * {@code plugin/entity/MyPet.updateVisuals} post-spawn.
     */
    public static void sync(MyPet pet, Mob mob) {
        sync(pet, mob, true);
    }

    /**
     * Overload that allows the caller to skip the tameable/owner application.
     * Used by {@code VanillaMobSpawner#releaseToWild} when respawning the pet
     * as a wild vanilla mob — the mob is no longer owned by the player, so
     * {@code setTamed/setOwner} must not be called.
     *
     * @param applyTameable when {@code false}, skip the {@code Tameable} block
     *                      so the synced mob remains un-tamed. The sit pose
     *                      is also suppressed since a released mob should not
     *                      spawn in the sitting animation.
     */
    public static void sync(MyPet pet, Mob mob, boolean applyTameable) {
        if (pet == null || mob == null) return;

        if (pet instanceof MyPetBaby baby && mob instanceof Ageable ageable) {
            if (baby.isBaby()) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }
        if (mob instanceof Sittable sittable) {
            sittable.setSitting(applyTameable && pet.isSitting());
        }
        if (applyTameable && mob instanceof Tameable tameable) {
            tameable.setTamed(true);
            if (pet.getOwner() != null && pet.getOwner().getPlayer() != null) {
                tameable.setOwner(pet.getOwner().getPlayer());
            }
        }
        // Hide the Wither's auto-managed boss bar. setVisible(false) flips the
        // NMS ServerBossEvent visibility flag; startSeenByPlayer checks it before
        // broadcasting add-packets, so the suppression persists across players
        // entering range. Called pre-spawn from VanillaMobSpawner.configureMob
        // so the bar is hidden before the initial spawn packet is sent.
        if (mob instanceof Wither wither) {
            try {
                wither.getBossBar().setVisible(false);
            } catch (Throwable ignored) {}
        }
    }
}
