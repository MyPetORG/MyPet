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
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skills.inventory.MyPetCustomInventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.NBTTagCompound;
import net.minecraft.server.v1_4_R1.Packet22Collect;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.Locale;

@SkillName("Pickup")
@SkillProperties(
        parameterNames = {"range", "addset_range"},
        parameterTypes = {NBTdatatypes.Double, NBTdatatypes.String},
        parameterDefaultValues = {"1.0", "add"})
public class Pickup extends MyPetGenericSkill
{
    private double range = 0;
    private boolean pickup = false;

    public Pickup(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return range > 0;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Pickup)
        {
            if (upgrade.getProperties().hasKey("range"))
            {
                if (!upgrade.getProperties().hasKey("addset_range") || upgrade.getProperties().getString("addset_range").equals("add"))
                {
                    range += upgrade.getProperties().getDouble("range");
                }
                else
                {
                    range = upgrade.getProperties().getDouble("range");
                }
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddPickup")).replace("%petname%", myPet.petName).replace("%range%", "" + String.format("%1.2f", range)));
                }
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return MyPetLanguage.getString("Name_Range") + ": " + String.format("%1.2f", range) + " " + MyPetLanguage.getString("Name_Blocks");
    }

    public void reset()
    {
        range = 0;
        pickup = false;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("range"))
        {
            html = html.replace("value=\"0.00\"", "value=\"" + String.format(Locale.ENGLISH, "%1.2f", getProperties().getDouble("range")) + "\"");
            if (getProperties().hasKey("addset_range"))
            {
                if (getProperties().getString("addset_range").equals("set"))
                {
                    html = html.replace("name=\"addset_range\" value=\"add\" checked", "name=\"addset_range\" value=\"add\"");
                    html = html.replace("name=\"addset_range\" value=\"set\"", "name=\"addset_range\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    @Override
    public void activate()
    {
        if (range > 0)
        {
            if (myPet.getSkills().isSkillActive("Inventory"))
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
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.getName()));
        }
    }

    @Override
    public void schedule()
    {
        if (range > 0 && pickup && myPet.getStatus() == PetState.Here && myPet.getSkills().isSkillActive("Inventory"))
        {
            for (Entity e : myPet.getCraftPet().getNearbyEntities(range, range, range))
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
                        myPet.getCraftPet().getHandle().makeSound("random.pop", 0.2F, 1.0F);
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
        NBTTagCompound nbtTagCompound = new NBTTagCompound(getName());
        nbtTagCompound.setBoolean("Active", pickup);
        return nbtTagCompound;

    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Pickup(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}