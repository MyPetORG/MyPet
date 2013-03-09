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

package de.Keyle.MyPet.skill;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.event.MyPetExpEvent;
import de.Keyle.MyPet.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.skill.experience.Default;
import de.Keyle.MyPet.skill.experience.Experience;
import de.Keyle.MyPet.skill.experience.JavaScript;
import de.Keyle.MyPet.util.MyPetConfiguration;
import org.bukkit.entity.EntityType;

import static org.bukkit.Bukkit.getServer;

public class MyPetExperience
{
    public static int LOSS_PERCENT = 0;
    public static double LOSS_FIXED = 0;
    public static boolean DROP_LOST_EXP = true;
    public static boolean GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS = true;
    public static String CALCULATION_MODE = "Default";

    private final MyPet myPet;

    private double exp = 0;
    Experience expMode;

    public MyPetExperience(MyPet pet)
    {
        this.myPet = pet;


        if (CALCULATION_MODE.equalsIgnoreCase("JS") || CALCULATION_MODE.equalsIgnoreCase("JavaScript"))
        {
            expMode = new JavaScript(myPet);
        }
        else
        {
            expMode = new Default(myPet);
        }
        if (!expMode.isUsable())
        {
            expMode = new Default(myPet);
        }

        for (short i = 1 ; i <= getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i, true));
        }
    }

    public void reset()
    {
        exp = 0;

        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
            getServer().getPluginManager().callEvent(spoutEvent);
        }

        for (short i = 1 ; i <= getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i, true));
        }
    }

    public void setExp(double Exp)
    {
        Exp = Exp < 0 ? 0 : Exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.getExp(), Exp);
        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            getServer().getPluginManager().callEvent(expEvent);
        }
        if (expEvent.isCancelled())
        {
            return;
        }
        short tmplvl = getLevel();
        this.exp = expEvent.getExp();

        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
            getServer().getPluginManager().callEvent(spoutEvent);
        }

        for (short i = tmplvl ; i < getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, (short) (i + 1), true));
        }
    }

    public double getExp()
    {
        return this.exp;
    }

    public short addExp(double exp)
    {
        MyPetExpEvent event = new MyPetExpEvent(myPet, this.exp, this.exp + exp);
        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            getServer().getPluginManager().callEvent(event);
        }
        if (event.isCancelled())
        {
            return 0;
        }
        short tmpLvl = getLevel();
        this.exp = event.getExp();

        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
            getServer().getPluginManager().callEvent(spoutEvent);
        }


        for (short i = tmpLvl ; i < getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, (short) (i + 1)));
        }
        return (short) (event.getNewExp() - event.getOldExp());
    }

    public short addExp(EntityType type)
    {
        if (MyPetMonsterExperience.hasMonsterExperience(type))
        {
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, MyPetMonsterExperience.getMonsterExperience(type).getRandomExp() + this.exp);
            if (MyPetConfiguration.ENABLE_EVENTS)
            {
                getServer().getPluginManager().callEvent(expEvent);
            }
            if (expEvent.isCancelled())
            {
                return 0;
            }
            short tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            if (MyPetConfiguration.ENABLE_EVENTS)
            {
                MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
                getServer().getPluginManager().callEvent(spoutEvent);
            }

            for (short i = tmpLvl ; i < getLevel() ; i++)
            {
                getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, (short) (i + 1)));
            }
            return (short) (expEvent.getNewExp() - expEvent.getOldExp());
        }
        return 0;
    }

    public short addExp(EntityType type, int percent)
    {
        if (MyPetMonsterExperience.hasMonsterExperience(type))
        {
            double exp = MyPetMonsterExperience.getMonsterExperience(type).getRandomExp() / 100. * percent;
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, exp + this.exp);
            if (MyPetConfiguration.ENABLE_EVENTS)
            {
                getServer().getPluginManager().callEvent(expEvent);
            }
            if (expEvent.isCancelled())
            {
                return 0;
            }
            short tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            if (MyPetConfiguration.ENABLE_EVENTS)
            {
                MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
                getServer().getPluginManager().callEvent(spoutEvent);
            }

            for (short i = tmpLvl ; i < getLevel() ; i++)
            {
                getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, (short) (i + 1)));
            }
            return (short) (expEvent.getNewExp() - expEvent.getOldExp());
        }
        return 0;
    }

    public void removeCurrentExp(double exp)
    {
        if (exp > getCurrentExp())
        {
            exp = getCurrentExp();
        }
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, this.exp - exp);
        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            getServer().getPluginManager().callEvent(expEvent);
        }
        if (expEvent.isCancelled())
        {
            return;
        }
        this.exp = expEvent.getExp();

        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
            getServer().getPluginManager().callEvent(spoutEvent);
        }
    }

    public void removeExp(double exp)
    {
        exp = this.exp - exp < 0 ? this.exp : exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, this.exp - exp);
        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            getServer().getPluginManager().callEvent(expEvent);
        }
        if (expEvent.isCancelled())
        {
            return;
        }
        this.exp = expEvent.getExp();
    }

    public double getCurrentExp()
    {
        double currentExp = expMode.getCurrentExp(this.exp);
        if (!expMode.isUsable())
        {
            expMode = new Default(myPet);
            return expMode.getCurrentExp(this.exp);
        }
        return currentExp;
    }

    public short getLevel()
    {
        short currentExp = expMode.getLevel(this.exp);
        if (!expMode.isUsable())
        {
            expMode = new Default(myPet);
            return expMode.getLevel(this.exp);
        }
        return currentExp;
    }

    public double getRequiredExp()
    {
        double requiredExp = expMode.getRequiredExp(this.exp);
        if (!expMode.isUsable())
        {
            expMode = new Default(myPet);
            return expMode.getRequiredExp(this.exp);
        }
        return requiredExp;
    }
}