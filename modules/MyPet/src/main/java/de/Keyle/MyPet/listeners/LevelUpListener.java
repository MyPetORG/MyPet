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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class LevelUpListener implements Listener {
    @EventHandler
    public void onLevelUp(MyPetLevelUpEvent event) {
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
            myPet.getEntity().get().getHandle().updateNameTag();
            if (!event.isQuiet()) {
                myPet.setHealth(myPet.getMaxHealth());
                myPet.setHungerValue(100);

                if (Configuration.LevelSystem.FIREWORK) {
                    Firework fw = (Firework) myPet.getLocation().get().getWorld().spawnEntity(myPet.getLocation().get(), EntityType.FIREWORK);
                    FireworkEffect fwe = FireworkEffect.builder().with(Type.STAR).withColor(Color.fromRGB(Configuration.LevelSystem.FIREWORK_COLOR)).withTrail().withFlicker().build();
                    FireworkMeta fwm = fw.getFireworkMeta();
                    fwm.addEffect(fwe);
                    fwm.addEffect(fwe);
                    fwm.addEffect(fwe);
                    fwm.setPower(0);
                    fw.setFireworkMeta(fwm);
                    //fw.detonate(); // the rocket just disappears when used
                }
            }
        }
    }
}