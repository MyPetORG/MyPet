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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skills.Taunt;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.TauntUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

public class TauntImpl extends AbstractSkill implements Taunt {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Taunt.class,
            UpgradeSchema.builder()
                    .number("range").label("Range").suffix(" blocks").cumulative()
                    .integer("interval").label("Interval (s)").cumulative()
                    .build(), json -> new TauntUpgrade()
            .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
            .setIntervalModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "interval"))));

    protected UpgradeComputer<Number> range = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> interval = new UpgradeComputer<>(0);
    private int timeCounter = 0;

    public TauntImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return range.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        range.removeAllUpgrades();
        interval.removeAllUpgrades();
        timeCounter = 0;
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Locale.getComponent("Name.Range", locale))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", range.getValue().doubleValue())).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Blocks", locale))
                .append(Component.text(" -> "))
                .append(Component.text(effectiveInterval()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Taunt.Upgrade", String.format("%1.2f", getRange().getValue().doubleValue()), effectiveInterval())
        };
    }

    /**
     * Periodic taunt pulse: retargets every hostile mob in range whose current
     * target is the owner onto the pet, playing a growl cue if any was diverted.
     */
    public void schedule() {
        if (pet.getStatus() != PetState.Here || !isActive()) {
            return;
        }
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.isDead()) {
            return;
        }
        if (--timeCounter > 0) {
            return;
        }
        timeCounter = effectiveInterval();

        if (isFriendly(pet)) {
            return;
        }
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) {
            return;
        }
        // Folia guard — mirror the target goals: never scan world data from a
        // region thread that doesn't own the pet entity.
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }

        double r = range.getValue().doubleValue();
        boolean taunted = false;
        for (Entity entity : mob.getNearbyEntities(r, r, r)) {
            // Only hostile mobs — not provoked neutrals or another player's tamed animal.
            if (!(entity instanceof Monster other) || other.isDead()) {
                continue;
            }
            // Never rewire another MyPet's targeting.
            if (MyPetApi.getPetManager().getPetFromEntity(other) != null) {
                continue;
            }
            LivingEntity target = other.getTarget();
            if (target == null || !target.equals(owner)) {
                continue;
            }
            other.setTarget(mob);
            taunted = true;
        }
        if (taunted) {
            playGrowlCue(mob);
        }
    }

    /** Plays the growl sound + a sonic-boom shockwave at the pet. Shared with the retarget listener. */
    public static void playGrowlCue(Mob petMob) {
        petMob.getWorld().playSound(petMob.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.8F, 0.7F);
        petMob.getWorld().spawnParticle(Particle.SONIC_BOOM, petMob.getLocation().add(0, petMob.getHeight() * 0.6, 0), 2, 0, 0, 0, 0);
    }

    /** True when the pet's Behavior skill is set to Friendly — a passive pet must not taunt. */
    public static boolean isFriendly(Pet pet) {
        Behavior behavior = pet.getSkills().get(Behavior.class);
        return behavior != null && behavior.isActive() && behavior.getBehavior() == BehaviorMode.Friendly;
    }

    private int effectiveInterval() {
        return Math.max(1, interval.getValue());
    }

    public UpgradeComputer<Number> getRange() {
        return range;
    }

    public UpgradeComputer<Integer> getInterval() {
        return interval;
    }
}
