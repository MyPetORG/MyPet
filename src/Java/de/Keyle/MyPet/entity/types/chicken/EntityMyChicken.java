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

package de.Keyle.MyPet.entity.types.chicken;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_5.Item;
import net.minecraft.server.v1_4_5.World;

public class EntityMyChicken extends EntityMyPet
{
    // Variables for flying of the chicken
    public float b = 0.0F;
    public float c = 0.0F;
    public float g;
    public float h;
    public float i = 1.0F;
    private int nextEggTimer;

    public EntityMyChicken(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/chicken.png";
        nextEggTimer = (random.nextInt(6000) + 6000);
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setBaby(((MyChicken) myPet).isBaby());
        }
    }

    public boolean isBaby()
    {
        return this.datawatcher.getInt(12) < 0;
    }

    @SuppressWarnings("boxing")
    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, new Integer(-24000));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyChicken) myPet).isBaby = flag;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyChicken(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, new Integer(0)); // age
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.chicken.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.chicken.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.chicken.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.chicken.hurt";
    }

    public void c()
    {
        super.c();
        this.h = this.b;
        this.g = this.c;
        this.c = (float) ((double) this.c + (double) (this.onGround ? -1 : 4) * 0.3D);
        if (this.c < 0.0F)
        {
            this.c = 0.0F;
        }

        if (this.c > 1.0F)
        {
            this.c = 1.0F;
        }

        if (!this.onGround && this.i < 1.0F)
        {
            this.i = 1.0F;
        }

        this.i = (float) ((double) this.i * 0.9D);
        if (!this.onGround && this.motY < 0.0D)
        {
            this.motY *= 0.6D;
        }

        this.b += this.i * 2.0F;

        if (!world.isStatic && --nextEggTimer <= 0)
        {
            world.makeSound(this, "mob.chicken.plop", 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            b(Item.EGG.id, 1);
            nextEggTimer = (random.nextInt(6000) + 6000);
        }
    }
}