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

import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.ai.BrainAccess;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import io.papermc.paper.event.entity.EntityToggleSitEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.CACTUS}, flySpeed = 0.1982D)
public class PetCamel extends PetImpl implements PetBaby, PetEquipment, PetMultiPassenger, PetNaturallyRideable, PetSaddleable, PetSittable, PetTameable {

    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Camel", "experience_bottle");

    public static final Supplier<Listener> AUTONOMOUS_SIT_SUPPRESSOR =
            PetListenerRegistry.register(AutonomousSitSuppressor::new);

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
     * Cancels every autonomous sit-pose transition on a marked Camel pet so
     * the vanilla {@code CamelAi} idle-sit brain task can never park the pet
     * in the sit pose on its own. The owner-commanded {@code /petsit} flow
     * is unaffected.
     *
     * <p>Why this is needed:
     * {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller}'s
     * {@code Bukkit.getMobGoals().removeAllGoals(mob)} sweep strips the
     * mob's {@code Goal}s but leaves its {@code Brain} schedule intact, and
     * the camel's idle-sit logic lives in a {@code Behavior<Camel>} on the
     * brain — the same Goals-vs-Brain gap that motivates
     * {@link PetBreeze.AutonomousAttackSuppressor} and the wander suppressor
     * below. The brain ticks independently of the goal list, so a pet camel
     * keeps getting parked in the sit pose every idle window unless
     * something cancels the transition. This was originally fixed only for
     * ridden camels; the wider always-on policy here matches the wander
     * suppressor's reasoning — brain-driven autonomous behavior is noise
     * for a pet, whether ridden or not.
     *
     * <p>Why this is event-driven rather than polled:
     * Paper fires {@link EntityToggleSitEvent} at the source of every
     * sit-pose transition — vanilla AI, brain task, or
     * {@code Sittable#setSitting} from any plugin — and the event is
     * {@code Cancellable}. Cancelling here prevents the transition outright,
     * so there is no per-tick scheduler overhead, no per-pet bookkeeping
     * map, and no visual flicker between the brain setting the pose and a
     * suppressor unsetting it.
     *
     * <p>Why the guard is {@code pet.isSitting()}, not
     * {@code !getPassengers().isEmpty()}:
     * the owner-commanded {@code /petsit} flow calls
     * {@code Pet.setSitting(true)}, which
     * {@link de.Keyle.MyPet.entity.visual.PetVisualSyncer#sync} eventually
     * pushes into {@code Camel.setSitting(true)} — that call also fires this
     * event. Unconditional cancellation would block the owner from sitting
     * their pet. Reading {@code pet.isSitting()} tells the two sources
     * apart: owner-driven sit has {@code pet.isSitting() == true} (the MyPet
     * pose state was set first), brain-driven sit has
     * {@code pet.isSitting() == false} (the brain bypasses the MyPet pose
     * state entirely and writes to {@code Camel.setSitting} directly).
     *
     * <p>{@code CamelHusk} is covered for free: {@code CamelHusk extends
     * Camel} in the Bukkit API (and shares the same vanilla brain), so the
     * {@code instanceof Camel} guard catches both pet types with no
     * additional listener. {@code PetCamelHusk} does not implement
     * {@code PetSittable} (no {@code /petsit} support on husks), so
     * {@code pet.isSitting()} is always {@code false} for it — every sit
     * transition is brain-driven and gets cancelled, which is the desired
     * outcome.
     *
     * <p>Wild camels are out of scope. The {@link PetEntityMarker#isMarked}
     * check scopes the listener to MyPet pets so regular vanilla camel
     * behavior on the server is untouched.
     */
    public static final class AutonomousSitSuppressor implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onPetCamelToggleSit(EntityToggleSitEvent event) {
            if (!event.getSittingState()) return;
            if (!(event.getEntity() instanceof Camel camel)) return;
            if (!PetEntityMarker.isMarked(camel)) return;
            Pet pet = MyPetApi.getPetManager().getPetFromEntity(camel);
            if (pet != null && pet.isSitting()) return;
            event.setCancelled(true);
        }
    }

    /**
     * Erases the brain's {@code WALK_TARGET} memory every tick so the
     * vanilla {@code CamelAi} idle-stroll (and every other brain producer
     * that writes a walk target) has nothing for the navigator to act on.
     * A pet camel can then only move via MyPet's installed goals (follow
     * owner, etc.) and player-driven mount controls — never autonomously
     * via the brain.
     *
     * <p>Same Goals-vs-Brain class of bug as {@link AutonomousSitSuppressor}.
     * {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller}'s
     * {@code removeAllGoals} sweep strips the mob's {@code Goal}s but leaves
     * its {@code Brain} schedule intact; the camel's random-stroll lives as
     * a {@code Behavior<Camel>} on the brain (whereas Horse/Donkey/Pig store
     * theirs in goals — which is why those rideable pets don't exhibit this
     * symptom). Several producers write {@code WALK_TARGET} besides
     * {@code RandomStroll} ({@code SetWalkTargetFromLookTarget},
     * {@code AnimalPanic}, the camel sit-recover step, etc.), so the
     * memory-erase shape catches them all in one place without enumerating
     * behavior classes.
     *
     * <p>Why no rider gate: brain-driven roaming for a pet is noise whether
     * the camel is ridden or not. An unmounted pet camel that wanders off
     * is just as much of a UX failure as a ridden one that walks the rider
     * into a wall. MyPet's follow-owner goal moves the camel through the
     * Goal/Pathfinder system, which doesn't go through {@code WALK_TARGET},
     * so disabling brain movement always doesn't take anything away.
     *
     * <p>Why memory-clear, not behavior-removal: see the class-level
     * Javadoc on {@link BrainAccess}.
     *
     * <p>Why this doesn't interfere with the rider: the Ride skill and
     * vanilla mount-control write the entity's velocity directly rather
     * than routing through brain memory or the navigator.
     *
     * <p>{@code CamelHusk} is covered: the lifecycle-hook registry keys on
     * the pet-type name string {@code "Camel"}, so this hook fires for
     * {@code PetCamel} only. A parallel hook on {@code PetCamelHusk} would
     * be a one-line addition if the same bug surfaces there (it almost
     * certainly does, since {@code CamelHusk} inherits the camel brain).
     *
     * <p>Per-pet scheduling follows {@link PetBreeze.AutonomousAttackSuppressor}:
     * {@code mob.getScheduler().runAtFixedRate(...)} runs on the entity's
     * region thread on Folia and on the main thread on Paper, and is
     * cancelled on despawn.
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
