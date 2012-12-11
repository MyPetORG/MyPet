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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.MyPetCustomInventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Inventory extends MyPetGenericSkill
{
    public MyPetCustomInventory inv = new MyPetCustomInventory("Pet's Inventory", 0);
    public static boolean creative = true;

    public Inventory()
    {
        super("Inventory",6);
    }

    @Override
    public void activate()
    {
        if (myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !creative && !MyPetPermissions.has(myPet.getOwner().getPlayer(), "MyPet.admin"))
        {
            myPet.sendMessageToOwner(MyPetLanguage.getString("Msg_InventoryCreative"));
        }
        else if (level > 0)
        {
            if (myPet.getLocation().getBlock().getType() != Material.STATIONARY_WATER && myPet.getLocation().getBlock().getType() != Material.WATER)
            {
                inv.setName(myPet.petName);
                OpenInventory(myPet.getOwner().getPlayer());
            }
            else
            {
                myPet.sendMessageToOwner(MyPetLanguage.getString("Msg_InventorySwimming"));
            }
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoInventory")).replace("%petname%", myPet.petName));
        }
    }

    @Override
    public void setLevel(int level)
    {
        super.setLevel(level);
        inv.setSize(this.level * 9);
    }

    @Override
    public void upgrade()
    {
        super.upgrade();
        inv.setSize(level * 9);
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Inventory")).replace("%petname%", myPet.petName).replace("%size%", "" + inv.getSize()));
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
        NBTTagCompound nbtTagCompound = new NBTTagCompound(skillName);
        inv.save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
        inv.setName(myPet.petName);
    }
}