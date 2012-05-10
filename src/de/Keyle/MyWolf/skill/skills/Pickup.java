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
import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.MyWolfCustomInventory;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.Packet22Collect;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class Pickup extends MyWolfGenericSkill
{
    public static double RangePerLevel = 1;
    private boolean Pickup = false;

    public Pickup()
    {
        super("Pickup");
    }

    @Override
    public void activate()
    {
        if (Level > 0)
        {
            if (MWolf.SkillSystem.hasSkill("Inventory") && MWolf.SkillSystem.getSkill("Inventory").getLevel() > 0)
            {
                Pickup = !Pickup;
                MWolf.sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString((Pickup ? "Msg_PickUpStart" : "Msg_PickUpStop"))).replace("%wolfname%", MWolf.Name));
            }
            else
            {
                MWolf.sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_PickButNoInventory")).replace("%wolfname%", MWolf.Name));
            }
        }
        else
        {
            MWolf.sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_NoSkill")).replace("%wolfname%", MWolf.Name).replace("%skill%", this.Name));
        }
    }

    @Override
    public void upgrade()
    {
        Level++;
        MWolf.sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_AddPickup")).replace("%wolfname%", MWolf.Name).replace("%range%", "" + (Level * RangePerLevel)));
    }

    @Override
    public void schedule()
    {
        if (Level > 0 && Pickup && MWolf.Status == MyWolf.WolfState.Here && MWolf.SkillSystem.hasSkill("Inventory") && MWolf.SkillSystem.getSkill("Inventory").getLevel() > 0)
        {
            for (Entity e : MWolf.Wolf.getNearbyEntities(Level * RangePerLevel, Level * RangePerLevel, RangePerLevel))
            {
                if (e instanceof Item)
                {
                    Item item = (Item) e;

                    PlayerPickupItemEvent ppievent = new PlayerPickupItemEvent(MWolf.getOwner().getPlayer(), item, item.getItemStack().getAmount());
                    MyWolfUtil.getServer().getPluginManager().callEvent(ppievent);

                    if (ppievent.isCancelled())
                    {
                        continue;
                    }

                    MyWolfCustomInventory inv = ((Inventory) MWolf.SkillSystem.getSkill("Inventory")).inv;
                    int ItemAmount = inv.addItem(item.getItemStack());
                    if (ItemAmount == 0)
                    {
                        for (Entity p : e.getNearbyEntities(20, 20, 20))
                        {
                            if (p instanceof Player)
                            {
                                ((CraftPlayer) p).getHandle().netServerHandler.sendPacket(new Packet22Collect(e.getEntityId(), MWolf.Wolf.getEntityId()));
                            }
                        }
                        e.remove();
                    }
                    else
                    {
                        item.getItemStack().setAmount(ItemAmount);
                    }
                }
            }
        }
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        Pickup = nbtTagCompound.getBoolean("Active");
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound(Name);
        nbtTagCompound.setBoolean("Active", Pickup);
        return nbtTagCompound;

    }
}