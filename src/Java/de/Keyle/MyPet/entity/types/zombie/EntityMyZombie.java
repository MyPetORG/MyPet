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

package de.Keyle.MyPet.entity.types.zombie;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_6.World;

@EntitySize(width = 0.9F, height = 0.9F)
public class EntityMyZombie extends EntityMyPet
{
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

            this.setBaby(((MyZombie) myPet).isBaby());
            this.setVillager(((MyZombie) myPet).isVillager());
        }
    }

    public boolean isBaby()
    {
        return getDataWatcher().getByte(12) == 1;
    }

    public void setBaby(boolean flag)
    {
        getDataWatcher().watch(12, (byte) (flag ? 1 : 0));
        ((MyZombie) myPet).isBaby = flag;
    }

    public boolean isVillager()
    {
        return getDataWatcher().getByte(13) == 1;
    }

    public void setVillager(boolean flag)
    {
        getDataWatcher().watch(13, (byte) (flag ? 1 : 0));
        ((MyZombie) myPet).isVillager = flag;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyZombie(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        getDataWatcher().a(12, new Byte((byte) 0)); // is baby
        getDataWatcher().a(13, new Byte((byte) 0)); // is villager
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.zombie.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.zombie.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.zombie.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.zombie.death";
    }
}