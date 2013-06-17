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

package de.Keyle.MyPet.entity.types.zombie;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.support.BattleArena;
import de.Keyle.MyPet.util.support.Minigames;
import de.Keyle.MyPet.util.support.MobArena;
import de.Keyle.MyPet.util.support.PvPArena;
import net.minecraft.server.v1_5_R3.*;

@EntitySize(width = 0.6F, height = 0.9F)
public class EntityMyZombie extends EntityMyPet
{
    public static org.bukkit.Material GROW_UP_ITEM = org.bukkit.Material.POTION;

    public EntityMyZombie(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/zombie.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);
            final MyZombie myZombie = (MyZombie) myPet;
            final EntityMyZombie entityMyZombie = this;

            this.setBaby(myZombie.isBaby());
            this.setVillager(myZombie.isVillager());

            MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable()
            {
                public void run()
                {
                    if (myZombie.status == PetState.Here)
                    {
                        for (EquipmentSlot slot : EquipmentSlot.values())
                        {
                            if (myZombie.getEquipment(slot) != null)
                            {
                                entityMyZombie.setPetEquipment(slot.getSlotId(), myZombie.getEquipment(slot));
                            }
                        }
                    }
                }
            }, 5L);
        }
    }

    public boolean isBaby()
    {
        return ((MyZombie) myPet).isBaby;
    }

    public void setBaby(boolean flag)
    {
        getDataWatcher().watch(12, (byte) (flag ? 1 : 0));
        ((MyZombie) myPet).isBaby = flag;
    }

    public boolean isVillager()
    {
        return ((MyZombie) myPet).isVillager;
    }

    public void setVillager(boolean flag)
    {
        getDataWatcher().watch(13, (byte) (flag ? 1 : 0));
        ((MyZombie) myPet).isVillager = flag;
    }

    public void setPetEquipment(int slot, ItemStack itemStack)
    {
        ((WorldServer) this.world).getTracker().a(this, new Packet5EntityEquipment(this.id, slot, itemStack));
        ((MyZombie) myPet).equipment.put(EquipmentSlot.getSlotById(slot), itemStack);
    }

    public ItemStack getPetEquipment(int slot)
    {
        return ((MyZombie) myPet).getEquipment(EquipmentSlot.getSlotById(slot));
    }

    public ItemStack[] getPetEquipment()
    {
        return ((MyZombie) myPet).getEquipment();
    }

    public boolean checkForEquipment(ItemStack itemstack)
    {
        int slot = b(itemstack);
        if (slot == 0)
        {
            if (itemstack.getItem() instanceof ItemSword)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemAxe)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemSpade)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemHoe)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemPickaxe)
            {
                return true;
            }
            return false;
        }
        else
        {
            return true;
        }
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        getDataWatcher().a(12, new Byte((byte) 0)); // is baby
        getDataWatcher().a(13, new Byte((byte) 0)); // is villager
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a_(EntityHuman entityhuman)
    {
        try
        {
            if (super.a_(entityhuman))
            {
                return true;
            }

            ItemStack itemStack = entityhuman.inventory.getItemInHand();

            if (getOwner().equals(entityhuman) && itemStack != null)
            {
                if (itemStack.id == Item.SHEARS.id)
                {
                    if (!MyPetPermissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Equip") || MobArena.isInMobArena(myPet.getOwner()) || Minigames.isInMinigame(myPet.getOwner()) || BattleArena.isInBattleArena(myPet.getOwner()) || PvPArena.isInPvPArena(myPet.getOwner()))
                    {
                        return false;
                    }
                    for (EquipmentSlot slot : EquipmentSlot.values())
                    {
                        ItemStack itemInSlot = ((MyZombie) myPet).getEquipment(slot);
                        if (itemInSlot != null)
                        {
                            EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                            entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                            entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                            entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                            setPetEquipment(slot.getSlotId(), null);
                        }
                    }
                    return true;
                }
                else if (checkForEquipment(itemStack) && getOwner().getPlayer().isSneaking())
                {
                    if (!MyPetPermissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Equip") || MobArena.isInMobArena(myPet.getOwner()) || Minigames.isInMinigame(myPet.getOwner()) || BattleArena.isInBattleArena(myPet.getOwner()) || PvPArena.isInPvPArena(myPet.getOwner()))
                    {
                        return false;
                    }
                    EquipmentSlot slot = EquipmentSlot.getSlotById(b(itemStack));
                    ItemStack itemInSlot = ((MyZombie) myPet).getEquipment(slot);
                    if (itemInSlot != null && !entityhuman.abilities.canInstantlyBuild)
                    {
                        EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    ItemStack itemStackClone = itemStack.cloneItemStack();
                    itemStackClone.count = 1;
                    setPetEquipment(b(itemStack), itemStackClone);
                    if (!entityhuman.abilities.canInstantlyBuild)
                    {
                        --itemStack.count;
                    }
                    if (itemStack.count <= 0)
                    {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                    return true;
                }
                else if (itemStack.id == GROW_UP_ITEM.getId())
                {
                    if (isBaby())
                    {
                        if (!entityhuman.abilities.canInstantlyBuild)
                        {
                            if (--itemStack.count <= 0)
                            {
                                entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                            }
                        }
                        this.setBaby(false);
                        return true;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.zombie.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String bb()
    {
        return !playIdleSound() ? "" : "mob.zombie.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String bc()
    {
        return "mob.zombie.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String bd()
    {
        return "mob.zombie.death";
    }
}