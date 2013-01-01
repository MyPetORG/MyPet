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
import de.Keyle.MyPet.skill.skills.beacon.ContainerBeacon;
import de.Keyle.MyPet.skill.skills.beacon.TileEntityBeacon;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.*;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_6.event.CraftEventFactory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class Beacon extends MyPetGenericSkill implements IInventory
{
    public static double rangePerLevel = 5;
    public static int hungerDecreaseTime = 60;

    public List<HumanEntity> transaction = new ArrayList<HumanEntity>();
    private int maxStack = 64;
    private ItemStack tributeItem;
    private TileEntityBeacon tileEntityBeacon;

    private int primaryEffectId = 0;
    private int secondaryEffectId = 0;
    private boolean active = false;
    private int hungerDecreaseTimer;


    public Beacon()
    {
        super("Beacon");
        hungerDecreaseTimer = hungerDecreaseTime;
    }

    @Override
    public void activate()
    {
        if (level > 0)
        {
            openBeacon(myPet.getOwner().getPlayer());
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
        }
    }

    public void activate(Player player)
    {
        if (level > 0)
        {
            openBeacon(player);
        }
        else
        {
            player.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
        }
    }

    @Override
    public void upgrade()
    {
        super.upgrade();
    }

    @Override
    public void schedule()
    {
        if (myPet.status == PetState.Here && this.level > 0 && this.active && this.primaryEffectId > 0)
        {
            byte amplification = 0;

            if (this.level >= 4 && this.primaryEffectId == this.secondaryEffectId)
            {
                amplification = 1;
            }
            double range = (rangePerLevel * level) * myPet.getHungerValue() / 100;
            for (Object entityObj : this.myPet.getCraftPet().getHandle().world.a(EntityHuman.class, myPet.getCraftPet().getHandle().boundingBox.grow(range, range, range)))
            {
                EntityHuman entityHuman = (EntityHuman) entityObj;
                entityHuman.addEffect(new MobEffect(this.primaryEffectId, 180, amplification, true));

                if (this.level >= 4 && this.primaryEffectId != this.secondaryEffectId && this.secondaryEffectId > 0)
                {
                    entityHuman.addEffect(new MobEffect(this.secondaryEffectId, 180, 0, true));
                }
            }

            if (hungerDecreaseTime > 0 && hungerDecreaseTimer-- < 0)
            {
                myPet.setHungerValue(myPet.getHungerValue() - 1);
                hungerDecreaseTimer = hungerDecreaseTime;
            }
        }
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        if (nbtTagCompound.hasKey("Primary"))
        {
            this.primaryEffectId = nbtTagCompound.getInt("Primary");
        }
        if (nbtTagCompound.hasKey("Secondary"))
        {
            this.secondaryEffectId = nbtTagCompound.getInt("Secondary");
        }

        if (nbtTagCompound.hasKey("Active"))
        {
            this.active = nbtTagCompound.getBoolean("Active");
        }
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound(skillName);
        nbtTagCompound.setInt("Primary", this.primaryEffectId);
        nbtTagCompound.setInt("Secondary", this.secondaryEffectId);
        nbtTagCompound.setBoolean("Active", this.active);
        return nbtTagCompound;
    }

    public void reset()
    {
        super.reset();
        stop(true);
    }

    public void openBeacon(Player p)
    {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();

        if (tileEntityBeacon == null)
        {
            tileEntityBeacon = new TileEntityBeacon(this);
        }
        Container container = CraftEventFactory.callInventoryOpenEvent(entityPlayer, new ContainerBeacon(entityPlayer.inventory, this, tileEntityBeacon));
        if (container == null)
        {
            return;
        }

        int containerCounter = entityPlayer.nextContainerCounter();
        entityPlayer.playerConnection.sendPacket(new Packet100OpenWindow(containerCounter, 7, this.getName(), this.getSize()));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.windowId = containerCounter;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }


    public int getPrimaryEffectId()
    {
        return primaryEffectId;
    }

    public void setPrimaryEffectId(int effectId)
    {
        if (effectId > 0)
        {
            this.primaryEffectId = effectId;
            active = true;
            hungerDecreaseTimer = hungerDecreaseTime;
        }
        else
        {
            this.primaryEffectId = 0;
            active = false;
        }
    }

    public int getSecondaryEffectId()
    {
        return secondaryEffectId;
    }

    public void setSecondaryEffectId(int effectId)
    {
        if (effectId > 0)
        {
            this.secondaryEffectId = effectId;
        }
        else
        {
            this.secondaryEffectId = 0;
        }
        hungerDecreaseTimer = hungerDecreaseTime;
    }

    public void stop(boolean reset)
    {
        this.active = false;
        if (reset)
        {
            primaryEffectId = 0;
            secondaryEffectId = 0;
        }
    }

    // Inventory Methods --------------------------------------------------------------------------------------------

    public ItemStack[] getContents()
    {
        return null;
    }

    public void onOpen(CraftHumanEntity who)
    {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers()
    {
        return this.transaction;
    }

    public InventoryHolder getOwner()
    {
        return null;
    }

    public int getSize()
    {
        return 1;
    }

    public ItemStack getItem(int slot)
    {
        return slot == 0 ? this.tributeItem : null;
    }

    public ItemStack splitStack(int slot, int amount)
    {
        if (slot == 0 && this.tributeItem != null)
        {
            if (amount >= this.tributeItem.count)
            {
                ItemStack itemstack = this.tributeItem;

                this.tributeItem = null;
                return itemstack;
            }
            this.tributeItem.count -= amount;
            return new ItemStack(this.tributeItem.id, amount, this.tributeItem.getData());
        }
        return null;
    }

    public ItemStack splitWithoutUpdate(int i)
    {
        if (i == 0 && this.tributeItem != null)
        {
            ItemStack itemstack = this.tributeItem;

            this.tributeItem = null;
            return itemstack;
        }
        return null;
    }

    public void setItem(int i, ItemStack itemStack)
    {
        if (i == 0)
        {
            this.tributeItem = itemStack;
        }
    }

    public int getMaxStackSize()
    {
        return this.maxStack;
    }

    public void setMaxStackSize(int size)
    {
        this.maxStack = size;
    }

    public void update()
    {
    }

    public void startOpen()
    {
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    public boolean a_(EntityHuman entityHuman)
    {
        return true;
    }

    public void f()
    {
    }
}