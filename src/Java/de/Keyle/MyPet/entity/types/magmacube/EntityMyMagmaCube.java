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

package de.Keyle.MyPet.entity.types.magmacube;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_6.PathEntity;
import net.minecraft.server.v1_4_6.World;

@EntitySize(width = 0.6F, height = 0.6F)
public class EntityMyMagmaCube extends EntityMyPet
{
    int jumpDelay;
    PathEntity lastPathEntity = null;

    public EntityMyMagmaCube(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/lava.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            setSize(((MyMagmaCube) myPet).getSize());
        }
    }

    public int getSize()
    {
        int size = this.datawatcher.getByte(16);
        return size <= 0 ? 1 : size;
    }

    public void setSize(int value)
    {
        this.datawatcher.watch(16, new Byte((byte) value));
        EntitySize es = EntityMyMagmaCube.class.getAnnotation(EntitySize.class);
        if (es != null)
        {
            this.a(es.height() * value, es.width() * value);
        }
        ((MyMagmaCube) myPet).size = value;
    }

    public boolean isBurning()
    {
        return false;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyMagmaCube(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 1)); //size
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return ba();
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.magmacube." + (getSize() > 1 ? "big" : "small");
    }

    /**
     * Method is called when pet moves
     * Is used to create the hopping motion
     */
    public void j_()
    {
        super.j_();

        if (this.onGround && jumpDelay-- <= 0 && lastPathEntity != getNavigation().d())
        {
            getControllerJump().a();
            jumpDelay = (this.random.nextInt(20) + 10);
            lastPathEntity = getNavigation().d();
            makeSound("mob.magmacube." + (getSize() > 1 ? "big" : "small"), aX(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
        }
    }
}