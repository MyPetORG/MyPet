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

package de.Keyle.MyWolf.Skill.Skills;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.MyWolfConfiguration;
import de.Keyle.MyWolf.util.MyWolfCustomInventory;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class Inventory extends MyWolfGenericSkill
{
    public MyWolfCustomInventory inv = new MyWolfCustomInventory("Wolf's Inventory",0);
    String PathToInventory;

    public Inventory()
    {
        super("Inventory");

        PathToInventory = MyWolfPlugin.getPlugin().getDataFolder().getPath() + File.separator + "Inventory";
        File pti = new File(PathToInventory);
        if(!pti.exists())
        {
            pti.mkdirs();
        }
    }

    @Override
    public void activate()
    {
        if (Level > 0)
        {
            if (MWolf.getLocation().getBlock().getType() != Material.STATIONARY_WATER && MWolf.getLocation().getBlock().getType() != Material.WATER)
            {
                inv.setName(MWolf.Name);
                OpenInventory(MWolf.getOwner());
                if (!MWolf.isSitting())
                {
                    MyWolfPlugin.WolfChestOpened.add(MWolf.getOwner());
                }
                MWolf.Wolf.setSitting(true);
            }
            else
            {
                MWolf.sendMessageToOwner(MyWolfLanguage.getString("Msg_InventorySwimming"));
            }
        }
        else
        {
            MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_NoInventory")).replace("%wolfname%", MWolf.Name));
        }
    }

    @Override
    public void upgrade()
    {
        if (Level >= 6)
        {
            return;
        }
        Level++;
        inv.setSize(Level * 9);
        MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Inventory")).replace("%wolfname%", MWolf.Name).replace("%size%", "" + inv.getSize()));
    }

    public void OpenInventory(Player p)
    {
        EntityPlayer eh = ((CraftPlayer) p).getHandle();
        eh.a(inv);
    }
    
    @Override
    public void load(MyWolfConfiguration configuration)
    {
        String Sinv = configuration.getConfig().getString("Wolves." + MWolf.getOwnerName() + ".inventory", "QwE");
        if(!Sinv.equals("QwE"))
        {
            String[] invSplit = Sinv.split(";");
            for (int i = 0 ; i < invSplit.length ; i++)
            {
                if (i < inv.getSize())
                {
                    String[] itemvalues = invSplit[i].split(",");
                    if (itemvalues.length == 3 && MyWolfUtil.isInt(itemvalues[0]) && MyWolfUtil.isInt(itemvalues[1]) && MyWolfUtil.isInt(itemvalues[2]))
                    {
                        if (Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
                        {
                            if (Integer.parseInt(itemvalues[1]) <= 64)
                            {
                                inv.setItem(i, new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
                            }
                        }
                    }
                }
            }
        }
        else
        {
            try
            {
                File invFile = new File(PathToInventory + File.separator + MWolf.getOwnerName() + ".MyWolfInventory");
                if(invFile.exists())
                {
                    inv.load(invFile);
                }
            }
            catch (IOException e)
            {
                MyWolfUtil.getLogger().info("[MyWolf] Can't load " + MWolf.getOwnerName() + ".MyWolfInventory" );
            }
        }
    }

    @Override
    public void save(MyWolfConfiguration configuration)
    {
        File invFile;
        try
        {
            invFile = new File(PathToInventory + File.separator + MWolf.getOwnerName() + ".MyWolfInventory");
            if(!invFile.exists())
            {
                invFile.createNewFile();
            }
            inv.save(invFile);
        }
        catch (IOException e)
        {
            MyWolfUtil.getLogger().info("[MyWolf] Can't save " + MWolf.getOwnerName() + ".MyWolfInventory" );
            e.printStackTrace();
        }
    }
    
    @Override
    public void setMyWolf(MyWolf MWolf)
    {
        this.MWolf = MWolf;
        inv.setName(MWolf.Name);
    }
}
