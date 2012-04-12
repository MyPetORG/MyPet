/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.entity;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.entity.pathfinder.PathfinderGoalAggressiveTarget;
import de.Keyle.MyWolf.entity.pathfinder.PathfinderGoalControl;
import de.Keyle.MyWolf.entity.pathfinder.PathfinderGoalControlTarget;
import de.Keyle.MyWolf.skill.skills.Control;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMyWolf extends EntityTameableAnimal
{
    private boolean b = false;
    private float c;
    private boolean h;
    private boolean i;
    private float j;
    private float k;
    public EntityLiving Goaltarget = null;

    boolean isMyWolf = false;
    MyWolf MWolf;

    public EntityMyWolf(World world)
    {
        super(world);
        MyWolfUtil.getLogger().severe("Don't try to get a MyWolf this way!");
    }

    public EntityMyWolf(World world, MyWolf MWolf)
    {
        super(world);
        this.texture = "/mob/wolf.png";
        this.b(0.6F, 0.8F);
        this.bb = 0.3F;
        this.al().a(true);
        setMyWolf(MWolf);
        MWolf.Wolf = (CraftMyWolf) this.getBukkitEntity();

        PathfinderGoalControl Control = new PathfinderGoalControl(MWolf, 0.4F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, this.a);
        this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, this.bb, true));
        this.goalSelector.a(5, Control);
        this.goalSelector.a(7, new de.Keyle.MyWolf.entity.pathfinder.PathfinderGoalFollowOwner(this, this.bb, 5.0F, 2.0F, Control));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new de.Keyle.MyWolf.entity.pathfinder.PathfinderGoalOwnerHurtTarget(MWolf));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(MWolf, Control, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(MWolf, 10));
    }

    public boolean isMyWolf()
    {
        return isMyWolf;
    }

    public void setMyWolf(MyWolf MWolf)
    {
        if (MWolf != null)
        {
            this.MWolf = MWolf;
            isMyWolf = true;
            if (!isTamed())
            {
                this.setTamed(true);
                this.setPathEntity(null);
                this.setSitting(MWolf.isSitting());
                this.setHealth(MWolf.getHealth() >= getMaxHealth() ? getMaxHealth() : MWolf.getHealth());
                this.setOwnerName(MWolf.getOwner().getName());
                this.world.broadcastEntityEffect(this, (byte) 7);
            }
        }
    }

    public int getMaxHealth()
    {
        return MyWolfConfig.StartHP + (isTamed() && MWolf.SkillSystem.hasSkill("HP") ? MWolf.SkillSystem.getSkill("HP").getLevel() : 0);
    }

    public boolean b(EntityHuman entityhuman)
    {
        ItemStack itemstack = entityhuman.inventory.getItemInHand();

        if (isMyWolf() && entityhuman.name.equalsIgnoreCase(this.getOwnerName()))
        {
            if (MWolf.SkillSystem.hasSkill("Control") && MWolf.SkillSystem.getSkill("Control").getLevel() > 0)
            {
                if (MWolf.getOwner().getPlayer().getItemInHand().getType() == Control.Item)
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
        int damage = 4 + (isMyWolf && MWolf.SkillSystem.hasSkill("Demage") ? MWolf.SkillSystem.getSkill("Demage").getLevel() : 0);

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

    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyWolf(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    public MyWolf getMyWolf()
    {
        return MWolf;
    }

    //Unused changed Vanilla Methods ---------------------------------------------------------------------------------------

    protected String i()
    {
        return (this.random.nextInt(3) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "mob.wolf.whine" : "mob.wolf.panting") : "mob.wolf.bark");
    }

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
        if (!isMyWolf)
        {
            super.d(nbttagcompound);
            EntityWolf entityWolf = new EntityWolf(world);
            entityWolf.d(nbttagcompound);
            this.getBukkitEntity().remove();
            MyWolfUtil.getLogger().severe("This shouldn't happen, please contact the developer and inform him about this!");
        }
        else
        {
            super.d(nbttagcompound);
        }
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


    protected String j()
    {
        return "mob.wolf.hurt";
    }

    protected String k()
    {
        return "mob.wolf.death";
    }

    protected float p()
    {
        return 0.4F;
    }

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