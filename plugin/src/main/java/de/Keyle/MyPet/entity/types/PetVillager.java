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
import de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetLightningConvertible;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.ai.BrainAccess;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ShopInfo
@DefaultInfo(food = {Material.APPLE}, flySpeed = 1.1013D)
public class PetVillager extends PetImpl implements PetBaby, PetEquipment, PetLightningConvertible {

    public static final ConfigKey<Boolean> ALLOW_LIGHTNING_CONVERSION = ConfigKey.bool("Villager", "AllowLightningConversion", false);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Villager", "experience_bottle");

    /**
     * Strip {@code SleepInBed} so even if vanilla gets the villager onto a
     * bed via some path we haven't enumerated, the sleep action itself can't
     * fire. (Most VillagerGoalPackages walk-to-block-memory behaviors are
     * {@code BehaviorBuilder}-wrapped — class-name matching misses them, so
     * the per-tick {@link WanderSuppressor} below is the load-bearing piece.)
     */
    public static final PetBrainBehaviorRemoval BRAIN_BEHAVIOR_REMOVAL = new PetBrainBehaviorRemoval(
            "Villager",
            "SleepInBed"
    );

    /** Per-tick {@code WALK_TARGET} clear so brain behaviors can never autonomously walk the pet anywhere. */
    public static final PetLifecycleHook WANDER_SUPPRESSOR_HOOK = new PetLifecycleHook(
            "Villager",
            WanderSuppressor::startForPet,
            WanderSuppressor::stopForPet
    );


    // Villager also gets its trade level reset alongside the profession change —
    // matches the legacy behavior (fresh-profession villagers have no trades).
    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofRegistry("profession", Villager.class, RegistryKey.VILLAGER_PROFESSION,
                    (Villager v, Villager.Profession p) -> { v.setProfession(p); v.setVillagerLevel(1); }),
            () -> OptionSpec.ofRegistry("type",       Villager.class, RegistryKey.VILLAGER_TYPE,       Villager::setVillagerType)
    );

    public PetVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND) return;
        super.setEquipment(slot, item);
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("HAND");
    }

    /**
     * Per-tick clear of the brain's {@code WALK_TARGET} memory — neutralizes
     * every vanilla VillagerAi behavior that would autonomously walk the pet
     * (bed-seeking, job-site walking, meeting-point socializing, raid hiding).
     * Most of those producers are {@code BehaviorBuilder}-wrapped factories,
     * so {@link PetBrainBehaviorRemoval} class-name matching can't catch them
     * individually; clearing the downstream memory each tick catches them all.
     */
    public static final class WanderSuppressor {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        private WanderSuppressor() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Villager villager)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (villager.isDead()) return;
                    BrainAccess.clearWalkTarget(villager);
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
        }
    }
}
