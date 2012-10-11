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

package de.Keyle.MyPet.entity.types.villager;

import de.Keyle.MyPet.entity.pathfinder.*;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class EntityMyVillager extends EntityMyPet
{
    public EntityMyVillager(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/villager/villager.png";
        this.a(0.6F, 0.8F);
        this.getNavigation().a(true);

        PathfinderGoalControl controlPathfinderGoal = new PathfinderGoalControl(myPet, 0.4F);

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, this.d);
        this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, 0.6F));
        this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, 0.5F + 0.3F, true));
        this.goalSelector.a(5, controlPathfinderGoal);
        this.goalSelector.a(7, new PathfinderGoalFollowOwner(this, 0.5F, 10.0F, 5.0F, controlPathfinderGoal));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.a(2, new PathfinderGoalOwnerHurtTarget(myPet));
        this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, true));
        this.targetSelector.a(4, new PathfinderGoalControlTarget(myPet, controlPathfinderGoal, 1));
        this.targetSelector.a(5, new PathfinderGoalAggressiveTarget(myPet, 13));

    }

    @Override
    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.myPet = myPet;
            isMyPet = true;
            if (!isTamed())
            {
                this.setTamed(true);
                this.setPathEntity(null);
                this.setSitting(myPet.isSitting());
                this.setHealth(myPet.getHealth() >= getMaxHealth() ? getMaxHealth() : myPet.getHealth());
                this.setOwnerName(myPet.getOwner().getName());
                this.world.broadcastEntityEffect(this, (byte) 7);
                this.e(true);
                this.d.a(true);
                this.setProfession(((MyVillager) myPet).getProfession());
            }
        }
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyVillager(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    //Changed Vanilla Methods ---------------------------------------------------------------------------------------

    public int getMaxHealth()
    {
        return MyVillager.getStartHP() + (isTamed() && myPet.getSkillSystem().hasSkill("HP") ? myPet.getSkillSystem().getSkill("HP").getLevel() : 0);
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

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (itemStack != null && itemStack.id == org.bukkit.Material.APPLE.getId())
        {
            ItemFood itemfood = (ItemFood) Item.byId[itemStack.id];

            if (getHealth() < getMaxHealth())
            {
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                this.heal(itemfood.getNutrition(), RegainReason.EATING);
                if (itemStack.count <= 0)
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

    public void bd()
    {
    }

    // Vanilla Methods

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, (byte) 0);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aQ()
    {
        return "mob.villager.default";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String aR()
    {
        return "mob.villager.defaulthurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String aS()
    {
        return "mob.villager.defaultdeath";
    }

    public int getProfession()
    {
        return this.datawatcher.getByte(16);
    }

    public void setProfession(int i)
    {
        this.datawatcher.watch(16, (byte) i);
    }

    public String getLocalizedName()
    {
        return this.isTamed() ? "entity.Cat.name" : super.getLocalizedName();
    }
}