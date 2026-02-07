/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetLevelDownEvent;
import de.Keyle.MyPet.api.event.MyPetLevelEvent;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.animation.particle.FixedCircleAnimation;
import de.Keyle.MyPet.api.util.animation.particle.SpiralAnimation;
import de.Keyle.MyPet.api.util.location.EntityLocationHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class LevelListener implements Listener {

    @EventHandler
    public void on(MyPetLevelUpEvent event) {
        MyPet myPet = event.getPet();
        int lvl = event.getLevel();
        int fromLvl = event.fromLevel();

        if (!event.isQuiet()) {
            int maxlevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : 0;
            if (maxlevel != 0 && lvl >= maxlevel) {
                myPet.getOwner().sendMessage(Util.formatTranslation("Message.LevelSystem.ReachedMaxLevel", event.getOwner(), myPet.getPetName(), maxlevel));
            } else {
                myPet.getOwner().sendMessage(Util.formatTranslation("Message.LevelSystem.LevelUp", event.getOwner(), myPet.getPetName(), event.getLevel()));
            }
        }
        Skilltree skilltree = myPet.getSkilltree();
        if (skilltree != null) {
            for (int i = fromLvl + 1; i <= lvl; i++) {
                if (!event.isQuiet()) {
                    List<String> notifications = skilltree.getNotifications(i);
                    for (String notification : notifications) {
                        notification = notification
                                .replace("{{owner}}", myPet.getOwner().getName())
                                .replace("{{level}}", "" + lvl)
                                .replace("{{pet}}", myPet.getPetName());
                        notification = Colorizer.setColors(notification);
                        String[] lines = notification.split("(<br>|\\\\n|\n|<br\\s?/>)");
                        for (String line : lines) {
                            myPet.getOwner().sendMessage(Component.text(line));
                        }
                    }
                }
                Set<Skill> affectedSkills = new HashSet<>();
                List<Upgrade> upgrades = skilltree.getUpgrades(i);
                for (Upgrade upgrade : upgrades) {
                    if (upgrade == null) {
                        continue;
                    }
                    SkillName sn = Util.getClassAnnotation(upgrade.getClass(), SkillName.class);
                    if (sn != null) {
                        Skill skill = myPet.getSkills().get(sn.value());
                        if (skill != null) {
                            upgrade.apply(skill);
                            affectedSkills.add(skill);
                        }
                    }
                }
                if (!event.isQuiet()) {
                    for (Skill skill : affectedSkills) {
                        String[] messages = skill.getUpgradeMessage();
                        if (messages != null) {
                            for (String message : messages) {
                                myPet.getOwner().sendMessage(Component.text("  " + message));
                            }
                        }
                    }
                }
            }
        }

        if (myPet.getStatus() == MyPet.PetState.Here) {
            myPet.getEntity().ifPresent(entity -> {
                entity.getHandle().updateNameTag();
                if (!event.isQuiet()) {
                    myPet.setHealth(myPet.getMaxHealth());
                    myPet.setSaturation(100);

                    new SpiralAnimation(1, entity.getEyeHeight() + 0.5, new EntityLocationHolder(entity)) {
                        @Override
                        protected void playParticleEffect(Location location) {
                            location.getWorld().spawnParticle(Particle.ENCHANTED_HIT, location, 1, 0, 0, 0, 0);
                        }
                    }.loop(2);

                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.7F);
                }
            });
        }
    }

    @EventHandler
    public void on(MyPetLevelDownEvent event) {
        MyPet myPet = event.getPet();
        int lvl = event.getLevel();
        int fromLvl = event.fromLevel();

        if (!event.isQuiet()) {
            myPet.getOwner().sendMessage(Util.formatTranslation("Message.LevelSystem.LevelDown", event.getOwner(), myPet.getPetName(), event.getLevel()));
        }
        Skilltree skilltree = myPet.getSkilltree();
        if (skilltree != null) {
            for (int i = fromLvl; i > lvl; i--) {
                List<Upgrade> upgrades = skilltree.getUpgrades(i);
                for (Upgrade upgrade : upgrades) {
                    if (upgrade == null) {
                        continue;
                    }
                    SkillName sn = Util.getClassAnnotation(upgrade.getClass(), SkillName.class);
                    if (sn != null) {
                        Skill skill = myPet.getSkills().get(sn.value());
                        if (skill != null) {
                            upgrade.invert(skill);
                        }
                    }
                }
            }
        }

        if (myPet.getStatus() == MyPet.PetState.Here) {
            myPet.getEntity().ifPresent(entity -> {
                entity.getHandle().updateNameTag();
                if (!event.isQuiet()) {
                    myPet.setHealth(myPet.getMaxHealth());
                    myPet.setSaturation(100);

                    new FixedCircleAnimation(1, entity.getEyeHeight() + 0.5, 10, new EntityLocationHolder(entity)) {
                        @Override
                        protected void playParticleEffect(Location location) {
                            location.getWorld().spawnParticle(Particle.BLOCK, location, 1, 0, 0, 0, 0, Material.REDSTONE_BLOCK.createBlockData());
                        }
                    }.once();

                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1F, 0.7F);
                }
            });
        }
    }

    @EventHandler
    public void on(MyPetLevelEvent event) {
        if (event instanceof MyPetLevelUpEvent || event instanceof MyPetLevelDownEvent) {
            return;
        }
        MyPet myPet = event.getPet();
        int lvl = event.getLevel();

        myPet.getSkills().all().forEach(Skill::reset);

        Skilltree skilltree = myPet.getSkilltree();
        if (skilltree != null) {
            for (int i = 1; i <= lvl; i++) {
                List<Upgrade> upgrades = skilltree.getUpgrades(i);
                for (Upgrade upgrade : upgrades) {
                    if (upgrade == null) {
                        continue;
                    }
                    SkillName sn = Util.getClassAnnotation(upgrade.getClass(), SkillName.class);
                    if (sn != null) {
                        Skill skill = myPet.getSkills().get(sn.value());
                        if (skill != null) {
                            upgrade.apply(skill);
                        }
                    }
                }
            }
        }
    }
}