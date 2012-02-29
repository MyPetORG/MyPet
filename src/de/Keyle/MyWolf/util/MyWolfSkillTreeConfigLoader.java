/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util;


import de.Keyle.MyWolf.skill.MyWolfSkillTree;
import de.Keyle.MyWolf.util.configuration.MyWolfYamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MyWolfSkillTreeConfigLoader
{
    private static Map<String, MyWolfSkillTree> SkillTrees = new HashMap<String, MyWolfSkillTree>();
    private static Map<String, String> Inheritances = new HashMap<String, String>();

    private static MyWolfYamlConfiguration MWConfig = null;

    public static void setConfig(MyWolfYamlConfiguration newMWConfig)
    {
        MWConfig = newMWConfig;
    }

    public static void loadSkillTrees()
    {
        if (MWConfig == null)
        {
            return;
        }
        SkillTrees.clear();
        Inheritances.clear();
        ConfigurationSection sec = MWConfig.getConfig().getConfigurationSection("skilltrees");
        if (sec == null)
        {
            return;
        }
        Set<String> SkillTree = sec.getKeys(false);
        if (SkillTree.size() > 0)
        {
            for (String ST : SkillTree)
            {
                String inherit = MWConfig.getConfig().getString("skilltrees." + ST + ".inherit", "%#_DeFaUlT_#%");
                if (!inherit.equals("%#_DeFaUlT_#%"))
                {
                    Inheritances.put(ST, inherit);
                }
                Set<String> Level = MWConfig.getConfig().getConfigurationSection("skilltrees." + ST).getKeys(false);
                if (Level.size() != 0)
                {
                    MyWolfSkillTree MWST = new MyWolfSkillTree(ST);
                    for (String Lv : Level)
                    {
                        if (MyWolfUtil.isInt(Lv))
                        {
                            MWST.addLevel(Integer.parseInt(Lv), MWConfig.getConfig().getStringList("skilltrees." + ST + "." + Lv));
                        }
                    }
                    SkillTrees.put(ST, MWST);
                }
            }
        }
    }

    public static MyWolfSkillTree getSkillTree(String Name)
    {
        if (SkillTrees.containsKey(Name))
        {
            MyWolfSkillTree MWST = (MyWolfSkillTree) SkillTrees.get(Name).cloneSkillTree();

            //MyWolfUtil.Log.info("----- clone: " + MWST.getName() + " ---- FI: " + MWST.getSkills(1)[0]);
            //MyWolfUtil.Log.info("------Inheritances: " + Inheritances.toString());
            if (Inheritances.containsKey(Name))
            {
                String NextInheritance = Inheritances.get(Name);
                while (!NextInheritance.isEmpty())
                {
                    //MyWolfUtil.Log.info("------Next: " + NextInheritance + " ---- is there: " + SkillTrees.containsKey(Name));
                    if (SkillTrees.containsKey(Name))
                    {
                        MyWolfSkillTree NextMWST = SkillTrees.get(NextInheritance);
                        //MyWolfUtil.Log.info("------Next: " + NextInheritance + " ---- Name(Object): " + NextMWST.getName());
                        if (NextMWST.getLevels() != null)
                        {
                            //MyWolfUtil.Log.info("------Next: " + NextInheritance + " ---- Name(Object): " + NextMWST.getName() + " -- Levels: " + NextMWST.getLevels().length);
                            for (int level : NextMWST.getLevels())
                            {
                                MWST.addSkillToLevel(level, NextMWST.getSkills(level));
                            }
                            //MyWolfUtil.Log.info("------Next: " + NextInheritance + " ---- Name(Object): " + NextMWST.getName() + " -- Next: " + Inheritances.get(NextInheritance));
                            if (Inheritances.containsKey(NextInheritance))
                            {
                                NextInheritance = Inheritances.get(NextInheritance);
                            }
                            else
                            {
                                NextInheritance = "";
                            }
                        }
                    }
                }
            }
            return MWST;
        }
        return null;
    }

    public static String getInheritance(String Name)
    {
        if (Inheritances.containsKey(Name))
        {
            return Inheritances.get(Name);
        }
        return null;
    }

    public static String[] getSkillTreeNames()
    {
        if (SkillTrees.keySet().size() > 0)
        {
            String[] TN = new String[SkillTrees.keySet().size()];
            int i = 0;
            for (String name : SkillTrees.keySet())
            {
                TN[i] = name;
                i++;
            }
            return TN;
        }
        return new String[0];
    }

    public static boolean existsSkillTree(String Name)
    {
        return SkillTrees.containsKey(Name);
    }
}
