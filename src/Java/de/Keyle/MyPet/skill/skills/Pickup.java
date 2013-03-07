/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.Packet22Collect;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.spout.nbt.*;

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
            if (upgrade.getProperties().getValue().containsKey("range"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_range") || ((StringTag) upgrade.getProperties().getValue().get("addset_range")).getValue().equals("add"))
                {
                    range += ((DoubleTag) upgrade.getProperties().getValue().get("range")).getValue();
                }
                else
                {
                    range = ((DoubleTag) upgrade.getProperties().getValue().get("range")).getValue();
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
        if (getProperties().getValue().containsKey("range"))
        {
            double range = ((DoubleTag) getProperties().getValue().get("range")).getValue();
            html = html.replace("value=\"0.00\"", "value=\"" + String.format(Locale.ENGLISH, "%1.2f", range) + "\"");
            if (getProperties().getValue().containsKey("addset_range"))
            {
                if (((StringTag) getProperties().getValue().get("addset_range")).getValue().equals("set"))
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
            if (!MyPetPermissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Pickup"))
            {
                pickup = false;
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_PickUpStop")).replace("%petname%", myPet.petName));
            }
            for (Entity entity : myPet.getCraftPet().getNearbyEntities(range, range, range))
            {
                if (entity instanceof Item)
                {
                    Item itemEntity = (Item) entity;

                    PlayerPickupItemEvent playerPickupEvent = new PlayerPickupItemEvent(myPet.getOwner().getPlayer(), itemEntity, itemEntity.getItemStack().getAmount());
                    MyPetUtil.getServer().getPluginManager().callEvent(playerPickupEvent);

                    if (playerPickupEvent.isCancelled())
                    {
                        continue;
                    }

                    MyPetCustomInventory inv = ((Inventory) myPet.getSkills().getSkill("Inventory")).inv;
                    int itemAmount = inv.addItem(itemEntity.getItemStack());
                    if (itemAmount == 0)
                    {
                        for (Entity p : itemEntity.getNearbyEntities(20, 20, 20))
                        {
                            if (p instanceof Player)
                            {
                                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new Packet22Collect(entity.getEntityId(), myPet.getCraftPet().getEntityId()));
                            }
                        }
                        myPet.getCraftPet().getHandle().makeSound("random.pop", 0.2F, 1.0F);
                        itemEntity.remove();
                    }
                    else
                    {
                        itemEntity.getItemStack().setAmount(itemAmount);
                    }
                }
            }
        }
    }

    @Override
    public void load(CompoundTag compound)
    {
        pickup = ((ByteTag) compound.getValue().get("Active")).getBooleanValue();
    }

    @Override
    public CompoundTag save()
    {
        CompoundTag nbtTagCompound = new CompoundTag(getName(), new CompoundMap());
        nbtTagCompound.getValue().put("Active", new ByteTag("Active", pickup));
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