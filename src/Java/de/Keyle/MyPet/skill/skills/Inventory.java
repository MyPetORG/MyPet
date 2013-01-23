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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skills.inventory.MyPetCustomInventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.EntityPlayer;
import net.minecraft.server.v1_4_R1.NBTTagCompound;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

@SkillName("Inventory")
@SkillProperties(parameterNames = {"add"},
        parameterTypes = {NBTdatatypes.Int},
        parameterDefaultValues = {"1"})
public class Inventory extends MyPetGenericSkill
{
    public MyPetCustomInventory inv = new MyPetCustomInventory("Pet's Inventory", 0);
    public static boolean OPEN_IN_CREATIVEMODE = true;
    private int rows = 0;

    public Inventory(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("add"))
        {
            html = html.replace("value=\"0\"", "value=\"" + getProperties().getInt("add") + "\"");
        }
        return html;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Inventory)
        {
            if (upgrade.getProperties().hasKey("add"))
            {
                rows += upgrade.getProperties().getInt("add");
                if (rows > 6)
                {
                    rows = 6;
                }
                inv.setSize(rows * 9);
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Inventory")).replace("%petname%", myPet.petName).replace("%size%", "" + inv.getSize()));
                }
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return rows + " " + MyPetLanguage.getString("Name_Rows");
    }

    public void reset()
    {
        rows = 0;
        inv.setSize(0);
    }

    @Override
    public void activate()
    {
        if (myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !OPEN_IN_CREATIVEMODE && !MyPetPermissions.has(myPet.getOwner().getPlayer(), "MyPet.admin"))
        {
            myPet.sendMessageToOwner(MyPetLanguage.getString("Msg_InventoryCreative"));
        }
        else if (rows > 0)
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
        NBTTagCompound nbtTagCompound = new NBTTagCompound(getName());
        inv.save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
        inv.setName(myPet.petName);
    }

    @Override
    public boolean isActive()
    {
        return rows > 0;
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Inventory(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}