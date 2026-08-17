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
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetMultiPassenger;
import de.Keyle.MyPet.api.entity.PetNaturallyRideable;
import de.Keyle.MyPet.api.entity.PetSaddleable;
import de.Keyle.MyPet.api.entity.PetSittable;
import de.Keyle.MyPet.api.entity.PetTameable;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.ai.BrainAccess;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ShopInfo
@DefaultInfo(food = {Material.CACTUS}, flySpeed = 0.1982D)
public class PetCamel extends PetImpl implements PetBaby, PetEquipment, PetMultiPassenger, PetNaturallyRideable, PetSaddleable, PetSittable, PetTameable {

    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Camel", "experience_bottle");

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     * Stops a pet camel autonomously parking itself in the sit pose;
     * owner-driven sitting is unaffected.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED = ConfigKey.stringList(
            "Camel", "Brain.Disabled",
            "behavior:RandomSitting");

    public static final PetLifecycleHook WANDER_SUPPRESSOR_HOOK = new PetLifecycleHook(
            "Camel",
            WanderSuppressor::startForPet,
            WanderSuppressor::stopForPet
    );

    public PetCamel(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof Camel camel)) return new ItemStack[]{null};
        return new ItemStack[]{camel.getInventory().getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof Camel camel)) return null;
        if ("SADDLE".equals(slot.name())) return camel.getInventory().getSaddle();
        return null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof Camel camel)) {
            super.setEquipmentBySlotName(slotName, item);
            return;
        }
        if ("SADDLE".equals(slotName)) {
            camel.getInventory().setSaddle(item);
        } else {
            super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof Camel camel)) return;
        AbstractHorseInventory inv = camel.getInventory();
        ItemStack saddle = inv.getSaddle();
        if (saddle != null && saddle.getType() != Material.AIR) {
            camel.getWorld().dropItem(camel.getLocation(), saddle);
            inv.setSaddle(null);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE");
    }

    /**
     * Per-tick clear of the brain's {@code WALK_TARGET} memory so a pet camel
     * never autonomously wanders. Memory-clear rather than behavior-removal
     * because many vanilla behaviors write {@code WALK_TARGET} (RandomStroll,
     * SetWalkTargetFromLookTarget, AnimalPanic, sit-recover)
     */
    public static final class WanderSuppressor {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        private WanderSuppressor() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Camel camel)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (camel.isDead()) return;
                    BrainAccess.clearWalkTarget(camel);
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
