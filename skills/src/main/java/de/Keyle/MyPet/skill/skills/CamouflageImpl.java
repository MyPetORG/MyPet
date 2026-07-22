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

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Camouflage;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.CamouflageUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

public class CamouflageImpl extends AbstractSkill implements Camouflage {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Camouflage.class,
            UpgradeSchema.builder()
                    .integer("Delay").label("Delay (s)").cumulative()
                    .build(), json -> new CamouflageUpgrade()
            .setDelayModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "Delay"))));

    /** How far (blocks) to look for a hostile targeting the pet before camouflage engages. */
    private static final double THREAT_RANGE = 20.0;

    protected UpgradeComputer<Integer> delay = new UpgradeComputer<>(0);
    private Location lastPosition = null;
    private int stillSeconds = 0;
    private boolean hidden = false;

    public CamouflageImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return delay.getValue() > 0;
    }

    @Override
    public void schedule() {
        Mob mob = pet.getBukkitEntity();
        if (mob == null || !mob.isValid()) {
            // Despawned mobs respawn visible; clear tracking so the fresh mob re-hides normally.
            hidden = false;
            stillSeconds = 0;
            lastPosition = null;
            return;
        }
        if (!isActive()) {
            // Level-down inverts upgrades without reset(); restore visibility if delay dropped to 0 while hidden.
            if (hidden) {
                mob.setInvisible(false);
                hidden = false;
            }
            stillSeconds = 0;
            return;
        }

        Location current = mob.getLocation();
        // Compare BLOCK coordinates only. Location.equals() also compares yaw/pitch, which an
        // idle mob changes constantly as it turns to face things — that would reset the stillness
        // timer almost every tick and keep the pet from ever reaching the hide threshold.
        boolean moved = lastPosition == null
                || !current.getWorld().equals(lastPosition.getWorld())
                || current.getBlockX() != lastPosition.getBlockX()
                || current.getBlockY() != lastPosition.getBlockY()
                || current.getBlockZ() != lastPosition.getBlockZ();
        boolean fighting = pet.getPetTarget() != null;
        lastPosition = current;

        if (moved || fighting) {
            stillSeconds = 0;
            if (hidden) {
                mob.setInvisible(false);
                hidden = false;
            }
            return;
        }

        if (hidden) {
            return; // already camouflaged and holding still — stay hidden until it moves or fights back
        }
        // Camouflage only engages under an active threat — a hostile mob hunting the pet — not whenever
        // the pet happens to stand still.
        if (!isThreatened(mob)) {
            stillSeconds = 0;
            return;
        }
        if (++stillSeconds >= delay.getValue()) {
            mob.setInvisible(true);
            hidden = true;
            mob.getWorld().spawnParticle(Particle.POOF, mob.getLocation().add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0.01);
        }
    }

    /** True if a nearby hostile mob is currently targeting (hunting or attacking) the pet. */
    private boolean isThreatened(Mob petMob) {
        for (Entity entity : petMob.getNearbyEntities(THREAT_RANGE, THREAT_RANGE, THREAT_RANGE)) {
            if (entity instanceof Mob other && petMob.equals(other.getTarget())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset() {
        delay.removeAllUpgrades();
        lastPosition = null;
        stillSeconds = 0;
        Mob mob = pet.getBukkitEntity();
        if (hidden && mob != null && mob.isValid()) {
            mob.setInvisible(false);
        }
        hidden = false;
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(delay.getValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Camouflage.Upgrade", getDelay().getValue())
        };
    }

    public UpgradeComputer<Integer> getDelay() {
        return delay;
    }
}
