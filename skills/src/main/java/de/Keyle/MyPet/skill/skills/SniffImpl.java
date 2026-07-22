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

package de.Keyle.MyPet.skill.skills;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.SkillStateCodecs;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.ToggleableSkill;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Sniff;
import de.Keyle.MyPet.api.util.ItemStrings;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.SniffUpgrade;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SniffImpl extends AbstractSkill implements Sniff {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Sniff.class,
            UpgradeSchema.builder()
                    .integer("interval").label("Interval (s)").cumulative()
                    .list("drops", row -> row
                            .string("item").label("Item")
                            .integer("weight").label("Weight")
                            .integer("amountMin").label("Min")
                            .integer("amountMax").label("Max"))
                    .build(), SniffImpl::parseUpgrade);

    public static final SkillStateCodecs STATE_CODEC = SkillStateCodecs.of(Sniff.class,
            ToggleableSkill.ToggleState.class, ToggleableSkill.TOGGLE_CODEC);

    /** Ticks the pet spends visibly digging before the item surfaces (~1.5s). */
    private static final int DIG_TICKS = 30;
    /** Cadence of dust puffs, and the tick step, during a dig. */
    private static final long DIG_PULSE = 5L;

    protected UpgradeComputer<Integer> interval = new UpgradeComputer<>(0);
    private int timeCounter = 0;
    /** True while a dig animation is playing, so the interval timer doesn't start a second one. */
    private boolean digging = false;
    /** The running dig-pulse task, held so {@link #reset()} can cancel it mid-animation. */
    private ScheduledTask digTask;
    /** Owner's runtime on/off toggle from the pet menu; defaults on, persisted via the state codec. */
    private boolean enabled = true;
    /** Weighted drop pool granted by the skilltree; rebuilt on every skilltree change. */
    private final List<Sniff.DropEntry> dropPool = new ArrayList<>();

    public SniffImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return interval.getValue() > 0 && !dropPool.isEmpty();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Optional<ToggleableSkill.ToggleState> getState() {
        return Optional.of(new ToggleableSkill.ToggleState(enabled));
    }

    @Override
    public void applyState(SkillState state) {
        if (state instanceof ToggleableSkill.ToggleState toggle) {
            enabled = toggle.enabled();
        }
    }

    @Override
    public void reset() {
        interval.removeAllUpgrades();
        dropPool.clear();
        timeCounter = 0;
        abortDigging();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(interval.getValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Sniff.Upgrade", getInterval().getValue())
        };
    }

    public void schedule() {
        if (!enabled || !isActive() || pet.getStatus() != PetState.Here) {
            timeCounter = 0;
            return;
        }
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.isDead()) {
            return;
        }
        if (digging) {
            // A dig is already playing — let it finish before timing the next one.
            return;
        }
        if (mob.isSwimming() || !mob.isOnGround()) {
            // Sniffing needs all paws on solid ground — pause (hold the timer) while
            // the pet is actively swimming (deep water) or airborne. Shallow wading is fine.
            return;
        }
        if (PetWorkFocus.isBusy(pet, this)) {
            // Another autonomous chore (Mining/Lumberjack/Fishing) is running or reserved — wait our turn.
            return;
        }
        if (timeCounter <= 0) { // (re)arm the timer after activation or respawn
            timeCounter = interval.getValue();
            return;
        }
        if (--timeCounter > 0) {
            return;
        }
        timeCounter = interval.getValue();
        startDigging(mob);
    }

    /**
     * Plays a short digging animation — dust kicked up from the ground on a steady pulse — and
     * drops the configured item when it finishes. Aborts with no drop if the pet starts swimming,
     * leaves the ground, or is removed mid-dig.
     */
    private void startDigging(Mob mob) {
        ItemStack drop = pickWeightedDrop();
        if (drop == null) {
            return;
        }
        if (!PetWorkFocus.acquire(pet, this)) {
            return; // another work skill grabbed the pet this tick
        }
        digging = true;
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SNIFFER_DIGGING, 0.8F, 1.0F);
        int[] elapsed = {0};
        digTask = mob.getScheduler().runAtFixedRate(MyPetApi.getPlugin(), task -> {
            if (!mob.isValid() || pet.getStatus() != PetState.Here
                    || mob.isSwimming() || !mob.isOnGround()) {
                abortDigging();
                return;
            }
            digDust(mob);
            elapsed[0] += DIG_PULSE;
            if (elapsed[0] >= DIG_TICKS) {
                mob.getWorld().dropItemNaturally(mob.getLocation(), drop);
                finishDigging(mob);
            }
        }, this::abortDigging, DIG_PULSE, DIG_PULSE);
        if (digTask == null) {
            // Scheduler already retired (mob mid-teleport/removed): neither the pulse nor the
            // retired callback will ever run, so clear the guard + free the focus here.
            abortDigging();
        }
    }

    /** Clears the dig guard and cancels the pulse task if one is running. */
    private void clearDig() {
        digging = false;
        if (digTask != null) {
            digTask.cancel();
            digTask = null;
        }
    }

    /** Ends a dig with no follow-up (interrupted/aborted/reset): clears state and frees the work focus. */
    private void abortDigging() {
        clearDig();
        PetWorkFocus.release(pet, this);
    }

    /** Ends a completed dig: clears state, then frees the focus after a Pickup linger if applicable. */
    private void finishDigging(Mob mob) {
        clearDig();
        if (!pet.getSkills().isActive(PickupImpl.class)) {
            PetWorkFocus.release(pet, this);
            return;
        }
        // Stay put a beat so the Pickup skill can grab the dropped item before the focus frees.
        ScheduledTask release = mob.getScheduler().runDelayed(MyPetApi.getPlugin(),
                task -> PetWorkFocus.release(pet, this), () -> PetWorkFocus.release(pet, this),
                PetWorkFocus.PICKUP_LINGER_TICKS);
        if (release == null) {
            PetWorkFocus.release(pet, this);
        }
    }

    /** Kicks up a puff of dust from the ground the pet is standing on — reads as digging. */
    private void digDust(Mob mob) {
        Location location = mob.getLocation();
        Block ground = location.getBlock().getRelative(BlockFace.DOWN);
        if (!ground.getType().isAir()) {
            mob.getWorld().spawnParticle(Particle.BLOCK, location, 10, 0.3, 0.05, 0.3, 0.05, ground.getBlockData());
        }
    }

    public UpgradeComputer<Integer> getInterval() {
        return interval;
    }

    @Override
    public void addDrop(Sniff.DropEntry entry) {
        dropPool.add(entry);
    }

    @Override
    public void removeDrop(Sniff.DropEntry entry) {
        dropPool.remove(entry);
    }

    @Override
    public List<Sniff.DropEntry> getDropPool() {
        return dropPool;
    }

    /** Picks one drop by weight and rolls its stack size, returning a ready-to-drop clone (or null if empty). */
    private ItemStack pickWeightedDrop() {
        if (dropPool.isEmpty()) {
            return null;
        }
        int total = 0;
        for (Sniff.DropEntry entry : dropPool) {
            total += entry.weight();
        }
        if (total <= 0) {
            return null; // guards against an external addDrop() caller supplying non-positive weights
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        Sniff.DropEntry chosen = dropPool.get(dropPool.size() - 1);
        for (Sniff.DropEntry entry : dropPool) {
            roll -= entry.weight();
            if (roll < 0) {
                chosen = entry;
                break;
            }
        }
        int amount = chosen.amountMin() == chosen.amountMax()
                ? chosen.amountMin()
                : ThreadLocalRandom.current().nextInt(chosen.amountMin(), chosen.amountMax() + 1);
        ItemStack out = chosen.item().clone();
        out.setAmount(amount);
        return out;
    }

    private static SniffUpgrade parseUpgrade(JsonObject json) {
        SniffUpgrade upgrade = new SniffUpgrade()
                .setIntervalModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "interval")));
        JsonElement drops = UpgradeParsers.get(json, "drops");
        if (drops != null && drops.isJsonArray()) {
            for (JsonElement element : drops.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                JsonElement itemElement = UpgradeParsers.get(row, "item");
                if (itemElement == null || !itemElement.isJsonPrimitive()) {
                    continue;
                }
                ItemStack item = ItemStrings.deserialize(itemElement.getAsString());
                if (item == null || item.getType().isAir()) {
                    continue; // unparseable/removed item — skip rather than crash the tree
                }
                int weight = Math.max(1, parsePlainInt(UpgradeParsers.get(row, "weight"), 1));
                int min = Math.max(1, parsePlainInt(UpgradeParsers.get(row, "amountMin"), 1));
                int max = Math.max(min, parsePlainInt(UpgradeParsers.get(row, "amountMax"), min));
                upgrade.addDrop(new Sniff.DropEntry(item, weight, min, max));
            }
        }
        return upgrade;
    }

    private static int parsePlainInt(JsonElement element, int fallback) {
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
