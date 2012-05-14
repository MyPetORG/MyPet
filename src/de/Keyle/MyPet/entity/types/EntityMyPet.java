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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.util.MyPetConfig;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public abstract class EntityMyPet extends EntityTameableAnimal
{
    protected boolean b = false;
    protected float c;
    protected boolean h;
    protected boolean i;
    protected float j;
    protected float k;
    public EntityLiving Goaltarget = null;

    protected boolean isMyPet = false;
    protected MyPet MPet;

    public EntityMyPet(World world)
    {
        super(world);
        MyPetUtil.getLogger().severe("Don't try to get a MyPet this way!");
    }

    public EntityMyPet(World world, MyPet MPet)
    {
        super(world);
        setMyPet(MPet);
        MPet.Pet = (CraftMyPet) this.getBukkitEntity();
    }

    public boolean isMyPet()
    {
        return isMyPet;
    }

    public abstract void setMyPet(MyPet MPet);

    public int getMaxHealth()
    {
        return MyPetConfig.StartHP + (isTamed() && MPet.getSkillSystem().hasSkill("HP") ? MPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    public boolean b(EntityHuman entityhuman)
    {
        ItemStack itemstack = entityhuman.inventory.getItemInHand();

        if (isMyPet() && entityhuman.name.equalsIgnoreCase(this.getOwnerName()))
        {
            if (MPet.getSkillSystem().hasSkill("Control") && MPet.getSkillSystem().getSkill("Control").getLevel() > 0)
            {
                if (MPet.getOwner().getPlayer().getItemInHand().getType() == Control.Item)
                {
                    return true;
                }
            }
        }

        if (this.a(itemstack))
        {
            ItemFood itemfood = (ItemFood) Item.byId[itemstack.id];

            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemstack.count;
                }
                this.heal(itemfood.getNutrition(), RegainReason.EATING);
                if (itemstack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                this.a(true);
                return true;
            }
        }
        else if (entityhuman.name.equalsIgnoreCase(this.getOwnerName()) && !this.world.isStatic)
        {
            this.a.a(!this.isSitting());
            this.aZ = false;
            this.setPathEntity(null);
        }

        return false;
    }

    public boolean a(Entity entity)
    {
        int damage = 4 + (isMyPet && MPet.getSkillSystem().hasSkill("Damage") ? MPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }

    public void setLocation(Location loc)
    {
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
    }

    public void setHealth(int i)
    {
        if (i > this.getMaxHealth())
        {
            i = this.getMaxHealth();
        }
        this.health = i;
    }

    public void setSitting(boolean flag)
    {
        this.a.a(flag);
        super.setSitting(flag);
    }

    public abstract org.bukkit.entity.Entity getBukkitEntity();

    public MyPet getMyPet()
    {
        return MPet;
    }

    //Unused changed Vanilla Methods ---------------------------------------------------------------------------------------

    protected abstract String i();

    public EntityAnimal createChild(EntityAnimal entityanimal)
    {
        return null;
    }

    public boolean mate(EntityAnimal entityanimal)
    {
        return false;
    }

    public void a(NBTTagCompound nbttagcompound)
    {
    }

    //Vanilla Methods ------------------------------------------------------------------------------------------------------

    public boolean c_()
    {
        return true;
    }

    protected void g()
    {
        this.datawatcher.watch(18, this.getHealth());
    }

    protected void b()
    {
        super.b();
        this.datawatcher.a(18, this.getHealth());
    }

    protected boolean g_()
    {
        return false;
    }

    protected boolean n()
    {
        return false;
    }


    protected abstract String j();

    protected abstract String k();

    protected abstract float p();

    protected int getLootId()
    {
        return -1;
    }

    public void e()
    {
        super.e();
        if (!this.world.isStatic && this.h && !this.i && !this.H() && this.onGround)
        {
            this.i = true;
            this.j = 0.0F;
            this.k = 0.0F;
            this.world.broadcastEntityEffect(this, (byte) 8);
        }
    }

    public void F_()
    {
        super.F_();
        if (this.b)
        {
            this.c += (1.0F - this.c) * 0.4F;
        }
        else
        {
            this.c += (0.0F - this.c) * 0.4F;
        }

        if (this.b)
        {
            this.bc = 10;
        }

        if (this.aT())
        {
            this.h = true;
            this.i = false;
            this.j = 0.0F;
            this.k = 0.0F;
        }
        else if ((this.h || this.i) && this.i)
        {
            if (this.j == 0.0F)
            {
                this.world.makeSound(this, "mob.wolf.shake", this.p(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            this.k = this.j;
            this.j += 0.05F;
            if (this.k >= 2.0F)
            {
                this.h = false;
                this.i = false;
                this.k = 0.0F;
                this.j = 0.0F;
            }

            if (this.j > 0.4F)
            {
                float f = (float) this.boundingBox.b;
                int i = (int) (MathHelper.sin((this.j - 0.4F) * 3.1415927F) * 7.0F);

                for (int j = 0 ; j < i ; ++j)
                {
                    float f1 = (this.random.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;
                    float f2 = (this.random.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;

                    this.world.a("splash", this.locX + (double) f1, (double) (f + 0.8F), this.locZ + (double) f2, this.motX, this.motY, this.motZ);
                }
            }
        }
    }

    public float getHeadHeight()
    {
        return this.length * 0.8F;
    }

    public int D()
    {
        return this.isSitting() ? 20 : super.D();
    }

    public boolean damageEntity(DamageSource damagesource, int i)
    {
        Entity entity = damagesource.getEntity();

        this.a.a(false);
        if (entity != null && !(entity instanceof EntityHuman) && !(entity instanceof EntityArrow))
        {
            i = (i + 1) / 2;
        }

        return super.damageEntity(damagesource, i);
    }

    public boolean a(ItemStack itemstack)
    {
        return itemstack != null && (Item.byId[itemstack.id] instanceof ItemFood && ((ItemFood) Item.byId[itemstack.id]).q());
    }

    public int q()
    {
        return 8;
    }

    public void e(boolean flag)
    {
        this.b = flag;
    }
}