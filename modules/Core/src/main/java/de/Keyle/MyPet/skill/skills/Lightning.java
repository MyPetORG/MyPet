/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.LightningInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Random;

public class Lightning extends LightningInfo implements SkillInstance, ActiveSkill {
    private static Random random = new Random();
    private MyPet myPet;
    private boolean isStriking = false;

    public Lightning(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance > 0 && damage > 0;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof LightningInfo) {
            boolean valuesEdit = false;
            if (upgrade.getProperties().getCompoundData().containsKey("chance")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_chance") || upgrade.getProperties().getAs("addset_chance", TagString.class).getStringData().equals("add")) {
                    chance += upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                } else {
                    chance = upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("damage")) {
                int damage = upgrade.getProperties().getAs("damage", TagInt.class).getIntData();
                upgrade.getProperties().getCompoundData().remove("damage");
                TagDouble TagDouble = new TagDouble(damage);
                upgrade.getProperties().getCompoundData().put("damage_double", TagDouble);
            }
            if (upgrade.getProperties().getCompoundData().containsKey("damage_double")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_damage") || upgrade.getProperties().getAs("addset_damage", TagString.class).getStringData().equals("add")) {
                    damage += upgrade.getProperties().getAs("damage_double", TagDouble.class).getDoubleData();
                } else {
                    damage = upgrade.getProperties().getAs("damage_double", TagDouble.class).getDoubleData();
                }
                valuesEdit = true;
            }
            chance = Math.min(chance, 100);
            if (!quiet && valuesEdit) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Lightning.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, damage));
            }
        }
    }

    public String getFormattedValue() {
        return "" + ChatColor.GOLD + chance + ChatColor.RESET + "% -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Translation.getString("Name.Damage", myPet.getOwner());
    }

    public void reset() {
        chance = 0;
        damage = 0;
    }

    public boolean activate() {
        return !isStriking && random.nextDouble() <= chance / 100.;
    }

    public void strikeLightning(Location loc) {
        Player owner = myPet.getOwner().getPlayer();
        isStriking = true;
        loc.getWorld().strikeLightningEffect(loc);
        for (Entity entity : myPet.getEntity().getNearbyEntities(1.5, 1.5, 1.5)) {
            if (entity instanceof LivingEntity && entity != owner) {
                ((LivingEntity) entity).damage(damage, myPet.getEntity());
            }
        }
        isStriking = false;
    }

    @Override
    public SkillInstance cloneSkill() {
        Lightning newSkill = new Lightning(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}