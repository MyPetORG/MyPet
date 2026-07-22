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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.event.PetLevelDownEvent;
import de.Keyle.MyPet.api.event.PetLevelUpEvent;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.entity.PetArmorApplier;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.util.animation.particle.FixedCircleAnimation;
import de.Keyle.MyPet.util.animation.particle.SpiralAnimation;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class LevelListener implements Listener {

    @EventHandler
    public void on(PetLevelUpEvent event) {
        Pet pet = event.getPet();
        int lvl = event.getLevel();
        int fromLvl = event.fromLevel();

        if (!event.isQuiet()) {
            int maxlevel = pet.getSkilltree() != null ? pet.getSkilltree().getMaxLevel() : 0;
            if (maxlevel != 0 && lvl >= maxlevel) {
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.LevelSystem.ReachedMaxLevel", event.getOwner(), pet.getDisplayName(), maxlevel));
            } else {
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.LevelSystem.LevelUp", event.getOwner(), pet.getDisplayName(), event.getLevel()));
            }
        }
        Skilltree skilltree = pet.getSkilltree();
        if (skilltree != null) {
            for (int i = fromLvl + 1; i <= lvl; i++) {
                if (!event.isQuiet()) {
                    List<String> notifications = skilltree.getNotifications(i);
                    for (String notification : notifications) {
                        notification = notification
                                .replace("{{owner}}", pet.getOwner().getName())
                                .replace("{{level}}", "" + lvl)
                                .replace("{{pet}}", pet.getPetName());
                        String[] lines = notification.split("(<br>|\\\\n|\n|<br\\s?/>)");
                        for (String line : lines) {
                            pet.getOwner().sendMessage(Util.SANITIZED_MINIMESSAGE.deserialize(line));
                        }
                    }
                }
                Set<Skill> affectedSkills = new HashSet<>();
                List<Upgrade<?>> upgrades = skilltree.getUpgrades(i);
                for (Upgrade<?> upgrade : upgrades) {
                    if (upgrade == null) {
                        continue;
                    }
                    SkillName sn = Util.getClassAnnotation(upgrade.getClass(), SkillName.class);
                    if (sn != null) {
                        Skill skill = pet.getSkills().get(sn.value());
                        if (skill != null) {
                            applyUpgrade(upgrade, skill);
                            affectedSkills.add(skill);
                        }
                    }
                }
                if (!event.isQuiet()) {
                    for (Skill skill : affectedSkills) {
                        Component[] messages = skill.getUpgradeMessage();
                        if (messages != null) {
                            for (Component message : messages) {
                                pet.getOwner().sendMessage(Component.text("  ").append(message));
                            }
                        }
                    }
                }
            }
        }
        PetArmorApplier.update(pet);

        if (pet.getStatus() == Pet.PetState.Here) {
            Mob entity = pet.getBukkitEntity();
            if (entity != null) {
                pet.updateNameTag();
                if (!event.isQuiet()) {
                    pet.setHealth(pet.getMaxHealth());
                    pet.setSaturation(100);

                    new SpiralAnimation(1, entity.getEyeHeight() + 0.5, () -> entity.isDead() ? null : entity.getLocation()) {
                        @Override
                        protected void playParticleEffect(Location location) {
                            location.getWorld().spawnParticle(Particle.ENCHANTED_HIT, location, 1, 0, 0, 0, 0);
                        }
                    }.loop(2);

                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.7F);
                }
            }
        }
    }

    @EventHandler
    public void on(PetLevelDownEvent event) {
        Pet pet = event.getPet();
        int lvl = event.getLevel();
        int fromLvl = event.fromLevel();

        if (!event.isQuiet()) {
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.LevelSystem.LevelDown", event.getOwner(), pet.getDisplayName(), event.getLevel()));
        }
        Skilltree skilltree = pet.getSkilltree();
        if (skilltree != null) {
            for (int i = fromLvl; i > lvl; i--) {
                List<Upgrade<?>> upgrades = skilltree.getUpgrades(i);
                for (Upgrade<?> upgrade : upgrades) {
                    if (upgrade == null) {
                        continue;
                    }
                    SkillName sn = Util.getClassAnnotation(upgrade.getClass(), SkillName.class);
                    if (sn != null) {
                        Skill skill = pet.getSkills().get(sn.value());
                        if (skill != null) {
                            invertUpgrade(upgrade, skill);
                        }
                    }
                }
            }
        }
        PetArmorApplier.update(pet);

        if (pet.getStatus() == Pet.PetState.Here) {
            Mob entity = pet.getBukkitEntity();
            if (entity != null) {
                pet.updateNameTag();
                if (!event.isQuiet()) {
                    pet.setHealth(pet.getMaxHealth());
                    pet.setSaturation(100);

                    new FixedCircleAnimation(1, entity.getEyeHeight() + 0.5, 10, () -> entity.isDead() ? null : entity.getLocation()) {
                        @Override
                        protected void playParticleEffect(Location location) {
                            location.getWorld().spawnParticle(Particle.BLOCK, location, 1, 0, 0, 0, 0, Material.REDSTONE_BLOCK.createBlockData());
                        }
                    }.once();

                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1F, 0.7F);
                }
            }
        }
    }

    @EventHandler
    public void on(PetSelectSkilltreeEvent event) {
        if (!(event.getPet() instanceof Pet pet)) {
            return;
        }
        int lvl = pet.getExperience().getLevel();

        // Skill objects are reused across skilltree changes: reset() zeroes their upgrades but keeps
        // held state (the Backpack keeps its stored items). If the new tree no longer grants Backpack,
        // those items would be orphaned — inaccessible and never dropped — so remember it was active.
        boolean hadBackpack = pet.getSkills().isActive(BackpackImpl.class);

        pet.getSkills().all().forEach(Skill::reset);

        Skilltree skilltree = event.getSkilltree();
        if (skilltree != null) {
            for (int i = 1; i <= lvl; i++) {
                List<Upgrade<?>> upgrades = skilltree.getUpgrades(i);
                for (Upgrade<?> upgrade : upgrades) {
                    if (upgrade == null) {
                        continue;
                    }
                    SkillName sn = Util.getClassAnnotation(upgrade.getClass(), SkillName.class);
                    if (sn != null) {
                        Skill skill = pet.getSkills().get(sn.value());
                        if (skill != null) {
                            applyUpgrade(upgrade, skill);
                        }
                    }
                }
            }
        }
        if (hadBackpack && !pet.getSkills().isActive(BackpackImpl.class)) {
            dropLostBackpack(pet);
        }
        PetArmorApplier.update(pet);
    }

    /** Drops a pet's backpack contents at its location when a skilltree change takes the Backpack skill away. */
    private static void dropLostBackpack(Pet pet) {
        BackpackImpl backpack = pet.getSkills().get(BackpackImpl.class);
        if (backpack == null) {
            return;
        }
        Location dropAt = pet.getLocation().orElseGet(() ->
                pet.getOwner() != null && pet.getOwner().isOnline() ? pet.getOwner().getPlayer().getLocation() : null);
        if (dropAt == null) {
            return; // nowhere to drop (pet stored, owner offline) — leave the items in place for next time
        }
        backpack.closeInventory();
        backpack.dropContents(dropAt);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Skill> void applyUpgrade(Upgrade<T> upgrade, Skill skill) {
        upgrade.apply((T) skill);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Skill> void invertUpgrade(Upgrade<T> upgrade, Skill skill) {
        upgrade.invert((T) skill);
    }
}