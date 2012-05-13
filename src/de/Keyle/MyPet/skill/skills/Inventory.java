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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetCustomInventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Inventory extends MyPetGenericSkill
{
    public static final List<Player> PetChestOpened = new ArrayList<Player>();
    public MyPetCustomInventory inv = new MyPetCustomInventory("Wolf's Inventory", 0);

    public Inventory()
    {
        super("Inventory");
    }

    @Override
    public void activate()
    {
        if (Level > 0)
        {
            if (MPet.getLocation().getBlock().getType() != Material.STATIONARY_WATER && MPet.getLocation().getBlock().getType() != Material.WATER)
            {
                inv.setName(MPet.Name);
                OpenInventory(MPet.getOwner().getPlayer());
                if (!MPet.isSitting())
                {
                    PetChestOpened.add(MPet.getOwner().getPlayer());
                }
                MPet.Wolf.setSitting(true);
            }
            else
            {
                MPet.sendMessageToOwner(MyPetLanguage.getString("Msg_InventorySwimming"));
            }
        }
        else
        {
            MPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoInventory")).replace("%wolfname%", MPet.Name));
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
        MPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Inventory")).replace("%wolfname%", MPet.Name).replace("%size%", "" + inv.getSize()));
    }

    public void OpenInventory(Player p)
    {
        EntityPlayer eh = ((CraftPlayer) p).getHandle();
        eh.openContainer(inv);
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
    public void setMyWolf(MyWolf MPet)
    {
        this.MPet = MPet;
        inv.setName(MPet.Name);
    }
}