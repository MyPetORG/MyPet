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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.compat.SoundCompat;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetLevelDownEvent;
import de.Keyle.MyPet.api.event.MyPetLevelEvent;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.animation.particle.FixedCircleAnimation;
import de.Keyle.MyPet.api.util.animation.particle.SpiralAnimation;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.location.EntityLocationHolder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.ReachedMaxLevel", event.getOwner()), myPet.getPetName(), maxlevel));
            } else {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.LevelUp", event.getOwner()), myPet.getPetName(), event.getLevel()));
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
                        if (messages != null && messages.length > 0) {
                            for (String message : messages) {
                                myPet.getOwner().sendMessage("  " + message);
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
                            MyPetApi.getPlatformHelper().playParticleEffect(location, ParticleCompat.CRIT_MAGIC.get(), 0, 0, 0, 0, 1, 32);
                        }
                    }.loop(2);

                    entity.getWorld().playSound(entity.getLocation(), Sound.valueOf(SoundCompat.LEVEL_UP.get()), 1F, 0.7F);
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
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.LevelDown", event.getOwner()), myPet.getPetName(), event.getLevel()));
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
                            MyPetApi.getPlatformHelper().playParticleEffect(location, ParticleCompat.BLOCK_CRACK.get(), 0, 0, 0, 0, 1, 32, ParticleCompat.REDSTONE_BLOCK_DATA);
                        }
                    }.once();

                    entity.getWorld().playSound(entity.getLocation(), Sound.valueOf(SoundCompat.LEVEL_DOWN.get()), 1F, 0.7F);
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

        // Snapshot backpack contents before the reset-reapply cycle. The reset
        // phase temporarily shrinks the inventory to its minimum size, which
        // would discard items beyond the first row. This must be handled here
        // rather than in BackpackImpl because the UpgradeComputer callback fires
        // incrementally during reapply and cannot determine the final row count.
        Backpack backpack = myPet.getSkills().get(Backpack.class);
        ItemStack[] savedBackpackContents = null;
        if (backpack != null && backpack.getInventory().getBukkitInventory() != null) {
            backpack.getInventory().close();
            ItemStack[] contents = backpack.getInventory().getBukkitInventory().getContents();
            savedBackpackContents = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                savedBackpackContents[i] = contents[i] != null ? contents[i].clone() : null;
            }
        }

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

        // Restore items that still fit and drop overflow as world items.
        // The clear pass prevents duplication when the new skilltree has no
        // backpack at all (inventory clamped to 9 slots still holds old items).
        if (savedBackpackContents != null) {
            Inventory inv = backpack.getInventory().getBukkitInventory();
            if (inv != null) {
                int newSize = backpack.isActive() ? inv.getSize() : 0;
                for (int i = 0; i < newSize && i < savedBackpackContents.length; i++) {
                    inv.setItem(i, savedBackpackContents[i]);
                }
                for (int i = newSize; i < inv.getSize(); i++) {
                    inv.clear(i);
                }
                if (savedBackpackContents.length > newSize) {
                    Location dropLoc = myPet.getLocation().orElse(null);
                    if (dropLoc == null || dropLoc.getWorld() == null) {
                        if (myPet.getOwner().getPlayer() != null) {
                            dropLoc = myPet.getOwner().getPlayer().getLocation();
                        }
                    }
                    if (dropLoc != null && dropLoc.getWorld() != null) {
                        for (int i = newSize; i < savedBackpackContents.length; i++) {
                            ItemStack item = savedBackpackContents[i];
                            if (item != null && item.getType() != Material.AIR) {
                                dropLoc.getWorld().dropItem(dropLoc, item);
                            }
                        }
                    }
                }
            }
        }
    }
}