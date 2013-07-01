/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.ISkillActive;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.LightningInfo;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import java.util.Random;

public class Lightning extends LightningInfo implements ISkillInstance, ISkillActive
{
    private static Random random = new Random();
    private MyPet myPet;
    private boolean isStriking = false;

    public Lightning(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public boolean isActive()
    {
        return chance > 0 && damage > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof LightningInfo)
        {
            boolean valuesEdit = false;
            if (upgrade.getProperties().getValue().containsKey("chance"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_chance") || ((StringTag) upgrade.getProperties().getValue().get("addset_chance")).getValue().equals("add"))
                {
                    chance += ((IntTag) upgrade.getProperties().getValue().get("chance")).getValue();
                }
                else
                {
                    chance = ((IntTag) upgrade.getProperties().getValue().get("chance")).getValue();
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getValue().containsKey("damage"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_damage") || ((StringTag) upgrade.getProperties().getValue().get("addset_damage")).getValue().equals("add"))
                {
                    damage += ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                }
                else
                {
                    damage = ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                }
                valuesEdit = true;
            }
            if (!quiet && valuesEdit)
            {
                myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.Skill.Lightning.Upgrade", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()).replace("%chance%", "" + chance).replace("%damage%", "" + damage));
            }
        }
    }

    public String getFormattedValue()
    {
        return "" + ChatColor.GOLD + chance + ChatColor.RESET + "% -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + MyPetLocales.getString("Name.Damage", myPet.getOwner());
    }

    public void reset()
    {
        chance = 0;
        damage = 0;
    }

    public boolean activate()
    {
        return !isStriking && random.nextDouble() <= chance / 100.;
    }

    public void strikeLightning(Location loc)
    {
        Player owner = myPet.getOwner().getPlayer();
        CraftMyPet craftMyPet = myPet.getCraftPet();
        isStriking = true;
        loc.getWorld().strikeLightningEffect(loc);
        for (LivingEntity entity : loc.getWorld().getLivingEntities())
        {
            if (craftMyPet != entity && owner != entity & loc.distance(entity.getLocation()) <= 1.2)
            {
                entity.damage(damage, myPet.getCraftPet());
            }
        }
        isStriking = false;
    }

    @Override
    public ISkillInstance cloneSkill()
    {
        Lightning newSkill = new Lightning(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}