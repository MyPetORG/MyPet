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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMyOcelot extends EntityMyPet
{
    public EntityMyOcelot(World world, MyPet MPet)
    {
        super(world, MPet);
        this.texture = "/mob/ozelot.png";
        this.a(0.6F, 0.8F);
        this.bw = 0.3F;
        this.getNavigation().a(true);

        PathfinderGoalControl Control = new PathfinderGoalControl(MPet, 0.4F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, this.d);
        this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, 0.6F));
        this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, this.bw + 0.3F, true));
        this.goalSelector.a(5, Control);
        this.goalSelector.a(7, new PathfinderGoalFollowOwner(this, this.bw, 10.0F, 5.0F, Control));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(MPet));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(MPet, Control, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(MPet, 13));
    }

    @Override
    public void setMyPet(MyPet MPet)
    {
        if (MPet != null)
        {
            this.myPet = MPet;
            isMyPet = true;

            this.setPathEntity(null);
            this.setSitting(MPet.isSitting());
            this.setHealth(MPet.getHealth() >= getMaxHealth() ? getMaxHealth() : MPet.getHealth());
            this.setOwnerName(MPet.getOwner().getName());
            this.world.broadcastEntityEffect(this, (byte) 7);
            this.e(true);
            this.d.a(true);
            this.setCatType(((MyOcelot) MPet).getCatType());

        }
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyOcelot(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    //Changed Vanilla Methods ---------------------------------------------------------------------------------------

    public int getMaxHealth()
    {
        return MyOcelot.getStartHP() + (isMyPet() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean c(EntityHuman entityhuman)
    {
        super.c(entityhuman);

        ItemStack itemstack = entityhuman.inventory.getItemInHand();

        if (itemstack != null && itemstack.id == org.bukkit.Material.RAW_FISH.getId())
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
                this.e(true);
                return true;
            }
        }
        else if (entityhuman.name.equalsIgnoreCase(this.getOwnerName()) && !this.world.isStatic)
        {
            this.d.a(!this.isSitting());
            this.bu = false;
            this.setPathEntity(null);
        }
        return false;
    }

    public boolean k(Entity entity)
    {
        int damage = 3 + (isMyPet && myPet.getSkillSystem().hasSkill("Damage") ? myPet.getSkillSystem().getSkill("Damage").getLevel() : 0);

        return entity.damageEntity(DamageSource.mobAttack(this), damage);
    }

    // Vanilla Methods

    protected void a()
    {
        super.a();
        this.datawatcher.a(18, (byte) 0);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aQ()
    {
        return this.s() ? "mob.cat.purr" : (this.random.nextInt(4) == 0 ? "mob.cat.purreow" : "mob.cat.meow");
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String aR()
    {
        return "mob.cat.hitt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String aS()
    {
        return "mob.cat.hitt";
    }

    public int getCatType()
    {
        return this.datawatcher.getByte(18);
    }

    public void setCatType(int i)
    {
        this.datawatcher.watch(18, (byte) i);
    }

    public String getLocalizedName()
    {
        return this.isTamed() ? "entity.Cat.name" : super.getLocalizedName();
    }
}