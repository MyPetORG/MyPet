/*
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.event.MyPetExpEvent;
import de.Keyle.MyPet.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class MyPetExperience
{
    public static int LOSS_PERCENT = 0;
    public static double LOSS_FIXED = 0;
    public static boolean DROP_LOST_EXP = true;

    private final MyPet myPet;

    private double exp = 0;
    MyPetJSexp JSexp;

    private short lastLevel = 1;
    private double lastExpL = Double.NaN;
    private double lastExpC = Double.NaN;
    private double lastExpR = Double.NaN;
    private double lastCurrentExp = 0.0;
    private double lastRequiredExp = 0.0;

    public static final Map<EntityType, MyPetMonsterExperience> mobExp = new HashMap<EntityType, MyPetMonsterExperience>();

    static
    {
        mobExp.put(EntityType.SKELETON, new MyPetMonsterExperience(5., EntityType.SKELETON));
        mobExp.put(EntityType.ZOMBIE, new MyPetMonsterExperience(5., EntityType.ZOMBIE));
        mobExp.put(EntityType.SPIDER, new MyPetMonsterExperience(5., EntityType.SPIDER));
        mobExp.put(EntityType.WOLF, new MyPetMonsterExperience(1., 3., EntityType.WOLF));
        mobExp.put(EntityType.CREEPER, new MyPetMonsterExperience(5., EntityType.CREEPER));
        mobExp.put(EntityType.GHAST, new MyPetMonsterExperience(5., EntityType.GHAST));
        mobExp.put(EntityType.PIG_ZOMBIE, new MyPetMonsterExperience(5., EntityType.PIG_ZOMBIE));
        mobExp.put(EntityType.ENDERMAN, new MyPetMonsterExperience(5., EntityType.ENDERMAN));
        mobExp.put(EntityType.CAVE_SPIDER, new MyPetMonsterExperience(5., EntityType.CAVE_SPIDER));
        mobExp.put(EntityType.MAGMA_CUBE, new MyPetMonsterExperience(1., 4., EntityType.MAGMA_CUBE));
        mobExp.put(EntityType.SLIME, new MyPetMonsterExperience(1., 4., EntityType.SLIME));
        mobExp.put(EntityType.SILVERFISH, new MyPetMonsterExperience(5., EntityType.SILVERFISH));
        mobExp.put(EntityType.BLAZE, new MyPetMonsterExperience(10., EntityType.BLAZE));
        mobExp.put(EntityType.GIANT, new MyPetMonsterExperience(25., EntityType.GIANT));
        mobExp.put(EntityType.COW, new MyPetMonsterExperience(1., 3., EntityType.COW));
        mobExp.put(EntityType.PIG, new MyPetMonsterExperience(1., 3., EntityType.PIG));
        mobExp.put(EntityType.CHICKEN, new MyPetMonsterExperience(1., 3., EntityType.CHICKEN));
        mobExp.put(EntityType.SQUID, new MyPetMonsterExperience(1., 3., EntityType.SQUID));
        mobExp.put(EntityType.SHEEP, new MyPetMonsterExperience(1., 3., EntityType.SHEEP));
        mobExp.put(EntityType.OCELOT, new MyPetMonsterExperience(1., 3., EntityType.OCELOT));
        mobExp.put(EntityType.MUSHROOM_COW, new MyPetMonsterExperience(1., 3., EntityType.MUSHROOM_COW));
        mobExp.put(EntityType.VILLAGER, new MyPetMonsterExperience(0., EntityType.VILLAGER));
        mobExp.put(EntityType.SNOWMAN, new MyPetMonsterExperience(0., EntityType.SNOWMAN));
        mobExp.put(EntityType.IRON_GOLEM, new MyPetMonsterExperience(0., EntityType.IRON_GOLEM));
        mobExp.put(EntityType.ENDER_DRAGON, new MyPetMonsterExperience(20000., EntityType.ENDER_DRAGON));
    }

    public MyPetExperience(MyPet pet)
    {
        this.myPet = pet;
        JSexp = new MyPetJSexp(pet, this);
        for (short i = 1 ; i <= getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i, true));
        }
    }

    public void reset()
    {
        exp = 0;

        MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
        getServer().getPluginManager().callEvent(spoutEvent);

        for (short i = 1 ; i <= getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i, true));
        }
    }

    public void setExp(double Exp)
    {
        Exp = Exp < 0 ? 0 : Exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.getExp(), Exp);
        getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled())
        {
            return;
        }
        short tmplvl = getLevel();
        this.exp = expEvent.getExp();

        MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
        getServer().getPluginManager().callEvent(spoutEvent);

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
        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
        {
            return 0;
        }
        short tmpLvl = getLevel();
        this.exp = event.getExp();

        MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
        getServer().getPluginManager().callEvent(spoutEvent);


        for (short i = tmpLvl ; i < getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, (short) (i + 1)));
        }
        return (short) (event.getNewExp() - event.getOldExp());
    }

    public short addExp(EntityType type)
    {
        if (mobExp.containsKey(type))
        {
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, mobExp.get(type).getRandomExp() + this.exp);
            getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled())
            {
                return 0;
            }
            short tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
            getServer().getPluginManager().callEvent(spoutEvent);

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
        if (mobExp.containsKey(type))
        {
            double exp = mobExp.get(type).getRandomExp() / 100. * percent;
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, exp + this.exp);
            getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled())
            {
                return 0;
            }
            short tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
            getServer().getPluginManager().callEvent(spoutEvent);

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
        getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled())
        {
            return;
        }
        this.exp = expEvent.getExp();

        MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.ExpChange);
        getServer().getPluginManager().callEvent(spoutEvent);
    }

    public void removeExp(double exp)
    {
        exp = this.exp - exp < 0 ? this.exp : exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, this.exp - exp);
        getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled())
        {
            return;
        }
        this.exp = expEvent.getExp();
    }

    public double getCurrentExp()
    {
        if (lastExpC == this.exp)
        {
            return lastCurrentExp;
        }
        lastExpC = this.exp;
        if (JSexp.isUsable())
        {
            lastCurrentExp = JSexp.getCurrentExp();
            return lastCurrentExp;
        }
        else
        {
            double tmpExp = this.exp;
            short tmplvl = 0;

            while (tmpExp >= 7 + (short) (tmplvl * 3.5))
            {
                tmpExp -= 7 + (short) (tmplvl * 3.5);
                tmplvl++;
            }
            lastCurrentExp = tmpExp;
            return lastCurrentExp;
        }
    }

    public short getLevel()
    {
        if (lastExpL == this.exp)
        {
            return lastLevel;
        }
        lastExpL = this.exp;
        if (JSexp.isUsable())
        {
            lastLevel = JSexp.getLvl();
            return lastLevel;
        }
        else
        {
            // Minecraft:   E = 7 + roundDown( n * 3.5)

            double tmpExp = this.exp;
            int tmpLvl = 0;

            while (tmpExp >= 7 + (int) (tmpLvl * 3.5))
            {
                tmpExp -= 7 + (int) (tmpLvl * 3.5);
                tmpLvl++;
            }
            lastLevel = (short) (tmpLvl + 1);
            return lastLevel;
        }
    }

    public double getRequiredExp()
    {
        if (lastExpR == this.exp)
        {
            return lastRequiredExp;
        }
        lastExpR = this.exp;
        if (JSexp.isUsable())
        {
            lastRequiredExp = JSexp.getRequiredExp();
            return lastRequiredExp;
        }
        else
        {
            lastRequiredExp = 7 + (short) ((this.getLevel() - 1) * 3.5);
            return lastRequiredExp;
        }
    }
}