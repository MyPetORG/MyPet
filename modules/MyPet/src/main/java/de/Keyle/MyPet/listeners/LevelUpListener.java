/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.animation.particle.SpiralAnimation;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.location.EntityLocationHolder;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class LevelUpListener implements Listener {
    @EventHandler
    public void on(MyPetLevelUpEvent event) {
        MyPet myPet = event.getPet();
        int lvl = event.getLevel();
        int lastLvl = event.getLastLevel();

        if (!event.isQuiet()) {
            int maxlevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : 0;
            if (maxlevel != 0 && lvl >= maxlevel) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.ReachedMaxLevel", event.getOwner().getLanguage()), myPet.getPetName(), maxlevel));
            } else {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.LevelSystem.LevelUp", event.getOwner().getLanguage()), myPet.getPetName(), event.getLevel()));
            }
        }
        SkillTree skillTree = myPet.getSkilltree();
        if (skillTree != null) {
            if (skillTree.getLastLevelWithSkills() < lvl) {
                lvl = skillTree.getLastLevelWithSkills();
            }
            for (int i = lastLvl + 1; i <= lvl; i++) {
                if (skillTree.hasLevel(i)) {
                    SkillTreeLevel level = skillTree.getLevel(i);
                    if (!event.isQuiet()) {
                        if (level.hasLevelupMessage()) {
                            myPet.getOwner().sendMessage(Colorizer.setColors(level.getLevelupMessage()));
                        }
                    }

                    List<SkillInfo> skillList = level.getSkills();
                    for (SkillInfo skill : skillList) {
                        myPet.getSkills().getSkill(skill.getName()).upgrade(skill, event.isQuiet());
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

                new SpiralAnimation(1, entity.getEyeHeight() + 0.5, new EntityLocationHolder(entity)) {
                    @Override
                    protected void playParticleEffect(Location location) {
                        MyPetApi.getPlatformHelper().playParticleEffect(location, "magicCrit", 0, 0, 0, 0, 1, 32);
                        //MyPetApi.getPlatformHelper().playParticleEffect(location, "flame", 0, 0, 0, 0, 1, 32);
                    }
                }.loop(2);

                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                    entity.getWorld().playSound(entity.getLocation(), "entity.player.levelup", 1F, 0.7F);
                } else {
                    entity.getWorld().playSound(entity.getLocation(), "random.levelup", 1F, 0.7F);
                }
            }
        }
    }
}