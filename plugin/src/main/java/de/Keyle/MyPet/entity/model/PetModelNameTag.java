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

package de.Keyle.MyPet.entity.model;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.PetImpl;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Display;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a pet's name as a floating {@link TextDisplay} above its custom model.
 *
 * <p>The vanilla nametag is bound to the host mob, which sits at the host's height, buried
 * inside a taller model — so modeled pets get this provider-independent floating name instead.
 * While a display exists it is the pet's <em>only</em> visible name: {@link #startForPet} hides
 * the host's vanilla tag and {@link #stopForPet} hands it back, so neither a renderer that
 * leaves the host visible nor a failed attach can double the name. One free TextDisplay —
 * <em>not</em> a passenger, so it never lands in the pet's persisted entity snapshot —
 * teleported to follow the host each tick on the host's region thread (Folia-safe).</p>
 */
public final class PetModelNameTag {

    private PetModelNameTag() {
    }

    /** pet UUID → the floating display. */
    private static final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();
    /** pet UUID → the per-pet follow task. */
    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    /** Spawn (or refresh) the floating name above this pet's model. Idempotent. */
    public static void startForPet(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        stopForPet(pet);
        Component name = nameComponent(pet);
        if (name == null) {
            return; // nametags globally disabled
        }
        final UUID petId = pet.getUUID();
        double height = PetModelService.nameHeight(pet);
        TextDisplay display = mob.getWorld().spawn(mob.getLocation().add(0, height, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setTeleportDuration(1);
            d.text(name);
        });
        displays.put(petId, display);
        // This display now owns the pet's name, so hide the host's vanilla tag. Renderers that
        // hide the host hide its tag with it, but one that fails to attach (e.g. an unknown
        // Model.Id) would otherwise leave both names rendering at once.
        mob.setCustomNameVisible(false);

        Plugin plugin = MyPetApi.getPlugin();
        ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin,
                t -> follow(pet), () -> stop(petId), 1L, 1L);
        if (task != null) {
            tasks.put(petId, task);
        }
    }

    /**
     * Remove the floating name and stop following, handing the name back to the host's vanilla
     * tag so a pet whose model went away isn't left nameless. Safe to call when none exists.
     */
    public static void stopForPet(Pet pet) {
        if (!stop(pet.getUUID())) {
            return; // no display of ours was showing -> the vanilla tag is already in charge
        }
        Mob mob = pet.getBukkitEntity();
        if (mob != null && nameComponent(pet) != null) {
            mob.setCustomNameVisible(true);
        }
    }

    /** Whether this pet currently renders its name through a floating display. */
    public static boolean hasDisplay(Pet pet) {
        return pet != null && displays.containsKey(pet.getUUID());
    }

    /** Refresh the floating name text (e.g. on rename). No-op if this pet has no model nametag. */
    public static void updateName(Pet pet) {
        TextDisplay display = displays.get(pet.getUUID());
        if (display == null || !display.isValid()) {
            return;
        }
        Component name = nameComponent(pet);
        if (name != null) {
            display.text(name);
        }
    }

    private static void follow(Pet pet) {
        UUID petId = pet.getUUID();
        TextDisplay display = displays.get(petId);
        Mob mob = pet.getBukkitEntity();
        if (display == null || !display.isValid() || mob == null || mob.isDead()) {
            stop(petId);
            return;
        }
        display.teleportAsync(mob.getLocation().add(0, PetModelService.nameHeight(pet), 0));
    }

    /** @return true when a display was actually removed. */
    private static boolean stop(UUID petId) {
        ScheduledTask task = tasks.remove(petId);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        return remove(petId);
    }

    /** @return true when a display was actually removed. */
    private static boolean remove(UUID petId) {
        TextDisplay display = displays.remove(petId);
        if (display == null) {
            return false;
        }
        try {
            display.remove();
        } catch (Exception ignored) {
        }
        return true;
    }

    private static Component nameComponent(Pet pet) {
        return pet instanceof PetImpl impl ? impl.nameTagComponent() : null;
    }
}
