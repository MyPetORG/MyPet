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

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.MyPetCustomInventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.Packet22Collect;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class Pickup extends MyPetGenericSkill
{
    public static double rangePerLevel = 1;
    private boolean pickup = false;

    public Pickup()
    {
        super("Pickup");
    }

    @Override
    public void activate()
    {
        if (level > 0)
        {
            if (myPet.getSkillSystem().getSkillLevel("Inventory") > 0)
            {
                pickup = !pickup;
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString((pickup ? "Msg_PickUpStart" : "Msg_PickUpStop"))).replace("%petname%", myPet.petName));
            }
            else
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_PickButNoInventory")).replace("%petname%", myPet.petName));
            }
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
        }
    }

    @Override
    public void upgrade()
    {
        level++;
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddPickup")).replace("%petname%", myPet.petName).replace("%range%", "" + (level * rangePerLevel)));
    }

    @Override
    public void schedule()
    {
        if (level > 0 && pickup && myPet.status == PetState.Here && myPet.getSkillSystem().getSkillLevel("Inventory") > 0)
        {
            for (Entity e : myPet.getCraftPet().getNearbyEntities(level * rangePerLevel, level * rangePerLevel, rangePerLevel))
            {
                if (e instanceof Item)
                {
                    Item item = (Item) e;

                    PlayerPickupItemEvent playerPickupEvent = new PlayerPickupItemEvent(myPet.getOwner().getPlayer(), item, item.getItemStack().getAmount());
                    MyPetUtil.getServer().getPluginManager().callEvent(playerPickupEvent);

                    if (playerPickupEvent.isCancelled())
                    {
                        continue;
                    }

                    MyPetCustomInventory inv = ((Inventory) myPet.getSkillSystem().getSkill("Inventory")).inv;
                    int itemAmount = inv.addItem(item.getItemStack());
                    if (itemAmount == 0)
                    {
                        for (Entity p : e.getNearbyEntities(20, 20, 20))
                        {
                            if (p instanceof Player)
                            {
                                ((CraftPlayer) p).getHandle().netServerHandler.sendPacket(new Packet22Collect(e.getEntityId(), myPet.getCraftPet().getEntityId()));
                            }
                        }
                        e.remove();
                    }
                    else
                    {
                        item.getItemStack().setAmount(itemAmount);
                    }
                }
            }
        }
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        pickup = nbtTagCompound.getBoolean("Active");
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound(skillName);
        nbtTagCompound.setBoolean("Active", pickup);
        return nbtTagCompound;

    }
}