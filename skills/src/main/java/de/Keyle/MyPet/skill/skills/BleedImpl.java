/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Bleed;
import de.Keyle.MyPet.api.util.locale.Translation;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BleedImpl implements Bleed {

    private static Random random = new Random();

    @Getter
    private MyPet myPet;

    @Getter
    protected UpgradeComputer<Number> damage = new UpgradeComputer<>(0);
    @Getter
    protected UpgradeComputer<Integer> interval = new UpgradeComputer<>(1);
    @Getter
    protected UpgradeComputer<Integer> duration = new UpgradeComputer<>(0);
    @Getter
    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);

    private final Map<UUID, BleedEffect> activeEffects = new ConcurrentHashMap<>();
    private BukkitTask bleedTask = null;

    public BleedImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    @Override
    public boolean isActive() {
        return chance.getValue() > 0 && duration.getValue() > 0 && damage.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        damage.removeAllUpgrades();
        interval.removeAllUpgrades();
        duration.removeAllUpgrades();
        chance.removeAllUpgrades();
        activeEffects.clear();
        stopBleedTask();
    }

    @Override
    public String toPrettyString(String locale) {
        return "" + ChatColor.GOLD + chance.getValue() + ChatColor.RESET + "% -> "
                + ChatColor.GOLD + damage.getValue().doubleValue() + ChatColor.RESET + " "
                + Translation.getString("Name.Damage", locale) + "/"
                + ChatColor.GOLD + interval.getValue() + ChatColor.RESET + "s for "
                + ChatColor.GOLD + duration.getValue() + ChatColor.RESET + " "
                + Translation.getString("Name.Seconds", locale);
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(
                        Translation.getString("Message.Skill.Bleed.Upgrade", myPet.getOwner().getLanguage()),
                        myPet.getPetName(),
                        getChance().getValue(),
                        getDamage().getValue().doubleValue(),
                        getInterval().getValue(),
                        getDuration().getValue()
                )
        };
    }

    @Override
    public boolean trigger() {
        return random.nextDouble() <= chance.getValue() / 100.;
    }

    @Override
    public void apply(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        BleedEffect existing = activeEffects.get(targetId);

        double damageValue = damage.getValue().doubleValue();
        int intervalTicks = interval.getValue() * 20;

        if (existing != null) {
            // Refresh + extend: add remaining duration to new duration
            int remainingSeconds = existing.remainingTicks / 20;
            int newDurationSeconds = duration.getValue() + remainingSeconds;
            existing.refresh(target, damageValue, intervalTicks, newDurationSeconds * 20);
        } else {
            // Create new bleed effect and apply first damage immediately
            BleedEffect effect = new BleedEffect(target, damageValue, intervalTicks, duration.getValue() * 20);
            activeEffects.put(targetId, effect);

            // Apply first bleed damage immediately on hit
            LivingEntity petEntity = myPet.getEntity().map(e -> (LivingEntity) e).orElse(null);
            effect.applyDamage(petEntity);
        }

        // Start the bleed task if not already running
        startBleedTask();
    }

    private void startBleedTask() {
        if (bleedTask != null) {
            return;
        }

        bleedTask = new BukkitRunnable() {
            @Override
            public void run() {
                processBleedEffects();

                // Stop task if no more effects
                if (activeEffects.isEmpty()) {
                    stopBleedTask();
                }
            }
        }.runTaskTimer(MyPetApi.getPlugin(), 20L, 20L);
    }

    private void stopBleedTask() {
        if (bleedTask != null) {
            bleedTask.cancel();
            bleedTask = null;
        }
    }

    private void processBleedEffects() {
        // Get pet entity if available (may be null if pet died)
        LivingEntity petEntity = myPet.getEntity().map(e -> (LivingEntity) e).orElse(null);

        Iterator<Map.Entry<UUID, BleedEffect>> iterator = activeEffects.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BleedEffect> entry = iterator.next();
            BleedEffect effect = entry.getValue();

            // Check if the target is still valid
            if (!effect.isTargetValid()) {
                iterator.remove();
                continue;
            }

            // Check if the effect has expired
            if (effect.isExpired()) {
                iterator.remove();
                continue;
            }

            // Process tick (petEntity may be null if pet died - damage continues without attribution)
            effect.tick(petEntity);
        }
    }

    @Override
    public void schedule() {
        // Bleed uses its own Bukkit task for persistence through pet death
        // This method is kept for interface compliance but does nothing
    }

    @Override
    public String toString() {
        return "BleedImpl{" +
                "damage=" + damage.getValue().doubleValue() +
                ", interval=" + interval.getValue() +
                ", duration=" + duration.getValue() +
                ", chance=" + chance.getValue() +
                ", activeEffects=" + activeEffects.size() +
                '}';
    }

    /**
     * Inner class representing an active bleed effect on a target entity
     */
    private static class BleedEffect {
        private LivingEntity target;
        private double damagePerTick;
        private int intervalTicks;
        private int remainingTicks;
        private int ticksSinceLastDamage;

        public BleedEffect(LivingEntity target, double damagePerTick, int intervalTicks, int durationTicks) {
            this.target = target;
            this.damagePerTick = damagePerTick;
            this.intervalTicks = intervalTicks;
            this.remainingTicks = durationTicks;
            this.ticksSinceLastDamage = 0; // First damage applied in apply(), start interval fresh
        }

        public void refresh(LivingEntity target, double newDamage, int newIntervalTicks, int newDurationTicks) {
            this.target = target;
            this.damagePerTick = newDamage;
            this.intervalTicks = newIntervalTicks;
            this.remainingTicks = newDurationTicks;
            // Don't reset ticksSinceLastDamage to maintain damage rhythm
        }

        public void tick(LivingEntity petEntity) {
            remainingTicks -= 20; // Called every second (20 ticks)
            ticksSinceLastDamage += 20;

            if (ticksSinceLastDamage >= intervalTicks) {
                applyDamage(petEntity);
                ticksSinceLastDamage = 0;
            }
        }

        private void applyDamage(LivingEntity petEntity) {
            if (!isTargetValid()) {
                return;
            }

            // Apply damage - with pet attribution if available, otherwise just raw damage
            if (petEntity != null) {
                target.damage(damagePerTick, petEntity);
            } else {
                target.damage(damagePerTick);
            }

            // Play red block particles for blood effect
            MyPetApi.getPlatformHelper().playParticleEffect(
                    target.getLocation().add(0, 1, 0),
                    ParticleCompat.BLOCK_CRACK.get(),
                    0.3f, 0.5f, 0.3f,
                    0.1f,
                    10,
                    16,
                    ParticleCompat.REDSTONE_BLOCK_DATA
            );
        }

        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public boolean isTargetValid() {
            return target != null && target.isValid() && !target.isDead();
        }
    }
}