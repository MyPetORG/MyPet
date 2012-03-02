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

package de.Keyle.MyWolf.skill.skills;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.MyWolfCustomInventory;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import de.Keyle.MyWolf.util.configuration.MyWolfYamlConfiguration;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemStack;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Inventory extends MyWolfGenericSkill
{
    public MyWolfCustomInventory inv = new MyWolfCustomInventory("Wolf's Inventory", 0);

    public Inventory()
    {
        super("Inventory");
    }

    @Override
    public void activate()
    {
        if (Level > 0)
        {
            if (MWolf.getLocation().getBlock().getType() != Material.STATIONARY_WATER && MWolf.getLocation().getBlock().getType() != Material.WATER)
            {
                inv.setName(MWolf.Name);
                OpenInventory(MWolf.getOwner().getPlayer());
                if (!MWolf.isSitting())
                {
                    MyWolfPlugin.WolfChestOpened.add(MWolf.getOwner().getPlayer());
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
    public void setLevel(int level)
    {
        Level = level > 6 ? 6 : level;
        inv.setSize(Level * 9);
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
        eh.openContainer(inv);
    }

    @Override
    public void load(MyWolfYamlConfiguration configuration)
    {
        String Sinv = configuration.getConfig().getString("Wolves." + MWolf.getOwner().getName() + ".inventory", "QwE");
        if (!Sinv.equals("QwE"))
        {
            String[] invSplit = Sinv.split(";");
            for (int i = 0; i < invSplit.length; i++)
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
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        inv.load(nbtTagCompound);
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound(Name);
        inv.save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void setMyWolf(MyWolf MWolf)
    {
        this.MWolf = MWolf;
        inv.setName(MWolf.Name);
    }
}
