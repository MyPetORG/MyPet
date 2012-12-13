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

package de.Keyle.MyPet.entity.types.skeleton;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.Item;
import net.minecraft.server.ItemStack;
import net.minecraft.server.World;

public class EntityMySkeleton extends EntityMyPet
{
    public EntityMySkeleton(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/skeleton.png";
        this.setPathfinder();
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setEquipment(0, new ItemStack(Item.IRON_SWORD));
        }
    }

    public void setEquipment(int slot, ItemStack item)
    {
        super.setEquipment(slot, item);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMySkeleton(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(13, new Byte((byte) 0)); // age
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.skeleton.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return "mob.skeleton.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String aZ()
    {
        return "mob.skeleton.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String ba()
    {
        return "mob.skeleton.death";
    }
}