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
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.function.Supplier;

@Getter
@ShopInfo
@DefaultInfo(food = {Material.SOUL_SAND}, flySpeed = 0.6608D)
public class PetEnderman extends PetImpl {

    /**
     * PDC flag persisting the "always angry" intent across despawn/respawn.
     * Vanilla NBT round-trips PDC under the {@code BukkitValues} tag, so
     * this survives the entity-snapshot envelope without extra wiring.
     */
    private static final NamespacedKey PERMA_SCREAMING_KEY = new NamespacedKey("mypet", "perma_screaming");

    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofFlag("screaming", Enderman.class, e ->
                    e.getPersistentDataContainer().set(PERMA_SCREAMING_KEY, PersistentDataType.BOOLEAN, true)),
            PetCreationOptions.blockSpec(Enderman.class)
    );

    /** Reads {@link #PERMA_SCREAMING_KEY} at spawn and restores {@link #permaScreaming}. */
    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "Enderman",
            PetEnderman::restorePermaScreaming,
            pet -> {}
    );

    public static final Supplier<Listener> DAMAGED_SCREAMING_SYNC =
            PetListenerRegistry.register(DamagedScreamingSync::new);

    private static void restorePermaScreaming(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (!(mob instanceof Enderman enderman)) return;
        if (!(pet instanceof PetEnderman petEnderman)) return;
        Boolean perma = enderman.getPersistentDataContainer()
                .get(PERMA_SCREAMING_KEY, PersistentDataType.BOOLEAN);
        if (perma != null && perma) {
            petEnderman.setPermaScreaming(true);
        }
    }

    /**
     * Pet-only override: vanilla {@link Enderman} screaming is AI-driven and
     * does not persist to NBT. When this flag is set, the live entity is
     * force-screamed on each {@code updateVisuals} pass.
     */
    protected boolean permaScreaming = false;

    public PetEnderman(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setPermaScreaming(boolean flag) {
        this.permaScreaming = flag;
        if (status == PetState.Here && getBukkitEntity() instanceof Enderman enderman) {
            enderman.setScreaming(flag);
        }
    }

    @Override
    public void updateVisuals() {
        super.updateVisuals();
        if (permaScreaming && getBukkitEntity() instanceof Enderman enderman) {
            enderman.setScreaming(true);
        }
    }

    /**
     * Re-asserts the configured {@link #permaScreaming} state after the
     * Enderman is damaged. Vanilla Enderman AI flips on screaming when the
     * mob takes damage; this listener counter-asserts the pet's desired
     * state — forcing {@code true} when perma-screaming is on, or clearing
     * the AI-induced scream when it's off.
     */
    public static final class DamagedScreamingSync implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPetEndermanDamaged(EntityDamageByEntityEvent event) {
            @SuppressWarnings("ConstantConditions")
            Entity damagedEntity = event.getEntity();
            if (damagedEntity == null) return;
            if (!(damagedEntity instanceof Enderman enderman)) return;
            if (!PetEntityMarker.isMarked(damagedEntity)) return;
            Pet pet = MyPetApi.getPetManager().getPetFromEntity(damagedEntity);
            if (pet instanceof PetEnderman petEnderman) {
                enderman.setScreaming(petEnderman.isPermaScreaming());
            }
        }
    }

}
