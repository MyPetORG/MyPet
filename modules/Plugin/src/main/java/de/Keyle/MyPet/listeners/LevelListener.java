/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
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
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.location.EntityLocationHolder;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

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
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.ReachedMaxLevel", event.getOwner().getLanguage()), myPet.getPetName(), maxlevel));
            } else {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.LevelUp", event.getOwner().getLanguage()), myPet.getPetName(), event.getLevel()));
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
                            myPet.getOwner().sendMessage(line);
                        }
                    }
                }
                List<Upgrade> upgrades = skilltree.getUpgrades(i);
                for (Upgrade upgrade : upgrades) {
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

        if (myPet.getStatus() == MyPet.PetState.Here) {
            MyPetBukkitEntity entity = myPet.getEntity().get();
            entity.getHandle().updateNameTag();
            if (!event.isQuiet()) {
                myPet.setHealth(myPet.getMaxHealth());
                myPet.setSaturation(100);

                final boolean version17 = MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.8") < 0;

                new SpiralAnimation(1, entity.getEyeHeight() + 0.5, new EntityLocationHolder(entity)) {
                    @Override
                    protected void playParticleEffect(Location location) {
                        if (version17) {
                            MyPetApi.getPlatformHelper().playParticleEffect(location, "magicCrit", 0, 0, 0, 0, 1, 32);
                        } else {
                            MyPetApi.getPlatformHelper().playParticleEffect(location, "CRIT_MAGIC", 0, 0, 0, 0, 1, 32);

                        }
                    }
                }.loop(2);

                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                    entity.getWorld().playSound(entity.getLocation(), "entity.player.levelup", 1F, 0.7F);
                } else {
                    entity.getWorld().playSound(entity.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 1F, 0.7F);
                }
            }
        }
    }

    @EventHandler
    public void on(MyPetLevelDownEvent event) {
        MyPet myPet = event.getPet();
        int lvl = event.getLevel();
        int fromLvl = event.fromLevel();

        if (!event.isQuiet()) {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.LevelDown", event.getOwner().getLanguage()), myPet.getPetName(), event.getLevel()));
        }
        Skilltree skilltree = myPet.getSkilltree();
        if (skilltree != null) {
            for (int i = fromLvl; i > lvl; i--) {
                List<Upgrade> upgrades = skilltree.getUpgrades(i);
                for (Upgrade upgrade : upgrades) {
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
            MyPetBukkitEntity entity = myPet.getEntity().get();
            entity.getHandle().updateNameTag();
            if (!event.isQuiet()) {
                myPet.setHealth(myPet.getMaxHealth());
                myPet.setSaturation(100);

                final boolean version17 = MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.8") < 0;

                new FixedCircleAnimation(1, entity.getEyeHeight() + 0.5, 10, new EntityLocationHolder(entity)) {
                    @Override
                    protected void playParticleEffect(Location location) {
                        if (version17) {
                            MyPetApi.getPlatformHelper().playParticleEffect(location, "blockcrack", 0, 0, 0, 0, 1, 32, 152);
                        } else {
                            MyPetApi.getPlatformHelper().playParticleEffect(location, "BLOCK_CRACK", 0, 0, 0, 0, 1, 32, 152);

                        }
                    }
                }.once();

                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                    entity.getWorld().playSound(entity.getLocation(), "random.anvil_break", 1F, 0.7F);
                } else {
                    entity.getWorld().playSound(entity.getLocation(), Sound.valueOf("ENTITY_WITHER_BREAK_BLOCK"), 1F, 0.7F);
                }
            }
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