/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.event.MyPetExpEvent;
import de.Keyle.MyPet.event.MyPetLevelUpEvent;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class MyPetExperience
{
    private final MyPet MPet;

    private double Exp = 0;
    MyPetJSexp JSexp;

    public static final Map<EntityType, MyPetMonsterExperience> MobEXP = new HashMap<EntityType, MyPetMonsterExperience>();

    static
    {
        MobEXP.put(EntityType.SKELETON, new MyPetMonsterExperience(5, EntityType.SKELETON));
        MobEXP.put(EntityType.ZOMBIE, new MyPetMonsterExperience(5, EntityType.ZOMBIE));
        MobEXP.put(EntityType.SPIDER, new MyPetMonsterExperience(5, EntityType.SPIDER));
        MobEXP.put(EntityType.WOLF, new MyPetMonsterExperience(1, 3, EntityType.WOLF));
        MobEXP.put(EntityType.CREEPER, new MyPetMonsterExperience(5, EntityType.CREEPER));
        MobEXP.put(EntityType.GHAST, new MyPetMonsterExperience(5, EntityType.GHAST));
        MobEXP.put(EntityType.PIG_ZOMBIE, new MyPetMonsterExperience(5, EntityType.PIG_ZOMBIE));
        MobEXP.put(EntityType.ENDERMAN, new MyPetMonsterExperience(5, EntityType.ENDERMAN));
        MobEXP.put(EntityType.CAVE_SPIDER, new MyPetMonsterExperience(5, EntityType.CAVE_SPIDER));
        MobEXP.put(EntityType.MAGMA_CUBE, new MyPetMonsterExperience(1, 4, EntityType.MAGMA_CUBE));
        MobEXP.put(EntityType.SLIME, new MyPetMonsterExperience(1, 4, EntityType.SLIME));
        MobEXP.put(EntityType.SILVERFISH, new MyPetMonsterExperience(5, EntityType.SILVERFISH));
        MobEXP.put(EntityType.BLAZE, new MyPetMonsterExperience(10, EntityType.BLAZE));
        MobEXP.put(EntityType.GIANT, new MyPetMonsterExperience(25, EntityType.GIANT));
        MobEXP.put(EntityType.COW, new MyPetMonsterExperience(1, 3, EntityType.COW));
        MobEXP.put(EntityType.PIG, new MyPetMonsterExperience(1, 3, EntityType.PIG));
        MobEXP.put(EntityType.CHICKEN, new MyPetMonsterExperience(1, 3, EntityType.CHICKEN));
        MobEXP.put(EntityType.SQUID, new MyPetMonsterExperience(1, 3, EntityType.SQUID));
        MobEXP.put(EntityType.SHEEP, new MyPetMonsterExperience(1, 3, EntityType.SHEEP));
        MobEXP.put(EntityType.OCELOT, new MyPetMonsterExperience(1, 3, EntityType.OCELOT));
        MobEXP.put(EntityType.MUSHROOM_COW, new MyPetMonsterExperience(1, 3, EntityType.MUSHROOM_COW));
        MobEXP.put(EntityType.VILLAGER, new MyPetMonsterExperience(0, EntityType.VILLAGER));
        MobEXP.put(EntityType.SNOWMAN, new MyPetMonsterExperience(0, EntityType.SNOWMAN));
        MobEXP.put(EntityType.IRON_GOLEM, new MyPetMonsterExperience(0, EntityType.IRON_GOLEM));
        MobEXP.put(EntityType.ENDER_DRAGON, new MyPetMonsterExperience(20000, EntityType.ENDER_DRAGON));
    }

    public MyPetExperience(MyPet pet)
    {
        this.MPet = pet;
        JSexp = new MyPetJSexp(pet, this);
        for (int i = 1 ; i <= getLevel() ; i++)
        {
            MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(MPet, i, true));
        }
    }

    public void reset()
    {
        Exp = 0;
        for (int i = 1 ; i <= getLevel() ; i++)
        {
            MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(MPet, i, true));
        }
    }

    public void setExp(double Exp)
    {
        MyPetExpEvent event = new MyPetExpEvent(MPet, this.getExp(), Exp);
        MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
        {
            return;
        }
        int tmplvl = getLevel();
        this.Exp = event.getEXP();
        for (int i = tmplvl ; i < getLevel() ; i++)
        {
            MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(MPet, i + 1, true));
        }
    }

    public double getExp()
    {
        return this.Exp;
    }

    public int addExp(double Exp)
    {
        MyPetExpEvent event = new MyPetExpEvent(MPet, this.Exp, this.Exp + Exp);
        MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
        {
            return 0;
        }
        int tmplvl = getLevel();
        this.Exp = event.getEXP();

        for (int i = tmplvl ; i < getLevel() ; i++)
        {
            MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(MPet, i + 1));
        }
        return (int) (event.getNewEXP() - event.getOldEXP());
    }

    public int addExp(EntityType type)
    {
        if (MobEXP.containsKey(type))
        {
            MyPetExpEvent event = new MyPetExpEvent(MPet, this.Exp, MobEXP.get(type).getRandomExp() + this.Exp);
            MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled())
            {
                return 0;
            }
            int tmplvl = getLevel();
            this.Exp = event.getEXP();
            for (int i = tmplvl ; i < getLevel() ; i++)
            {
                MyPetPlugin.getPlugin().getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(MPet, i + 1));
            }
            return (int) (event.getNewEXP() - event.getOldEXP());
        }
        return 0;
    }

    public double getCurrentExp()
    {
        if (JSexp.isUsable())
        {
            return JSexp.getCurrentExp();
        }
        else
        {
            double tmpEXP = this.Exp;
            int tmplvl = 0;

            while (tmpEXP >= 7 + (int) (tmplvl * 3.5))
            {
                tmpEXP -= 7 + (int) (tmplvl * 3.5);
                tmplvl++;
            }
            return tmpEXP;
        }
    }

    public int getLevel()
    {
        if (JSexp.isUsable())
        {
            return JSexp.getLvl();
        }
        else
        {
            // Minecraft:   E = 7 + roundDown( n * 3.5)

            double tmpEXP = this.Exp;
            int tmplvl = 0;

            while (tmpEXP >= 7 + (int) (tmplvl * 3.5))
            {
                tmpEXP -= 7 + (int) (tmplvl * 3.5);
                tmplvl++;
            }
            return tmplvl + 1;
        }
    }

    public double getRequiredExp()
    {
        if (JSexp.isUsable())
        {
            return JSexp.getRequiredExp();
        }
        else
        {
            return 7 + (int) ((this.getLevel() - 1) * 3.5);
        }
    }
}