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

package de.Keyle.MyPet.skill.skills.implementation.beacon;

import de.Keyle.MyPet.skill.skills.implementation.Beacon;
import net.minecraft.server.v1_4_R1.*;
import org.bukkit.craftbukkit.v1_4_R1.inventory.CraftInventoryView;
import org.bukkit.entity.Player;

public class ContainerBeacon extends net.minecraft.server.v1_4_R1.ContainerBeacon
{
    private final SlotBeacon slotBeacon;
    private CraftInventoryView bukkitEntity = null;
    private PlayerInventory playerInventory;
    private TileEntityBeacon tileEntityBeacon;
    MyPetCustomBeaconInventory beaconInv;
    Beacon beaconSkill;

    public ContainerBeacon(PlayerInventory playerInventory, MyPetCustomBeaconInventory beaconInv, TileEntityBeacon tileEntityBeacon, Beacon beaconSkill)
    {
        super(playerInventory, tileEntityBeacon);
        this.c.clear();
        this.b.clear();
        this.beaconInv = beaconInv;
        this.beaconSkill = beaconSkill;
        this.tileEntityBeacon = tileEntityBeacon;
        this.playerInventory = playerInventory;
        a(this.slotBeacon = new SlotBeacon(beaconInv, 0, 136, 110));

        for (int i = 0 ; i < 3 ; i++)
        {
            for (int j = 0 ; j < 9 ; j++)
            {
                a(new Slot(playerInventory, j + i * 9 + 9, 36 + j * 18, 137 + i * 18));
            }
        }

        for (int i = 0 ; i < 9 ; i++)
        {
            a(new Slot(playerInventory, i, 36 + i * 18, 195));
        }
    }

    public void addSlotListener(ICrafting icrafting)
    {
        super.addSlotListener(icrafting);
        icrafting.setContainerData(this, 0, this.beaconSkill.getLevel());
        icrafting.setContainerData(this, 1, this.beaconSkill.getPrimaryEffectId());
        icrafting.setContainerData(this, 2, this.beaconSkill.getSecondaryEffectId());
    }

    public CraftInventoryView getBukkitView()
    {
        if (this.bukkitEntity != null)
        {
            return this.bukkitEntity;
        }

        CraftMyPetInventoryBeacon craftBeaconInventory = new CraftMyPetInventoryBeacon(this.beaconInv);
        Player player = (Player) this.playerInventory.player.getBukkitEntity();

        this.bukkitEntity = new CraftInventoryView(player, craftBeaconInventory, this);
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    public boolean a(EntityHuman entityhuman)
    {
        return true;
    }

    public ItemStack b(EntityHuman entityhuman, int slotNumber)
    {
        ItemStack slotItemClone = null;
        Slot slot = (Slot) this.c.get(slotNumber); // c -> List<Slot>

        if ((slot != null) && (slot.d())) // slot.d() -> Item in Slot != null
        {
            ItemStack slotItem = slot.getItem();

            slotItemClone = slotItem.cloneItemStack();
            if (slotNumber == 0)
            {
                if (!a(slotItem, 1, 37, true))
                {
                    return null;
                }

                slot.a(slotItem, slotItemClone);
            }
            else if ((!this.slotBeacon.d()) && (this.slotBeacon.isAllowed(slotItem)) && (slotItem.count == 1))
            {
                if (!a(slotItem, 0, 1, false))
                {
                    return null;
                }
            }
            else if ((slotNumber >= 1) && (slotNumber < 28))
            {
                if (!a(slotItem, 28, 37, false))
                {
                    return null;
                }
            }
            else if ((slotNumber >= 28) && (slotNumber < 37))
            {
                if (!a(slotItem, 1, 28, false))
                {
                    return null;
                }
            }
            else if (!a(slotItem, 1, 37, false))
            {
                return null;
            }

            if (slotItem.count == 0)
            {
                slot.set(null);
            }
            else
            {
                slot.e();
            }

            if (slotItem.count == slotItemClone.count)
            {
                return null;
            }

            slot.a(entityhuman, slotItem);
        }

        return slotItemClone;
    }

    @Override
    public TileEntityBeacon d()
    {
        return tileEntityBeacon;
    }
}
