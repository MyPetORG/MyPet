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

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.PropertyHandler;
import de.Keyle.MyPet.skill.PropertyHandler.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetCustomInventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.NBTTagCompound;
import net.minecraft.server.v1_4_6.Packet22Collect;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;

@PropertyHandler(html = Fire.html, parameterNames = {"add"}, parameterTypes = {NBTdatatypes.Double})
public class Pickup extends MyPetGenericSkill
{
    protected final static String html = "<html>\n" +
            "   <body>\n" +
            "       <form action=\"#\">\n" +
            "           <p>Increase pickup range by:</p>" +
            "           <input name=\"add\" type=\"text\" /><br/><br/>" +
            "           <input type=\"submit\" value=\"Save\" />" +
            "       </form>\n" +
            "   </body>\n" +
            "</html>\n";

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
            if (myPet.getSkills().getSkillLevel("Inventory") > 0)
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
        super.upgrade();
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddPickup")).replace("%petname%", myPet.petName).replace("%range%", "" + (level * rangePerLevel)));
    }

    @Override
    public void schedule()
    {
        if (level > 0 && pickup && myPet.status == PetState.Here && myPet.getSkills().getSkillLevel("Inventory") > 0)
        {
            for (Entity e : myPet.getCraftPet().getNearbyEntities(level * rangePerLevel, rangePerLevel, level * rangePerLevel))
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

                    MyPetCustomInventory inv = ((Inventory) myPet.getSkills().getSkill("Inventory")).inv;
                    int itemAmount = inv.addItem(item.getItemStack());
                    if (itemAmount == 0)
                    {
                        for (Entity p : e.getNearbyEntities(20, 20, 20))
                        {
                            if (p instanceof Player)
                            {
                                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new Packet22Collect(e.getEntityId(), myPet.getCraftPet().getEntityId()));
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

    public void reset()
    {
        super.reset();
        pickup = false;
    }
}