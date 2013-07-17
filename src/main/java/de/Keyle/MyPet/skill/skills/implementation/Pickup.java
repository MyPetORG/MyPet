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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.ISkillActive;
import de.Keyle.MyPet.skill.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.inventory.MyPetCustomInventory;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.PickupInfo;
import de.Keyle.MyPet.util.Colorizer;
import de.Keyle.MyPet.util.IScheduler;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import net.minecraft.server.v1_6_R2.Packet22Collect;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.spout.nbt.*;

public class Pickup extends PickupInfo implements ISkillInstance, IScheduler, ISkillStorage, ISkillActive
{
    private boolean pickup = false;
    private MyPet myPet;

    public Pickup(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public boolean isActive()
    {
        return range > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof PickupInfo)
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
                    myPet.sendMessageToOwner(Colorizer.setColors(MyPetLocales.getString("Message.Skill.Pickup.Upgrade", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()).replace("%range%", "" + String.format("%1.2f", range)));
                }
            }
        }
    }

    public String getFormattedValue()
    {
        return MyPetLocales.getString("Name.Range", myPet.getOwner().getLanguage()) + ": " + String.format("%1.2f", range) + " " + MyPetLocales.getString("Name.Blocks", myPet.getOwner().getPlayer());
    }

    public void reset()
    {
        range = 0;
        pickup = false;
    }

    public boolean activate()
    {
        if (range > 0)
        {
            if (myPet.getSkills().isSkillActive("Inventory"))
            {
                pickup = !pickup;
                myPet.sendMessageToOwner(Colorizer.setColors(MyPetLocales.getString((pickup ? "Message.Skill.Pickup.Start" : "Message.Skill.Pickup.Stop"), myPet.getOwner().getPlayer())).replace("%petname%", myPet.getPetName()));
                return true;
            }
            else
            {
                myPet.sendMessageToOwner(Colorizer.setColors(MyPetLocales.getString("Message.Skill.Pickup.NoInventory", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
                return false;
            }
        }
        else
        {
            myPet.sendMessageToOwner(Colorizer.setColors(MyPetLocales.getString("Message.NoSkill", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()).replace("%skill%", this.getName()));
            return false;
        }
    }

    public void schedule()
    {
        if (pickup && (!MyPetPermissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Pickup") || myPet.getOwner().isInExternalGames()))
        {
            pickup = false;
            myPet.sendMessageToOwner(Colorizer.setColors(MyPetLocales.getString("Message.Skill.Pickup.Stop", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()));
            return;
        }
        if (range > 0 && pickup && myPet.getStatus() == PetState.Here && myPet.getSkills().isSkillActive("Inventory"))
        {
            for (Entity entity : myPet.getCraftPet().getNearbyEntities(range, range, range))
            {
                if (entity instanceof Item)
                {
                    Item itemEntity = (Item) entity;

                    PlayerPickupItemEvent playerPickupEvent = new PlayerPickupItemEvent(myPet.getOwner().getPlayer(), itemEntity, itemEntity.getItemStack().getAmount());
                    Bukkit.getServer().getPluginManager().callEvent(playerPickupEvent);

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

    public void load(CompoundTag compound)
    {
        pickup = ((ByteTag) compound.getValue().get("Active")).getBooleanValue();
    }

    public CompoundTag save()
    {
        CompoundTag nbtTagCompound = new CompoundTag(getName(), new CompoundMap());
        nbtTagCompound.getValue().put("Active", new ByteTag("Active", pickup));
        return nbtTagCompound;

    }

    @Override
    public ISkillInstance cloneSkill()
    {
        Pickup newSkill = new Pickup(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}